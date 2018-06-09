/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityService;

/**
 * Abstract base class for locking support.
 *
 * @author christian
 */
public abstract class AbstractLockingService implements GeoServerSecurityService {

    protected final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);
    protected final Lock readLock = readWriteLock.readLock();
    protected final Lock writeLock = readWriteLock.writeLock();

    protected GeoServerSecurityService service;

    protected AbstractLockingService(GeoServerSecurityService service) {
        this.service = service;
    }

    /** @return the wrapped service */
    public GeoServerSecurityService getService() {
        return service;
    }

    /**
     * NO_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getName()
     */
    public String getName() {
        return getService().getName();
    }

    @Override
    public void setName(String name) {
        writeLock();
        try {
            getService().setName(name);
        } finally {
            writeUnLock();
        }
    }

    @Override
    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        writeLock();
        try {
            getService().setSecurityManager(securityManager);
        } finally {
            writeUnLock();
        }
    }

    /** NO_LOCK */
    @Override
    public GeoServerSecurityManager getSecurityManager() {
        return getService().getSecurityManager();
    }

    /** NO_LOCK */
    @Override
    public boolean canCreateStore() {
        return getService().canCreateStore();
    }

    @Override
    public String toString() {
        return "Locking " + getName();
    }

    /** get a read lock */
    protected void readLock() {
        readLock.lock();
    }

    /** free read lock */
    protected void readUnLock() {
        readLock.unlock();
    }

    /** get a write lock */
    protected void writeLock() {
        writeLock.lock();
    }

    /** free write lock */
    protected void writeUnLock() {
        writeLock.unlock();
    }
}
