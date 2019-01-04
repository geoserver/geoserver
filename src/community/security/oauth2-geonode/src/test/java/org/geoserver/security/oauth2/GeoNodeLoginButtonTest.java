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
package org.geoserver.security.oauth2;

import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class GeoNodeLoginButtonTest extends GeoServerWicketTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GeoServerSecurityManager manager = getSecurityManager();
        GeoNodeOAuth2FilterConfig filterConfig = new GeoNodeOAuth2FilterConfig();
        filterConfig.setName("geonode");
        filterConfig.setClassName(GeoNodeOAuthAuthenticationFilter.class.getName());
        filterConfig.setCliendId("foo");
        filterConfig.setClientSecret("bar");
        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("geonode", "anonymous");
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
        LOGGER.info("Last page HTML:\n" + html);

        // the login form is there and has the link
        assertTrue(
                html.contains(
                        "<form style=\"display: inline-block;\" method=\"post\" action=\"../web/j_spring_oauth2_geonode_login\">"));
        assertTrue(
                html.contains(
                        "<img src=\"./wicket/resource/org.geoserver.web.security.oauth2.GeoNodeOAuth2AuthProviderPanel/geonode"));
    }
}
