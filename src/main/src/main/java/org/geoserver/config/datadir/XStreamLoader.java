/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.impl.ResolvingProxy;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityManager;
import org.geotools.util.logging.Logging;
import org.springframework.util.StringUtils;

/**
 * Thread-safe utility class for deserializing GeoServer configuration objects from XML files.
 *
 * <p>This class provides optimized XML deserialization for the parallel loading process by:
 *
 * <ul>
 *   <li>Using thread-local {@link XStreamPersister} instances to avoid contention in {@link XStream}
 *   <li>Separating file I/O from XML parsing for better parallelism
 *   <li>Disabling password decryption during parsing to avoid threading issues, as
 *       {@link GeoServerExtensions#bean(Class)} would be called, forcing Spring to resolve beans outside its original
 *       plan and causing deadlocks
 *   <li>Providing a specialized {@link ForkJoinPool.ManagedBlocker} for file loading
 * </ul>
 *
 * <p>The {@link #depersist(Path, Catalog)} method uses a per-thread {@link XStreamPersister} via {@link ThreadLocal},
 * since it's always called from within a {@link ForkJoinPool} thread, avoiding the thread contention that would
 * otherwise occur in {@link XStream}.
 *
 * <p>{@link XStreamPersister#setEncryptPasswordFields(false)} is set on all XStream persisters, and password decryption
 * for {@link StoreInfo} objects is deferred to be performed on the main thread after all stores are loaded into the
 * Catalog. This avoids deadlocks that would otherwise occur when {@link GeoServerSecurityManager} is obtained through
 * {@link GeoServerExtensions} in worker threads.
 */
class XStreamLoader {

    private static final Logger LOGGER =
            Logging.getLogger(XStreamLoader.class.getPackage().getName());

    private XStreamPersisterFactory xpf;

    /**
     * Holds per-thread {@link XStreamPersister}s. No need to call {@link ThreadLocal#remove()} because the calling
     * thread dies with the managed {@link ForkJoinPool} used to call {@link #depersist}.
     */
    static final ThreadLocal<XStreamPersister> XP = new ThreadLocal<>();

    /** @param xpfac {@link XStreamPersister} factory providing a new instance for each Thread */
    public XStreamLoader(XStreamPersisterFactory xpfac) {
        this.xpf = xpfac;
    }

    /**
     * Returns the XStreamPersisterFactory used by this loader.
     *
     * <p>This factory is used to create XStreamPersister instances for XML serialization and deserialization of
     * GeoServer configuration objects. Each thread gets its own persister instance to avoid contention when multiple
     * threads are processing XML files concurrently.
     *
     * <p>The factory can be used by clients that need to create additional persisters with the same configuration as
     * those used by this loader, ensuring consistent XML handling across the application.
     *
     * @return the XStreamPersisterFactory used by this loader
     */
    XStreamPersisterFactory getPersisterFactory() {
        return xpf;
    }

    /**
     * Deserializes a GeoServer configuration or catalog object from an XML file.
     *
     * <p>This method performs both the file I/O and XML deserialization steps while ensuring thread safety and proper
     * error handling. The implementation separates the I/O operation from the parsing operation to optimize
     * performance:
     *
     * <ol>
     *   <li>The method opens an input stream to the file and passes it directly to the parse method
     *   <li>The parse method uses a thread-local {@link XStreamPersister} to deserialize the XML
     *   <li>The {@link XStreamPersister} has no {@code Catalog} set, hence it won't resolve {@link ResolvingProxy
     *       proxies}. That's to be done by the caller in a thread-safe way.
     * </ol>
     *
     * <p>Any errors during loading or parsing are properly logged, and an empty Optional is returned in case of
     * failure.
     *
     * <p>This method is safe to call from multiple threads concurrently, as it uses thread-local resources and has no
     * shared mutable state.
     *
     * @param <C> the type of the configuration or catalog object to deserialize
     * @param file the path to the XML file to deserialize
     * @return an Optional containing the deserialized object, or empty if loading or parsing failed
     */
    public <C extends Info> Optional<C> depersist(Path file) {
        try (InputStream contents = Files.newInputStream(file, StandardOpenOption.READ)) {
            return Optional.of(parse(contents));
        } catch (IOException | RuntimeException e) {
            logParseError(file, e);
        }
        return Optional.empty();
    }

    public void persist(CatalogInfo info, Path path) throws IOException {
        XStreamPersister persister = getXStream();
        try (OutputStream out = Files.newOutputStream(path)) {
            persister.save(info, out);
        }
    }

    /**
     * Parses an XML input stream into a GeoServer configuration or catalog object.
     *
     * <p>This method uses a thread-local {@link XStreamPersister} to deserialize the XML content into a GeoServer
     * {@link Info} object. The thread-local approach prevents contention in the underlying XStream instance when
     * multiple threads are parsing XML files concurrently.
     *
     * <p>The method obtains a configured XStreamPersister via {@link #getXStream()} which ensures that:
     *
     * <ul>
     *   <li>Each thread has its own XStreamPersister instance
     *   <li>Password decryption is disabled to avoid threading issues (see class javadocs)
     *   <li>The persister is configured with no catalog, {@link ResolvingProxy} resolution is deferred to a later stage
     * </ul>
     *
     * @param <C> the type of the configuration or catalog object to deserialize
     * @param contents the XML input stream to parse
     * @return the deserialized object, or null if parsing failed
     * @throws UncheckedIOException if an error occurs during parsing
     */
    @SuppressWarnings("unchecked")
    private <C extends Info> C parse(InputStream contents) {
        try {
            XStreamPersister xp = getXStream();
            return (C) xp.load(contents, Info.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /** Logs errors that occur during XML parsing in a consistent format. */
    private void logParseError(Path file, Exception e) {
        String err = e.getMessage();
        if (!StringUtils.hasText(err)) err = e.getCause().getMessage();
        LOGGER.log(Level.SEVERE, "Error parsing " + file, err);
    }

    /**
     * Returns an {@link XStreamPersister} for the current Thread
     *
     * <p>Disables password decrypt at this stage, though we're parsing StoreInfo's on the main (calling) thread and
     * hence GeoServerExtensions won't lead to deadlocks in spring initialization, failure to decode a password would
     * make the whole catalog loading fail. Instead, DataDirectoryGeoServerLoader will decode them after the fact in a
     * safe way and disable the failing stores
     */
    private XStreamPersister getXStream() {
        XStreamPersister xp = XP.get();
        if (xp == null) {
            xp = xpf.createXMLPersister();
            XP.set(xp);
        }
        xp.setUnwrapNulls(false);
        xp.setEncryptPasswordFields(false);
        return xp;
    }
}
