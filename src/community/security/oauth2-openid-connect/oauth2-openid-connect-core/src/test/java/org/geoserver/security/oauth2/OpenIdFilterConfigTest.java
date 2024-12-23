/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import org.apache.commons.io.FileUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

public class OpenIdFilterConfigTest extends GeoServerSystemTestSupport {

    @BeforeClass
    public static void init() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @AfterClass
    public static void finalizing() {
        System.setProperty("ALLOW_ENV_PARAMETRIZATION", "false");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        FileUtils.copyFileToDirectory(
                new File("./src/test/resources/geoserver-environment.properties"), testData.getDataDirectoryRoot());
    }

    private OpenIdConnectFilterConfig getFilterConfig() {
        OpenIdConnectFilterConfig filterConfig = new OpenIdConnectFilterConfig();
        String baseUrl = "http://localhost:8080";
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(OpenIdConnectAuthenticationFilter.class.getName());
        filterConfig.setCliendId("${oidc_client_id}");
        filterConfig.setClientSecret("${oidc_client_secret}");
        filterConfig.setAccessTokenUri(baseUrl + "/token");
        filterConfig.setUserAuthorizationUri(baseUrl + "/authorize");
        filterConfig.setCheckTokenEndpointUrl(baseUrl + "/userinfo");
        filterConfig.setLoginEndpoint("/j_spring_oauth2_openid_connect_login");
        filterConfig.setLogoutEndpoint("/j_spring_oauth2_openid_connect_logout");
        filterConfig.setScopes("${oidc_scopes}");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(true);
        filterConfig.setPrincipalKey("${oidc_principal_key}");
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
        assertTrue(logouturl.contains(
                "?id_token_hint=aToken&post_logout_redirect_uri=http://localhost:8080/post-redirect"));
    }

    @Test
    public void testEndSessionUrlNullToken() {
        OpenIdConnectFilterConfig filterConfig = getFilterConfig();
        filterConfig.setPostLogoutRedirectUri("http://localhost:8080/post-redirect");
        String logouturl = filterConfig.buildEndSessionUrl(null).toString();
        assertTrue(logouturl.contains("?post_logout_redirect_uri=http://localhost:8080/post-redirect"));
    }

    @Test
    public void testGeoServerFilterConfigParametrization() {
        OpenIdConnectFilterConfig filterConfig = getFilterConfig();

        OpenIdConnectFilterConfig target = (OpenIdConnectFilterConfig) filterConfig.clone(true);
        assertEquals("efhrufuu3uhu3uhu", target.getCliendId());
        assertEquals("48ru48tj58fr8jf", target.getClientSecret());
        assertEquals("email", target.getPrincipalKey());
        assertEquals("openid profile email phone address", target.getScopes());
    }
}
