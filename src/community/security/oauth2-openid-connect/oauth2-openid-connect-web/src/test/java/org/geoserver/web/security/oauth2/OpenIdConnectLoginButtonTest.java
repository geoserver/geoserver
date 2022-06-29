package org.geoserver.web.security.oauth2;

import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.OpenIdConnectAuthenticationFilter;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class OpenIdConnectLoginButtonTest extends GeoServerWicketTestSupport {

    @Override
    protected String getLogConfiguration() {
        return "DEFAULT_LOGGING";
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerSecurityManager manager = getSecurityManager();
        OpenIdConnectFilterConfig filterConfig = new OpenIdConnectFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(OpenIdConnectAuthenticationFilter.class.getName());
        filterConfig.setCliendId("foo");
        filterConfig.setClientSecret("bar");
        filterConfig.setAccessTokenUri("https://www.connectid/fake/test");
        filterConfig.setUserAuthorizationUri("https://www.connectid/fake/test");
        filterConfig.setCheckTokenEndpointUrl("https://www.connectid/fake/test");
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");
        manager.saveSecurityConfig(config);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data to setup, this is a smoke test
    }

    @Test
    public void testLoginButton() {
        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertTrue(
                html.contains(
                        "<form style=\"display: inline-block;\" method=\"post\" action=\"http://localhost/context/web/j_spring_oauth2_openid_connect_login\">"));
        assertTrue(
                html.contains(
                        "<img src=\"./wicket/resource/org.geoserver.web.security.oauth2.OpenIdConnectAuthProviderPanel/openid"));
    }
}
