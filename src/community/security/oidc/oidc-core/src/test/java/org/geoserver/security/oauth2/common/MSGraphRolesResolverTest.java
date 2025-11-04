/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2.common;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import org.junit.Test;

public class MSGraphRolesResolverTest {

    @Test
    public void testMSGraphAPIEndpoint() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        assertEquals("https://graph.microsoft.com/v1.0/me/memberOf", resolver.memberOfEndpoint.toString());
    }

    @Test
    public void testHTTPConnection() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        // HttpURLConnection treats "Authorization" request header as private so we cannot verify
        // it.  We change its name so we can access it!
        resolver.authorizationHeaderName = "AuthorizationZZZ";
        HttpURLConnection http = resolver.createMemberOfHTTPRequest("accesstoken");
        try {

            assertEquals(
                    "https://graph.microsoft.com/v1.0/me/memberOf",
                    http.getURL().toString());
            assertEquals("Bearer accesstoken", http.getRequestProperty("AuthorizationZZZ"));
            assertEquals("application/json", http.getRequestProperty("Accept"));
        } finally {
            if (http != null) http.disconnect();
        }
    }

    // typical MSGraph response
    String json1 =
            """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#directoryObjects",
              "value": [
                {
                  "@odata.type": "#microsoft.graph.directoryRole",
                  "id": "fced5395-6be6-436c-a7f9-5c638cbdeb20",
                  "deletedDateTime": null,
                  "description": null,
                  "displayName": null,
                  "roleTemplateId": null
                },
                {
                  "@odata.type": "#microsoft.graph.group",
                  "id": "d93c6444-feee-4b67-8c0f-15d6796370cb",
                  "deletedDateTime": null,
                  "classification": null,
                  "createdDateTime": "2022-05-04T17:57:04Z",
                  "creationOptions": [],
                  "description": "geoserverAdmin",
                  "displayName": "geoserverAdmin",
                  "expirationDateTime": null,
                  "groupTypes": [],
                  "isAssignableToRole": null,
                  "mail": null,
                  "mailEnabled": false,
                  "mailNickname": "e93d884d-3",
                  "membershipRule": null,
                  "membershipRuleProcessingState": null,
                  "onPremisesDomainName": null,
                  "onPremisesLastSyncDateTime": null,
                  "onPremisesNetBiosName": null,
                  "onPremisesSamAccountName": null,
                  "onPremisesSecurityIdentifier": null,
                  "onPremisesSyncEnabled": null,
                  "preferredDataLocation": null,
                  "preferredLanguage": null,
                  "proxyAddresses": [],
                  "renewedDateTime": "2022-05-04T17:57:04Z",
                  "resourceBehaviorOptions": [],
                  "resourceProvisioningOptions": [],
                  "securityEnabled": true,
                  "securityIdentifier": "S-1-12-1-3644613700-1265106670-3591704460-3413140345",
                  "theme": null,
                  "visibility": null,
                  "onPremisesProvisioningErrors": []
                },
                {
                  "@odata.type": "#microsoft.graph.group",
                  "id": "3a94275f-7d53-4205-8d78-11f39e9ffa5a",
                  "deletedDateTime": null,
                  "classification": null,
                  "createdDateTime": "2022-05-18T20:21:11Z",
                  "creationOptions": [],
                  "description": "geonetworkAdmin",
                  "displayName": "geonetworkAdmin",
                  "expirationDateTime": null,
                  "groupTypes": [],
                  "isAssignableToRole": null,
                  "mail": null,
                  "mailEnabled": false,
                  "mailNickname": "52fa2d5e-5",
                  "membershipRule": null,
                  "membershipRuleProcessingState": null,
                  "onPremisesDomainName": null,
                  "onPremisesLastSyncDateTime": null,
                  "onPremisesNetBiosName": null,
                  "onPremisesSamAccountName": null,
                  "onPremisesSecurityIdentifier": null,
                  "onPremisesSyncEnabled": null,
                  "preferredDataLocation": null,
                  "preferredLanguage": null,
                  "proxyAddresses": [],
                  "renewedDateTime": "2022-05-18T20:21:11Z",
                  "resourceBehaviorOptions": [],
                  "resourceProvisioningOptions": [],
                  "securityEnabled": true,
                  "securityIdentifier": "S-1-12-1-982787935-1107656019-4078008461-1526374302",
                  "theme": null,
                  "visibility": null,
                  "onPremisesProvisioningErrors": []
                }\s
              ]
            }\
            """;

    @Test
    public void testParse() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        List<String> groups = resolver.parseMemberOfJson(json1);

        assertEquals(2, groups.size());
        assertEquals("d93c6444-feee-4b67-8c0f-15d6796370cb", groups.get(0));
        assertEquals("3a94275f-7d53-4205-8d78-11f39e9ffa5a", groups.get(1));
    }
}
