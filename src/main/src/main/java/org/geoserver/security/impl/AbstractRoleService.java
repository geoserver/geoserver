/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.springframework.util.StringUtils;

/**
 * Default in memory implementation for {@link GeoServerRoleService}
 *
 * @author Christian
 */
public abstract class AbstractRoleService extends AbstractGeoServerSecurityService
        implements GeoServerRoleService {

    protected String adminRoleName, groupAdminRoleName;
    protected RoleStoreHelper helper;

    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    protected AbstractRoleService() {
        helper = new RoleStoreHelper();
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        this.name = config.getName();
        adminRoleName = ((SecurityRoleServiceConfig) config).getAdminRoleName();
        groupAdminRoleName = ((SecurityRoleServiceConfig) config).getGroupAdminRoleName();
    }

    @Override
    public GeoServerRole getAdminRole() {
        if (StringUtils.hasLength(adminRoleName) == false) return null;
        try {
            return getRoleByName(adminRoleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        if (StringUtils.hasLength(groupAdminRoleName) == false) return null;
        try {
            return getRoleByName(groupAdminRoleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRoleStore createStore() throws IOException {
        // return null, subclasses can override if they support a store along with a service
        return null;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoles()
     */
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return helper.getRoles();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#load()
     */
    public void load() throws IOException {
        LOGGER.info("Start reloading roles for service named " + getName());
        // prevent concurrent write from store and
        // read from service
        synchronized (this) {
            deserialize();
        }
        LOGGER.info("Reloading roles successful for service named " + getName());
        fireRoleLoadedEvent();
    }

    /** Subclasses must implement this method Load role assignments from backend */
    protected abstract void deserialize() throws IOException;

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForUser(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        return helper.getRolesForUser(username);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForGroup(java.lang.String)
     */
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return helper.getRolesForGroup(groupname);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#createRoleObject(java.lang.String)
     */
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(role);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentRole(org.geoserver.security.impl.GeoserverRole)
     */
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return helper.getParentRole(role);
    }

    //    protected void checkRole(GeoserverRole role) {
    //        if (roleMap.containsKey(role.getAuthority())==false)
    //            throw new IllegalArgumentException("Role: " +  role.getAuthority()+ " does not
    // exist");
    //    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoleByName(java.lang.String)
     */
    public GeoServerRole getRoleByName(String role) throws IOException {
        return helper.getRoleByName(role);
    }

    /** Fire {@link RoleLoadedEvent} for all listeners */
    protected void fireRoleLoadedEvent() {
        RoleLoadedEvent event = new RoleLoadedEvent(this);
        for (RoleLoadedListener listener : listeners) {
            listener.rolesChanged(event);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getGroupNamesForRole(org.geoserver.security.impl.GeoserverRole)
     */
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return helper.getGroupNamesForRole(role);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoserverRole)
     */
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        return helper.getUserNamesForRole(role);
    }

    /** internal use, clear the maps */
    protected void clearMaps() {
        helper.clearMaps();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentMappings()
     */
    public Map<String, String> getParentMappings() throws IOException {
        return helper.getParentMappings();
    }

    /**
     * (non-Javadoc)
     *
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String,
     *     java.util.Properties, java.lang.String, java.util.Properties)
     *     <p>Default implementation: if a user property name equals a role property name, take the
     *     value from to user property and use it for the role property.
     */
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        Properties props = null;

        // this is true if the set is modified --> common
        // property names exist

        props = new Properties();
        boolean personalized = false;

        for (Object key : roleParams.keySet()) {
            if (userProps.containsKey(key)) {
                props.put(key, userProps.get(key));
                personalized = true;
            } else {
                props.put(key, roleParams.get(key));
            }
        }
        return personalized ? props : null;
    }

    /** The root configuration for the role service. */
    public Resource getConfigRoot() throws IOException {
        return getSecurityManager().role().get(getName());
    }

    public int getRoleCount() throws IOException {
        return helper.getRoleCount();
    }
}
