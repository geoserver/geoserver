/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.PathNotFoundException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import net.minidev.json.JSONArray;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/** @author Alessio Fabiani, GeoSolutions S.A.S. */
public class GeoServerRestRoleService extends AbstractGeoServerSecurityService
        implements GeoServerRoleService {

    static final SortedSet<String> emptyStringSet =
            Collections.unmodifiableSortedSet(new TreeSet<String>());

    static final Map<String, String> emptyMap = Collections.emptyMap();

    static Cache<String, String> cachedResponses;

    /**
     * Sets a specified timeout value, in milliseconds, to be used when opening a communications
     * link to the resource referenced by this URLConnection. If the timeout expires before the
     * connection can be established, a {@link java.net.SocketTimeoutException} is raised.
     *
     * <p>A timeout of zero is interpreted as an infinite timeout.
     *
     * <p>Some non-standard implementation of this method may ignore the specified timeout. To see
     * the connect timeout set, please call getConnectTimeout().
     *
     * @param timeout an int that specifies the connect timeout value in milliseconds
     * @throws {@link IllegalArgumentException} - if the timeout parameter is negative
     */
    private static final int CONN_TIMEOUT = 30000;

    /**
     * Sets the read timeout to a specified timeout, in milliseconds. A non-zero value specifies the
     * timeout when reading from Input stream when a connection is established to a resource. If the
     * timeout expires before there is data available for read, a {@link
     * java.net.SocketTimeoutException} is raised.
     *
     * <p>A timeout of zero is interpreted as an infinite timeout.
     *
     * <p>Some non-standard implementation of this method may ignore the specified timeout. To see
     * the read timeout set, please call getReadTimeout().
     *
     * @param timeout an int that specifies the timeout value to be used in milliseconds
     * @throws {@link IllegalArgumentException} - if the timeout parameter is negative
     */
    private static final int READ_TIMEOUT = 30000;

    private RestTemplate restTemplate;

    private static String rolePrefix = "ROLE_";

    static boolean isEmpty(String property) {
        return property == null || property.isEmpty();
    }

    GeoServerRestRoleServiceConfig restRoleServiceConfig;

    private boolean convertToUpperCase = true;

    private String adminGroup;

    private String groupAdminGroup;

    protected Set<RoleLoadedListener> listeners =
            Collections.synchronizedSet(new HashSet<RoleLoadedListener>());

    /** Default Constructor */
    public GeoServerRestRoleService(SecurityNamedServiceConfig config) throws IOException {
        initializeFromConfig(config);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);
        restRoleServiceConfig = (GeoServerRestRoleServiceConfig) config;
        if (!isEmpty(restRoleServiceConfig.getAdminRoleName())) {
            this.adminGroup = restRoleServiceConfig.getAdminRoleName();
        }
        if (!isEmpty(restRoleServiceConfig.getGroupAdminRoleName())) {
            this.groupAdminGroup = restRoleServiceConfig.getGroupAdminRoleName();
        }

        cachedResponses =
                CacheBuilder.newBuilder()
                        .concurrencyLevel(restRoleServiceConfig.getCacheConcurrencyLevel())
                        .maximumSize(restRoleServiceConfig.getCacheMaximumSize())
                        .expireAfterWrite(
                                restRoleServiceConfig.getCacheExpirationTime(),
                                TimeUnit.MILLISECONDS)
                        .build(); // look Ma, no CacheLoader
    }

    /** Read only store. */
    @Override
    public boolean canCreateStore() {
        return false;
    }

    /** Read only store. */
    @Override
    public GeoServerRoleStore createStore() throws IOException {
        return null;
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerRoleService#registerRoleLoadedListener(RoleLoadedListener)
     */
    public void registerRoleLoadedListener(RoleLoadedListener listener) {
        listeners.add(listener);
    }

    /**
     * @see
     *     org.geoserver.security.GeoServerRoleService#unregisterRoleLoadedListener(RoleLoadedListener)
     */
    public void unregisterRoleLoadedListener(RoleLoadedListener listener) {
        listeners.remove(listener);
    }

    /** Roles to group association is not supported */
    @Override
    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        return emptyStringSet;
    }

    @Override
    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        final SortedSet<String> users = new TreeSet<String>();

        return Collections.unmodifiableSortedSet(users);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();

        try {
            return (SortedSet<GeoServerRole>)
                    connectToRESTEndpoint(
                            restRoleServiceConfig.getBaseUrl(),
                            restRoleServiceConfig.getUsersRESTEndpoint() + "/" + username,
                            restRoleServiceConfig
                                    .getUsersJSONPath()
                                    .replace("${username}", username),
                            restRoleServiceConfig.getAuthApiKey(),
                            new RestEndpointConnectionCallback() {

                                @Override
                                public Object executeWithContext(String json) throws Exception {
                                    try {
                                        List<Object> rolesString =
                                                JsonPath.read(
                                                        json,
                                                        restRoleServiceConfig
                                                                .getUsersJSONPath()
                                                                .replace("${username}", username));

                                        for (Object roleObj : rolesString) {
                                            if (roleObj instanceof String) {
                                                populateRoles((String) roleObj, roles);
                                            } else if (roleObj instanceof JSONArray) {
                                                for (Object role : ((JSONArray) roleObj)) {
                                                    populateRoles((String) role, roles);
                                                }
                                            }
                                        }
                                    } catch (PathNotFoundException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                        roles.clear();
                                        roles.add(GeoServerRole.AUTHENTICATED_ROLE);
                                    }

                                    SortedSet<GeoServerRole> finalRoles =
                                            Collections.unmodifiableSortedSet(
                                                    fixGeoServerRoles(roles));

                                    if (LOGGER.isLoggable(Level.FINE)) {
                                        LOGGER.fine(
                                                "Setting ROLES for User ["
                                                        + username
                                                        + "] to "
                                                        + finalRoles);
                                    }

                                    return finalRoles;
                                }

                                private void populateRoles(
                                        String role, final SortedSet<GeoServerRole> roles)
                                        throws IOException {
                                    if (role.startsWith(rolePrefix)) {
                                        // remove standard role prefix
                                        role = role.substring(rolePrefix.length());
                                    }

                                    roles.add(createRoleObject(role));
                                }
                            });
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.FINEST, null, ex);
        }

        return Collections.unmodifiableSortedSet(roles);
    }

    protected SortedSet<GeoServerRole> fixGeoServerRoles(SortedSet<GeoServerRole> roles) {
        // Check if is an ADMIN
        GeoServerRole adminRole = getAdminRole();
        if (roles.contains(GeoServerRole.ADMIN_ROLE) || roles.contains(adminRole)) {
            roles.clear();
            roles.add(GeoServerRole.ADMIN_ROLE);
        }

        // Check if Role Anonymous is present other than other roles
        if (roles.size() > 1 && roles.contains(GeoServerRole.ANONYMOUS_ROLE)) {
            roles.remove(GeoServerRole.ANONYMOUS_ROLE);
        }

        return roles;
    }

    @Override
    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        SortedSet<GeoServerRole> set = new TreeSet<GeoServerRole>();
        GeoServerRole role = getRoleByName(groupname);
        if (role != null) {
            set.add(role);
        }

        return Collections.unmodifiableSortedSet(set);
    }

    @SuppressWarnings("unchecked")
    @Override
    public SortedSet<GeoServerRole> getRoles() throws IOException {
        final SortedSet<GeoServerRole> roles = new TreeSet<GeoServerRole>();

        try {
            return (SortedSet<GeoServerRole>)
                    connectToRESTEndpoint(
                            restRoleServiceConfig.getBaseUrl(),
                            restRoleServiceConfig.getRolesRESTEndpoint(),
                            restRoleServiceConfig.getRolesJSONPath(),
                            restRoleServiceConfig.getAuthApiKey(),
                            new RestEndpointConnectionCallback() {

                                @Override
                                public Object executeWithContext(String json) throws Exception {
                                    try {
                                        List<String> rolesString =
                                                JsonPath.read(
                                                        json,
                                                        restRoleServiceConfig.getRolesJSONPath());

                                        for (String role : rolesString) {
                                            if (role.startsWith(rolePrefix)) {
                                                // remove standard role prefix
                                                role = role.substring(rolePrefix.length());
                                            }

                                            roles.add(createRoleObject(role));
                                        }
                                    } catch (PathNotFoundException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                    }

                                    return Collections.unmodifiableSortedSet(roles);
                                }
                            });
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.FINEST, null, ex);
        }

        return Collections.unmodifiableSortedSet(roles);
    }

    @Override
    public Map<String, String> getParentMappings() throws IOException {
        return emptyMap;
    }

    @Override
    public GeoServerRole createRoleObject(String role) throws IOException {
        return new GeoServerRole(rolePrefix + (convertToUpperCase ? role.toUpperCase() : role));
    }

    @Override
    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getRoleByName(String role) throws IOException {
        if (role.startsWith(rolePrefix)) {
            // remove standard role prefix
            role = role.substring(rolePrefix.length());
        }
        final String roleName = role;

        try {
            return (GeoServerRole)
                    connectToRESTEndpoint(
                            restRoleServiceConfig.getBaseUrl(),
                            restRoleServiceConfig.getRolesRESTEndpoint(),
                            restRoleServiceConfig.getRolesJSONPath(),
                            restRoleServiceConfig.getAuthApiKey(),
                            new RestEndpointConnectionCallback() {

                                @Override
                                public Object executeWithContext(String json) throws Exception {
                                    try {
                                        List<String> rolesString =
                                                JsonPath.read(
                                                        json,
                                                        restRoleServiceConfig.getRolesJSONPath());

                                        for (String targetRole : rolesString) {
                                            if (targetRole.startsWith(rolePrefix)) {
                                                // remove standard role prefix
                                                targetRole =
                                                        targetRole.substring(rolePrefix.length());
                                            }

                                            if (roleName.equalsIgnoreCase(targetRole)) {
                                                return createRoleObject(roleName);
                                            }
                                        }
                                    } catch (PathNotFoundException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                    }

                                    return null;
                                }
                            });
        } catch (Exception ex) {
            Logger.getLogger(getClass().getName()).log(Level.FINEST, null, ex);
        }

        return null;
    }

    @Override
    public void load() throws IOException {
        // Not needed
    }

    @Override
    public Properties personalizeRoleParams(
            String roleName, Properties roleParams, String userName, Properties userProps)
            throws IOException {
        return null;
    }

    @Override
    public GeoServerRole getAdminRole() {
        if (adminGroup == null) {
            try {
                return (GeoServerRole)
                        connectToRESTEndpoint(
                                restRoleServiceConfig.getBaseUrl(),
                                restRoleServiceConfig.getAdminRoleRESTEndpoint(),
                                restRoleServiceConfig.getAdminRoleJSONPath(),
                                restRoleServiceConfig.getAuthApiKey(),
                                new RestEndpointConnectionCallback() {

                                    @Override
                                    public Object executeWithContext(String json) throws Exception {
                                        try {
                                            String targetRole =
                                                    JsonPath.read(
                                                            json,
                                                            restRoleServiceConfig
                                                                    .getAdminRoleJSONPath());

                                            if (targetRole.startsWith(rolePrefix)) {
                                                // remove standard role prefix
                                                targetRole =
                                                        targetRole.substring(rolePrefix.length());
                                            }

                                            return createRoleObject(targetRole);
                                        } catch (PathNotFoundException ex) {
                                            Logger.getLogger(getClass().getName())
                                                    .log(Level.FINEST, null, ex);
                                        }

                                        return null;
                                    }
                                });
            } catch (Exception ex) {
                Logger.getLogger(getClass().getName()).log(Level.FINEST, null, ex);
            }
        }

        try {
            return getRoleByName(adminGroup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public GeoServerRole getGroupAdminRole() {
        if (groupAdminGroup == null) {
            return getAdminRole();
        }
        try {
            return getRoleByName(groupAdminGroup);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public int getRoleCount() throws IOException {
        return getRoles().size();
    }

    /** @return the restTemplate */
    public RestTemplate getRestTemplate() {
        if (restTemplate == null) {
            restTemplate = restTemplate();
        }

        return restTemplate;
    }

    /** @param restTemplate the restTemplate to set */
    public void setRestTemplate(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    private RestTemplate restTemplate() {
        return new RestTemplate(clientHttpRequestFactory());
    }

    private ClientHttpRequestFactory clientHttpRequestFactory() {
        HttpComponentsClientHttpRequestFactory factory =
                new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(READ_TIMEOUT);
        factory.setConnectTimeout(CONN_TIMEOUT);
        return factory;
    }

    /** Execute REST CALL, and then call the given callback on HTTP JSON Response. */
    protected Object connectToRESTEndpoint(
            final String roleRESTBaseURL,
            final String roleRESTEndpoint,
            final String roleJSONPath,
            final String authApiKey,
            RestEndpointConnectionCallback callback)
            throws Exception {
        final String restEndPoint = roleRESTBaseURL + roleRESTEndpoint + roleJSONPath;
        // First search on cache
        final String hash = getHash(restEndPoint);

        try {
            // If the key wasn't in the "easy to compute" group, we need to
            // do things the hard way.
            final String cachedResponse =
                    cachedResponses.get(
                            hash,
                            new Callable<String>() {

                                @Override
                                public String call() throws Exception {

                                    LOGGER.fine(
                                            "GeoServer REST Role Service CACHE MISS for '"
                                                    + restEndPoint
                                                    + "'");

                                    ClientHttpRequest clientRequest = null;
                                    ClientHttpResponse clientResponse = null;
                                    try {
                                        final URI baseURI = new URI(roleRESTBaseURL);

                                        URL url = baseURI.resolve(roleRESTEndpoint).toURL();

                                        clientRequest =
                                                getRestTemplate()
                                                        .getRequestFactory()
                                                        .createRequest(url.toURI(), HttpMethod.GET);

                                        if (authApiKey != null) {
                                            clientRequest
                                                    .getHeaders()
                                                    .add("Authorization", "ApiKey " + authApiKey);
                                        }
                                        clientResponse = clientRequest.execute();
                                        int status = clientResponse.getRawStatusCode();

                                        switch (status) {
                                            case 200:
                                            case 201:
                                                BufferedReader br =
                                                        new BufferedReader(
                                                                new InputStreamReader(
                                                                        clientResponse.getBody()));
                                                StringBuilder sb = new StringBuilder();
                                                String line;
                                                while ((line = br.readLine()) != null) {
                                                    sb.append(line + "\n");
                                                }
                                                br.close();

                                                String json = sb.toString();

                                                return json;
                                        }
                                    } catch (MalformedURLException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                    } catch (IOException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                    } catch (URISyntaxException ex) {
                                        Logger.getLogger(getClass().getName())
                                                .log(Level.FINEST, null, ex);
                                    } finally {
                                        if (clientResponse != null) {
                                            try {
                                                clientResponse.close();
                                            } catch (Exception ex) {
                                                Logger.getLogger(getClass().getName())
                                                        .log(Level.SEVERE, null, ex);
                                            }
                                        }
                                    }

                                    return null;
                                }
                            });

            return callback.executeWithContext(cachedResponse);
        } catch (ExecutionException e) {
            LOGGER.log(Level.FINEST, e.getMessage(), e);
            return null;
        }
    }

    private static String getHash(String stringToEncrypt) throws NoSuchAlgorithmException {
        MessageDigest messageDigest = MessageDigest.getInstance("SHA-256");
        messageDigest.update(stringToEncrypt.getBytes());
        final String encryptedString = new String(messageDigest.digest());

        return encryptedString;
    }

    /**
     * Callback interface to be used in the REST call methods for performing operations on
     * individually HTTP JSON responses.
     *
     * @author Alessio Fabiani, GeoSolutions S.A.S.
     */
    interface RestEndpointConnectionCallback {

        /**
         * Perform specific operations accordingly to the caller needs.
         *
         * @param json the <code>JSON</code> string to perform an operation on.
         */
        Object executeWithContext(final String json) throws Exception;
    }
}
