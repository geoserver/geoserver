/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence;

import static org.junit.Assert.assertEquals;

import java.util.TreeSet;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.PermsResult;
import org.geoserver.geofence.services.dto.RuleFilter;
import org.geoserver.geofence.utils.RuleReaderServiceAdapter;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.Or;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.PropertyName;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

/**
 * Tests for the {@link GeofenceAccessManager#getSecurityFilter} method added as part of
 * GEOS-12096.
 *
 * <p>These tests verify the new GeoFence-backed catalog pre-filter behavior without requiring a
 * live GeoFence service. A controlled {@link RuleReaderService} stub is injected to supply
 * predictable {@link PermsResult} values.
 */
public class GeofenceAccessManagerSecurityFilterTest extends GeofenceBaseTest {

    private RuleReaderService originalRulesService;

    @Before
    public void saveOriginalRulesService() {
        originalRulesService = accessManager.rulesService;
    }

    @After
    public void restoreOriginalRulesService() {
        accessManager.rulesService = originalRulesService;
    }

    // -----------------------------------------------------------------------
    // Admin user
    // -----------------------------------------------------------------------

    @Test
    public void testAdminGetSecurityFilterReturnsInclude() {
        Authentication admin = getUser("admin", "geoserver", "ROLE_ADMINISTRATOR");
        // Admin users must always receive Filter.INCLUDE so every catalog item is visible
        assertEquals(Filter.INCLUDE, accessManager.getSecurityFilter(admin, LayerInfo.class));
        assertEquals(Filter.INCLUDE, accessManager.getSecurityFilter(admin, WorkspaceInfo.class));
        assertEquals(Filter.INCLUDE, accessManager.getSecurityFilter(admin, LayerGroupInfo.class));
    }

    // -----------------------------------------------------------------------
    // Null PermsResult (GeoFence unavailable or returned null)
    // -----------------------------------------------------------------------

    @Test
    public void testNullPermsResultReturnsExclude() {
        // Inject a stub that always returns null from getPermissionFilter
        accessManager.rulesService = new RuleReaderServiceAdapter() {
            @Override
            public PermsResult getPermissionFilter(RuleFilter filter) {
                return null;
            }
        };

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        assertEquals(
                "Null PermsResult from GeoFence should produce Filter.EXCLUDE to deny access",
                Filter.EXCLUDE,
                accessManager.getSecurityFilter(user, LayerInfo.class));
    }

    // -----------------------------------------------------------------------
    // Non-admin user with PermsResult – WorkspaceInfo
    // -----------------------------------------------------------------------

    @Test
    public void testWorkspaceFilterDerivedFromAccessibleResources() {
        // Stub returns a PermsResult that grants access to "ws1:layerA" and "ws2:layerB"
        accessManager.rulesService = buildStubService(buildPermsResult("INCLUDE", "ws1:layerA", "ws2:layerB"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        Filter result = accessManager.getSecurityFilter(user, WorkspaceInfo.class);

        // Expect an OR of two workspace-name equality checks (ws1, ws2)
        Or orFilter = assertOr(result);
        assertEquals(
                "Expected one filter branch per distinct workspace",
                2,
                orFilter.getChildren().size());
    }

    @Test
    public void testWorkspaceFilterGlobalWildcard() {
        accessManager.rulesService = buildStubService(buildPermsResult("INCLUDE", "*:*"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        assertEquals(
                "Global '*:*' resource grant should produce Filter.INCLUDE for workspaces",
                Filter.INCLUDE,
                accessManager.getSecurityFilter(user, WorkspaceInfo.class));
    }

    @Test
    public void testWorkspaceFilterEmptyResourcesExcluded() {
        accessManager.rulesService = buildStubService(buildPermsResult("INCLUDE"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        assertEquals(
                "Empty accessible resources should produce Filter.EXCLUDE for workspaces",
                Filter.EXCLUDE,
                accessManager.getSecurityFilter(user, WorkspaceInfo.class));
    }

    // -----------------------------------------------------------------------
    // Non-admin user with PermsResult – LayerInfo
    // -----------------------------------------------------------------------

    @Test
    public void testLayerInfoFilterWorkspacePropertyRemapped() {
        accessManager.rulesService = buildStubService(buildPermsResult("workspace = 'myws'"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        Filter result = accessManager.getSecurityFilter(user, LayerInfo.class);

        PropertyIsEqualTo eq = assertPropertyIsEqualTo(result);
        assertEquals(
                "workspace property in CQL must be remapped to 'resource.store.workspace.name' for LayerInfo",
                "resource.store.workspace.name",
                ((PropertyName) eq.getExpression1()).getPropertyName());
        assertEquals("myws", eq.getExpression2().evaluate(null));
    }

    @Test
    public void testLayerInfoFilterCqlInclude() {
        accessManager.rulesService = buildStubService(buildPermsResultNoCqlResources("INCLUDE"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        assertEquals(
                "CQL 'INCLUDE' should pass through as Filter.INCLUDE for LayerInfo",
                Filter.INCLUDE,
                accessManager.getSecurityFilter(user, LayerInfo.class));
    }

    @Test
    public void testLayerInfoFilterCqlExclude() {
        accessManager.rulesService = buildStubService(buildPermsResultNoCqlResources("EXCLUDE"));

        Authentication user = getUser("testuser", "password", "ROLE_AUTHENTICATED");
        assertEquals(
                "CQL 'EXCLUDE' should pass through as Filter.EXCLUDE for LayerInfo",
                Filter.EXCLUDE,
                accessManager.getSecurityFilter(user, LayerInfo.class));
    }

    // -----------------------------------------------------------------------
    // Anonymous user
    // -----------------------------------------------------------------------

    @Test
    public void testAnonymousUserGetSecurityFilter() {
        // Anonymous authentication should be treated like any non-admin user
        accessManager.rulesService = buildStubService(buildPermsResultNoCqlResources("EXCLUDE"));

        Authentication anon = new AnonymousAuthenticationToken(
                "key", "anonymous", java.util.List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        assertEquals(
                "Anonymous user should not bypass GeoFence permission filter",
                Filter.EXCLUDE,
                accessManager.getSecurityFilter(anon, LayerInfo.class));
    }

    @Test
    public void testNullUserGetSecurityFilter() {
        // null authentication should be treated like any non-admin user
        accessManager.rulesService = buildStubService(buildPermsResultNoCqlResources("INCLUDE"));

        assertEquals(
                "Null authentication should not bypass GeoFence permission filter",
                Filter.INCLUDE,
                accessManager.getSecurityFilter(null, LayerInfo.class));
    }

    // -----------------------------------------------------------------------
    // Helper / factory methods
    // -----------------------------------------------------------------------

    private static RuleReaderService buildStubService(PermsResult permsResult) {
        return new RuleReaderServiceAdapter() {
            @Override
            public PermsResult getPermissionFilter(RuleFilter filter) {
                return permsResult;
            }
        };
    }

    private static PermsResult buildPermsResult(String cqlFilter, String... resources) {
        PermsResult pr = new PermsResult();
        pr.setCqlFilter(cqlFilter);
        TreeSet<String> set = new TreeSet<>();
        for (String r : resources) {
            set.add(r);
        }
        pr.setAccessibleResources(set);
        return pr;
    }

    /** Creates a PermsResult with the given CQL filter and no accessible resources. */
    private static PermsResult buildPermsResultNoCqlResources(String cqlFilter) {
        PermsResult pr = new PermsResult();
        pr.setCqlFilter(cqlFilter);
        return pr;
    }

    private static PropertyIsEqualTo assertPropertyIsEqualTo(Filter filter) {
        if (!(filter instanceof PropertyIsEqualTo)) {
            throw new AssertionError("Expected PropertyIsEqualTo, got: " + filter.getClass().getSimpleName());
        }
        return (PropertyIsEqualTo) filter;
    }

    private static Or assertOr(Filter filter) {
        if (!(filter instanceof Or)) {
            throw new AssertionError("Expected Or filter, got: " + filter.getClass().getSimpleName());
        }
        return (Or) filter;
    }
}
