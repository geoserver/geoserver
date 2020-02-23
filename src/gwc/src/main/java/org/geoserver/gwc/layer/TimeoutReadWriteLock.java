/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.geoserver.platform.ServiceException;

/**
 * A wrapper around a ReadWriteLock that will perform all locking operations under a timeout to
 * prevent deadlocks.
 *
 * @author Andrea Aime - GeoSolutions
 */
class TimeoutReadWriteLock {

    ReadWriteLock lock = new ReentrantReadWriteLock();

    int timeoutMs;

    String name;

    /** Builds the {@link ReadWriteLock} wrapper with a given timeout, in milliseconds */
    public TimeoutReadWriteLock(int timeoutMs, String name) {
        this.timeoutMs = timeoutMs;
        this.name = name;
    }

    /**
     * Acquires a read lock with the configured timeout, will throw a {@link ServiceException} if
     * the lock is not acquired
     */
    public void acquireReadLock() {
        boolean acquired = false;
        try {
            acquired = lock.readLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ServiceException(
                    "Failed to acquire read lock on '" + name + "' due to interruption", e);
        }
        if (!acquired) {
            throw new ServiceException(
                    "Failed to acquire read lock on '"
                            + name
                            + "' in less than "
                            + timeoutMs
                            + " ms");
        }
    }

    /** Releases a previously acquired read lock */
    public void releaseReadLock() {
        lock.readLock().unlock();
    }

    /**
     * Acquires a write lock with the configured timeout, will throw a {@link ServiceException} if
     * the lock is not acquired
     */
    public void acquireWriteLock() {
        boolean acquired = false;
        try {
            acquired = lock.writeLock().tryLock(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            throw new ServiceException(
                    "Failed to acquire write lock on '" + name + "' due to interruption", e);
        }
        if (!acquired) {
            throw new ServiceException(
                    "Failed to acquire write lock on '"
                            + name
                            + "' in less than "
                            + timeoutMs
                            + " ms");
        }
    }

    /** Releases a previously acquired write lock */
    public void releaseWriteLock() {
        lock.writeLock().unlock();
    }

    /**
     * Downgrades a write lock to a read lock. The write lock gets released, the caller must still
     * release the read lock after this is called
     */
    public void downgradeToReadLock() {
        // Downgrade by acquiring read lock before releasing write lock
        lock.readLock().lock();
        // Unlock write, still hold read
        lock.writeLock().unlock();
    }
}
