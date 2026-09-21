/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

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

    /**
     * The 2.28.x constructor resolves the post-logout redirect URI eagerly, so a proxy base must be in place before one
     * can be built at all. {@link #setUp()} deliberately clears it for isolation, so every test that constructs a
     * config sets it first.
     */
    private GeoServerOAuth2LoginFilterConfig newConfig() {
        System.setProperty(GeoServerOAuth2LoginFilterConfig.OPENID_TEST_GS_PROXY_BASE, "http://example/gs");
        return new GeoServerOAuth2LoginFilterConfig();
    }

    /**
     * The shipped default. A configuration that has never seen this option must behave as if admin logins are allowed,
     * which is what makes the upgrade a no-op.
     */
    @Test
    public void testAllowAdminLoginDefaultsToTrue() {
        assertTrue(newConfig().getAllowAdminLogin());
    }

    /**
     * The field is a nullable Boolean rather than a primitive because security configurations are deserialized through
     * an Unsafe-based reflection provider, which does not run field initializers. This pins the raw field staying null
     * while the getter still answers true.
     */
    @Test
    public void testAllowAdminLoginIsNullUntilSetButReadsAsTrue() throws Exception {
        GeoServerOAuth2LoginFilterConfig lConfig = newConfig();
        java.lang.reflect.Field lField = GeoServerOAuth2LoginFilterConfig.class.getDeclaredField("allowAdminLogin");
        lField.setAccessible(true);
        assertNull("raw field must stay null so a pre-existing datadir is not read as false", lField.get(lConfig));
        assertTrue(lConfig.getAllowAdminLogin());
    }

    @Test
    public void testAllowAdminLoginRoundTrips() {
        GeoServerOAuth2LoginFilterConfig lConfig = newConfig();
        lConfig.setAllowAdminLogin(Boolean.FALSE);
        assertFalse(lConfig.getAllowAdminLogin());
        lConfig.setAllowAdminLogin(Boolean.TRUE);
        assertTrue(lConfig.getAllowAdminLogin());
    }

    /** root is refused whatever allowAdminLogin says; admin only when it is off; anything else is never refused. */
    @Test
    public void testIsPrincipalBlocked() {
        GeoServerOAuth2LoginFilterConfig lConfig = newConfig();

        // default: admin allowed, root still refused
        assertFalse(lConfig.isPrincipalBlocked("admin"));
        assertTrue(lConfig.isPrincipalBlocked("root"));
        assertFalse(lConfig.isPrincipalBlocked("someuser"));
        assertFalse("a null principal must not blow up", lConfig.isPrincipalBlocked(null));

        // disallowed: both refused
        lConfig.setAllowAdminLogin(Boolean.FALSE);
        assertTrue(lConfig.isPrincipalBlocked("admin"));
        assertTrue(lConfig.isPrincipalBlocked("root"));
        assertFalse(lConfig.isPrincipalBlocked("someuser"));
    }

    /** The comparison is case-insensitive, so "Admin" and "ROOT" cannot slip past the guard. */
    @Test
    public void testIsPrincipalBlockedIsCaseInsensitive() {
        GeoServerOAuth2LoginFilterConfig lConfig = newConfig();
        lConfig.setAllowAdminLogin(Boolean.FALSE);
        assertTrue(lConfig.isPrincipalBlocked("Admin"));
        assertTrue(lConfig.isPrincipalBlocked("ADMIN"));
        assertTrue(lConfig.isPrincipalBlocked("Root"));
        assertTrue(lConfig.isPrincipalBlocked("ROOT"));
    }
}
