/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
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
     * Deserializes a GeoServer configuration or catalog object from an XML file.
     *
     * <p>This method performs both the file I/O and XML deserialization steps while ensuring thread safety and proper
     * error handling. The implementation separates the I/O operation from the parsing operation to optimize
     * performance:
     *
     * <ol>
     *   <li>First, the file contents are loaded using a {@link ForkJoinPool.ManagedBlocker}
     *   <li>Then, the XML content is parsed using a thread-local {@link XStreamPersister}
     *   <li>The {@link XStreamPersister} has no {@code Catalog} set, hence it won't resolve {@link ResolvingProxy
     *       proxies}. That's to be done by the caller in a thread-safe way.
     * </ol>
     *
     * <p>Any errors during loading or parsing are properly logged, and an empty Optional is returned in case of
     * failure.
     *
     * @param <C> the type of the configuration or catalog object to deserialize
     * @param file the path to the XML file to deserialize
     * @return an Optional containing the deserialized object, or empty if loading or parsing failed
     */
    public <C extends Info> Optional<C> depersist(Path file) {

        try {
            // potentially offload the I/O to another thread to avoid blocking
            byte[] contents = load(file);
            if (contents != null) {
                return Optional.of(parse(contents));
            }
        } catch (RuntimeException e) {
            logParseError(file, e);
        } catch (InterruptedException e) {
            LOGGER.log(Level.SEVERE, "Therad interruped while parsing " + file + ". Aborting.", e);
            Thread.currentThread().interrupt();
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
     * <p>The method obtains a configured XStreamPersister via {@link #getXStream(Catalog)} which ensures that:
     *
     * <ul>
     *   <li>Each thread has its own XStreamPersister instance
     *   <li>Password decryption is disabled to avoid threading issues (see class javadocs)
     *   <li>The persister is configured with the correct catalog for reference resolution
     * </ul>
     *
     * @param <C> the type of the configuration or catalog object to deserialize
     * @param contents the XML input stream to parse
     * @return the deserialized object, or null if parsing failed
     * @throws UncheckedIOException if an error occurs during parsing
     */
    @SuppressWarnings("unchecked")
    private <C extends Info> C parse(byte[] contents) {
        try {
            XStreamPersister xp = getXStream();
            return (C) xp.load(new ByteArrayInputStream(contents), Info.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Loads the contents of a file into memory using a {@link ForkJoinPool.ManagedBlocker}.
     *
     * <p>This method offloads the I/O operation to the ForkJoinPool in a way that allows the pool to compensate for
     * blocking by creating additional worker threads if needed. It uses the {@link FileLoader} class to read the file
     * contents, which implements the {@link ForkJoinPool.ManagedBlocker} interface.
     *
     * <p>In case of an {@link InterruptedException}, the method restores the interrupt flag and returns an empty
     * Optional. All other errors are handled within the {@link FileLoader}.
     *
     * @param file the path to the file to load
     * @return an Optional containing the file contents as a byte array, or empty if loading failed
     * @throws InterruptedException
     */
    private byte[] load(Path file) throws InterruptedException {
        FileLoader offloadingRead = new FileLoader(file);
        ForkJoinPool.managedBlock(offloadingRead);
        return offloadingRead.contents();
    }

    /** Logs errors that occur during XML parsing in a consistent format. */
    private void logParseError(Path file, Exception e) {
        String err = e.getMessage();
        if (!StringUtils.hasText(err)) err = e.getCause().getMessage();
        LOGGER.log(Level.SEVERE, "Error parsing " + file, err);
    }

    /**
     * A specialized {@link ForkJoinPool.ManagedBlocker} implementation for loading files.
     *
     * <p>This class implements the ManagedBlocker interface, which allows a ForkJoinPool to compensate for blocking
     * operations by creating additional worker threads if needed. It provides efficient, interruptible file loading for
     * the parallel processing pipeline.
     *
     * <p>The file contents are loaded into memory in the {@link #block()} method and made available through the
     * {@link #contents()} method.
     */
    static class FileLoader implements ForkJoinPool.ManagedBlocker {
        private final Path file;
        private byte[] contents;
        private boolean done;

        FileLoader(Path f) {
            this.file = f;
        }

        byte[] contents() {
            return contents;
        }

        @Override
        public boolean isReleasable() {
            return done;
        }

        @Override
        public boolean block() throws InterruptedException {
            if (Thread.currentThread().isInterrupted()) {
                throw new InterruptedException();
            }
            if (!done) {
                try {
                    contents = Files.readAllBytes(file);
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error loading file " + file, e);
                } finally {
                    done = true;
                }
            }
            return done;
        }
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
