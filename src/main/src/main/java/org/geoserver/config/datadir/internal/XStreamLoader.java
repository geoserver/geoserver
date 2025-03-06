/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.datadir.internal;

import com.thoughtworks.xstream.XStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.StoreInfo;
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

    XStreamLoader(XStreamPersisterFactory xpfac) {
        this.xpf = xpfac;
    }

    /**
     * Holds per-thread {@link XStreamPersister}s. No need to call {@link ThreadLocal#remove()} because the calling
     * thread dies with the managed {@link ForkJoinPool} used to call {@link #depersist}.
     */
    static final ThreadLocal<XStreamPersister> XP = new ThreadLocal<>();

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
     * </ol>
     *
     * <p>Any errors during loading or parsing are properly logged, and an empty Optional is returned in case of
     * failure.
     *
     * @param <C> the type of the configuration or catalog object to deserialize
     * @param file the path to the XML file to deserialize
     * @param catalog the GeoServer catalog instance (used for resolving references)
     * @return an Optional containing the deserialized object, or empty if loading or parsing failed
     */
    public <C extends Info> Optional<C> depersist(Path file, Catalog catalog) {

        // potentially offload pure I/O task to a separate thread
        Optional<byte[]> contents = load(file);

        // proceed with pure parsing task in the worker thread
        return contents.map(ByteArrayInputStream::new).map(in -> {
            try {
                return parse(in, catalog);
            } catch (IOException | RuntimeException e) {
                logParseError(file, e);
                return null;
            }
        });
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
     */
    private Optional<byte[]> load(Path file) {
        FileLoader block = new FileLoader(file);
        try {
            ForkJoinPool.managedBlock(block);
        } catch (InterruptedException e) {
            LOGGER.log(Level.WARNING, "Interrupted!", e);
            Thread.currentThread().interrupt();
        }
        return block.contents();
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
     * @param catalog the GeoServer catalog instance (used for resolving references)
     * @return the deserialized object, or null if parsing failed
     * @throws IOException if an error occurs during parsing
     */
    @SuppressWarnings("unchecked")
    private <C extends Info> C parse(InputStream contents, Catalog catalog) throws IOException {
        XStreamPersister xp = getXStream(catalog);
        return (C) xp.load(contents, Info.class);
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
     * {@link #contents()} method, which returns the file contents wrapped in an Optional.
     */
    static class FileLoader implements ForkJoinPool.ManagedBlocker {
        private final Path file;
        private byte[] contents;
        private boolean done;

        FileLoader(Path f) {
            this.file = f;
        }

        Optional<byte[]> contents() {
            return Optional.ofNullable(contents);
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
                    LOGGER.log(Level.WARNING, "Error loading file " + file, e);
                } finally {
                    done = true;
                }
            }
            return done;
        }
    }

    /**
     * Returns an {@link XStreamPersister} for the current Thread to avoid race conditions, and with
     * {@link XStreamPersister#setEncryptPasswordFields encrypt passwords} set to false to avoid Spring dead-locking on
     * the main thread.
     *
     * <p>The use of {@link GeoServerExtensions} in {@link XStreamPersister#getSecurityManager()} and in
     * {@link GeoServerSecurityManager} itself causes Spring to force loading beans unresolved beans and ultimately to
     * deadlock when called from a different thread than the {@code main} thread.
     */
    XStreamPersister getXStream(Catalog catalog) {
        XStreamPersister xp = XP.get();
        if (xp == null) {
            xp = xpf.createXMLPersister();
            XP.set(xp);
        }
        xp.setCatalog(catalog);
        xp.setUnwrapNulls(false);
        // disable password decrypt at this stage, or xp will use GeoServerExtensions to
        // lookup the GeoServerSecurityManager, dead-locking on the main thread
        xp.setEncryptPasswordFields(false);
        return xp;
    }
}
