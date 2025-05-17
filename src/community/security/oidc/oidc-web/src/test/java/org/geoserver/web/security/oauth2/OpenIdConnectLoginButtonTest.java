package org.geoserver.web.security.oauth2;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.TestSetup;
import org.geoserver.test.TestSetupFrequency;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.BeforeClass;
import org.junit.Test;

@TestSetup(run = TestSetupFrequency.REPEAT)
public class OpenIdConnectLoginButtonTest extends GeoServerWicketTestSupport {

    private static final String MARKUP_IMG =
            "<img src=\"./wicket/resource/org.geoserver.web.security.oauth2.login.OAuth2LoginAuthProviderPanel/openid";
    private static final String MARKUP_FORM =
            "<form class=\"d-inline-block\" method=\"GET\" action=\"http://localhost/context/oauth2/authorization/oidc\">";

    @Override
    protected String getLogConfiguration() {
        return "DEFAULT_LOGGING";
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    private void activateOidcFilterWithEnabledState(boolean pEnabled)
            throws IOException, SecurityConfigException, Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig filterConfig = new GeoServerOAuth2LoginFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        filterConfig.setOidcEnabled(pEnabled);
        filterConfig.setOidcClientId("foo");
        filterConfig.setOidcClientSecret("bar");
        filterConfig.setOidcTokenUri("https://www.connectid/fake/test");
        filterConfig.setOidcAuthorizationUri("https://www.connectid/fake/test");
        filterConfig.setOidcUserInfoUri("https://www.connectid/fake/test");
        filterConfig.setOidcJwkSetUri("https://www.connectid/fake/test");
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

    @BeforeClass
    public static void setup() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://localhost/geoserver");
    }

    @Test
    public void testLoginButtonPresentWithOidcEnabled() throws SecurityConfigException, IOException, Exception {
        boolean lOidcEnabled = true;
        activateOidcFilterWithEnabledState(lOidcEnabled);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertTrue(html.contains(MARKUP_FORM));
        assertTrue(html.contains(MARKUP_IMG));
    }

    @Test
    public void testLoginButtonOmittedWithOidcDisabled() throws SecurityConfigException, IOException, Exception {
        boolean lOidcEnabled = false;
        activateOidcFilterWithEnabledState(lOidcEnabled);

        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertFalse(html.contains(MARKUP_FORM));
        assertFalse(html.contains(MARKUP_IMG));
    }
}
