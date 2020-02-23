/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.io.IOException;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.validation.PasswordPolicyException;

/**
 * This is a wrapper class for a {@link GeoServerUserGroupStore} protected internal data structures
 * using read/write locks
 *
 * @author christian
 */
public class LockingUserGroupStore extends LockingUserGroupService
        implements GeoServerUserGroupStore {

    /** Constructor for the locking wrapper */
    public LockingUserGroupStore(GeoServerUserGroupStore store) {
        super(store);
    }

    /** @return the wrapped store */
    public GeoServerUserGroupStore getStore() {
        return (GeoServerUserGroupStore) super.getService();
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#addUser(org.geoserver.security.impl.GeoServerUser)
     */
    public void addUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        writeLock();
        try {
            getStore().addUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#updateUser(org.geoserver.security.impl.GeoServerUser)
     */
    public void updateUser(GeoServerUser user) throws IOException, PasswordPolicyException {
        writeLock();
        try {
            getStore().updateUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#removeUser(org.geoserver.security.impl.GeoServerUser)
     */
    public boolean removeUser(GeoServerUser user) throws IOException {
        writeLock();
        try {
            return getStore().removeUser(user);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#addGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void addGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            getStore().addGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#updateGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void updateGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            getStore().updateGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#removeGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public boolean removeGroup(GeoServerUserGroup group) throws IOException {
        writeLock();
        try {
            return getStore().removeGroup(group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupStore#store()
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
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#associateUserToGroup(org.geoserver.security.impl.GeoServerUser,
     *     org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void associateUserToGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        writeLock();
        try {
            getStore().associateUserToGroup(user, group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupStore#disAssociateUserFromGroup(org.geoserver.security.impl.GeoServerUser,
     *     org.geoserver.security.impl.GeoServerUserGroup)
     */
    public void disAssociateUserFromGroup(GeoServerUser user, GeoServerUserGroup group)
            throws IOException {
        writeLock();
        try {
            getStore().disAssociateUserFromGroup(user, group);
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupStore#isModified()
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
     * @see org.geoserver.security.GeoServerUserGroupStore#clear()
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
     *     org.geoserver.security.GeoServerUserGroupStore#initializeFromService(org.geoserver.security.GeoServerUserGroupService)
     */
    public void initializeFromService(GeoServerUserGroupService service) throws IOException {
        writeLock();
        try {
            getStore().initializeFromService(service);
        } finally {
            writeUnLock();
        }
    }
}
