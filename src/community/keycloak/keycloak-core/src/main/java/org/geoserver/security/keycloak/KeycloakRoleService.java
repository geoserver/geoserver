/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
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
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityRoleServiceConfig;
import org.geoserver.security.event.RoleLoadedEvent;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;

/**
 * {@link GeoServerRoleService} backed by the Keycloak Admin REST API.
 *
 * <p>Roles and user-role mappings are fetched directly from Keycloak rather than extracted from tokens. This allows
 * GeoServer's role administration UI to reflect Keycloak realm and client roles without relying solely on token claims.
 *
 * <p>Results are cached with a default TTL of {@value #DEFAULT_CACHE_EXPIRE_MINUTES} minutes.
 */
public class KeycloakRoleService extends AbstractGeoServerSecurityService implements GeoServerRoleService {

    private static final int DEFAULT_CACHE_EXPIRE_MINUTES = 60;

    protected String adminRoleName;
    protected String groupAdminRoleName;
    protected final SortedSet<GeoServerRole> emptySet = Collections.unmodifiableSortedSet(new TreeSet<>());
    protected final SortedSet<String> emptyStringSet = Collections.unmodifiableSortedSet(new TreeSet<>());
    protected Map<String, String> parentMappings;
    protected SortedSet<GeoServerRole> roleSet;
    protected final Set<RoleLoadedListener> listeners = Collections.synchronizedSet(new HashSet<>());

    private KeycloakRESTClient client;
    private LoadingCache<String, SortedSet<GeoServerRole>> userRolesCache;
    private LoadingCache<String, Optional<GeoServerRole>> rolesCache;
    private LoadingCache<String, SortedSet<String>> usersInRoleCache;

    public KeycloakRoleService() throws IOException {}

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        adminRoleName = ((SecurityRoleServiceConfig) config).getAdminRoleName();
        groupAdminRoleName = ((SecurityRoleServiceConfig) config).getGroupAdminRoleName();

        if (config instanceof KeycloakRoleServiceConfig keycloakConfig) {
            List<String> idsOfClients = null;
            if (!ObjectUtils.isEmpty(keycloakConfig.getIdsOfClientsList())) {
                idsOfClients =
                        Arrays.asList(keycloakConfig.getIdsOfClientsList().split(","));
            }
            this.client = new KeycloakRESTClient(
                    keycloakConfig.getServerURL(),
                    keycloakConfig.getRealm(),
                    keycloakConfig.getClientID(),
                    keycloakConfig.getClientSecret(),
                    idsOfClients);
        }
        this.rolesCache = buildCache(new RoleCacheLoader(), DEFAULT_CACHE_EXPIRE_MINUTES);
        this.userRolesCache = buildCache(new UserRoleCacheLoader(), DEFAULT_CACHE_EXPIRE_MINUTES);
        this.usersInRoleCache = buildCache(new RoleUsersCacheLoader(), DEFAULT_CACHE_EXPIRE_MINUTES);
        load();
    }

    @Override
    public GeoServerRole getAdminRole() {
        if (!StringUtils.hasLength(adminRoleName)) return null;
        try {
            return getRoleByName(adminRoleName);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        if (!StringUtils.hasLength(groupAdminRoleName)) return null;
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

    @Override
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    @Override
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        return roleSet != null ? roleSet : emptySet;
    }

    @Override
    public synchronized void load() throws IOException {
        LOGGER.config("Start reloading roles for service named " + getName());
        roleSet = new TreeSet<>(client.getRoles());
        LOGGER.config("Reloading roles successful for service named " + getName());
        fireRoleLoadedEvent();
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        try {
            SortedSet<GeoServerRole> roles = userRolesCache.get(username);
            return roles != null ? roles : emptySet;
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error while getting roles of user.", e.getCause());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        return emptySet;
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(role);
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        try {
            return rolesCache.get(role).orElse(null);
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Exception while retrieving role by name.", e.getCause());
            throw new RuntimeException(e);
        }
    }

    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        try {
            return usersInRoleCache.get(role.getAuthority());
        } catch (ExecutionException e) {
            LOGGER.log(Level.SEVERE, "Error while retrieving user names in role " + role.getAuthority(), e.getCause());
            throw new RuntimeException(e);
        }
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return parentMappings;
    }

    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps) throws IOException {
        return null;
    }

    @Override
    public int getRoleCount() throws IOException {
        return roleSet != null ? roleSet.size() : 0;
    }

    protected void fireRoleLoadedEvent() {
        RoleLoadedEvent event = new RoleLoadedEvent(this);
        for (RoleLoadedListener listener : listeners) {
            listener.rolesChanged(event);
        }
    }

    private <T> LoadingCache<String, T> buildCache(CacheLoader<String, T> loader, int expireAfterMinutes) {
        return CacheBuilder.newBuilder()
                .maximumSize(1000)
                .initialCapacity(100)
                .expireAfterAccess(expireAfterMinutes, TimeUnit.MINUTES)
                .build(loader);
    }

    private class RoleCacheLoader extends CacheLoader<String, Optional<GeoServerRole>> {
        @Override
        public Optional<GeoServerRole> load(String roleName) throws Exception {
            return Optional.ofNullable(client.getRoleByName(roleName));
        }
    }

    private class UserRoleCacheLoader extends CacheLoader<String, SortedSet<GeoServerRole>> {
        @Override
        public SortedSet<GeoServerRole> load(String username) {
            try {
                return client.getUserRoles(username);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving roles for user " + username + " from Keycloak.", e);
            }
            return emptySet;
        }
    }

    private class RoleUsersCacheLoader extends CacheLoader<String, SortedSet<String>> {
        @Override
        public SortedSet<String> load(String roleName) {
            try {
                return client.getUserInRole(roleName);
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error retrieving users in role " + roleName + " from Keycloak.", e);
            }
            return emptyStringSet;
        }
    }
}
