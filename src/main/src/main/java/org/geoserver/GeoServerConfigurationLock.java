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
 * The global configuration lock. At the moment it is called by coarse grained request level callbacks to lock both the
 * GUI and the REST configuration so that concurrent access does not end up causing issues (like corrupt configuration
 * and the like).
 *
 * <p>The locking code can be disabled by calling {@link GeoServerConfigurationLock#setEnabled(boolean)} or by setting
 * the system variable {code}-DGeoServerConfigurationLock.enabled=false{code}
 *
 * @author Andrea Aime - GeoSolution
 */
public class GeoServerConfigurationLock {

    /** Environment property resolved according to {@link GeoServerExtensions#getProperty(String)} */
    static final String TRYLOCK_TIMEOUT_SYSTEM_PROPERTY = "CONFIGURATION_TRYLOCK_TIMEOUT";

    /**
     * DEFAULT_TRY_LOCK_TIMEOUT_MS.
     *
     * @see #getLockTimeoutMillis()
     */
    public static long DEFAULT_TRY_LOCK_TIMEOUT_MS = 30000;

    private static final Level LEVEL = Level.FINE;

    private static final Logger LOGGER = Logging.getLogger(GeoServerConfigurationLock.class);

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    private static final ThreadLocal<LockType> currentLock = new ThreadLocal<>();

    /**
     * The thread currently holding the write lock, or {@code null} when none does. Recorded so the write lock can be
     * force-released - by interrupting its owner - when that thread becomes wedged (see
     * {@link #tryForceReleaseWriteLock()}). Only ever written while the write lock is held, so a plain volatile
     * suffices.
     */
    private static volatile Thread writeOwner;

    public static enum LockType {
        READ,
        WRITE
    }

    private volatile boolean enabled;

    public GeoServerConfigurationLock() {
        String pvalue = System.getProperty("GeoServerConfigurationLock.enabled");
        if (pvalue != null) {
            enabled = Boolean.parseBoolean(pvalue);
        } else {
            enabled = true;
        }

        LOGGER.config("GeoServer configuration lock is " + (enabled ? "enabled" : "disabled"));
    }

    private long getLockTimeoutMillis() {
        String configValue = GeoServerExtensions.getProperty(TRYLOCK_TIMEOUT_SYSTEM_PROPERTY);
        return configValue == null || configValue.isEmpty() ? DEFAULT_TRY_LOCK_TIMEOUT_MS : Long.valueOf(configValue);
    }

    /**
     * Queries if the write lock is held by any thread. This method is designed for use in monitoring system state, not
     * for synchronization control.
     *
     * @return {@code true} if any thread holds the write lock and {@code false} otherwise
     */
    public boolean isWriteLocked() {
        return readWriteLock.isWriteLocked();
    }

    /**
     * Opens a lock in the specified mode. To avoid deadlocks make sure the corresponding unlock method is called as
     * well before the code exits.
     *
     * <p>If a write lock is already held by the current thread, and a read lock is requested, the write lock is
     * preserved.
     */
    public void lock(LockType type) {
        if (!enabled) {
            return;
        }

        if (LockType.READ == type && readWriteLock.isWriteLockedByCurrentThread()) {
            // preserve the write lock
            return;
        }

        Lock lock = getLock(type);

        lock.lock();
        currentLock.set(type);
        if (LockType.WRITE == type) {
            writeOwner = Thread.currentThread();
        }

        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " got the lock in mode " + type);
        }
    }

    /**
     * Tries to open a lock in the specified mode. Acquires the lock if it is available and returns immediately with the
     * value true. If the lock is not available then this method will return immediately with the value false.
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
     * This usage ensures that the lock is unlocked if it was acquired, and doesn't try to unlock if the lock was not
     * acquired.
     *
     * <p>If a write lock is already held by the current thread, and a read lock is requested, the write lock is
     * preserved.
     *
     * @return true if the lock was acquired and false otherwise
     */
    public boolean tryLock(LockType type) {
        if (!enabled) {
            return true;
        }

        if (LockType.READ == type && readWriteLock.isWriteLockedByCurrentThread()) {
            // preserve the write lock
            return true;
        }

        Lock lock = getLock(type);

        boolean res = false;
        try {
            res = lock.tryLock(getLockTimeoutMillis(), TimeUnit.MILLISECONDS);
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
                if (LockType.WRITE == type) {
                    writeOwner = Thread.currentThread();
                }
            }
        }

        if (LOGGER.isLoggable(LEVEL)) {
            if (res) {
                LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " got the lock in mode " + type);
            } else {
                LOGGER.log(
                        LEVEL, "Thread " + Thread.currentThread().getId() + " could not get the lock in mode " + type);
            }
        }

        return res;
    }

    /**
     * Tries to upgrade the current read lock to a write lock. If the current lock is not a read one, it will throw an
     * {@link IllegalStateException}
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
                throw new RuntimeException("Failed to upgrade lock from read to write "
                        + "state, please re-try the configuration operation");
            }
        }
    }

    /** Unlocks a previously acquired lock. The lock type must match the previous {@link #lock(LockType)} call */
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
                LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " releasing the lock in mode " + type);
            }
            lock.unlock();
        } finally {
            final int currThreadReentrantReadLockCount = readWriteLock.getReadHoldCount();
            final int currThreadReentrantWriteLockCount = readWriteLock.getWriteHoldCount();
            // reentrancy check
            final boolean canUnset = (LockType.READ == type && currThreadReentrantReadLockCount == 0)
                    || (LockType.WRITE == type && currThreadReentrantWriteLockCount == 0);
            if (canUnset) {
                currentLock.set(null);
                if (LockType.WRITE == type) {
                    writeOwner = null;
                }
            }
        }
    }

    /**
     * Break-glass recovery for a write lock held by a wedged thread.
     *
     * <p>The configuration lock is a {@link ReentrantReadWriteLock}: it can only be released by the very thread that
     * acquired it, so no other thread can {@code unlock()} it. If a thread (for example a Spring Batch backup/restore
     * job thread) becomes wedged while holding the write lock, every other configuration operation - GUI and REST alike
     * - is blocked indefinitely with no recovery short of a JVM restart.
     *
     * <p>This method interrupts the thread that currently owns the write lock so that, if it is parked or blocked at an
     * interruptible point, it unwinds and releases the lock through its own normal code path (e.g. a job's
     * {@code afterJob} listener) - on its owning thread, preserving the lock's ownership invariant rather than forcing
     * the monitor open from the outside. It is therefore best-effort: a thread wedged at a non-interruptible point (a
     * tight CPU loop, or non-interruptible I/O) cannot be freed this way and still requires a restart.
     *
     * @return {@code true} if a write-lock owner thread was found and interrupted; {@code false} if the write lock is
     *     not currently held, its owner is unknown, or the calling thread is itself the owner
     */
    public boolean tryForceReleaseWriteLock() {
        Thread owner = writeOwner;
        if (owner == null || !readWriteLock.isWriteLocked() || owner == Thread.currentThread()) {
            return false;
        }
        LOGGER.warning("Force-releasing the GeoServer configuration write lock by interrupting its owning thread '"
                + owner.getName()
                + "'. This is a break-glass recovery for a wedged write-lock holder; if that thread is not at an "
                + "interruptible point a JVM restart may still be required.");
        owner.interrupt();
        return true;
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
            LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " locking in mode " + type);
        }
        return lock;
    }

    /** Returns the lock type owned by the current thread (could be {@code null} for no lock) */
    public LockType getCurrentLock() {
        return currentLock.get();
    }
}
