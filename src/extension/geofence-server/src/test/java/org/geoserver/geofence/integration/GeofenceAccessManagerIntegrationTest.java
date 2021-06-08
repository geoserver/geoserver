/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.integration;

import static org.geoserver.geofence.integration.GeofenceGetMapIntegrationTest.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.*;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.GeofenceAccessManager;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

public class GeofenceAccessManagerIntegrationTest extends GeoServerSystemTestSupport {

    private GeofenceAccessManager accessManager;
    private RuleAdminService ruleService;

    private static final String AREA_WKT =
            "MULTIPOLYGON(((0.0016139656066815888 -0.0006386457758059581,0.0019599705696027314 -0.0006386457758059581,0.0019599705696027314 -0.0008854090051601674,0.0016139656066815888 -0.0008854090051601674,0.0016139656066815888 -0.0006386457758059581)))";

    private static final String AREA_WKT_2 =
            "MULTIPOLYGON(((0.0011204391479413545 -0.0006405065746780663,0.0015764146804730927 -0.0006405065746780663,0.0015764146804730927 -0.0014612625330857614,0.0011204391479413545 -0.0014612625330857614,0.0011204391479413545 -0.0006405065746780663)))";

    @Before
    public void setUp() {
        accessManager =
                applicationContext.getBean(
                        "geofenceRuleAccessManager", GeofenceAccessManager.class);
        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
        // add rule to grant access to all to everything with a very low priority
        if (ruleService.getRuleByPriority(9999) == null)
            addRule(GrantType.ALLOW, null, null, null, null, null, null, 9999, ruleService);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @Test
    public void testAllowedAreaLayerInTwoGroups() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, if one of the container
        // LayerGroup doesn't have an allowed area, there will be no allowedArea in the final
        // filter.
        Long idRule = null;
        Long idRule2 = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();

            LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
            LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

            group1 =
                    createsLayerGroup(
                            catalog,
                            "group21",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(places, forests));
            group2 =
                    createsLayerGroup(
                            catalog,
                            "group22",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(places, forests));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group21",
                            0,
                            ruleService);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group22",
                            1,
                            ruleService);

            // add allowed Area only to the first layer group
            addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326, ruleService);

            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, places);

            assertEquals(vl.getReadFilter(), Filter.INCLUDE);
            assertEquals(vl.getWriteFilter(), Filter.INCLUDE);
            logout();
        } finally {
            removeLayerGroup(group1, group2);
            deleteRules(ruleService, idRule, idRule2);
        }
    }

    @Test
    public void testAllowedAreaLayerInTwoGroups2() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, the allowedArea
        // applied to the filter is the union of the allowed area of each LayerGroup
        Long idRule = null;
        Long idRule2 = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo bridges = catalog.getLayerByName(getLayerId(MockData.BRIDGES));
            LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
            group1 =
                    createsLayerGroup(
                            catalog,
                            "group1",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(bridges, buildings));
            group2 =
                    createsLayerGroup(
                            catalog,
                            "group2",
                            LayerGroupInfo.Mode.NAMED,
                            null,
                            Arrays.asList(bridges, buildings));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group1",
                            2,
                            ruleService);
            // add allowed Area
            addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326, ruleService);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group2",
                            3,
                            ruleService);

            // add allowed Area to layer groups rules
            addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326, ruleService);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();
            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, bridges);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_2);
            MultiPolygon totalArea = (MultiPolygon) allowedArea1.union(allowedArea2);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            Intersects intersects2 = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    intersects2.getExpression2().evaluate(null, MultiPolygon.class);
            totalArea.normalize();
            // normalize geometries to avoids assertion failures for
            // a different internal order of the polygons
            readFilterArea.normalize();
            writeFilterArea.normalize();
            assertTrue(totalArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(totalArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1, group2);
            deleteRules(ruleService, idRule, idRule2);
        }
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsModeSingle() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to LayerGroups with SINGLE mode, the LayerGroup
        // is not applied
        Long idRule = null;
        Long idRule2 = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 =
                    createsLayerGroup(
                            catalog,
                            "group31",
                            LayerGroupInfo.Mode.SINGLE,
                            null,
                            Arrays.asList(lakes, fifteen));
            group2 =
                    createsLayerGroup(
                            catalog,
                            "group32",
                            LayerGroupInfo.Mode.SINGLE,
                            null,
                            Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            4,
                            ruleService);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group32",
                            5,
                            ruleService);

            // add allowed Area to layer groups rules
            addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326, ruleService);
            addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326, ruleService);

            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            assertEquals(vl.getReadFilter(), Filter.INCLUDE);
            assertEquals(vl.getWriteFilter(), Filter.INCLUDE);
            logout();
        } finally {
            removeLayerGroup(group1, group2);
            deleteRules(ruleService, idRule, idRule2);
        }
    }

    protected Authentication getUser(String username, String password, String... roles) {

        List<GrantedAuthority> l = new ArrayList<GrantedAuthority>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(username, password, l);
    }

    protected LayerGroupInfo createsLayerGroup(
            Catalog catalog,
            String name,
            LayerGroupInfo.Mode mode,
            LayerInfo rootLayer,
            List<LayerInfo> layers)
            throws Exception {
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(name);

        group.setMode(mode);
        if (rootLayer != null) {
            group.setRootLayer(rootLayer);
            group.setRootLayerStyle(rootLayer.getDefaultStyle());
        }
        for (LayerInfo li : layers) group.getLayers().add(li);
        group.getStyles().add(null);
        group.getStyles().add(null);

        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.calculateLayerGroupBounds(group);

        catalog.add(group);
        return group;
    }

    private void removeLayerGroup(LayerGroupInfo... groups) {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
        for (LayerGroupInfo group : groups) {
            if (group != null) {
                getCatalog().remove(group);
            }
        }
        logout();
    }
}
