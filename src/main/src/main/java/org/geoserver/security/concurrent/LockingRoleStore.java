/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.io.IOException;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.GeoServerRole;

/**
 * This is a wrapper class for a {@link GeoServerRoleStore} Thsi wrapper protects internal data
 * structures using read/write locks
 *
 * @author christian
 */
public class LockingRoleStore extends LockingRoleService implements GeoServerRoleStore {

    /** Constructor for the locking wrapper */
    public LockingRoleStore(GeoServerRoleStore store) {
        super(store);
    }

    /** @return the wrapped store */
    public GeoServerRoleStore getStore() {
        return (GeoServerRoleStore) super.getService();
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#addRole(org.geoserver.security.impl.GeoServerRole)
     */
    public void addRole(GeoServerRole role) throws IOException {
        writeLock();
        try {
            getStore().addRole(role);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#updateRole(org.geoserver.security.impl.GeoServerRole)
     */
    public void updateRole(GeoServerRole role) throws IOException {
        writeLock();
        try {
            getStore().updateRole(role);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#removeRole(org.geoserver.security.impl.GeoServerRole)
     */
    public boolean removeRole(GeoServerRole role) throws IOException {
        writeLock();
        try {
            return getStore().removeRole(role);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#associateRoleToGroup(org.geoserver.security.impl.GeoServerRole,
     *     java.lang.String)
     */
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        writeLock();
        try {
            getStore().associateRoleToGroup(role, groupname);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#disAssociateRoleFromGroup(org.geoserver.security.impl.GeoServerRole,
     *     java.lang.String)
     */
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        writeLock();
        try {
            getStore().disAssociateRoleFromGroup(role, groupname);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#associateRoleToUser(org.geoserver.security.impl.GeoServerRole,
     *     java.lang.String)
     */
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        writeLock();
        try {
            getStore().associateRoleToUser(role, username);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#disAssociateRoleFromUser(org.geoserver.security.impl.GeoServerRole,
     *     java.lang.String)
     */
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        writeLock();
        try {
            getStore().disAssociateRoleFromUser(role, username);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleStore#store()
     */
    public void store() throws IOException {
        writeLock();
        try {
            getStore().store();
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleStore#isModified()
     */
    public boolean isModified() {
        readLock();
        try {
            return getStore().isModified();
        } finally {
            readUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#setParentRole(org.geoserver.security.impl.GeoServerRole,
     *     org.geoserver.security.impl.GeoServerRole)
     */
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {
        writeLock();
        try {
            getStore().setParentRole(role, parentRole);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleStore#clear()
     */
    public void clear() throws IOException {
        writeLock();
        try {
            getStore().clear();
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleStore#initializeFromService(org.geoserver.security.GeoServerRoleService)
     */
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        writeLock();
        try {
            getStore().initializeFromService(service);
        } finally {
            writeUnLock();
        }
    }
}
