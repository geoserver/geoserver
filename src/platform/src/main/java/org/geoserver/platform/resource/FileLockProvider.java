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

import jakarta.servlet.ServletContext;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.codec.digest.DigestUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;
import org.springframework.web.context.ServletContextAware;

/**
 * A lock provider based on file system locks. Reentrant: a thread that already holds the lock for a given key can
 * acquire it again (e.g. via a nested call), and the underlying OS lock is only released once all nested acquisitions
 * on that thread have released.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileLockProvider implements LockProvider, ServletContextAware {

    private static final Logger LOGGER = Logging.getLogger(FileLockProvider.class.getName());
    private final int timeoutSeconds;

    private File root;
    /** The wait to occur in case the lock cannot be acquired */
    int waitBeforeRetry = 20;

    private final ConcurrentMap<String, LockEntry> lockEntries = new ConcurrentHashMap<>();

    private static final class LockEntry {
        final ReentrantLock gate = new ReentrantLock();
        FileOutputStream fos;
        FileLock lock;
    }

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
    public Resource.Lock acquire(final String lockKey) {
        // gate serializes access to this key among threads in the same JVM (the nio lock
        // below won't do that on its own: FileLock is documented as unsuitable for excluding
        // threads within the same JVM) and, being a ReentrantLock, also lets the thread that
        // already holds it re-enter without blocking
        final LockEntry entry = lockEntries.computeIfAbsent(lockKey, k -> new LockEntry());
        final ReentrantLock gate = entry.gate;
        acquireGate(lockKey, gate);

        final File file = getFile(lockKey);
        if (gate.getHoldCount() > 1) {
            // nested acquire on the same thread: the OS lock is already held
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine("Lock " + lockKey + " re-entered by thread "
                        + Thread.currentThread().getId() + ", hold count " + gate.getHoldCount());
            }
        } else {
            acquireOsLock(lockKey, entry, file, gate);
        }

        return newLock(lockKey, entry, file, gate);
    }

    /** Blocks (up to {@link #timeoutSeconds}) until {@code gate} is acquired by the current thread. */
    private void acquireGate(String lockKey, ReentrantLock gate) {
        try {
            if (!gate.tryLock(timeoutSeconds, TimeUnit.SECONDS)) {
                throw new IllegalStateException("Failed to get in-process lock on " + lockKey + " after "
                        + (timeoutSeconds * 1000L) + "ms (another thread in this JVM is holding it)");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while trying to acquire lock for key " + lockKey, e);
        }
    }

    /**
     * First acquire of this key by this thread: takes the real OS lock and stores it in {@code entry}. On failure,
     * cleans up and releases {@code gate} before rethrowing (the caller won't get a {@link Resource.Lock} to release it
     * through).
     */
    @SuppressWarnings({"PMD.CloseResource"})
    private void acquireOsLock(String lockKey, LockEntry entry, File file, ReentrantLock gate) {
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

            entry.fos = currFos;
            entry.lock = currLock;
        } catch (Exception e) {
            // If we get here, acquisition failed or timed out
            if (currLock != null) {
                try {
                    currLock.release();
                } catch (IOException ignored) {
                }
            }
            IOUtils.closeQuietly(currFos);
            lockEntries.remove(lockKey, entry);
            gate.unlock();
            throw (e instanceof RuntimeException) ? (RuntimeException) e : new IllegalStateException(e);
        }
    }

    /** No finally block deleting the file here, it's done in the returned lock's {@code release()}. */
    private Resource.Lock newLock(String lockKey, LockEntry entry, File file, ReentrantLock gate) {
        return new Resource.Lock() {
            boolean released;

            @Override
            public void release() {
                if (released) return;
                released = true;
                try {
                    if (gate.getHoldCount() == 1) {
                        releaseFileLock(lockKey, entry, file);
                    }
                } finally {
                    gate.unlock();
                }
            }
        };
    }

    /**
     * Drops the OS-level lock, closes the channel and deletes the lock file. Only called on the last nested release,
     * i.e. once {@code entry.gate.getHoldCount()} is about to drop to 0.
     */
    private void releaseFileLock(String lockKey, LockEntry entry, File file) {
        try {
            if (entry.lock != null && entry.lock.isValid()) {
                entry.lock.release();
                IOUtils.closeQuietly(entry.fos);
                file.delete(); // Proper place for deletion

                if (LOGGER.isLoggable(Level.FINE)) {
                    LOGGER.fine("Lock " + lockKey + " mapped onto " + file + " released by thread "
                            + Thread.currentThread().getId());
                }
            } else {
                // should not happen: by the time release() calls into here, entry.lock was always
                // set by this same thread's earlier successful acquire. Logged rather than thrown
                // since lock usage is only there to prevent duplication of work, not correctness.
                LOGGER.warning("Lock key " + lockKey + " had no valid OS lock to release; this should not happen");
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failure releasing lock " + lockKey, e);
        } finally {
            lockEntries.remove(lockKey, entry);
        }
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
