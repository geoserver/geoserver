/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/** Unit tests for {@link GeoServerOAuth2LoginFilterConfig}. */
public class GeoServerOAuth2LoginFilterConfigTest {

    @Before
    public void setUp() {
        System.clearProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE);
        System.clearProperty("PROXY_BASE_URL");
    }

    @After
    public void tearDown() {
        System.clearProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE);
        System.clearProperty("PROXY_BASE_URL");
    }

    /**
     * Regression test pinning the single-provider auto-redirect entry-point URL shape. On the {@code main} branch the
     * authorization-base constant lacks a trailing slash and produced the broken
     * {@code .../web/oauth2/authorizationoidc} URL; on 2.28.x the constant already includes the slash so the URL is
     * correctly delimited. This test guards against regressing that contract.
     */
    @Test
    public void testAuthenticationEntryPointRedirectUri_isProperlyDelimited() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://example/gs");
        GeoServerOAuth2LoginFilterConfig config = new GeoServerOAuth2LoginFilterConfig();
        config.setOidcEnabled(true);

        assertEquals(
                "http://example/gs/web/oauth2/authorization/oidc", config.getAuthenticationEntryPointRedirectUri());
    }
}
