/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.integration;

import static org.geoserver.catalog.LayerGroupInfo.Mode.NAMED;
import static org.geoserver.catalog.LayerGroupInfo.Mode.SINGLE;
import static org.geoserver.geofence.core.model.enums.AdminGrantType.ADMIN;
import static org.geoserver.geofence.core.model.enums.AdminGrantType.USER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.geofence.GeofenceAccessManager;
import org.geoserver.geofence.config.GeoFenceConfiguration;
import org.geoserver.geofence.config.GeoFenceConfigurationManager;
import org.geoserver.geofence.core.model.enums.CatalogMode;
import org.geoserver.geofence.core.model.enums.GrantType;
import org.geoserver.geofence.core.model.enums.SpatialFilterType;
import org.geoserver.geofence.services.RuleAdminService;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.geometry.jts.JTS;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.opengis.filter.Filter;
import org.opengis.filter.spatial.Intersects;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

public class GeofenceAccessManagerIntegrationTest extends GeoServerSystemTestSupport {

    public GeofenceIntegrationTestSupport support;

    private GeofenceAccessManager accessManager;
    private GeoFenceConfigurationManager configManager;
    private RuleAdminService ruleService;

    private static final String AREA_WKT =
            "MULTIPOLYGON(((0.0016139656066815888 -0.0006386457758059581,0.0019599705696027314 -0.0006386457758059581,0.0019599705696027314 -0.0008854090051601674,0.0016139656066815888 -0.0008854090051601674,0.0016139656066815888 -0.0006386457758059581)))";

    private static final String AREA_WKT_2 =
            "MULTIPOLYGON(((0.0011204391479413545 -0.0006405065746780663,0.0015764146804730927 -0.0006405065746780663,0.0015764146804730927 -0.0014612625330857614,0.0011204391479413545 -0.0014612625330857614,0.0011204391479413545 -0.0006405065746780663)))";

    private static final String AREA_WKT_3 =
            "MULTIPOLYGON (((0.00136827777777778 0.002309, 0.00372027777777778 0.00224366666666667, 0.00244083333333333 -0.00133877777777778, 0.00044272222222222 -0.00131155555555556, 0.00136827777777778 0.002309)))";

    private static final String AREA_WKT_4 =
            "MULTIPOLYGON (((0.00099261111111111 0.00175366666666667, 0.00298527777777778 0.00110577777777778, 0.00188005555555556 -0.00123533333333333, 0.00107972222222222 -0.00126255555555556, 0.00057338888888889 0.00096422222222222, 0.00099261111111111 0.00175366666666667)))";

    private static final String AREA_WKT_INTERSECT_1 =
            "MULTIPOLYGON (((-0.15605493133583015 0.52434456928838946, 0.22097378277153568 0.51435705368289641, 0.22846441947565554 0.2247191011235955, -0.06866416978776524 0.23470661672908866, -0.15605493133583015 0.52434456928838946)))";

    private static final String AREA_WKT_INTERSECT_2 =
            "MULTIPOLYGON (((-0.2359550561797753 0.36704119850187267, 0.37328339575530589 0.33957553058676659, 0.37328339575530589 0.25468164794007497, -0.16104868913857673 0.25717852684144826, -0.2359550561797753 0.36704119850187267)))";

    @Before
    public void setUp() {
        support =
                new GeofenceIntegrationTestSupport(
                        () -> GeoServerSystemTestSupport.applicationContext);
        support.before();
        accessManager =
                applicationContext.getBean(
                        "geofenceRuleAccessManager", GeofenceAccessManager.class);
        configManager = applicationContext.getBean(GeoFenceConfigurationManager.class);

        ruleService = (RuleAdminService) applicationContext.getBean("ruleAdminService");
        // add rule to grant access to all to everything with a very low priority
        if (ruleService.getRuleByPriority(9999) == null)
            support.addRule(GrantType.ALLOW, null, null, null, null, null, null, 9999);
    }

    @After
    public void clearRules() {
        support.after();
        SecurityContextHolder.getContext().setAuthentication(null);
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsEnlargement() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, if one of the container
        // LayerGroup doesn't have an allowed area, there will be no allowedArea in the final
        // filter.
        Long idRule = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();

            LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
            LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

            group1 = createsLayerGroup("group21", NAMED, null, Arrays.asList(places, forests));
            group2 = createsLayerGroup("group22", NAMED, null, Arrays.asList(places, forests));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group21",
                            0);

            // limit rule for anonymousUser on LayerGroup group2
            support.addRule(
                    GrantType.LIMIT,
                    "anonymousUser",
                    "ROLE_ANONYMOUS",
                    "WMS",
                    null,
                    null,
                    "group22",
                    1);

            // add allowed Area only to the first layer group
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);

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
        }
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsRestrictAccess() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, the allowedArea
        // applied to the filter is the intersection of the allowed area of each LayerGroup
        // if the groups' rules were defined for the same role.
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
            group1 = createsLayerGroup("group1", NAMED, null, Arrays.asList(bridges, buildings));
            group2 = createsLayerGroup("group2", NAMED, null, Arrays.asList(bridges, buildings));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group1",
                            2);
            // add allowed Area
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group2",
                            3);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_INTERSECT_2, 4326);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();
            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, bridges);

            // intersects the allowed areas for test
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT_INTERSECT_1);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_INTERSECT_2);
            MultiPolygon intersectionArea =
                    Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            Intersects intersects2 = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    intersects2.getExpression2().evaluate(null, MultiPolygon.class);
            intersectionArea.normalize();
            // normalize geometries to avoids assertion failures for
            // a different internal order of the polygons
            readFilterArea.normalize();
            writeFilterArea.normalize();
            assertTrue(intersectionArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(intersectionArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsEnlargement2() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one group the two areas are applied.
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            group2 = createsLayerGroup("group32", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            Long idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            4);

            // limit rule for anonymousUser on LayerGroup group2
            Long idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group32",
                            5);

            Long idRule3 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group32",
                            4);

            // limit rule for anonymousUser on LayerGroup group2
            Long idRule4 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group31",
                            5);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326);
            support.addRuleLimits(idRule3, CatalogMode.HIDE, AREA_WKT_3, 4326);
            support.addRuleLimits(idRule4, CatalogMode.HIDE, AREA_WKT_3, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_2);
            Geometry allowedArea3 = new WKTReader().read(AREA_WKT_3);
            Geometry intersectOne = allowedArea1.intersection(allowedArea3);
            Geometry intersectTwo = allowedArea2.intersection(allowedArea3);
            MultiPolygon unionedArea = (MultiPolygon) intersectOne.union(intersectTwo);
            unionedArea.normalize();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS", "ROLE_ANONYMOUS2"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects readFilter = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    readFilter.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            Intersects writeFilter = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    writeFilter.getExpression2().evaluate(null, MultiPolygon.class);
            writeFilterArea.normalize();
            assertTrue(unionedArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(unionedArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testAllowedAreaSRIDIsPreserved() throws Exception {
        // test that when adding an allowed area with a SRID different from
        // the layerGroup one, the final filter has been reprojected to the correct CRS
        Long idRule = null;
        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo basicPolygons = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 =
                    createsLayerGroup(
                            "group41", NAMED, null, Arrays.asList(basicPolygons, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group41",
                            7);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 3857);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, basicPolygons);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon allowedArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            allowedArea.normalize();

            Geometry geom = new WKTReader().read(AREA_WKT);
            geom.setSRID(3857);
            MathTransform mt =
                    CRS.findMathTransform(
                            CRS.decode("EPSG:3857"), basicPolygons.getResource().getCRS(), true);
            MultiPolygon reproj = (MultiPolygon) JTS.transform(geom, mt);
            reproj.normalize();
            assertTrue(allowedArea.equalsExact(reproj, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }

    @Test
    public void testLayerGroupsAllowedAreaWithDifferentSRIDS() throws Exception {
        // tests that when having a Layer directly accessed for WMS request
        // belonging to two LayerGroups each with a different CRS for the allowed area
        // the resulting geometry filter has a geometry that is a union of the two areas
        // in the correct CRS.
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;

        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo namedPlaces = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
            group1 = createsLayerGroup("group51", NAMED, null, Arrays.asList(lakes, namedPlaces));
            group2 = createsLayerGroup("group52", NAMED, null, Arrays.asList(lakes, namedPlaces));
            // limit rule for anonymousUser on LayerGroup group1
            Long idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group51",
                            8);

            // limit rule for anonymousUser on LayerGroup group1
            Long idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group52",
                            9);

            // limit rule for anonymousUser on LayerGroup group1
            Long idRule3 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group52",
                            8);

            // limit rule for anonymousUser on LayerGroup group1
            Long idRule4 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group51",
                            9);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);
            support.addRuleLimits(
                    idRule2,
                    CatalogMode.HIDE,
                    repWkt(AREA_WKT_2, CRS.decode("EPSG:4326"), CRS.decode("EPSG:3857"), 3857),
                    3857);
            support.addRuleLimits(idRule3, CatalogMode.HIDE, AREA_WKT_3, 4326);
            support.addRuleLimits(idRule4, CatalogMode.HIDE, AREA_WKT_4, 4326);

            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS", "ROLE_ANONYMOUS2"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon allowedArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            allowedArea.normalize();

            Geometry geom = new WKTReader().read(AREA_WKT);
            geom.setSRID(4326);
            Geometry geom2 = new WKTReader().read(AREA_WKT_2);
            geom2.setSRID(4326);
            Geometry geom3 = new WKTReader().read(AREA_WKT_3);
            geom.setSRID(4326);
            Geometry geom4 = new WKTReader().read(AREA_WKT_4);
            geom.setSRID(4326);
            Geometry intersect1 = geom.intersection(geom3);
            Geometry intersect2 = geom2.intersection(geom4);
            Geometry union = intersect1.union(intersect2);
            union.setSRID(4326);
            union.normalize();

            assertTrue(allowedArea.equalsExact(union, 10.0E-12));
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testLayerGroupsClipAndIntersectsSpatialFilterUnion() throws Exception {
        // test that when having two layergroups each with two rules with clip and intersects
        // allowed area, the geometries are correctly merged
        Long idRule = null;
        Long idRule2 = null;
        Long idRule3 = null;
        Long idRule4 = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;

        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo droutes = catalog.getLayerByName(getLayerId(MockData.DIVIDED_ROUTES));
            LayerInfo ponds = catalog.getLayerByName(getLayerId(MockData.PONDS));
            group1 = createsLayerGroup("group61", NAMED, null, Arrays.asList(droutes, ponds));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group61",
                            10);

            idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group61",
                            11);

            group2 = createsLayerGroup("group62", NAMED, null, Arrays.asList(droutes, ponds));
            // limit rule for anonymousUser on LayerGroup group1
            idRule3 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group62",
                            12);

            idRule4 =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            null,
                            "group62",
                            13);

            String areaWKT1 =
                    "MultiPolygon (((-97.48185823120911664 0.02172899055096349, -97.4667765271758384 0.02148629646307176, -97.46795532703131926 0.01663241470523705, -97.48165020770520073 0.01607768536148451, -97.48185823120911664 0.02172899055096349)))";
            String areaWKT2 =
                    "MultiPolygon (((-97.48109547836145339 0.026374848804891, -97.46934215039070182 0.02672155464473634, -97.46993155031843514 0.02294246099042217, -97.48102613719348142 0.02294246099042217, -97.48109547836145339 0.026374848804891)))";
            String areaWKT3 =
                    "MultiPolygon (((-97.48119949011341134 0.00914356856457779, -97.46941149155865958 0.00973296849231486, -97.46955017389460352 0.00605788658995429, -97.48182356062513065 0.00581519250206256, -97.48119949011341134 0.00914356856457779)))";
            String areaWKT4 =
                    "MultiPolygon (((-97.48161553712121474 0.00449771031065027, -97.46889143279889822 0.00435902797471214, -97.46948083272663155 0.00127334600008865, -97.48178889004114467 0.00120400483211958, -97.48161553712121474 0.00449771031065027)))";
            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, areaWKT1, 4326);
            support.addRuleLimits(
                    idRule2, CatalogMode.HIDE, areaWKT2, 4326, SpatialFilterType.CLIP);
            support.addRuleLimits(idRule3, CatalogMode.HIDE, areaWKT3, 4326);
            support.addRuleLimits(
                    idRule4, CatalogMode.HIDE, areaWKT4, 4326, SpatialFilterType.CLIP);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS", "ROLE_ANONYMOUS2"});

            VectorAccessLimits vl =
                    (VectorAccessLimits) accessManager.getAccessLimits(user, droutes);
            Geometry intersectsArea = vl.getIntersectVectorFilter();
            Geometry clipArea = vl.getClipVectorFilter();
            intersectsArea.normalize();
            clipArea.normalize();

            // union of the allowed area where the 3857 is reprojected to 4326
            WKTReader reader = new WKTReader();
            Geometry geom = reader.read(areaWKT1);
            geom.setSRID(4326);
            Geometry geom2 = new WKTReader().read(areaWKT2);
            geom2.setSRID(4326);

            Geometry geom3 = reader.read(areaWKT3);
            geom.setSRID(4326);
            Geometry geom4 = new WKTReader().read(areaWKT4);
            geom2.setSRID(4326);

            Geometry intersectIntersection = geom.intersection(geom3);
            intersectIntersection.setSRID(4326);
            intersectIntersection.normalize();

            Geometry clipIntersection = geom2.intersection(geom4);
            clipIntersection.setSRID(4326);
            clipIntersection.normalize();

            assertTrue(intersectsArea.equalsExact(intersectIntersection, 10.0E-15));
            assertTrue(clipArea.equalsExact(clipIntersection, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1, group2);
        }
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        configManager.getConfiguration().setGrantWriteToWorkspacesToAuthenticatedUsers(false);
        Authentication user = getUser("cite", "cite", "ROLE_AUTHENTICATED");

        // check workspace access
        WorkspaceInfo citeWS = getCatalog().getWorkspaceByName(MockData.CITE_PREFIX);
        WorkspaceAccessLimits wl = accessManager.getAccessLimits(user, citeWS);
        assertTrue(wl.isReadable());
        assertFalse(wl.isWritable());
    }

    @Test
    public void testAdminRule_WorkspaceAccessLimits_Role_based_rule() {
        final String citeUserRole = "CITE_USER";
        final String citeAdminRole = "CITE_ADMIN";
        final String sfUserRole = "SF_USER";
        final String sfAdminRole = "SF_ADMIN";

        final Authentication citeUser = getUser("citeuser", "cite", citeUserRole);
        final Authentication citeAdmin = getUser("citeadmin", "cite", citeAdminRole);
        final Authentication sfUser = getUser("sfuser", "sfuser", sfUserRole);
        final Authentication sfAdmin = getUser("sfadmin", "sfadmin", sfAdminRole);

        final WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        final WorkspaceInfo sf = getCatalog().getWorkspaceByName("sf");

        support.addAdminRule(0, null, citeAdminRole, cite.getName(), ADMIN);
        support.addAdminRule(1, null, citeUserRole, cite.getName(), USER);
        support.addAdminRule(2, null, sfAdminRole, sf.getName(), ADMIN);
        support.addAdminRule(3, null, sfUserRole, sf.getName(), USER);

        setUser(citeUser);
        assertAdminAccess(citeUser, cite, false);
        assertAdminAccess(citeUser, sf, false);

        setUser(sfUser);
        assertAdminAccess(sfUser, cite, false);
        assertAdminAccess(sfUser, sf, false);

        setUser(citeAdmin);
        assertAdminAccess(citeAdmin, cite, true);
        assertAdminAccess(citeAdmin, sf, false);

        setUser(sfAdmin);
        assertAdminAccess(sfAdmin, cite, false);
        assertAdminAccess(sfAdmin, sf, true);
    }

    @Test
    public void testAdminRule_WorkspaceAccessLimits_Username_based_rule() {
        final String citeUserRole = "CITE_USER";
        final String citeAdminRole = "CITE_ADMIN";
        final String sfUserRole = "SF_USER";
        final String sfAdminRole = "SF_ADMIN";

        final Authentication citeUser = getUser("citeuser", "cite", citeUserRole);
        final Authentication citeAdmin = getUser("citeadmin", "cite", citeAdminRole);
        final Authentication sfUser = getUser("sfuser", "sfuser", sfUserRole);
        final Authentication sfAdmin = getUser("sfadmin", "sfadmin", sfAdminRole);

        final WorkspaceInfo cite = getCatalog().getWorkspaceByName("cite");
        final WorkspaceInfo sf = getCatalog().getWorkspaceByName("sf");

        support.addAdminRule(0, citeAdmin.getName(), null, cite.getName(), ADMIN);
        support.addAdminRule(1, citeUser.getName(), null, cite.getName(), USER);
        support.addAdminRule(2, sfAdmin.getName(), null, sf.getName(), ADMIN);
        support.addAdminRule(3, sfUser.getName(), null, sf.getName(), USER);

        assertAdminAccess(citeUser, cite, false);
        assertAdminAccess(citeUser, sf, false);
        assertAdminAccess(sfUser, cite, false);
        assertAdminAccess(sfUser, sf, false);

        assertAdminAccess(citeAdmin, cite, true);
        assertAdminAccess(citeAdmin, sf, false);
        assertAdminAccess(sfAdmin, cite, false);
        assertAdminAccess(sfAdmin, sf, true);
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsEnlargementWithSingle() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, if one of the container
        // LayerGroup is SINGLE, there will be no allowedArea in the final
        // filter.
        Long idRule = null;
        LayerGroupInfo group1 = null;
        LayerGroupInfo group2 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();

            LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
            LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

            group1 = createsLayerGroup("group21", SINGLE, null, Arrays.asList(places, forests));
            group2 = createsLayerGroup("group22", NAMED, null, Arrays.asList(places, forests));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            "anonymousUser",
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group21",
                            0);

            // limit rule for anonymousUser on LayerGroup group2
            support.addRule(
                    GrantType.LIMIT,
                    "anonymousUser",
                    "ROLE_ANONYMOUS",
                    "WMS",
                    null,
                    null,
                    "group22",
                    1);

            // add allowed Area only to the first layer group
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);

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
        }
    }

    @Test
    public void testLayerInGroupAreaEnlargement() throws Exception {
        // tests that when a Layer is directly accessed in a WMS request if it is belonging to a
        // group and both groups and layer have rules with allowed areas, if the areas are for
        // different roles, union is applied.
        Long idRule = null;
        Long idRule2 = null;

        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            3);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            "cite",
                            "Lakes",
                            2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_2);
            MultiPolygon unionedArea = (MultiPolygon) allowedArea1.union(allowedArea2);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS", "ROLE_ANONYMOUS2"});

            unionedArea.normalize();
            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            Intersects intersects2 = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    intersects2.getExpression2().evaluate(null, MultiPolygon.class);
            writeFilterArea.normalize();
            assertTrue(unionedArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(unionedArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }

    @Test
    public void testLayerInGroupAreaRestriction() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to a group and both the group and the layer have rule with allowed
        // area,
        // if the areas are for same role, intersection is applied.
        Long idRule = null;
        Long idRule2 = null;

        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            3);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            "cite",
                            "Lakes",
                            2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_INTERSECT_2, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT_INTERSECT_1);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_INTERSECT_2);
            MultiPolygon intersectedArea =
                    Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
            intersectedArea.normalize();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            Intersects intersects2 = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    intersects2.getExpression2().evaluate(null, MultiPolygon.class);
            writeFilterArea.normalize();
            assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(intersectedArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }

    @Test
    public void testLayerDeniedInGroup() throws Exception {
        Long idRule = null;

        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1

            idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            null,
                            "group31",
                            3);

            // limit rule for anonymousUser on LayerGroup group2

            support.addRule(
                    GrantType.DENY, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Lakes", 2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT_INTERSECT_1);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_INTERSECT_2);
            MultiPolygon intersectedArea =
                    Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
            intersectedArea.normalize();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS"});

            VectorAccessLimits vl =
                    (VectorAccessLimits)
                            accessManager.getAccessLimits(user, lakes, Arrays.asList(group1));
            assertEquals(Filter.EXCLUDE, vl.getReadFilter());
            assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }

    @Test
    public void testLayerInGroupDirectAccessLimitResolutionByRole() throws Exception {

        Long idRule = null;
        Long idRule2 = null;

        LayerGroupInfo group1 = null;
        GeoFenceConfigurationManager configurationManager =
                applicationContext.getBean(
                        "geofenceConfigurationManager", GeoFenceConfigurationManager.class);
        GeoFenceConfiguration config = configurationManager.getConfiguration();
        config.setUseRolesToFilter(true);
        config.getRoles().add("ROLE_ONE");
        try {
            Authentication user = getUser("aUser", "", "ROLE_ONE", "ROLE_TWO");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT, null, "ROLE_ONE", "WMS", null, null, "group31", 3);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT, null, "ROLE_TWO", "WMS", null, "cite", "Lakes", 2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_INTERSECT_2, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT_INTERSECT_1);
            MultiPolygon intersectedArea = Converters.convert(allowedArea1, MultiPolygon.class);
            intersectedArea.normalize();
            logout();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);

            login("anonymousUser", "", new String[] {"ROLE_ONE", "ROLE_TWO"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
            config.setUseRolesToFilter(false);
            config.getRoles().remove("ROLE_ONE");
        }
    }

    @Test
    public void testLayerInGroupLimitResolutionByRole() throws Exception {
        // tests group limit resolution with filtering by role option enabled in geofence config.
        Long idRule = null;
        Long idRule2 = null;

        LayerGroupInfo group1 = null;
        GeoFenceConfigurationManager configurationManager =
                applicationContext.getBean(
                        "geofenceConfigurationManager", GeoFenceConfigurationManager.class);
        GeoFenceConfiguration config = configurationManager.getConfiguration();
        config.setUseRolesToFilter(true);
        config.getRoles().add("ROLE_TWO");
        try {
            Authentication user = getUser("aUser", "", "ROLE_ONE", "ROLE_TWO");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT, null, "ROLE_ONE", "WMS", null, null, "group31", 3);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT, null, "ROLE_TWO", "WMS", null, "cite", "Lakes", 2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_INTERSECT_2, 4326);

            // Merge the allowed areas
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_INTERSECT_2);
            MultiPolygon intersectedArea = Converters.convert(allowedArea2, MultiPolygon.class);
            intersectedArea.normalize();
            logout();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);

            login("anonymousUser", "", new String[] {"ROLE_ONE", "ROLE_TWO"});

            VectorAccessLimits vl =
                    (VectorAccessLimits)
                            accessManager.getAccessLimits(user, lakes, Arrays.asList(group1));
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
            config.setUseRolesToFilter(false);
            config.getRoles().remove("ROLE_TWO");
        }
    }

    @Test
    public void testLayerInGroupAreaRestrictionRulesByUser() throws Exception {
        Long idRule = null;
        Long idRule2 = null;

        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("user1", "", "ROLE1");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("group71", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            idRule =
                    support.addRule(
                            GrantType.LIMIT, "user1", null, "WMS", null, null, "group71", 20);

            // limit rule for anonymousUser on LayerGroup group2
            idRule2 =
                    support.addRule(
                            GrantType.LIMIT, "user1", null, "WMS", null, "cite", "Lakes", 21);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT_INTERSECT_1, 4326);
            support.addRuleLimits(idRule2, CatalogMode.HIDE, AREA_WKT_INTERSECT_2, 4326);

            // Merge the allowed areas
            Geometry allowedArea1 = new WKTReader().read(AREA_WKT_INTERSECT_1);
            Geometry allowedArea2 = new WKTReader().read(AREA_WKT_INTERSECT_2);
            MultiPolygon intersectedArea =
                    Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
            intersectedArea.normalize();
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("user1", "", new String[] {"ROLE1"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            Intersects intersects = (Intersects) vl.getReadFilter();
            MultiPolygon readFilterArea =
                    intersects.getExpression2().evaluate(null, MultiPolygon.class);
            readFilterArea.normalize();
            Intersects intersects2 = (Intersects) vl.getWriteFilter();
            MultiPolygon writeFilterArea =
                    intersects2.getExpression2().evaluate(null, MultiPolygon.class);
            writeFilterArea.normalize();
            assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
            assertTrue(intersectedArea.equalsExact(writeFilterArea, 10.0E-15));
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }

    private void assertAdminAccess(Authentication user, WorkspaceInfo ws, boolean expectedAdmin) {
        WorkspaceAccessLimits userAccess = accessManager.getAccessLimits(user, ws);
        assertEquals("Unexpected admin access", expectedAdmin, userAccess.isAdminable());
    }

    protected Authentication getUser(String username, String password, String... roles) {

        List<GrantedAuthority> l = new ArrayList<>();
        for (String role : roles) {
            l.add(new SimpleGrantedAuthority(role));
        }

        return new UsernamePasswordAuthenticationToken(username, password, l);
    }

    protected void setUser(Authentication user) {
        SecurityContextHolder.getContext().setAuthentication(user);
    }

    protected LayerGroupInfo createsLayerGroup(
            String name, LayerGroupInfo.Mode mode, LayerInfo rootLayer, List<LayerInfo> layers)
            throws Exception {
        return createsLayerGroup(name, mode, rootLayer, layers, null);
    }

    protected LayerGroupInfo createsLayerGroup(
            String name,
            LayerGroupInfo.Mode mode,
            LayerInfo rootLayer,
            List<LayerInfo> layers,
            CoordinateReferenceSystem groupCRS)
            throws Exception {
        Catalog catalog = getCatalog();
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
        if (groupCRS != null) {
            ReferencedEnvelope re = group.getBounds();
            MathTransform transform =
                    CRS.findMathTransform(
                            group.getBounds().getCoordinateReferenceSystem(), groupCRS);
            Envelope bbox = JTS.transform(re, transform);
            ReferencedEnvelope newRe =
                    new ReferencedEnvelope(
                            bbox.getMinX(),
                            bbox.getMaxX(),
                            bbox.getMinY(),
                            bbox.getMaxY(),
                            groupCRS);
            group.setBounds(newRe);
        }
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

    private String repWkt(
            String srcWKT,
            CoordinateReferenceSystem srcCRS,
            CoordinateReferenceSystem targetCRS,
            int targetSRID)
            throws ParseException, FactoryException, TransformException {
        Geometry geometry = new WKTReader().read(srcWKT);
        MathTransform mt = CRS.findMathTransform(srcCRS, targetCRS, true);
        Geometry transformed = JTS.transform(geometry, mt);
        transformed.setSRID(targetSRID);
        return new WKTWriter().write(transformed);
    }

    @Test
    public void testLayerBothAreas() throws Exception {
        LayerGroupInfo group1 = null;
        try {
            Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
            login("admin", "geoserver", new String[] {"ROLE_ADMINISTRATOR"});
            Catalog catalog = getCatalog();
            LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
            LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
            group1 = createsLayerGroup("groupTree31", NAMED, null, Arrays.asList(lakes, fifteen));
            // limit rule for anonymousUser on LayerGroup group1
            Long idRule =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS",
                            "WMS",
                            null,
                            "cite",
                            "Lakes",
                            3);

            // limit rule for anonymousUser on LayerGroup group2
            Long idRule2 =
                    support.addRule(
                            GrantType.LIMIT,
                            null,
                            "ROLE_ANONYMOUS2",
                            "WMS",
                            null,
                            "cite",
                            "Lakes",
                            2);

            // add allowed Area to layer groups rules
            support.addRuleLimits(idRule, CatalogMode.HIDE, AREA_WKT, 4326, SpatialFilterType.CLIP);
            support.addRuleLimits(
                    idRule2, CatalogMode.HIDE, AREA_WKT_2, 4326, SpatialFilterType.INTERSECT);
            // mock a WMS request to check contained layers direct access
            Request req = new Request();
            req.setService("WMS");
            req.setRequest("GetMap");
            Dispatcher.REQUEST.set(req);
            logout();

            login("anonymousUser", "", new String[] {"ROLE_ANONYMOUS", "ROLE_ANONYMOUS2"});

            VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
            assertNotNull(vl.getClipVectorFilter());
            assertNotNull(vl.getIntersectVectorFilter());
            logout();
        } finally {
            removeLayerGroup(group1);
        }
    }
}
