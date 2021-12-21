/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */
package org.geoserver.security.keycloak;

import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;
import org.keycloak.representations.adapters.config.AdapterConfig;
import org.springframework.security.core.context.SecurityContextHolder;

/** Tests for {@link org.geoserver.security.keycloak.GeoServerHomePage}. */
public class GeoServerKeycloakFilterButtonTest extends GeoServerWicketTestSupport {

    // identifiers for the auth context
    public static final String REALM = "ImaginaryRealm";
    public static final String CLIENT_ID = "DistinguishedPerson";

    // locations for useful resources
    public static final String AUTH_URL = "https://place:8000/auth";
    public static final String OPENID_URL = AUTH_URL + "/realms/" + REALM;

    private SecurityManagerConfig smConfig;
    private GeoServerSecurityManager manager;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        manager = getSecurityManager();

        AdapterConfig aConfig = new AdapterConfig();
        aConfig.setRealm(REALM);
        aConfig.setResource(CLIENT_ID);
        aConfig.setAuthServerUrl(AUTH_URL);

        GeoServerKeycloakFilterConfig keycloakConfig = new GeoServerKeycloakFilterConfig();

        keycloakConfig.writeAdapterConfig(aConfig);
        keycloakConfig.setName("geoserver");
        keycloakConfig.setClassName(GeoServerKeycloakFilter.class.getName());

        manager.saveFilter(keycloakConfig);

        smConfig = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = smConfig.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("geoserver", "anonymous");
        manager.saveSecurityConfig(smConfig);
    }

    @Override
    protected void onTearDown(SystemTestData testData) throws Exception {
        super.onTearDown(testData);
        SecurityContextHolder.getContext().setAuthentication(null);
        manager.destroy();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no test data to setup, this is a smoke test
    }

    @Test
    public void testLoginButton() {
        tester.startPage(GeoServerHomePage.class);
        String html = tester.getLastResponseAsString();

        // the login form is there and has the link
        assertTrue(
                html.matches(
                        "(?s).*<form style=\"display: inline-block;\" method=\"post\" action=\".*j_spring_keycloak_security_login\">.*"));
        assertTrue(
                html.matches(
                        "(?s).*<img src=.*org.geoserver.web.security.keycloak.KeycloakAuthFilterPanel.*"));
    }
}
