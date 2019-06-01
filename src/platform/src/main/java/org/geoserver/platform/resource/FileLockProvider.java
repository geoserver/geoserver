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
import javax.servlet.ServletContext;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.util.IOUtils;
import org.springframework.web.context.ServletContextAware;

/**
 * A lock provider based on file system locks
 *
 * @author Andrea Aime - GeoSolutions
 */
public class FileLockProvider implements LockProvider, ServletContextAware {

    public static Log LOGGER = LogFactory.getLog(FileLockProvider.class);

    private File root;
    /** The wait to occur in case the lock cannot be acquired */
    int waitBeforeRetry = 20;
    /** max lock attempts */
    int maxLockAttempts = 120 * 1000 / waitBeforeRetry;

    MemoryLockProvider memoryProvider = new MemoryLockProvider();

    public FileLockProvider() {
        // base directory obtained from servletContext
    }

    public FileLockProvider(File basePath) {
        this.root = basePath;
    }

    public Resource.Lock acquire(final String lockKey) {
        // first off, synchronize among threads in the same jvm (the nio locks won't lock
        // threads in the same JVM)
        final Resource.Lock memoryLock = memoryProvider.acquire(lockKey);

        // then synch up between different processes
        final File file = getFile(lockKey);
        try {
            FileOutputStream currFos = null;
            FileLock currLock = null;
            try {
                // try to lock
                int count = 0;
                while (currLock == null && count < maxLockAttempts) {
                    // the file output stream can also fail to be acquired due to the
                    // other nodes deleting the file
                    currFos = new FileOutputStream(file);
                    try {
                        currLock = currFos.getChannel().lock();
                    } catch (OverlappingFileLockException e) {
                        IOUtils.closeQuietly(currFos);
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException ie) {
                            // ok, moving on
                        }
                    } catch (IOException e) {
                        // this one is also thrown with a message "avoided fs deadlock"
                        IOUtils.closeQuietly(currFos);
                        try {
                            Thread.sleep(20);
                        } catch (InterruptedException ie) {
                            // ok, moving on
                        }
                    }
                    count++;
                }

                // verify we managed to get the FS lock
                if (count >= maxLockAttempts) {
                    throw new IllegalStateException(
                            "Failed to get a lock on key "
                                    + lockKey
                                    + " after "
                                    + count
                                    + " attempts");
                }

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug(
                            "Lock "
                                    + lockKey
                                    + " acquired by thread "
                                    + Thread.currentThread().getId()
                                    + " on file "
                                    + file);
                }

                // store the results in a final variable for the inner class to use
                final FileOutputStream fos = currFos;
                final FileLock lock = currLock;

                // nullify so that we don't close them, the locking occurred as expected
                currFos = null;
                currLock = null;

                return new Resource.Lock() {

                    boolean released;

                    public void release() {
                        if (released) {
                            return;
                        }

                        try {
                            released = true;
                            if (!lock.isValid()) {
                                // do not crap out, locks usage is only there to prevent duplication
                                // of work
                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug(
                                            "Lock key "
                                                    + lockKey
                                                    + " for releasing lock is unkonwn, it means "
                                                    + "this lock was never acquired, or was released twice. "
                                                    + "Current thread is: "
                                                    + Thread.currentThread().getId()
                                                    + ". "
                                                    + "Are you running two instances in the same JVM using NIO locks? "
                                                    + "This case is not supported and will generate exactly this error message");
                                    return;
                                }
                            }
                            try {
                                lock.release();
                                IOUtils.closeQuietly(fos);
                                file.delete();

                                if (LOGGER.isDebugEnabled()) {
                                    LOGGER.debug(
                                            "Lock "
                                                    + lockKey
                                                    + " released by thread "
                                                    + Thread.currentThread().getId());
                                }
                            } catch (IOException e) {
                                throw new IllegalStateException(
                                        "Failure while trying to release lock for key " + lockKey,
                                        e);
                            }
                        } finally {
                            memoryLock.release();
                        }
                    }

                    @Override
                    public String toString() {
                        return "FileLock " + file.getName();
                    }
                };
            } finally {
                if (currLock != null) {
                    currLock.release();
                    memoryLock.release();
                }
                IOUtils.closeQuietly(currFos);
                file.delete();
            }
        } catch (IOException e) {
            throw new IllegalStateException(
                    "Failure while trying to get lock for key " + lockKey, e);
        }
    }

    private File getFile(String lockKey) {
        File locks = new File(root, "filelocks"); // avoid same directory as GWC
        locks.mkdirs();
        String sha1 = DigestUtils.sha1Hex(lockKey);
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
