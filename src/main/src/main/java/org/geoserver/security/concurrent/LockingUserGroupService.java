/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.UserGroupLoadedEvent;
import org.geoserver.security.event.UserGroupLoadedListener;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.springframework.dao.DataAccessException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

/**
 * This is a wrapper class for a {@link GeoServerUserGroupService} This wrapper protects internal
 * data structures using read/write locks
 *
 * @author christian
 */
public class LockingUserGroupService extends AbstractLockingService
        implements GeoServerUserGroupService, UserGroupLoadedListener {

    protected Set<UserGroupLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<UserGroupLoadedListener>());

    /** Constructor for the locking wrapper */
    public LockingUserGroupService(GeoServerUserGroupService service) {
        super(service);
        service.registerUserGroupLoadedListener(this);
    }

    /** @return the wrapped service */
    public GeoServerUserGroupService getService() {
        return (GeoServerUserGroupService) super.getService();
    }

    @Override
    public GeoServerUserGroupStore createStore() throws IOException {
        GeoServerUserGroupStore store = getService().createStore();
        return store != null ? new LockingUserGroupStore(store) : null;
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getGroupByGroupname(java.lang.String)
     */
    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        readLock();
        try {
            return getService().getGroupByGroupname(groupname);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#createUserObject(java.lang.String,
     *     java.lang.String, boolean)
     */
    public GeoServerUser createUserObject(String username, String password, boolean isEnabled)
            throws IOException {
        readLock();
        try {
            return getService().createUserObject(username, password, isEnabled);
        } finally {
            readUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#load()
     */
    public void load() throws IOException {
        writeLock();
        try {
            getService().load();
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserByUsername(java.lang.String)
     */
    public GeoServerUser getUserByUsername(String username) throws IOException {
        readLock();
        try {
            return getService().getUserByUsername(username);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#createGroupObject(java.lang.String,
     *     boolean)
     */
    public GeoServerUserGroup createGroupObject(String groupname, boolean isEnabled)
            throws IOException {
        readLock();
        try {
            return getService().createGroupObject(groupname, isEnabled);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUsers()
     */
    public SortedSet<GeoServerUser> getUsers() throws IOException {
        readLock();
        try {
            return getService().getUsers();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserGroups()
     */
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {
        readLock();
        try {
            return getService().getUserGroups();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getUsersForGroup(org.geoserver.security.impl.GeoServerUserGroup)
     */
    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        readLock();
        try {
            return getService().getUsersForGroup(group);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#getGroupsForUser(org.geoserver.security.impl.GeoServerUser)
     */
    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        readLock();
        try {
            return getService().getGroupsForUser(user);
        } finally {
            readUnLock();
        }
    }

    /** Fire {@link UserGroupLoadedEvent} for all listeners */
    protected void fireUserGroupLoadedEvent() {
        UserGroupLoadedEvent event = new UserGroupLoadedEvent(this);
        for (UserGroupLoadedListener listener : listeners) {
            listener.usersAndGroupsChanged(event);
        }
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#registerUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void registerUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#unregisterUserGroupChangedListener(org.geoserver.security.event.UserGroupChangedListener)
     */
    public void unregisterUserGroupLoadedListener(UserGroupLoadedListener listener) {
        listeners.remove(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.event.UserGroupChangedListener#usersAndGroupsChanged(org.geoserver.security.event.UserGroupChangedEvent)
     */
    public void usersAndGroupsChanged(UserGroupLoadedEvent event) {
        //        if (rwl.isWriteLockedByCurrentThread())
        //            writeUnLock();
        //        else
        //            readUnLock();
        fireUserGroupLoadedEvent();
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerUserGroupService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        writeLock();
        try {
            getService().initializeFromConfig(config);
        } finally {
            writeUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.springframework.security.core.userdetails.UserDetailsService#loadUserByUsername(java.lang.String)
     */
    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException, DataAccessException {
        readLock();
        try {
            return getService().loadUserByUsername(username);
        } finally {
            readUnLock();
        }
    }

    /** NO_LOCK */
    @Override
    public String getPasswordEncoderName() {
        return getService().getPasswordEncoderName();
    }

    /** NO_LOCK */
    @Override
    public String getPasswordValidatorName() {
        return getService().getPasswordValidatorName();
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getUserCount()
     */
    public int getUserCount() throws IOException {
        readLock();
        try {
            return getService().getUserCount();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerUserGroupService#getGroupCount()
     */
    public int getGroupCount() throws IOException {
        readLock();
        try {
            return getService().getGroupCount();
        } finally {
            readUnLock();
        }
    }
    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUsersHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUserCountHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUsersNotHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountNotHavingProperty(String propname) throws IOException {
        readLock();
        try {
            return getService().getUserCountNotHavingProperty(propname);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        readLock();
        try {
            return getService().getUsersHavingPropertyValue(propname, propvalue);
        } finally {
            readUnLock();
        }
    }

    /** READ_LOCK */
    @Override
    public int getUserCountHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        readLock();
        try {
            return getService().getUserCountHavingPropertyValue(propname, propvalue);
        } finally {
            readUnLock();
        }
    }
}
