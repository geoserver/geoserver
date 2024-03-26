/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.roles;

import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON;
import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT;

import java.text.ParseException;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.junit.Assert;
import org.junit.Test;

public class JwtHeadersRolesExtractorTest {

    public JwtHeadersRolesExtractor getExtractor(
            GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource roleSource,
            String roleConverterMapString,
            String path) {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setRoleSource(roleSource);
        config.setRoleConverterString(roleConverterMapString);
        config.setRolesJsonPath(path);
        return new JwtHeadersRolesExtractor(config);
    }

    @Test
    public void testSimpleJwt() throws ParseException {
        String accessToken =
                "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItWEdld190TnFwaWRrYTl2QXNJel82WEQtdnJmZDVyMlNWTWkwcWMyR1lNIn0.eyJleHAiOjE3MDcxNTMxNDYsImlhdCI6MTcwNzE1Mjg0NiwiYXV0aF90aW1lIjoxNzA3MTUyNjQ1LCJqdGkiOiJlMzhjY2ZmYy0zMWNjLTQ0NmEtYmU1Yy04MjliNDE0NTkyZmQiLCJpc3MiOiJodHRwczovL2xvZ2luLWxpdmUtZGV2Lmdlb2NhdC5saXZlL3JlYWxtcy9kYXZlLXRlc3QyIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImVhMzNlM2NjLWYwZTEtNDIxOC04OWNiLThkNDhjMjdlZWUzZCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImxpdmUta2V5MiIsIm5vbmNlIjoiQldzc2M3cTBKZ0tHZC1OdFc1QlFhVlROMkhSa25LQmVIY0ZMTHZ5OXpYSSIsInNlc3Npb25fc3RhdGUiOiIxY2FiZmU1NC1lOWU0LTRjMmMtODQwNy03NTZiMjczZmFmZmIiLCJhY3IiOiIwIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtZGF2ZS10ZXN0MiIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJsaXZlLWtleTIiOnsicm9sZXMiOlsiR2Vvc2VydmVyQWRtaW5pc3RyYXRvciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcGhvbmUgb2ZmbGluZV9hY2Nlc3MgbWljcm9wcm9maWxlLWp3dCBwcm9maWxlIGFkZHJlc3MgZW1haWwiLCJzaWQiOiIxY2FiZmU1NC1lOWU0LTRjMmMtODQwNy03NTZiMjczZmFmZmIiLCJ1cG4iOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiYWRkcmVzcyI6e30sIm5hbWUiOiJkYXZpZCBibGFzYnkiLCJncm91cHMiOlsiZGVmYXVsdC1yb2xlcy1kYXZlLXRlc3QyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImdpdmVuX25hbWUiOiJkYXZpZCIsImZhbWlseV9uYW1lIjoiYmxhc2J5IiwiZW1haWwiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCJ9.fHzXd7oISnqWb09ah9wikfP2UOBeiOA3vd_aDg3Bw-xcfv9aD3CWhAK5FUDPYSPyj4whAcknZbUgUzcm0qkaI8V_aS65F3Fug4jt4nC9YPL4zMSJ5an4Dp6jlQ3OQhrKFn4FwaoW61ndMmScsZZWEQyj6gzHnn5cknqySB26tVydT6q57iTO7KQFcXRdbXd6GWIoFGS-ud9XzxQMUdNfYmsDD7e6hoWhe9PJD9Zq4KT6JN13hUU4Dos-Z5SBHjRa6ieHoOe9gqkjKyA1jT1NU42Nqr-mTV-ql22nAoXuplpvOYc5-09-KDDzSDuVKFwLCNMN3ZyRF1wWuydJeU-gOQ";

        List<GeoServerRole> roles =
                getExtractor(JWT, "", "resource_access.live-key2.roles").getRoles(accessToken)
                        .stream()
                        .collect(Collectors.toList());
        Assert.assertEquals(1, roles.size());
        Assert.assertEquals("GeoserverAdministrator", roles.get(0).getAuthority());
    }

    @Test
    public void testSimpleJson() throws ParseException {
        String json =
                "{\"exp\":1707155912,\"iat\":1707155612,\"jti\":\"888715ae-a79d-4633-83e5-9b97dee02bbc\",\"iss\":\"https://login-live-dev.geocat.live/realms/dave-test2\",\"aud\":\"account\",\"sub\":\"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d\",\"typ\":\"Bearer\",\"azp\":\"live-key2\",\"session_state\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"acr\":\"1\",\"realm_access\":{\"roles\":[\"default-roles-dave-test2\",\"offline_access\",\"uma_authorization\"]},\"resource_access\":{\"live-key2\":{\"roles\":[\"GeoserverAdministrator\"]},\"account\":{\"roles\":[\"manage-account\",\"manage-account-links\",\"view-profile\"]}},\"scope\":\"openidprofileemail\",\"sid\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"email_verified\":false,\"name\":\"davidblasby\",\"preferred_username\":\"david.blasby@geocat.net\",\"given_name\":\"david\",\"family_name\":\"blasby\",\"email\":\"david.blasby@geocat.net\"}";
        List<GeoServerRole> roles =
                getExtractor(JSON, "", "resource_access.live-key2.roles").getRoles(json).stream()
                        .collect(Collectors.toList());
        Assert.assertEquals(1, roles.size());
        Assert.assertEquals("GeoserverAdministrator", roles.get(0).getAuthority());
    }
}
