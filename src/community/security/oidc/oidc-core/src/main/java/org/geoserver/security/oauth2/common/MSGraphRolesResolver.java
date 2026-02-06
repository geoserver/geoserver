/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.apache.commons.lang3.StringUtils;
import org.geotools.util.logging.Logging;

/**
 * Verify role using Azure graph.
 *
 * <p>Make sure your Azure AD application has "GroupMember.Read.All" permission: a) go to your application in Azure AD
 * (in the portal) b) On the left, go to "API permissions" c) click "Add a permission" d) press "Microsoft Graph" e)
 * press "Delegated permission" f) Scroll down to "GroupMember" g) Choose "GroupMemeber.Read.All" h) press "Add
 * permission" i) on the API Permission screen, press the "Grant admin consent for ..." text
 *
 * <p>This class will go to the "https://graph.microsoft.com/v1.0/me/memberOf" and attach your access token. It will
 * then read the response and find all the user's groups.
 *
 * <p>NOTE: to be consistent with the rest of Azure, we use the Groups OID (guid) NOT its name.
 */
public class MSGraphRolesResolver {

    private static final Logger LOGGER = Logging.getLogger(MSGraphRolesResolver.class);

    static URL memberOfEndpoint;
    static URL appRoleAssignmentsEndpoint;

    static {
        try {
            memberOfEndpoint = new URL("https://graph.microsoft.com/v1.0/me/memberOf");
            appRoleAssignmentsEndpoint = new URL("https://graph.microsoft.com/v1.0/me/appRoleAssignments");
        } catch (MalformedURLException e) {
            // this shouldn't happen (unless typo in above line)
            LOGGER.log(Level.WARNING, "Error parsing MS GRAPH API URL", e);
        }
    }

    // for testing, we make this package readable
    String authorizationHeaderName = "Authorization";

    public HttpURLConnection createMemberOfHTTPRequest(String accessToken) throws IOException {
        String tokenHeaderValue = "Bearer " + accessToken;

        HttpURLConnection http = (HttpURLConnection) memberOfEndpoint.openConnection();
        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty(authorizationHeaderName, tokenHeaderValue);

        return http;
    }

    /**
     * talk to the actual azure graph api to get user's group memberships. 1. attaches the access token to the request.
     * 2. sets the Accepts header to "application/json" (required)
     *
     * @param accessToken
     * @return
     * @throws IOException
     */
    private String resolveMemberOfUrl(String accessToken) throws IOException {

        HttpURLConnection http = createMemberOfHTTPRequest(accessToken);
        try (BufferedReader lReader = new BufferedReader(new InputStreamReader(http.getInputStream()))) {
            String result = lReader.lines().collect(Collectors.joining("\n"));
            return result;
        } finally {
            http.disconnect();
        }
    }

    // parses the resulting json from the user's group memberships json result.
    // returns a list of the groups (object id) that the user is a member of.
    public List<String> parseMemberOfJson(String jsonString) throws JSONException {
        List<String> result = new ArrayList<>();
        JSONObject json = JSONObject.fromObject(jsonString);
        JSONArray values = json.getJSONArray("value");

        for (Object value : values) {
            JSONObject object = (JSONObject) value;
            if (!object.get("@odata.type").equals("#microsoft.graph.group")) continue;
            result.add(object.get("id").toString());
        }
        return result;
    }

    /**
     * call the MS Graph API and get a list of object ids (guid strings) - one for each group the user is a member of.
     *
     * @param accessToken - access token (from MS azure ad)
     * @return list of groups (guid strings) the user is a member of
     * @throws IOException
     */
    public List<String> resolveRoles(
            String accessToken, Boolean getMemberOf, Boolean getAppRoles, String enterpriseAppObjectId)
            throws Exception {
        if (getMemberOf == null) {
            getMemberOf = Boolean.FALSE;
        }
        if (getAppRoles == null) {
            getAppRoles = Boolean.FALSE;
        }
        List<String> result = new ArrayList<>();
        if (getMemberOf) {
            String jsonStr = resolveMemberOfUrl(accessToken);
            List<String> resultMemberOf = parseMemberOfJson(jsonStr);
            result.addAll(resultMemberOf);
        }
        if (getAppRoles) {
            if (StringUtils.isEmpty(enterpriseAppObjectId)) {
                throw new Exception("enterpriseAppObjectId is null!");
            }
            UUID uuid = UUID.fromString(enterpriseAppObjectId);
            String json = resolveAppRoles(accessToken, uuid);
            List<String> resultAppRoles = parseAppRolesJson(json);
            result.addAll(resultAppRoles);
        }
        return result;
    }

    // Made package-protected for testing
    List<String> parseAppRolesJson(String jsonString) {
        List<String> result = new ArrayList<>();
        JSONObject json = JSONObject.fromObject(jsonString);
        JSONArray values = json.getJSONArray("value");

        for (Object value : values) {
            JSONObject object = (JSONObject) value;
            result.add(object.get("appRoleId").toString());
        }
        return result;
    }

    private String resolveAppRoles(String accessToken, UUID uuid) throws IOException {
        HttpURLConnection http = createAppRolesHTTPRequest(accessToken, uuid);
        try (BufferedReader lReader = new BufferedReader(new InputStreamReader(http.getInputStream()))) {
            String result = lReader.lines().collect(Collectors.joining("\n"));
            return result;
        } finally {
            http.disconnect();
        }
    }

    public HttpURLConnection createAppRolesHTTPRequest(String accessToken, UUID uuid) throws IOException {
        String tokenHeaderValue = "Bearer " + accessToken;

        URL url = new URL(appRoleAssignmentsEndpoint.toString() + "?$filter=resourceId+eq+" + uuid.toString());
        HttpURLConnection http = (HttpURLConnection) url.openConnection();
        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty(authorizationHeaderName, tokenHeaderValue);

        return http;
    }
}
