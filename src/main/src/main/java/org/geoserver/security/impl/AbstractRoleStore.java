/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Logger;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;

/**
 * Abstract base class for role store implementations
 *
 * @author christian
 */
public abstract class AbstractRoleStore implements GeoServerRoleStore {

    /** logger */
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.geoserver.security");

    private boolean modified = false;
    protected AbstractRoleService service;
    protected RoleStoreHelper helper;

    public AbstractRoleStore() {
        helper = new RoleStoreHelper();
    }

    public String getName() {
        return service.getName();
    }

    public void setName(String name) {
        service.setName(name);
    }

    public GeoServerSecurityManager getSecurityManager() {
        return service.getSecurityManager();
    }

    public void setSecurityManager(GeoServerSecurityManager securityManager) {
        service.setSecurityManager(securityManager);
    }

    public boolean canCreateStore() {
        return service.canCreateStore();
    }

    public GeoServerRole getAdminRole() {
        return service.getAdminRole();
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        return service.getGroupAdminRole();
    }

    public GeoServerRoleStore createStore() throws IOException {
        return service.createStore();
    }

    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        service.registerRoleLoadedListener(listener);
    }

    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        service.unregisterRoleLoadedListener(listener);
    }

    public GeoServerRole createRoleObject(String role) throws IOException {
        return service.createRoleObject(role);
    }

    public Resource getConfigRoot() throws IOException {
        return service.getConfigRoot();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#isModified()
     */
    public boolean isModified() {
        return modified;
    }

    /** Setter for modified flag */
    public void setModified(Boolean value) {
        modified = value;
    }
    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#addRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void addRole(GeoServerRole role) throws IOException {

        if (helper.roleMap.containsKey(role.getAuthority()))
            throw new IllegalArgumentException(
                    "The role " + role.getAuthority() + " already exists");
        else {
            helper.roleMap.put(role.getAuthority(), role);
            setModified(true);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#updateRole(org.geoserver.security.impl.GeoserverRole)
     */
    public void updateRole(GeoServerRole role) throws IOException {

        if (helper.roleMap.containsKey(role.getAuthority())) {
            helper.roleMap.put(role.getAuthority(), role);
            setModified(true);
        } else
            throw new IllegalArgumentException(
                    "The role " + role.getAuthority() + " does not exist");
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#removeRole(org.geoserver.security.impl.GeoserverRole)
     */
    public boolean removeRole(GeoServerRole role) throws IOException {

        if (helper.roleMap.containsKey(role.getAuthority()) == false) // nothing to do
        return false;

        for (SortedSet<GeoServerRole> set : helper.user_roleMap.values()) {
            set.remove(role);
        }
        for (SortedSet<GeoServerRole> set : helper.group_roleMap.values()) {
            set.remove(role);
        }

        // role hierarchy
        helper.role_parentMap.remove(role);
        Set<GeoServerRole> toBeRemoved = new HashSet<GeoServerRole>();
        for (Entry<GeoServerRole, GeoServerRole> entry : helper.role_parentMap.entrySet()) {
            if (role.equals(entry.getValue())) toBeRemoved.add(entry.getKey());
        }
        for (GeoServerRole ga : toBeRemoved) {
            helper.role_parentMap.put(ga, null);
        }

        // remove role
        helper.roleMap.remove(role.getAuthority());
        setModified(true);
        return true;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#store()
     */
    public void store() throws IOException {
        if (isModified()) {
            LOGGER.info("Start storing roles for service named " + getName());
            // prevent concurrent write from store and
            // read from service
            synchronized (service) {
                serialize();
            }
            setModified(false);
            LOGGER.info("Storing roles successful for service named " + getName());
            service.load(); // service must reload
        } else {
            LOGGER.info("Storing unnecessary, no change for roles");
        }
    }

    /** Subclasses must implement this method Save role assignments to backend */
    protected abstract void serialize() throws IOException;

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromGroup(GeoServerRole role, String groupname) throws IOException {
        SortedSet<GeoServerRole> roles = helper.group_roleMap.get(groupname);
        if (roles != null && roles.contains(role)) {
            roles.remove(role);
            setModified(true);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToGroup(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToGroup(GeoServerRole role, String groupname) throws IOException {
        SortedSet<GeoServerRole> roles = helper.group_roleMap.get(groupname);
        if (roles == null) {
            roles = new TreeSet<GeoServerRole>();
            helper.group_roleMap.put(groupname, roles);
        }
        if (roles.contains(role) == false) { // something changed ?
            roles.add(role);
            setModified(true);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#associateRoleToUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void associateRoleToUser(GeoServerRole role, String username) throws IOException {
        SortedSet<GeoServerRole> roles = helper.user_roleMap.get(username);
        if (roles == null) {
            roles = new TreeSet<GeoServerRole>();
            helper.user_roleMap.put(username, roles);
        }
        if (roles.contains(role) == false) { // something changed
            roles.add(role);
            setModified(true);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#disAssociateRoleFromUser(org.geoserver.security.impl.GeoserverRole, java.lang.String)
     */
    public void disAssociateRoleFromUser(GeoServerRole role, String username) throws IOException {
        SortedSet<GeoServerRole> roles = helper.user_roleMap.get(username);
        if (roles != null && roles.contains(role)) {
            roles.remove(role);
            setModified(true);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#setParentRole(org.geoserver.security.impl.GeoserverRole, org.geoserver.security.impl.GeoserverRole)
     */
    public void setParentRole(GeoServerRole role, GeoServerRole parentRole) throws IOException {

        RoleHierarchyHelper hhelper = new RoleHierarchyHelper(getParentMappings());
        if (hhelper.isValidParent(
                        role.getAuthority(), parentRole == null ? null : parentRole.getAuthority())
                == false)
            throw new IOException(
                    parentRole.getAuthority()
                            + " is not a valid parent for "
                            + role.getAuthority());

        checkRole(role);
        if (parentRole == null) {
            helper.role_parentMap.remove(role);
        } else {
            checkRole(parentRole);
            helper.role_parentMap.put(role, parentRole);
        }
        setModified(true);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#clear()
     */
    public void clear() throws IOException {
        clearMaps();
        setModified(true);
    }

    /**
     * Make a deep copy (using serialization) from the service to the store.
     *
     * @see org.geoserver.security.impl.AbstractRoleService#deserialize()
     */
    @SuppressWarnings("unchecked")
    protected void deserialize() throws IOException {

        // make a deep copy of the maps using serialization

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream oout = new ObjectOutputStream(out);
        oout.writeObject(service.helper.roleMap);
        oout.writeObject(service.helper.role_parentMap);
        oout.writeObject(service.helper.user_roleMap);
        oout.writeObject(service.helper.group_roleMap);
        byte[] byteArray = out.toByteArray();
        oout.close();

        clearMaps();
        ByteArrayInputStream in = new ByteArrayInputStream(byteArray);
        ObjectInputStream oin = new ObjectInputStream(in);
        try {
            helper.roleMap = (TreeMap<String, GeoServerRole>) oin.readObject();
            helper.role_parentMap = (HashMap<GeoServerRole, GeoServerRole>) oin.readObject();
            helper.user_roleMap = (TreeMap<String, SortedSet<GeoServerRole>>) oin.readObject();
            helper.group_roleMap = (TreeMap<String, SortedSet<GeoServerRole>>) oin.readObject();
        } catch (ClassNotFoundException e) {
            throw new IOException(e);
        }
        setModified(false);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleStore#initializeFromService(org.geoserver.security.GeoserverRoleService)
     */
    @Override
    public void initializeFromService(GeoServerRoleService service) throws IOException {
        this.service = (AbstractRoleService) service;
        load();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoServerSecurityService#initializeFromConfig(org.geoserver.security.config.SecurityNamedServiceConfig)
     */
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        service.initializeFromConfig(config);
    }

    protected void checkRole(GeoServerRole role) {
        if (helper.roleMap.containsKey(role.getAuthority()) == false)
            throw new IllegalArgumentException("Role: " + role.getAuthority() + " does not exist");
    }

    /** internal use, clear the maps */
    protected void clearMaps() {
        helper.clearMaps();
    }

    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return helper.getGroupNamesForRole(role);
    }

    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        return helper.getUserNamesForRole(role);
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        return helper.getRolesForUser(username);
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return helper.getRolesForGroup(groupname);
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return helper.getRoles();
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return helper.getParentMappings();
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return helper.getParentRole(role);
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        return helper.getRoleByName(role);
    }

    @Override
    public void load() throws IOException {
        deserialize();
    }

    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return service.personalizeRoleParams(roleName, roleParams, userName, userProps);
    }

    public int getRoleCount() throws IOException {
        return helper.getRoleCount();
    }
}
