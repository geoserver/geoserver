/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
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
import java.util.List;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.util.StringUtils;

/**
 * Class that provides methods to invoke Keycloak REST api endpoints to retrieve roles and users.
 */
class KeycloakRESTClient {

    private KeycloakUrlBuilder builder;
    private String clientID;
    private String clientSecret;
    private List<String> listOfClientIds;

    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(KeycloakRESTClient.class);

    KeycloakRESTClient(
            String serverUrl,
            String realm,
            String clientID,
            String clientSecret,
            List<String> listOfClientIds) {
        this.builder = new KeycloakUrlBuilder(realm, serverUrl);
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.listOfClientIds = listOfClientIds;
    }

    /**
     * Get all the keycloak realm roles and eventually the client roles if id were configured. It
     * converts keycloak roles to {@link GeoServerRole}
     *
     * @return a SortedSet of {@link GeoServerRole}.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    SortedSet<GeoServerRole> getRoles() throws IOException {
        SortedSet<GeoServerRole> sortedSet = new TreeSet<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            LOGGER.config("Obtaining access token for Keycloak");
            String accessToken = getAccessToken(httpClient);
            if (!StringUtils.isEmpty(accessToken)) {
                LOGGER.config("Retrieving roles from Keycloak");
                List<Object> result =
                        invoke(httpClient, builder.allRoles().build(), accessToken, List.class);
                if (result == null) result = new ArrayList<>();
                result.add(rolesFromClients(httpClient, accessToken));
                sortedSet = toGeoServerRoles(result);
            }
        }
        return sortedSet;
    }

    /**
     * Get the list of the user realm roles. If client ids were configured it will get also the user
     * clients' roles.
     *
     * @param username the username.
     * @return a SortedSet of {@link GeoServerRole}.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    SortedSet<GeoServerRole> getUserRoles(String username) throws IOException {
        SortedSet<GeoServerRole> sortedSet = new TreeSet<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            LOGGER.config("Obtaining access token for Keycloak");
            String accessToken = getAccessToken(httpClient);
            if (!StringUtils.isEmpty(accessToken)) {
                LOGGER.config("Retrieving roles from Keycloak");
                List<Object> result =
                        invoke(
                                httpClient,
                                builder.userByName(username).build(),
                                accessToken,
                                List.class);
                if (result != null && !result.isEmpty()) {
                    LinkedTreeMap<?, ?> user = (LinkedTreeMap<?, ?>) result.get(0);
                    String userId = user.get("id").toString();
                    String realmRolesUrl = builder.userById(userId).allUserRoles(true).build();
                    List<Object> roles = invoke(httpClient, realmRolesUrl, accessToken, List.class);
                    if (roles != null && !roles.isEmpty()) {
                        sortedSet.addAll(toGeoServerRoles(roles));
                    }
                    List<Object> clientRoles =
                            userRolesFromClients(httpClient, accessToken, userId);
                    sortedSet.addAll(toGeoServerRoles(clientRoles));
                }
            }
        }
        return sortedSet;
    }

    /**
     * Get a GeoServer role by name.
     *
     * @param roleName the role name.
     * @return the {@link GeoServerRole} if found. Null otherwise.
     * @throws IOException
     */
    GeoServerRole getRoleByName(String roleName) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            LOGGER.config("Obtaining access token for Keycloak");
            String accessToken = getAccessToken(httpClient);
            if (!StringUtils.isEmpty(accessToken)) {
                LOGGER.config("Retrieving roles from Keycloak");
                Map<?, ?> result =
                        invoke(httpClient, builder.role(roleName).build(), accessToken, Map.class);
                if (result == null)
                    result = getRoleByNameInClients(httpClient, accessToken, roleName);
                if (result != null) return toGeoServerRole(result);
            }
        }
        return null;
    }

    private Map<?, ?> getRoleByNameInClients(
            CloseableHttpClient httpClient, String accessToken, String roleName) {
        Map<?, ?> result = null;
        if (listOfClientIds != null && !listOfClientIds.isEmpty()) {
            for (String clientId : listOfClientIds) {
                result =
                        invoke(
                                httpClient,
                                builder.client(clientId).role(roleName).build(),
                                accessToken,
                                Map.class);
                if (result != null) break;
            }
        }
        return result;
    }

    /**
     * Get the users to which the role with the specified role name was assigned.
     *
     * @param roleName the role name.
     * @return a SortedSet of username strings.
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    SortedSet<String> getUserInRole(String roleName) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            LOGGER.config("Obtaining access token for Keycloak");
            String accessToken = getAccessToken(httpClient);
            if (!StringUtils.isEmpty(accessToken)) {
                LOGGER.config("Retrieving roles from Keycloak");
                List<Object> users =
                        invoke(
                                httpClient,
                                builder.role(roleName).users().build(),
                                accessToken,
                                List.class);
                if (users != null) return toUserNames(users);
            }
        }
        return new TreeSet<>();
    }

    @SuppressWarnings("unchecked")
    private List<Object> rolesFromClients(CloseableHttpClient httpClient, String accessToken) {
        List<Object> results = new ArrayList<>();
        if (listOfClientIds != null && !listOfClientIds.isEmpty()) {
            for (String clientId : listOfClientIds) {
                String url = builder.client(clientId).allRoles().build();
                List<Object> result = invoke(httpClient, url, accessToken, List.class);
                if (result != null) results.addAll(result);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Object> userRolesFromClients(
            CloseableHttpClient httpClient, String accessToken, String userId) {
        List<Object> results = new ArrayList<>();
        if (listOfClientIds != null && !listOfClientIds.isEmpty()) {
            for (String clientId : listOfClientIds) {
                String url = builder.userById(userId).allUserRoles(false).client(clientId).build();
                List<Object> result = invoke(httpClient, url, accessToken, List.class);
                if (result != null) results.addAll(result);
            }
        }
        return results;
    }

    /**
     * Invoke a REST endpoint and return the response.
     *
     * @param httpClient the httpclient instance.
     * @param endpoint the endpoint to invoke.
     * @param accessToken the access_token to authorize the request.
     * @param resultType the Type to which convert the result.
     * @param <T> the type of the result.
     * @return the response converted to the type passed.
     */
    <T> T invoke(
            CloseableHttpClient httpClient,
            String endpoint,
            String accessToken,
            Class<T> resultType) {
        HttpGet httpGet = new HttpGet(endpoint);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);
        LOGGER.fine(() -> "Calling Keycloak REST endpoint " + endpoint);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = response.getStatusLine();
            if (statusLine == null || statusLine.getStatusCode() != 200) {
                LOGGER.log(
                        Level.FINE,
                        "Issue involing endpoint "
                                + endpoint
                                + ". Response status is "
                                + statusLine);
                return null;
            }
            String jsonString = getStringResponseMessage(response);
            LOGGER.fine(() -> "Response obtained is " + jsonString);
            return new Gson().fromJson(jsonString, resultType);
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while executing the REST call error is: " + e.getMessage(),
                    e);
        }

        return null;
    }

    /**
     * Obtain an access token from Keycloak, using the server URL, realm name, and client secret
     * from the config. Returns null if an error occurs.
     *
     * @param httpClient a CloseableHttpClient to send the request through
     * @return a String access token on success, null on error.
     */
    String getAccessToken(CloseableHttpClient httpClient) {
        HttpPost httpPost = new HttpPost(builder.buildTokenEndpoint());
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
            Map<?, ?> map = new Gson().fromJson(jsonString, Map.class);
            return map.get("access_token").toString();
        } catch (Exception e) {
            LOGGER.log(
                    Level.SEVERE,
                    "Error while executing the token call error is: " + e.getMessage(),
                    e);
            return null;
        }
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

    @SuppressWarnings("unchecked")
    private SortedSet<GeoServerRole> toGeoServerRoles(List<Object> list) {
        SortedSet<GeoServerRole> roles = new TreeSet<>();
        for (Object role : list) {
            if (role instanceof List) {
                roles.addAll(toGeoServerRoles((List) role));
            } else {
                GeoServerRole gsRole = toGeoServerRole(role);
                if (gsRole != null) roles.add(gsRole);
            }
        }
        return roles;
    }

    private GeoServerRole toGeoServerRole(Object obj) {
        String roleName = getIfMap(obj, "name");
        if (roleName != null) return new GeoServerRole(roleName);
        return null;
    }

    private String getIfMap(Object o, String attrName) {
        if (o instanceof Map) {
            Map<?, ?> role = (Map<?, ?>) o;
            Object value = role.get(attrName);
            if (value != null) return value.toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private SortedSet<String> toUserNames(List<Object> users) {
        SortedSet<String> userNames = new TreeSet<>();
        for (Object o : users) {
            if (o instanceof List) {
                userNames.addAll(toUserNames((List) o));
            } else {
                String userName = toUserName(o);
                if (userName != null) userNames.add(userName);
            }
        }
        return userNames;
    }

    private String toUserName(Object object) {
        return getIfMap(object, "username");
    }
}
