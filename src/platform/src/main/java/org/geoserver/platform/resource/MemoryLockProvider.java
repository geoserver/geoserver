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

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geotools.util.logging.Logging;

/**
 * An in memory lock provider based on a striped lock
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MemoryLockProvider implements LockProvider {

    static final Logger LOGGER = Logging.getLogger(MemoryLockProvider.class.getName());

    ConcurrentHashMap<String, LockAndCounter> lockAndCounters = new ConcurrentHashMap<>();

    @Override
    public Resource.Lock acquire(String lockKey) {
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Acquiring lock key " + lockKey);

        // Atomically create a new LockAndCounter, or increment the existing one
        LockAndCounter lockAndCounter = lockAndCounters.compute(lockKey, (key, internalLockAndCounter) -> {
            if (internalLockAndCounter == null) {
                internalLockAndCounter = new LockAndCounter();
            }
            internalLockAndCounter.counter.incrementAndGet();
            return internalLockAndCounter;
        });

        try {
            if (!lockAndCounter.lock.tryLock(GS_LOCK_TIMEOUT, TimeUnit.SECONDS)) {
                // Throwing an exception prevents the thread from hanging indefinitely
                throw new RuntimeException(String.format("Lock acquisition timeout for key [%s].", lockKey));
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Interrupted while trying to acquire lock for key " + lockKey, e);
        }

        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Acquired lock key " + lockKey);

        return new Resource.Lock() {

            boolean released = false;

            @Override
            public void release() {
                if (!released) {
                    released = true;

                    LockAndCounter lockAndCounter = lockAndCounters.get(lockKey);
                    lockAndCounter.lock.unlock();

                    // Attempt to remove lock if no other thread is waiting for it
                    if (lockAndCounter.counter.decrementAndGet() == 0) {

                        // Try to remove the lock, but we have to check the count AGAIN inside of "compute" so that we
                        // know it hasn't been incremented since the if-statement above was evaluated
                        lockAndCounters.compute(lockKey, (key, existingLockAndCounter) -> {
                            if (existingLockAndCounter == null || existingLockAndCounter.counter.get() == 0) {
                                return null;
                            }
                            return existingLockAndCounter;
                        });
                    }

                    if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("Released lock key " + lockKey);
                }
            }
        };
    }

    @Override
    public String toString() {
        return "MemoryLockProvider";
    }

    /**
     * A ReentrantLock with a counter to track how many threads are waiting on this lock so we know if it's safe to
     * remove it during a release.
     */
    private static class LockAndCounter {
        private final ReentrantLock lock = new ReentrantLock();

        // The count of threads holding or waiting for this lock
        private final AtomicInteger counter = new AtomicInteger(0);
    }
}
