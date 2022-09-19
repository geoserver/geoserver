/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONObject;
import org.geotools.util.logging.Logging;

/**
 * Verify role using Azure graph.
 *
 * <p>Make sure your Azure AD application has "GroupMember.Read.All" permission: a) go to your
 * application in Azure AD (in the portal) b) On the left, go to "API permissions" c) click "Add a
 * permission" d) press "Microsoft Graph" e) press "Delegated permission" f) Scroll down to
 * "GroupMember" g) Choose "GroupMemeber.Read.All" h) press "Add permission" i) on the API
 * Permission screen, press the "Grant admin consent for ..." text
 *
 * <p>This class will go to the "https://graph.microsoft.com/v1.0/me/memberOf" and attach your
 * access token. It will then read the response and find all the user's groups.
 *
 * <p>NOTE: to be consistent with the rest of Azure, we use the Groups OID (guid) NOT its name.
 */
public class MSGraphRolesResolver {

    private static final Logger LOGGER = Logging.getLogger(MSGraphRolesResolver.class);

    static URL memberOfEndpoint;

    static {
        try {
            memberOfEndpoint = new URL("https://graph.microsoft.com/v1.0/me/memberOf");
        } catch (MalformedURLException e) {
            // this shouldn't happen (unless typo in above line)
            LOGGER.log(Level.WARNING, "Error parsing MS GRAPH API URL", e);
        }
    }

    // for testing, we make this package readable
    String authorizationHeaderName = "Authorization";

    public HttpURLConnection createHTTPRequest(String accessToken) throws IOException {
        String tokenHeaderValue = "Bearer " + accessToken;

        HttpURLConnection http = (HttpURLConnection) memberOfEndpoint.openConnection();
        http.setRequestProperty("Accept", "application/json");
        http.setRequestProperty(authorizationHeaderName, tokenHeaderValue);

        return http;
    }

    /**
     * talk to the actual azure graph api to get user's group memberships. 1. attaches the access
     * token to the request. 2. sets the Accepts header to "application/json" (required)
     *
     * @param accessToken
     * @return
     * @throws IOException
     */
    private String resolveUrl(String accessToken) throws IOException {
        HttpURLConnection http = null;

        try {
            http = createHTTPRequest(accessToken);
            String result =
                    new BufferedReader(new InputStreamReader(http.getInputStream()))
                            .lines()
                            .collect(Collectors.joining("\n"));
            return result;
        } finally {
            if (http != null) http.disconnect();
        }
    }

    // parses the resulting json from the user's group memberships json result.
    // returns a list of the groups (object id) that the user is a member of.
    public List<String> parseJson(String jsonString) throws JSONException {
        List<String> result = new ArrayList<>();
        JSONObject json = JSONObject.fromObject(jsonString);
        JSONArray values = json.getJSONArray("value");
        for (int i = 0; i < values.size(); i++) {
            JSONObject object = (JSONObject) values.get(i);
            if (!object.get("@odata.type").equals("#microsoft.graph.group")) continue;
            result.add(object.get("id").toString());
        }
        return result;
    }

    /**
     * call the MS Graph API and get a list of object ids (guid strings) - one for each group the
     * user is a member of.
     *
     * @param accessToken - access token (from MS azure ad)
     * @return list of groups (guid strings) the user is a member of
     * @throws Exception
     */
    public List<String> resolveRoles(String accessToken) throws Exception {
        try {
            String jsonStr = resolveUrl(accessToken);
            List<String> result = parseJson(jsonStr);
            return result;
        } catch (Exception e) {
            throw e;
        }
    }
}
