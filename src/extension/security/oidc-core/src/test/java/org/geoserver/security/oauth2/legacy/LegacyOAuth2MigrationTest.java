/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.legacy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.SecurityConfigDiagnostics;
import org.geoserver.security.SecurityConfigDiagnostics.DisabledComponent;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

/**
 * Verifies that, with the OIDC community module installed, authentication filters created by the removed OAuth2/OIDC
 * community plugins (OpenID Connect / Azure AD, Keycloak, Google, GitHub, GeoNode) found in an old data directory are
 * recognized precisely (alias + originating plugin), disabled, removed from the filter chains non-destructively, and do
 * not prevent GeoServer from starting.
 */
public class LegacyOAuth2MigrationTest extends GeoServerSystemTestSupport {

    private void install(String fixture, String securityPath) throws IOException {
        try (InputStream in = getClass().getResourceAsStream(fixture)) {
            assertNotNull("Missing test fixture: " + fixture, in);
            Resource target = getResourceLoader().get(securityPath);
            try (OutputStream out = target.out()) {
                in.transferTo(out);
            }
        }
    }

    private static DisabledComponent byName(GeoServerSecurityManager m, String name) {
        return m.getConfigDiagnostics().getDisabledComponents().stream()
                .filter(c -> name.equals(c.name()))
                .findFirst()
                .orElse(null);
    }

    @Test
    public void testLegacyOAuth2FiltersRecognizedDisabledAndStripped() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // a 2.28.x data directory produced by the now-removed community plugins
        install("openIdConnect-filter.xml", "security/filter/oidc/config.xml");
        install("keycloak-filter.xml", "security/filter/keycloak/config.xml");
        install("google-filter.xml", "security/filter/google/config.xml");
        install("github-filter.xml", "security/filter/github/config.xml");
        install("geonode-filter.xml", "security/filter/geonode/config.xml");

        // reference a couple of them in real chains, ahead of the trailing anonymous filter
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        config.getFilterChain()
                .getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME)
                .getFilterNames()
                .add(0, "oidc");
        config.getFilterChain()
                .getRequestChainByName(GeoServerSecurityFilterChain.REST_CHAIN_NAME)
                .getFilterNames()
                .add(0, "keycloak");
        secMgr.saveSecurityConfig(config);

        // reload() is the startup code path; it must NOT throw
        secMgr.reload();

        SecurityConfigDiagnostics diag = secMgr.getConfigDiagnostics();
        assertFalse("expected disabled components to be reported", diag.isEmpty());

        // OpenID Connect (also the Azure AD path) recognized precisely
        DisabledComponent oidc = byName(secMgr, "oidc");
        assertNotNull(oidc);
        assertEquals("openIdConnectAuthentication", oidc.alias());
        assertTrue(oidc.sourcePlugin().contains("openid-connect"));
        assertTrue(oidc.sourcePlugin().toLowerCase().contains("azure"));

        // Keycloak, Google, GitHub, GeoNode all recognized
        DisabledComponent keycloak = byName(secMgr, "keycloak");
        assertNotNull(keycloak);
        assertEquals("keycloakAdapter", keycloak.alias());
        assertTrue(keycloak.sourcePlugin().contains("keycloak"));
        assertNotNull(byName(secMgr, "google"));
        assertNotNull(byName(secMgr, "github"));
        assertNotNull(byName(secMgr, "geonode"));

        // the referenced filters were removed from the in-memory chains
        assertFalse(secMgr.getSecurityConfig()
                .getFilterChain()
                .getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME)
                .getFilterNames()
                .contains("oidc"));
        assertFalse(secMgr.getSecurityConfig()
                .getFilterChain()
                .getRequestChainByName(GeoServerSecurityFilterChain.REST_CHAIN_NAME)
                .getFilterNames()
                .contains("keycloak"));

        // non-destructive: the on-disk configuration still references the filter until manually migrated
        assertTrue(secMgr.loadSecurityConfig()
                .getFilterChain()
                .getRequestChainByName(GeoServerSecurityFilterChain.WEB_CHAIN_NAME)
                .getFilterNames()
                .contains("oidc"));
    }

    /**
     * A data directory whose active role service was the now-removed Keycloak role service must not silently swap users
     * to the default roles: the placeholder is recognized (alias + originating plugin), recorded in the diagnostics
     * with the lost-roles consequence spelled out, and the security subsystem falls back to the default role service.
     */
    @Test
    public void testLegacyKeycloakRoleServiceRecognizedDisabledAndReported() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();

        // a 2.28.x data directory whose role service was provided by the now-removed gs-sec-keycloak plugin
        install("keycloak-roleservice.xml", "security/role/keycloakRoles/config.xml");

        // make it the active role service, exactly as the old data directory would have it
        SecurityManagerConfig config = secMgr.loadSecurityConfig();
        config.setRoleServiceName("keycloakRoles");
        secMgr.saveSecurityConfig(config);

        // reload() is the startup code path; it must NOT throw despite the unresolvable role service
        secMgr.reload();

        // (a) the disabled role service is reported with the precise keycloak alias / source plugin
        DisabledComponent roleService = byName(secMgr, "keycloakRoles");
        assertNotNull("the legacy keycloak role service should be reported as disabled", roleService);
        assertEquals(SecurityConfigDiagnostics.ComponentType.ROLE_SERVICE, roleService.type());
        assertEquals("keycloakRoleService", roleService.alias());
        assertTrue(
                "source plugin should identify keycloak, was: " + roleService.sourcePlugin(),
                roleService.sourcePlugin().toLowerCase().contains("keycloak"));

        // (b) the active role service fell back to the default one (roles still resolve, GeoServer still starts)
        assertEquals("default", secMgr.getActiveRoleService().getName());

        // (c) the message makes the consequence explicit: the operator is told roles are lost until reconfiguration
        String reason = roleService.reason().toLowerCase();
        assertTrue(
                "the reason should mention the fallback to the default role service: " + roleService.reason(),
                reason.contains("default role service"));
        assertTrue(
                "the reason should warn that role assignments are lost: " + roleService.reason(),
                reason.contains("lost"));
    }
}
