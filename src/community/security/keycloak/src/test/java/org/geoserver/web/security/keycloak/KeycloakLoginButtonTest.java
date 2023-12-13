/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.keycloak;

import static org.geoserver.security.keycloak.GeoServerKeycloakFilterTest.AUTH_URL;
import static org.geoserver.security.keycloak.GeoServerKeycloakFilterTest.CLIENT_ID;
import static org.geoserver.security.keycloak.GeoServerKeycloakFilterTest.REALM;
import static org.junit.Assert.assertTrue;

import java.util.logging.Level;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.keycloak.GeoServerKeycloakFilter;
import org.geoserver.security.keycloak.GeoServerKeycloakFilterConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;

public class KeycloakLoginButtonTest extends GeoServerWicketTestSupport {

    @Override
    protected String getLogConfiguration() {
        return "DEFAULT_LOGGING";
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerKeycloakFilterConfig filterConfig = new GeoServerKeycloakFilterConfig();
        AdapterConfig aConfig = new AdapterConfig();
        aConfig.setRealm(REALM);
        aConfig.setResource(CLIENT_ID);
        aConfig.setAuthServerUrl(AUTH_URL);
        filterConfig.setName("keycloaklogin");
        filterConfig.setClassName(GeoServerKeycloakFilter.class.getName());
        filterConfig.setEnableRedirectEntryPoint(false);
        filterConfig.writeAdapterConfig(aConfig);
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("keycloaklogin", "anonymous");
        manager.saveSecurityConfig(config);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data to setup
    }

    @Test
    public void testLoginButton() {
        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();
        LOGGER.log(Level.INFO, "Last HTML page output:\n" + html);

        // the login form is there and has the link
        assertTrue(
                html.contains(
                        "<form class=\"d-inline-block\" method=\"post\" action=\"http://localhost/context/web?j_spring_keycloak_login=true\">"));
        // the img is there as well
        assertTrue(
                html.contains(
                        "<img src=\"./wicket/resource/org.geoserver.web.security.keycloak.KeycloakAuthFilterPanel/keycloak"));
    }
}
