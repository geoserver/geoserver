/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.keycloak;

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
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.StatusLine;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.util.ObjectUtils;
import tools.jackson.databind.ObjectMapper;

/** Invokes Keycloak Admin REST API endpoints to retrieve roles and users. */
public class KeycloakRESTClient {

    /** Holds the role counts returned by {@link #testConnection()}. */
    public record ConnectionTestResult(int realmRoleCount, int clientRoleCount) {}

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private final KeycloakUrlBuilder builder;
    private final String clientID;
    private final String clientSecret;
    private final List<String> listOfClientIds;

    private static final Logger LOGGER = org.geotools.util.logging.Logging.getLogger(KeycloakRESTClient.class);

    public KeycloakRESTClient(
            String serverUrl, String realm, String clientID, String clientSecret, List<String> listOfClientIds) {
        this.builder = new KeycloakUrlBuilder(realm, serverUrl);
        this.clientID = clientID;
        this.clientSecret = clientSecret;
        this.listOfClientIds = listOfClientIds;
    }

    /**
     * Tests the connection to Keycloak by obtaining an access token and fetching realm and client roles.
     *
     * @return counts of realm roles and client roles found
     * @throws IOException if authentication fails or the roles endpoint cannot be reached
     */
    public ConnectionTestResult testConnection() throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost httpPost = new HttpPost(builder.buildTokenEndpoint());
            String body = "client_id=" + clientID + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
            httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
            String accessToken;
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                StatusLine statusLine = new StatusLine(response);
                if (statusLine.getStatusCode() != 200) {
                    throw new IOException(
                            "Authentication failed (" + statusLine + "): check Base URL, realm, client ID and secret");
                }
                try {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> map = OBJECT_MAPPER.readValue(readResponseBody(response), Map.class);
                    accessToken = map.get("access_token").toString();
                } catch (Exception e) {
                    throw new IOException("Failed to parse token response: " + e.getMessage(), e);
                }
            }
            List<Object> realmRoles = invoke(httpClient, builder.allRoles().build(), accessToken, List.class);
            int realmCount = realmRoles != null ? realmRoles.size() : 0;
            int clientCount = rolesFromClients(httpClient, accessToken).size();
            return new ConnectionTestResult(realmCount, clientCount);
        }
    }

    /** Returns all realm roles plus any configured client roles, as {@link GeoServerRole} objects. */
    @SuppressWarnings("unchecked")
    SortedSet<GeoServerRole> getRoles() throws IOException {
        SortedSet<GeoServerRole> sortedSet = new TreeSet<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String accessToken = getAccessToken(httpClient);
            if (!ObjectUtils.isEmpty(accessToken)) {
                List<Object> result = invoke(httpClient, builder.allRoles().build(), accessToken, List.class);
                if (result == null) result = new ArrayList<>();
                result.add(rolesFromClients(httpClient, accessToken));
                sortedSet = toGeoServerRoles(result);
            }
        }
        return sortedSet;
    }

    /** Returns all roles assigned to the given username (realm + configured client roles). */
    @SuppressWarnings("unchecked")
    SortedSet<GeoServerRole> getUserRoles(String username) throws IOException {
        SortedSet<GeoServerRole> sortedSet = new TreeSet<>();
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String accessToken = getAccessToken(httpClient);
            if (!ObjectUtils.isEmpty(accessToken)) {
                List<Object> result =
                        invoke(httpClient, builder.userByName(username).build(), accessToken, List.class);
                if (result != null && !result.isEmpty()) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> user = (Map<String, Object>) result.get(0);
                    String userId = user.get("id").toString();
                    List<Object> roles = invoke(
                            httpClient,
                            builder.userById(userId).allUserRoles(true).build(),
                            accessToken,
                            List.class);
                    if (roles != null && !roles.isEmpty()) {
                        sortedSet.addAll(toGeoServerRoles(roles));
                    }
                    sortedSet.addAll(toGeoServerRoles(userRolesFromClients(httpClient, accessToken, userId)));
                }
            }
        }
        return sortedSet;
    }

    /** Returns the {@link GeoServerRole} for the given role name, or {@code null} if not found. */
    GeoServerRole getRoleByName(String roleName) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String accessToken = getAccessToken(httpClient);
            if (!ObjectUtils.isEmpty(accessToken)) {
                Map<?, ?> result = invoke(httpClient, builder.role(roleName).build(), accessToken, Map.class);
                if (result == null) result = getRoleByNameInClients(httpClient, accessToken, roleName);
                if (result != null) return toGeoServerRole(result);
            }
        }
        return null;
    }

    private Map<?, ?> getRoleByNameInClients(CloseableHttpClient httpClient, String accessToken, String roleName) {
        if (listOfClientIds != null && !listOfClientIds.isEmpty()) {
            for (String clientId : listOfClientIds) {
                Map<?, ?> result = invoke(
                        httpClient, builder.client(clientId).role(roleName).build(), accessToken, Map.class);
                if (result != null) return result;
            }
        }
        return null;
    }

    /** Returns the usernames of all users assigned the given role. */
    @SuppressWarnings("unchecked")
    SortedSet<String> getUserInRole(String roleName) throws IOException {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String accessToken = getAccessToken(httpClient);
            if (!ObjectUtils.isEmpty(accessToken)) {
                List<Object> users =
                        invoke(httpClient, builder.role(roleName).users().build(), accessToken, List.class);
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
                List<Object> result =
                        invoke(httpClient, builder.client(clientId).allRoles().build(), accessToken, List.class);
                if (result != null) results.addAll(result);
            }
        }
        return results;
    }

    @SuppressWarnings("unchecked")
    private List<Object> userRolesFromClients(CloseableHttpClient httpClient, String accessToken, String userId) {
        List<Object> results = new ArrayList<>();
        if (listOfClientIds != null && !listOfClientIds.isEmpty()) {
            for (String clientId : listOfClientIds) {
                List<Object> result = invoke(
                        httpClient,
                        builder.userById(userId)
                                .allUserRoles(false)
                                .client(clientId)
                                .build(),
                        accessToken,
                        List.class);
                if (result != null) results.addAll(result);
            }
        }
        return results;
    }

    <T> T invoke(CloseableHttpClient httpClient, String endpoint, String accessToken, Class<T> resultType) {
        HttpGet httpGet = new HttpGet(endpoint);
        httpGet.setHeader("Authorization", "Bearer " + accessToken);
        LOGGER.fine(() -> "Calling Keycloak REST endpoint " + endpoint);
        try (CloseableHttpResponse response = httpClient.execute(httpGet)) {
            StatusLine statusLine = new StatusLine(response);
            if (statusLine.getStatusCode() != 200) {
                LOGGER.log(Level.FINE, "Issue invoking endpoint {0}. Response status is {1}", new Object[] {
                    endpoint, statusLine
                });
                return null;
            }
            String jsonString = readResponseBody(response);
            LOGGER.fine(() -> "Response obtained is " + jsonString);
            return OBJECT_MAPPER.readValue(jsonString, resultType);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while executing REST call: " + e.getMessage(), e);
        }
        return null;
    }

    String getAccessToken(CloseableHttpClient httpClient) {
        HttpPost httpPost = new HttpPost(builder.buildTokenEndpoint());
        String body = "client_id=" + clientID + "&client_secret=" + clientSecret + "&grant_type=client_credentials";
        httpPost.setEntity(new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
        try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
            StatusLine statusLine = new StatusLine(response);
            if (statusLine.getStatusCode() != 200) {
                LOGGER.info("Issue retrieving access token: " + statusLine);
                return null;
            }
            @SuppressWarnings("unchecked")
            Map<String, Object> map = OBJECT_MAPPER.readValue(readResponseBody(response), Map.class);
            return map.get("access_token").toString();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error while obtaining access token: " + e.getMessage(), e);
            return null;
        }
    }

    private String readResponseBody(CloseableHttpResponse response) throws Exception {
        HttpEntity responseEntity = response.getEntity();
        if (responseEntity == null) {
            throw new NullPointerException("HTTP response from Keycloak contained no body");
        }
        try (InputStream in = responseEntity.getContent()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @SuppressWarnings("unchecked")
    private SortedSet<GeoServerRole> toGeoServerRoles(List<Object> list) {
        SortedSet<GeoServerRole> roles = new TreeSet<>();
        for (Object item : list) {
            if (item instanceof List<?> nested) {
                roles.addAll(toGeoServerRoles((List<Object>) nested));
            } else {
                GeoServerRole role = toGeoServerRole(item);
                if (role != null) roles.add(role);
            }
        }
        return roles;
    }

    private GeoServerRole toGeoServerRole(Object obj) {
        String name = getStringAttr(obj, "name");
        return name != null ? new GeoServerRole(name) : null;
    }

    private String getStringAttr(Object o, String attrName) {
        if (o instanceof Map<?, ?> map) {
            Object value = map.get(attrName);
            if (value != null) return value.toString();
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private SortedSet<String> toUserNames(List<Object> users) {
        SortedSet<String> userNames = new TreeSet<>();
        for (Object o : users) {
            if (o instanceof List<?> nested) {
                userNames.addAll(toUserNames((List<Object>) nested));
            } else {
                String userName = getStringAttr(o, "username");
                if (userName != null) userNames.add(userName);
            }
        }
        return userNames;
    }
}
