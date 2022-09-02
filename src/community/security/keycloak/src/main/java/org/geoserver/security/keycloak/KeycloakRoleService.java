/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.util.StringUtils;

/** Keycloak implementation of {@link org.geoserver.security.GeoServerRoleService} */
public class KeycloakRoleService extends AbstractGeoServerSecurityService
        implements GeoServerRoleService {

    protected String adminRoleName, groupAdminRoleName;
    protected SortedSet<GeoServerRole> emptySet;
    protected SortedSet<String> emptyStringSet;
    protected Map<String, String> parentMappings;
    protected SortedSet<GeoServerRole> roleSet;
    protected Set<RoleLoadedListener> listeners = Collections.synchronizedSet(new HashSet<>());

    private KeycloakRESTClient client;

    private LoadingCache<String, SortedSet<GeoServerRole>> userRolesCache;

    private LoadingCache<String, Optional<GeoServerRole>> rolesCache;

    private LoadingCache<String, SortedSet<String>> usersInRoleCache;

    private Integer roleCacheExpiresAfter = 60;

    public KeycloakRoleService() throws IOException {
        emptySet = Collections.unmodifiableSortedSet(new TreeSet<>());
        emptyStringSet = Collections.unmodifiableSortedSet(new TreeSet<>());
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        adminRoleName = ((SecurityRoleServiceConfig) config).getAdminRoleName();
        groupAdminRoleName = ((SecurityRoleServiceConfig) config).getGroupAdminRoleName();

        if (config instanceof KeycloakRoleServiceConfig) {
            KeycloakRoleServiceConfig keycloakConfig = (KeycloakRoleServiceConfig) config;
            List<String> idsOfClients = null;
            if (!StringUtils.isEmpty(keycloakConfig.getIdsOfClientsList())) {
                idsOfClients = Arrays.asList(keycloakConfig.getIdsOfClientsList().split(","));
            }
            this.client =
                    new KeycloakRESTClient(
                            keycloakConfig.getServerURL(),
                            keycloakConfig.getRealm(),
                            keycloakConfig.getClientID(),
                            keycloakConfig.getClientSecret(),
                            idsOfClients);
        }
        this.rolesCache = buildCache(new RoleCacheLoader(), roleCacheExpiresAfter);
        this.userRolesCache = buildCache(new UserRoleCacheLoader(), roleCacheExpiresAfter);
        this.usersInRoleCache = buildCache(new RoleUsersCacheLoader(), roleCacheExpiresAfter);
        load();
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
        return null;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#registerRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    @Override
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#unregisterRoleLoadedListener(org.geoserver.security.event.RoleLoadedListener)
     */
    @Override
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoles()
     */
    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        if (roleSet != null) {
            return roleSet;
        }
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#load()
     */
    @Override
    public synchronized void load() throws IOException {
        LOGGER.config("Start reloading roles for service named " + getName());
        roleSet = new TreeSet<>();
        roleSet.addAll(client.getRoles());
        LOGGER.config("Reloading roles successful for service named " + getName());
        fireRoleLoadedEvent();
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForUser(java.lang.String)
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        SortedSet<GeoServerRole> roles = null;
        try {
            roles = userRolesCache.get(username);
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error while getting roles of user.", e.getCause());
            throw new RuntimeException(e);
        }
        if (roles == null) roles = emptySet;
        return roles;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForGroup(java.lang.String)
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#createRoleObject(java.lang.String)
     */
    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(role);
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentRole(org.geoserver.security.impl.GeoserverRole)
     */
    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRoleByName(java.lang.String)
     */
    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        try {
            Optional<GeoServerRole> op = rolesCache.get(role);
            if (op.isPresent()) return op.get();
            return null;
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Exception while retrieving role by name,", e.getCause());
            throw new RuntimeException(e);
        }
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
    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getUserNamesForRole(org.geoserver.security.impl.GeoserverRole)
     */
    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        try {
            return usersInRoleCache.get(role.getAuthority());
        } catch (ExecutionException e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while retrieving user names in role " + role.getAuthority(),
                    e.getCause());
            throw new RuntimeException(e);
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getParentMappings()
     */
    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return parentMappings;
    }

    /* (non-Javadoc)
     *
     * @see org.geoserver.security.GeoServerRoleService#personalizeRoleParams(java.lang.String,
     * java.util.Properties, java.lang.String, java.util.Properties)
     */
    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return null;
    }

    /** The root configuration for the role service. */
    public Resource getConfigRoot() throws IOException {
        return getSecurityManager().role().get(getName());
    }

    @Override
    public int getRoleCount() throws IOException {
        if (roleSet != null) return roleSet.size();
        return 0;
    }

    private <T> LoadingCache<String, T> buildCache(
            CacheLoader<String, T> cacheLoader, Integer rolesCacheExpireAfter) {
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .initialCapacity(100)
                .expireAfterAccess(rolesCacheExpireAfter, TimeUnit.MINUTES)
                .build(cacheLoader);
    }

    private class RoleCacheLoader extends CacheLoader<String, Optional<GeoServerRole>> {

        @Override
        public Optional<GeoServerRole> load(String s) throws Exception {
            return Optional.ofNullable(client.getRoleByName(s));
        }
    }

    private class UserRoleCacheLoader extends CacheLoader<String, SortedSet<GeoServerRole>> {

        @Override
        public SortedSet<GeoServerRole> load(String key) {
            try {
                return client.getUserRoles(key);
            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error while retrieve user " + key + " roles from keycloak.",
                        e);
            }
            return emptySet;
        }
    }

    private class RoleUsersCacheLoader extends CacheLoader<String, SortedSet<String>> {

        @Override
        public SortedSet<String> load(String key) {
            try {
                return client.getUserInRole(key);
            } catch (IOException e) {
                LOGGER.log(
                        Level.SEVERE,
                        "Error while retrieve users in role " + key + " from keycloak.",
                        e);
            }
            return emptyStringSet;
        }
    }
}
