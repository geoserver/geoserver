/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
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
    protected HashMap<String, GeoServerRole> roleMap;
    protected SortedSet<GeoServerRole> roleSet;
    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    private String serverURL;
    private String realm;
    private String clientID;
    private String idOfClient;
    private String clientSecret;

    public KeycloakRoleService() throws IOException {
        emptySet = Collections.unmodifiableSortedSet(new TreeSet<GeoServerRole>());
        emptyStringSet = Collections.unmodifiableSortedSet(new TreeSet<String>());
        parentMappings = new HashMap<String, String>();
        load();
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        adminRoleName = ((SecurityRoleServiceConfig) config).getAdminRoleName();
        groupAdminRoleName = ((SecurityRoleServiceConfig) config).getGroupAdminRoleName();

        if (config instanceof KeycloakSecurityServiceConfig) {
            KeycloakSecurityServiceConfig keycloakConfig = (KeycloakSecurityServiceConfig) config;
            serverURL = keycloakConfig.getServerURL();
            realm = keycloakConfig.getRealm();
            clientID = keycloakConfig.getClientID();
            idOfClient = keycloakConfig.getIdOfClient();
            clientSecret = keycloakConfig.getClientSecret();
        }

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
        if (roleSet != null) return roleSet;
        return emptySet;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#load()
     */
    @Override
    public synchronized void load() throws IOException {
        LOGGER.info("Start reloading roles for service named " + getName());

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            Gson gson = new Gson();
            LOGGER.info("Obtaining access token for Keycloak");
            String accessToken = getAccessToken(httpClient, gson);
            if (accessToken == null || accessToken.trim().isEmpty()) {
                return;
            }

            LOGGER.info("Retrieving roles from Keycloak");
            List<GeoServerRole> roles = getRoles(httpClient, gson, accessToken);
            if (roles == null) {
                return;
            }
            roleSet = new TreeSet<GeoServerRole>();
            roleSet.addAll(roles);

            LOGGER.info("Reloading roles successful for service named " + getName());
            fireRoleLoadedEvent();
        }
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.GeoserverRoleService#getRolesForUser(java.lang.String)
     */
    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        return emptySet;
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
        if (roleMap != null) return roleMap.get(role);
        return null;
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
        return emptyStringSet;
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

    /**
     * Extract the message (body) from the given HTTP response.
     *
     * @param response the CloseableHttpResponse to retrieve the message from
     * @return the content of the response entity, as a String
     * @throws Exception
     */
    private String getStringResponseMessage(CloseableHttpResponse response) throws Exception {
        HttpEntity responseEntity = response.getEntity();
        if (responseEntity == null) {
            throw new NullPointerException("HTTP response from Keycloak contained no message");
        }

        try (InputStream contentStream = responseEntity.getContent()) {
            return IOUtils.toString(contentStream, StandardCharsets.UTF_8);
        }
    }

    /**
     * Obtain an access token from Keycloak, using the server URL, realm name, and client secret
     * from the config. Returns null if an error occurs.
     *
     * @param httpClient a CloseableHttpClient to send the request through
     * @return a String access token on success, null on error.
     */
    public String getAccessToken(CloseableHttpClient httpClient, Gson gson) {
        HttpPost httpPost =
                new HttpPost(
                        this.serverURL
                                + "/auth/realms/"
                                + this.realm
                                + "/protocol/openid-connect/token");
        String body =
                "client_id="
                        + this.clientID
                        + "&client_secret="
                        + this.clientSecret
                        + "&grant_type=client_credentials";
        HttpEntity entity = new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED);
        httpPost.setEntity(entity);

        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine == null || statusLine.getStatusCode() != 200) {
                LOGGER.info("Issue retrieving access token: " + statusLine);
                return null;
            }
            String jsonString = getStringResponseMessage(response);
            Map<?, ?> map = gson.fromJson(jsonString, Map.class);
            return map.get("access_token").toString();
        } catch (Exception exception) {
            LOGGER.info(exception.getMessage());
            return null;
        }
    }

    /**
     * Retrieve a List of GeoServerRole objects from Keyclaok, using the server URL, realm name, and
     * ID of client from the config. Returns null if an error occurs.
     *
     * @param httpClient a CloseableHttpClient to send the request through
     * @param accessToken an access token for the geoserver-client user
     * @return a List of GeoServerRole objects from Keycloak on success, null on error.
     */
    public List<GeoServerRole> getRoles(
            CloseableHttpClient httpClient, Gson gson, String accessToken) {
        HttpGet httpGet =
                new HttpGet(
                        this.serverURL
                                + "/auth/admin/realms/"
                                + this.realm
                                + "/clients/"
                                + this.idOfClient
                                + "/roles");
        httpGet.setHeader("Authorization", "Bearer " + accessToken);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine == null || statusLine.getStatusCode() != 200) {
                LOGGER.info("Issue retrieving roles: " + statusLine);
                return null;
            }
            String jsonString = getStringResponseMessage(response);
            List<GeoServerRole> roles = new ArrayList<>();
            for (Object obj : gson.fromJson(jsonString, List.class)) {
                LinkedTreeMap<?, ?> role = (LinkedTreeMap<?, ?>) obj;
                roles.add(createRoleObject(role.get("name").toString()));
            }
            return roles;
        } catch (Exception exception) {
            LOGGER.info(exception.getMessage());
        }

        return null;
    }
}
