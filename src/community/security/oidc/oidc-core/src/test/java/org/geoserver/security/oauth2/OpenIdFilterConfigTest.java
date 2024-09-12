/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class OpenIdFilterConfigTest {

    private OpenIdConnectFilterConfig getFilterConfig() {
        OpenIdConnectFilterConfig filterConfig = new OpenIdConnectFilterConfig();
        String baseUrl = "http://localhost:8080";
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(OpenIdConnectAuthenticationFilter.class.getName());
        filterConfig.setCliendId("efhrufuu3uhu3uhu");
        filterConfig.setClientSecret("48ru48tj58fr8jf");
        filterConfig.setAccessTokenUri(baseUrl + "/token");
        filterConfig.setUserAuthorizationUri(baseUrl + "/authorize");
        filterConfig.setCheckTokenEndpointUrl(baseUrl + "/userinfo");
        filterConfig.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        filterConfig.setLogoutEndpoint("/j_spring_oauth2_openid_connect_logout");
        filterConfig.setScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        filterConfig.setPrincipalKey("email");
        filterConfig.setLogoutUri("http://localhost:8181/global-logout-uri");
        return filterConfig;
    }

    @Test
    public void testResponseModeInAuthorizationUrl() {
        OpenIdConnectFilterConfig filterConfig = getFilterConfig();
        filterConfig.setRoleSource(OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken);
        filterConfig.setTokenRolesClaim("roles");
        // for ease of testing, do not use HTTPS
        filterConfig.setForceUserAuthorizationUriHttps(false);
        filterConfig.setForceAccessTokenUriHttps(false);
        filterConfig.setResponseMode("query");

        String authUrl = filterConfig.buildAuthorizationUrl().toString();
        assertTrue(authUrl.contains("response_mode=query"));
    }

    @Test
    public void testEndSessionUrl() {
        OpenIdConnectFilterConfig filterConfig = getFilterConfig();
        filterConfig.setPostLogoutRedirectUri("http://localhost:8080/post-redirect");
        String logouturl = filterConfig.buildEndSessionUrl("aToken").toString();
        assertTrue(
                logouturl.contains(
                        "?id_token_hint=aToken&post_logout_redirect_uri=http://localhost:8080/post-redirect"));
    }

    @Test
    public void testEndSessionUrlNullToken() {
        OpenIdConnectFilterConfig filterConfig = getFilterConfig();
        filterConfig.setPostLogoutRedirectUri("http://localhost:8080/post-redirect");
        String logouturl = filterConfig.buildEndSessionUrl(null).toString();
        assertTrue(
                logouturl.contains(
                        "?post_logout_redirect_uri=http://localhost:8080/post-redirect"));
    }
}
