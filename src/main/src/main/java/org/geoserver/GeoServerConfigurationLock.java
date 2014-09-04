/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.util.logging.Logging;

/**
 * The global configuration lock. At the moment it is called by coarse grained request level
 * callbacks to lock both the GUI and the REST configuration so that concurrent access does not end
 * up causing issues (like corrupt configuration and the like).
 * <p>
 * The locking code can be disabled by calling
 * {@link GeoServerConfigurationLock#setEnabled(boolean)} or by setting the system variable
 * {code}-DGeoServerConfigurationLock.enabled=false{code}
 * 
 * @author Andrea Aime - GeoSolution
 * 
 */
public class GeoServerConfigurationLock {
    private static final Level LEVEL = Level.FINE;

    private static final Logger LOGGER = Logging.getLogger(GeoServerConfigurationLock.class);

    private static final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    public static enum LockType {
        READ, WRITE
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
     * 
     * @param type
     */
    public void lock(LockType type) {
        if (!enabled) {
            return;
        }

        Lock lock;
        if (type == LockType.WRITE) {
            lock = readWriteLock.writeLock();
        } else {
            lock = readWriteLock.readLock();
        }
        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " locking in mode "
                    + type);
        }
        lock.lock();
        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId() + " got the lock in mode "
                    + type);
        }
    }

    /**
     * Unlocks a previously acquired lock. The lock type must match the previous
     * {@link #lock(LockType)} call
     * 
     * @param type
     */
    public void unlock(LockType type) {
        if (!enabled) {
            return;
        }

        Lock lock;
        if (type == LockType.WRITE) {
            lock = readWriteLock.writeLock();
        } else {
            lock = readWriteLock.readLock();
        }
        if (LOGGER.isLoggable(LEVEL)) {
            LOGGER.log(LEVEL, "Thread " + Thread.currentThread().getId()
                    + " releasing the lock in mode " + type);
        }
        lock.unlock();
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

}
