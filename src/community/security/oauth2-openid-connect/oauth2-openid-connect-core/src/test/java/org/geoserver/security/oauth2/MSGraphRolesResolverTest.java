/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import org.junit.Test;

public class MSGraphRolesResolverTest {

    @Test
    public void testMSGraphAPIEndpoint() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        assertEquals(
                "https://graph.microsoft.com/v1.0/me/memberOf",
                resolver.memberOfEndpoint.toString());
    }

    @Test
    public void testHTTPConnection() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        // HttpURLConnection treats "Authorization" request header as private so we cannot verify
        // it.  We change its name so we can access it!
        resolver.authorizationHeaderName = "AuthorizationZZZ";
        HttpURLConnection http = resolver.createHTTPRequest("accesstoken");
        try {

            assertEquals("https://graph.microsoft.com/v1.0/me/memberOf", http.getURL().toString());
            assertEquals("Bearer accesstoken", http.getRequestProperty("AuthorizationZZZ"));
            assertEquals("application/json", http.getRequestProperty("Accept"));
        } finally {
            if (http != null) http.disconnect();
        }
    }

    // typical MSGraph response
    String json1 =
            "{\n"
                    + "  \"@odata.context\": \"https://graph.microsoft.com/v1.0/$metadata#directoryObjects\",\n"
                    + "  \"value\": [\n"
                    + "    {\n"
                    + "      \"@odata.type\": \"#microsoft.graph.directoryRole\",\n"
                    + "      \"id\": \"fced5395-6be6-436c-a7f9-5c638cbdeb20\",\n"
                    + "      \"deletedDateTime\": null,\n"
                    + "      \"description\": null,\n"
                    + "      \"displayName\": null,\n"
                    + "      \"roleTemplateId\": null\n"
                    + "    },\n"
                    + "    {\n"
                    + "      \"@odata.type\": \"#microsoft.graph.group\",\n"
                    + "      \"id\": \"d93c6444-feee-4b67-8c0f-15d6796370cb\",\n"
                    + "      \"deletedDateTime\": null,\n"
                    + "      \"classification\": null,\n"
                    + "      \"createdDateTime\": \"2022-05-04T17:57:04Z\",\n"
                    + "      \"creationOptions\": [],\n"
                    + "      \"description\": \"geoserverAdmin\",\n"
                    + "      \"displayName\": \"geoserverAdmin\",\n"
                    + "      \"expirationDateTime\": null,\n"
                    + "      \"groupTypes\": [],\n"
                    + "      \"isAssignableToRole\": null,\n"
                    + "      \"mail\": null,\n"
                    + "      \"mailEnabled\": false,\n"
                    + "      \"mailNickname\": \"e93d884d-3\",\n"
                    + "      \"membershipRule\": null,\n"
                    + "      \"membershipRuleProcessingState\": null,\n"
                    + "      \"onPremisesDomainName\": null,\n"
                    + "      \"onPremisesLastSyncDateTime\": null,\n"
                    + "      \"onPremisesNetBiosName\": null,\n"
                    + "      \"onPremisesSamAccountName\": null,\n"
                    + "      \"onPremisesSecurityIdentifier\": null,\n"
                    + "      \"onPremisesSyncEnabled\": null,\n"
                    + "      \"preferredDataLocation\": null,\n"
                    + "      \"preferredLanguage\": null,\n"
                    + "      \"proxyAddresses\": [],\n"
                    + "      \"renewedDateTime\": \"2022-05-04T17:57:04Z\",\n"
                    + "      \"resourceBehaviorOptions\": [],\n"
                    + "      \"resourceProvisioningOptions\": [],\n"
                    + "      \"securityEnabled\": true,\n"
                    + "      \"securityIdentifier\": \"S-1-12-1-3644613700-1265106670-3591704460-3413140345\",\n"
                    + "      \"theme\": null,\n"
                    + "      \"visibility\": null,\n"
                    + "      \"onPremisesProvisioningErrors\": []\n"
                    + "    },\n"
                    + "    {\n"
                    + "      \"@odata.type\": \"#microsoft.graph.group\",\n"
                    + "      \"id\": \"3a94275f-7d53-4205-8d78-11f39e9ffa5a\",\n"
                    + "      \"deletedDateTime\": null,\n"
                    + "      \"classification\": null,\n"
                    + "      \"createdDateTime\": \"2022-05-18T20:21:11Z\",\n"
                    + "      \"creationOptions\": [],\n"
                    + "      \"description\": \"geonetworkAdmin\",\n"
                    + "      \"displayName\": \"geonetworkAdmin\",\n"
                    + "      \"expirationDateTime\": null,\n"
                    + "      \"groupTypes\": [],\n"
                    + "      \"isAssignableToRole\": null,\n"
                    + "      \"mail\": null,\n"
                    + "      \"mailEnabled\": false,\n"
                    + "      \"mailNickname\": \"52fa2d5e-5\",\n"
                    + "      \"membershipRule\": null,\n"
                    + "      \"membershipRuleProcessingState\": null,\n"
                    + "      \"onPremisesDomainName\": null,\n"
                    + "      \"onPremisesLastSyncDateTime\": null,\n"
                    + "      \"onPremisesNetBiosName\": null,\n"
                    + "      \"onPremisesSamAccountName\": null,\n"
                    + "      \"onPremisesSecurityIdentifier\": null,\n"
                    + "      \"onPremisesSyncEnabled\": null,\n"
                    + "      \"preferredDataLocation\": null,\n"
                    + "      \"preferredLanguage\": null,\n"
                    + "      \"proxyAddresses\": [],\n"
                    + "      \"renewedDateTime\": \"2022-05-18T20:21:11Z\",\n"
                    + "      \"resourceBehaviorOptions\": [],\n"
                    + "      \"resourceProvisioningOptions\": [],\n"
                    + "      \"securityEnabled\": true,\n"
                    + "      \"securityIdentifier\": \"S-1-12-1-982787935-1107656019-4078008461-1526374302\",\n"
                    + "      \"theme\": null,\n"
                    + "      \"visibility\": null,\n"
                    + "      \"onPremisesProvisioningErrors\": []\n"
                    + "    } \n"
                    + "  ]\n"
                    + "}";

    @Test
    public void testParse() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        List<String> groups = resolver.parseJson(json1);

        assertEquals(2, groups.size());
        assertEquals("d93c6444-feee-4b67-8c0f-15d6796370cb", groups.get(0));
        assertEquals("3a94275f-7d53-4205-8d78-11f39e9ffa5a", groups.get(1));
    }
}
