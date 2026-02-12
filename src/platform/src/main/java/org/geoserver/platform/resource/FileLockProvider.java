/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * (c) 2008-2010 GeoSolutions
 *
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoWebCache 1.5.1 under a LGPL license
 */

package org.geoserver.platform.resource;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;
import org.springframework.web.context.ServletContextAware;

/**
 * A lock provider based on file system locks
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileLockProvider implements LockProvider, ServletContextAware {

    private static final Logger LOGGER = Logging.getLogger(FileLockProvider.class.getName());
    private final int timeoutSeconds;

    private File root;
    /** The wait to occur in case the lock cannot be acquired */
    int waitBeforeRetry = 20;

    MemoryLockProvider memoryProvider = new MemoryLockProvider();

    public FileLockProvider() {
        // base directory obtained from servletContext
        this.timeoutSeconds = GS_LOCK_TIMEOUT;
    }

    public FileLockProvider(File basePath) {
        this.root = basePath;
        this.timeoutSeconds = GS_LOCK_TIMEOUT;
    }

    /**
     * Constructor allowing to specify a timeout for lock acquisition, in seconds. If the lock cannot be acquired within
     * the specified time, an exception will be thrown.
     *
     * @param basePath the base directory for lock files
     * @param timeoutSeconds the maximum time to wait for lock acquisition, in seconds
     */
    FileLockProvider(File basePath, int timeoutSeconds) {
        this.root = basePath;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Override
    @SuppressWarnings({"PMD.CloseResource"})
    public Resource.Lock acquire(final String lockKey) {
        // first off, synchronize among threads in the same jvm (the nio locks won't lock
        // threads in the same JVM)
        final Resource.Lock memoryLock = memoryProvider.acquire(lockKey);
        final File file = getFile(lockKey);

        // Track these to ensure cleanup on failure
        FileOutputStream currFos = null;
        FileLock currLock = null;

        if (LOGGER.isLoggable(Level.FINE))
            LOGGER.fine("Mapped lock key " + lockKey + " to lock file " + file + ". Attempting to lock on it.");
        try {
            long startTime = System.currentTimeMillis();
            long lockTimeoutMs = timeoutSeconds * 1000L;

            while (currLock == null && (System.currentTimeMillis() - startTime) < lockTimeoutMs) {
                try {
                    currFos = new FileOutputStream(file);
                    currLock = currFos.getChannel().tryLock();

                    if (currLock == null) {
                        IOUtils.closeQuietly(currFos);
                        Thread.sleep(waitBeforeRetry);
                    }
                } catch (OverlappingFileLockException | IOException | InterruptedException e) {
                    IOUtils.closeQuietly(currFos);
                    if (e instanceof InterruptedException) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                    Thread.sleep(waitBeforeRetry);
                }
            }

            if (currLock == null) {
                throw new IllegalStateException("Failed to get lock on " + lockKey + " after " + lockTimeoutMs + "ms");
            }

            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Lock "
                        + lockKey
                        + " acquired by thread "
                        + Thread.currentThread().getId()
                        + " on file "
                        + file);
            }

            final FileOutputStream finalFos = currFos;
            final FileLock finalLock = currLock;

            return new Resource.Lock() {
                boolean released;

                @Override
                public void release() {
                    if (released) return;
                    try {
                        released = true;
                        if (finalLock.isValid()) {
                            finalLock.release();
                            IOUtils.closeQuietly(finalFos);
                            file.delete(); // Proper place for deletion

                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine("Lock "
                                        + lockKey
                                        + " mapped onto "
                                        + file
                                        + " released by thread "
                                        + Thread.currentThread().getId());
                            }
                        } else {
                            // do not crap out, locks usage is only there to prevent duplication
                            // of work
                            if (LOGGER.isLoggable(Level.FINE)) {
                                LOGGER.fine(("Lock key %s for releasing lock is unknown, it means this lock was never"
                                                + " acquired, or was released twice. Current thread is: %d. Are you"
                                                + " running two instances in the same JVM using NIO locks? This case is"
                                                + " not supported and will generate exactly this error message")
                                        .formatted(
                                                lockKey, Thread.currentThread().getId()));
                            }
                        }

                    } catch (IOException e) {
                        throw new IllegalStateException("Failure releasing lock " + lockKey, e);
                    } finally {
                        memoryLock.release();
                    }
                }
            };
        } catch (Exception e) {
            // If we get here, acquisition failed or timed out
            if (currLock != null) {
                try {
                    currLock.release();
                } catch (IOException ignored) {
                }
            }
            IOUtils.closeQuietly(currFos);
            memoryLock.release(); // Must release memory lock on failure
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new IllegalStateException(e);
        }
        // Note: No finally block deleting the file here, it's done in the returned lock
    }

    private File getFile(String lockKey) {
        File locks = new File(root, "filelocks"); // avoid same directory as GWC
        locks.mkdirs();
        // use a hash of the lock key to avoid issues with special characters and long file names
        // SHA-256 has such low collision probability that if you care anyways, you should probably do something
        // to defend against asteroids levelling your data center instead of worrying about lock collisions
        String sha1 = DigestUtils.sha256Hex(lockKey);
        return new File(locks, sha1 + ".lock");
    }

    @Override
    public void setServletContext(ServletContext servletContext) {
        String data = GeoServerResourceLoader.lookupGeoServerDataDirectory(servletContext);
        if (data != null) {
            root = new File(data);
        } else {
            throw new IllegalStateException("Unable to determine data directory");
        }
    }

    @Override
    public String toString() {
        return "FileLockProvider " + root;
    }
}
