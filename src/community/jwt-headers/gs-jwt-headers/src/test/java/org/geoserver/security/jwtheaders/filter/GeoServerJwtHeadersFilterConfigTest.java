/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.junit.Test;

public class GeoServerJwtHeadersFilterConfigTest {

    @Test
    public void testNewFilterDefaults() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        JwtConfiguration jwt = config.getJwtConfiguration();

        assertEquals(JwtConfiguration.UserNameHeaderFormat.JWT, jwt.getUserNameFormatChoice());
        assertTrue(jwt.isValidateToken());
        assertTrue(jwt.isValidateTokenSignature());
        assertTrue(jwt.isValidateTokenExpiry());
        assertTrue(jwt.isOnlyExternalListedRoles());
        assertFalse(config.getAllowAdminLogin());
        assertFalse(config.getTrustUnvalidatedRolesHeader());
    }

    @Test
    public void testSetJwtConfiguration() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        JwtConfiguration replacement = new JwtConfiguration();
        replacement.setUserNameHeaderAttributeName("X-User");

        config.setJwtConfiguration(replacement);

        assertSame(replacement, config.getJwtConfiguration());
        assertEquals("X-User", config.getUserNameHeaderAttributeName());
        assertFalse(config.isValidateToken());
    }

    /** Configurations written before these options existed read them back as null. */
    @Test
    public void testUnsetOptionsKeepPreviousBehaviour() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setAllowAdminLogin(null);
        config.setTrustUnvalidatedRolesHeader(null);

        assertTrue(config.getAllowAdminLogin());
        assertFalse(config.getTrustUnvalidatedRolesHeader());
    }

    @Test
    public void testRootIsAlwaysBlocked() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setAllowAdminLogin(true);

        assertTrue(config.isPrincipalBlocked("root"));
        assertTrue(config.isPrincipalBlocked("ROOT"));
        assertTrue(config.isPrincipalBlocked(" root "));
    }

    @Test
    public void testAdminBlockedUnlessAllowed() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        config.setAllowAdminLogin(false);
        assertTrue(config.isPrincipalBlocked("admin"));
        assertTrue(config.isPrincipalBlocked(" Admin"));

        config.setAllowAdminLogin(true);
        assertFalse(config.isPrincipalBlocked("admin"));

        config.setAllowAdminLogin(null);
        assertFalse(config.isPrincipalBlocked("admin"));
    }

    /** Invisible, spacing and compatibility variants of the names are the same account. */
    @Test
    public void testNameVariantsBlocked() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setAllowAdminLogin(false);

        assertTrue(config.isPrincipalBlocked("ad\u200Bmin"));
        assertTrue(config.isPrincipalBlocked("admin\u00A0"));
        assertTrue(config.isPrincipalBlocked("\uFF41\uFF44\uFF4D\uFF49\uFF4E"));
        assertTrue(config.isPrincipalBlocked("r\u00ADoot"));
        assertTrue(config.isPrincipalBlocked("\uFF52\uFF4F\uFF4F\uFF54"));
    }

    @Test
    public void testRolesHeaderBinding() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setUserNameHeaderAttributeName("Authorization");
        config.setRoleSource(GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT);

        assertTrue(config.rolesHeaderIsUserNameHeader());
        assertFalse(config.readsRolesFromUnvalidatedHeader());

        jwt.setRolesHeaderName(" authorization ");
        assertTrue(config.rolesHeaderIsUserNameHeader());

        jwt.setRolesHeaderName("X-Roles");
        assertFalse(config.rolesHeaderIsUserNameHeader());
        assertTrue(config.readsRolesFromUnvalidatedHeader());

        config.setTrustUnvalidatedRolesHeader(true);
        assertFalse(config.readsRolesFromUnvalidatedHeader());

        config.setTrustUnvalidatedRolesHeader(null);
        jwt.setValidateToken(false);
        assertFalse(config.readsRolesFromUnvalidatedHeader());
    }

    @Test
    public void testOtherPrincipalsNotBlocked() {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();

        assertFalse(config.isPrincipalBlocked(null));
        assertFalse(config.isPrincipalBlocked("alice"));
        assertFalse(config.isPrincipalBlocked("rooter"));
        assertFalse(config.isPrincipalBlocked("administrator"));
    }
}
