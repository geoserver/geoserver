/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * The global configuration lock. At the moment it is called by coarse grained request level
 * callbacks to lock both the GUI and the REST configuration so that concurrent access does not end
 * up causing issues (like corrupt configuration and the like).
 *
 * <p>The locking code can be disabled by calling {@link
 * GeoServerConfigurationLock#setEnabled(boolean)} or by setting the system variable
 * {code}-DGeoServerConfigurationLock.enabled=false{code}
 *
 * @author Andrea Aime - GeoSolution
 */
public class GeoServerConfigurationLock {

    /** DEFAULT_TRY_LOCK_TIMEOUT_MS */
    public static long DEFAULT_TRY_LOCK_TIMEOUT_MS =
            (GeoServerExtensions.getProperty("CONFIGURATION_TRYLOCK_TIMEOUT") != null
                    ? Long.valueOf(GeoServerExtensions.getProperty("CONFIGURATION_TRYLOCK_TIMEOUT"))
                    : 30000);

    private static final Level LEVEL = Level.FINE;

    private static final Logger LOGGER = Logging.getLogger(GeoServerConfigurationLock.class);

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private static final ThreadLocal<LockType> currentLock = new ThreadLocal<>();

    public static enum LockType {
        READ,
        WRITE
    };

    private boolean enabled;

    public GeoServerConfigurationLock() {
        String pvalue = System.getProperty("GeoServerConfigurationLock.enabled");
        if (pvalue != null) {
            enabled = Boolean.parseBoolean(pvalue);
        } else {
            enabled = true;
        }

        LOGGER.info("GeoServer configuration lock is " + (enabled ? "enabled" : "disabled"));
    }

    /**
     * Opens a lock in the specified mode. To avoid deadlocks make sure the corresponding unlock
     * method is called as well before the code exits
     */
    public void lock(LockType type) {
        if (!enabled) {
            return;
        }

        Lock lock = getLock(type);

        lock.lock();
        currentLock.set(type);

        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(
                    LEVEL,
                    "Thread " + Thread.currentThread().getId() + " got the lock in mode " + type);
        }
    }

    /**
     * Tries to open a lock in the specified mode. Acquires the lock if it is available and returns
     * immediately with the value true. If the lock is not available then this method will return
     * immediately with the value false.
     *
     * <p>A typical usage idiom for this method would be:
     *
     * <pre>
     *  Lock lock = ...;
     *  if (lock.tryLock()) {
     *    try {
     *      // manipulate protected state
     *    } finally {
     *      lock.unlock();
     *    }
     *  } else {
     *    // perform alternative actions
     *  }}
     * </pre>
     *
     * This usage ensures that the lock is unlocked if it was acquired, and doesn't try to unlock if
     * the lock was not acquired.
     *
     * @return true if the lock was acquired and false otherwise
     */
    public boolean tryLock(LockType type) {
        if (!enabled) {
            return true;
        }

        Lock lock = getLock(type);

        boolean res = false;
        try {
            res = lock.tryLock(DEFAULT_TRY_LOCK_TIMEOUT_MS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            LOGGER.log(
                    Level.WARNING,
                    "Thread "
                            + Thread.currentThread().getId()
                            + " thrown an InterruptedException on GeoServerConfigurationLock TryLock.",
                    e);
            res = false;
        } finally {
            if (res) {
                currentLock.set(type);
            }
        }

        if (LOGGER.isLoggable(LEVEL)) {
            if (res) {
                LOGGER.log(
                        LEVEL,
                        "Thread "
                                + Thread.currentThread().getId()
                                + " got the lock in mode "
                                + type);
            } else {
                LOGGER.log(
                        LEVEL,
                        "Thread "
                                + Thread.currentThread().getId()
                                + " could not get the lock in mode "
                                + type);
            }
        }

        return res;
    }

    /**
     * Tries to upgrade the current read lock to a write lock. If the current lock is not a read
     * one, it will throw an {@link IllegalStateException}
     */
    public void tryUpgradeLock() {
        LockType lock = currentLock.get();
        if (lock == null) {
            throw new IllegalStateException("No lock currently held");
        } else if (lock == LockType.WRITE) {
            throw new IllegalStateException("Already owning a write lock");
        } else {
            // core java does not have a notion of lock upgrade, one has to release the
            // read lock and get a write one
            unlock();
            if (tryLock(LockType.WRITE)) {
                currentLock.set(LockType.WRITE);
            } else {
                currentLock.set(null);
                throw new RuntimeException(
                        "Failed to upgrade lock from read to write "
                                + "state, please re-try the configuration operation");
            }
        }
    }

    /**
     * Unlocks a previously acquired lock. The lock type must match the previous {@link
     * #lock(LockType)} call
     */
    public void unlock() {
        if (!enabled) {
            return;
        }

        final LockType type = getCurrentLock();
        if (type == null) {
            return;
        }
        try {
            Lock lock = getLock(type);

            if (LOGGER.isLoggable(LEVEL)) {
                LOGGER.log(
                        LEVEL,
                        "Thread "
                                + Thread.currentThread().getId()
                                + " releasing the lock in mode "
                                + type);
            }
            lock.unlock();
        } finally {
            currentLock.set(null);
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /** */
    private Lock getLock(LockType type) {
        Lock lock;
        if (type == LockType.WRITE) {
            lock = readWriteLock.writeLock();
        } else {
            lock = readWriteLock.readLock();
        }
        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(
                    LEVEL, "Thread " + Thread.currentThread().getId() + " locking in mode " + type);
        }
        return lock;
    }

    /** Returns the lock type owned by the current thread (could be {@code null} for no lock) */
    public LockType getCurrentLock() {
        return currentLock.get();
    }
}
