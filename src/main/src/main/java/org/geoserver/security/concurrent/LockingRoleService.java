/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.concurrent;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.GeoServerRole;

/**
 * This is a wrapper class for a {@link GeoServerRoleService}. This wrapper protects internal data
 * structures using read/write locks
 *
 * @author christian
 */
public class LockingRoleService extends AbstractLockingService
        implements GeoServerRoleService, RoleLoadedListener {

    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    /** Constructor for the locking wrapper */
    public LockingRoleService(GeoServerRoleService service) {
        super(service);
        service.registerRoleLoadedListener(this);
    }

    /** @return the wrapped service */
    public GeoServerRoleService getService() {
        return (GeoServerRoleService) super.getService();
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        GeoServerRoleStore store = getService().createStore();
        return store != null ? new LockingRoleStore(store) : null;
    }

    /**
     * WRITE_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#load()
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
     * @see org.geoserver.security.GeoServerRoleService#getRolesForUser(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        readLock();
        try {
            return getService().getRolesForUser(username);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRolesForGroup(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        readLock();
        try {
            return getService().getRolesForGroup(groupname);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoles()
     */
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        readLock();
        try {
            return getService().getRoles();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#createRoleObject(java.lang.String)
     */
    public GeoServerRole createRoleObject(String role) throws IOException {
        readLock();
        try {
            return getService().createRoleObject(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getParentRole(org.geoserver.security.impl.GeoServerRole)
     */
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getParentRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoleByName(java.lang.String)
     */
    public GeoServerRole getRoleByName(String role) throws IOException {
        readLock();
        try {
            return getService().getRoleByName(role);
        } finally {
            readUnLock();
        }
    }

    /** Fire {@link RoleLoadedEvent} for all listeners */
    protected void fireRoleChangedEvent() {
        RoleLoadedEvent event = new RoleLoadedEvent(this);
        for (RoleLoadedListener listener : listeners) {
            listener.rolesChanged(event);
        }
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * NO_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /** NO_LOCK */
    public void rolesChanged(RoleLoadedEvent event) {
        // release the locks to avoid deadlock situations
        //        if (rwl.isWriteLockedByCurrentThread())
        //            writeUnLock();
        //        else
        //            readUnLock();
        fireRoleChangedEvent();
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getGroupNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getGroupNamesForRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoServerRole)
     */
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        readLock();
        try {
            return getService().getUserNamesForRole(role);
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getParentMappings()
     */
    public Map<String, String> getParentMappings() throws IOException {
        readLock();
        try {
            return getService().getParentMappings();
        } finally {
            readUnLock();
        }
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String,
     *     java.util.Properties, java.lang.String, java.util.Properties)
     */
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {

        readLock();
        try {
            return getService().personalizeRoleParams(roleName, roleParams, userName, userProps);
        } finally {
            readUnLock();
        }
    }

    /**
     * WRITE_LOCK
     *
     * @see
     *     org.geoserver.security.GeoServerRoleService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
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

    /** NO_LOCK */
    @Override
    public GeoServerRole getAdminRole() {
        return getService().getAdminRole();
    }

    /** NO_LOCK */
    @Override
    public GeoServerRole getGroupAdminRole() {
        return getService().getGroupAdminRole();
    }

    /**
     * READ_LOCK
     *
     * @see org.geoserver.security.GeoServerRoleService#getRoleCount()
     */
    public int getRoleCount() throws IOException {
        readLock();
        try {
            return getService().getRoleCount();
        } finally {
            readUnLock();
        }
    }
}
