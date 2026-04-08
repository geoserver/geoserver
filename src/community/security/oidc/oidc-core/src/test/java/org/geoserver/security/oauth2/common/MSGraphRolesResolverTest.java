/*
 * (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.oauth2.common;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.util.List;
import java.util.UUID;
import org.junit.Test;

public class MSGraphRolesResolverTest {

    @Test
    public void testMSGraphAPIEndpoint() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        assertEquals("https://graph.microsoft.com/v1.0/me/memberOf", resolver.memberOfEndpoint.toString());
    }

    @Test
    @SuppressWarnings("PMD.UnusedLocalVariable")
    public void testAppRoleAssignmentsEndpoint() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        assertEquals(
                "https://graph.microsoft.com/v1.0/me/appRoleAssignments",
                MSGraphRolesResolver.appRoleAssignmentsEndpoint.toString());
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

    @Test
    public void testAppRolesHTTPConnection() throws IOException {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        resolver.authorizationHeaderName = "AuthorizationZZZ";

        UUID uuid = UUID.fromString("12345678-1234-1234-1234-123456789012");
        HttpURLConnection http = resolver.createAppRolesHTTPRequest("myaccesstoken", uuid);
        try {
            String expectedUrl =
                    "https://graph.microsoft.com/v1.0/me/appRoleAssignments?$filter=resourceId+eq+12345678-1234-1234-1234-123456789012";
            assertEquals(expectedUrl, http.getURL().toString());
            assertEquals("Bearer myaccesstoken", http.getRequestProperty("AuthorizationZZZ"));
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
                }
              ]
            }
            """;

    @Test
    public void testParse() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        List<String> groups = resolver.parseMemberOfJson(json1);

        assertEquals(2, groups.size());
        assertEquals("d93c6444-feee-4b67-8c0f-15d6796370cb", groups.get(0));
        assertEquals("3a94275f-7d53-4205-8d78-11f39e9ffa5a", groups.get(1));
    }

    @Test
    public void testParseEmptyValues() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        String emptyJson =
                """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#directoryObjects",
              "value": []
            }
            """;

        List<String> groups = resolver.parseMemberOfJson(emptyJson);

        assertEquals(0, groups.size());
    }

    @Test
    public void testParseOnlyDirectoryRoles() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        // JSON with only directoryRole entries (no groups)
        String noGroupsJson =
                """
            {
              "value": [
                {
                  "@odata.type": "#microsoft.graph.directoryRole",
                  "id": "fced5395-6be6-436c-a7f9-5c638cbdeb20"
                }
              ]
            }
            """;

        List<String> groups = resolver.parseMemberOfJson(noGroupsJson);

        assertEquals(0, groups.size());
    }

    @Test
    public void testResolveRolesWithNullGetMemberOf() throws Exception {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        // Both null - should default to false and return empty list
        List<String> roles = resolver.resolveRoles("token", null, null, null);

        assertNotNull(roles);
        assertEquals(0, roles.size());
    }

    @Test
    public void testResolveRolesWithBothFalse() throws Exception {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        // Both explicitly false - should return empty list
        List<String> roles = resolver.resolveRoles("token", false, false, null);

        assertNotNull(roles);
        assertEquals(0, roles.size());
    }

    @Test
    public void testResolveRolesAppRolesWithEmptyEnterpriseAppId() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        // getAppRoles=true but no enterpriseAppObjectId - should throw
        try {
            resolver.resolveRoles("token", false, true, "");
            fail("Expected exception for empty enterpriseAppObjectId");
        } catch (Exception e) {
            assertEquals("enterpriseAppObjectId is null!", e.getMessage());
        }
    }

    @Test
    public void testResolveRolesAppRolesWithNullEnterpriseAppId() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        // getAppRoles=true but null enterpriseAppObjectId - should throw
        try {
            resolver.resolveRoles("token", false, true, null);
            fail("Expected exception for null enterpriseAppObjectId");
        } catch (Exception e) {
            assertEquals("enterpriseAppObjectId is null!", e.getMessage());
        }
    }

    @Test
    public void testResolveRolesAppRolesWithInvalidUUID() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();

        // getAppRoles=true with invalid UUID format - should throw IllegalArgumentException
        try {
            resolver.resolveRoles("token", false, true, "not-a-valid-uuid");
            fail("Expected IllegalArgumentException for invalid UUID");
        } catch (IllegalArgumentException e) {
            // Expected - UUID.fromString throws this for invalid format
            assertTrue(e.getMessage().contains("Invalid UUID"));
        } catch (Exception e) {
            fail("Expected IllegalArgumentException but got: " + e.getClass().getName());
        }
    }

    // App roles JSON response tests
    @Test
    public void testParseAppRolesJson() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        String appRolesJson =
                """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#appRoleAssignments",
              "value": [
                {
                  "id": "abc123",
                  "appRoleId": "00000000-0000-0000-0000-000000000001",
                  "principalId": "user-id-1",
                  "resourceId": "app-id-1"
                },
                {
                  "id": "def456",
                  "appRoleId": "00000000-0000-0000-0000-000000000002",
                  "principalId": "user-id-1",
                  "resourceId": "app-id-1"
                }
              ]
            }
            """;

        List<String> roles = resolver.parseAppRolesJson(appRolesJson);

        assertEquals(2, roles.size());
        assertEquals("00000000-0000-0000-0000-000000000001", roles.get(0));
        assertEquals("00000000-0000-0000-0000-000000000002", roles.get(1));
    }

    @Test
    public void testParseAppRolesJsonEmpty() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        String emptyJson = """
            {
              "value": []
            }
            """;

        List<String> roles = resolver.parseAppRolesJson(emptyJson);

        assertEquals(0, roles.size());
    }

    @Test
    public void testParseAppRolesJsonSingleRole() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        String singleRoleJson =
                """
            {
              "value": [
                {
                  "id": "assignment-1",
                  "appRoleId": "admin-role-id",
                  "principalId": "user-123",
                  "resourceId": "app-456"
                }
              ]
            }
            """;

        List<String> roles = resolver.parseAppRolesJson(singleRoleJson);

        assertEquals(1, roles.size());
        assertEquals("admin-role-id", roles.get(0));
    }

    @Test
    public void testParseAppRolesJsonWithExtraFields() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        // JSON with additional fields that should be ignored
        String jsonWithExtras =
                """
            {
              "@odata.context": "https://graph.microsoft.com/v1.0/$metadata#appRoleAssignments",
              "@odata.count": 1,
              "value": [
                {
                  "id": "xyz789",
                  "deletedDateTime": null,
                  "appRoleId": "reader-role-id",
                  "createdDateTime": "2024-01-01T00:00:00Z",
                  "principalDisplayName": "Test User",
                  "principalId": "user-abc",
                  "principalType": "User",
                  "resourceDisplayName": "My App",
                  "resourceId": "app-def"
                }
              ]
            }
            """;

        List<String> roles = resolver.parseAppRolesJson(jsonWithExtras);

        assertEquals(1, roles.size());
        assertEquals("reader-role-id", roles.get(0));
    }

    @Test
    public void testParseMemberOfJsonMixedTypes() {
        MSGraphRolesResolver resolver = new MSGraphRolesResolver();
        // JSON with a mix of groups, directory roles, and other types
        String mixedJson =
                """
            {
              "value": [
                {
                  "@odata.type": "#microsoft.graph.group",
                  "id": "group-1"
                },
                {
                  "@odata.type": "#microsoft.graph.directoryRole",
                  "id": "role-1"
                },
                {
                  "@odata.type": "#microsoft.graph.group",
                  "id": "group-2"
                },
                {
                  "@odata.type": "#microsoft.graph.administrativeUnit",
                  "id": "unit-1"
                },
                {
                  "@odata.type": "#microsoft.graph.group",
                  "id": "group-3"
                }
              ]
            }
            """;

        List<String> groups = resolver.parseMemberOfJson(mixedJson);

        // Should only include groups, not directoryRoles or administrativeUnits
        assertEquals(3, groups.size());
        assertEquals("group-1", groups.get(0));
        assertEquals("group-2", groups.get(1));
        assertEquals("group-3", groups.get(2));
    }
}
