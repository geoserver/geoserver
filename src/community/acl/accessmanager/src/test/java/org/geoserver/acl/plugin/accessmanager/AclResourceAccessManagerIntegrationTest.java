/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Original from GeoServer 2.24-SNAPSHOT under GPL 2.0 license
 */
package org.geoserver.acl.plugin.accessmanager;

import static org.geoserver.acl.domain.adminrules.AdminGrantType.ADMIN;
import static org.geoserver.acl.domain.adminrules.AdminGrantType.USER;
import static org.geoserver.acl.domain.rules.CatalogMode.HIDE;
import static org.geoserver.acl.domain.rules.GrantType.ALLOW;
import static org.geoserver.acl.domain.rules.GrantType.DENY;
import static org.geoserver.acl.domain.rules.GrantType.LIMIT;
import static org.geoserver.acl.domain.rules.SpatialFilterType.CLIP;
import static org.geoserver.acl.domain.rules.SpatialFilterType.INTERSECT;
import static org.geoserver.catalog.LayerGroupInfo.Mode.NAMED;
import static org.geoserver.catalog.LayerGroupInfo.Mode.SINGLE;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.acl.domain.rules.Rule;
import org.geoserver.acl.domain.rules.RuleLimits;
import org.geoserver.acl.plugin.it.support.AclIntegrationTestSupport;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.spatial.Intersects;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.geometry.jts.JTS;
import org.geotools.referencing.CRS;
import org.geotools.util.Converters;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.io.WKTWriter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

@SuppressWarnings("unused")
public class AclResourceAccessManagerIntegrationTest extends GeoServerSystemTestSupport {
    /*
     * Simple test geometries with their spatial relationships:
     *
     *               (10,30)                                                     (40,30)
     * +-------------------+                             +-----------------------------+
     * |                   |                             |                             |
     * |        P2         |                             |             P4              |
     * |                   |                             |                             |
     * |                   |                             |               (36,25)       |
     * |           +-------+-----------------------------+---------------------+       |
     * |           |       |                             |                     |       |
     * |           |       |               P3            |                     |       |
     * |           |       |                             |                     |       |
     * |           |       |                   (23,20)   |                     |       |
     * |     +-----+-------+-------------------------+   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |     P1                  |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     |     |       |                         |   |                     |       |
     * |     +-----+-------+-------------------------+   |                     |       |
     * |     (3,10)|       |                             |                     |       |
     * |           |       |                             |                     |       |
     * |           |       |                             |                     |       |
     * |           |       |                             |                     |       |
     * |           +-------+-----------------------------+---------------------+       |
     * |           (6,5)   |                             |                             |
     * |                   |                             |                             |
     * |                   |                             |                             |
     * |                   |                             |                             |
     * +-------------------+                             +-----------------------------+
     * (0,0)                                             (25,0)
     *
     * WKT_WGS84_1 (P1): (3,10) to (23,20)  - overlaps with P2 and P3
     * WKT_WGS84_2 (P2): (0,0)  to (10,30)  - overlaps with P1 and P3
     * WKT_WGS84_3 (P3): (6,5)  to (36,25)  - overlaps with P1, P2, and P4
     * WKT_WGS84_4 (P4): (25,0) to (40,30)  - overlaps with P3
     */
    private static final String WKT_WGS84_1 = "MULTIPOLYGON(((3 10, 3 20, 23 20, 23 10, 3 10)))";
    private static final String WKT_WGS84_2 = "MULTIPOLYGON(((0 0, 0 30, 10 30, 10 0, 0 0)))";
    private static final String WKT_WGS84_3 = "MULTIPOLYGON(((6 5, 6 25, 36 25, 36 5, 6 5)))";
    private static final String WKT_WGS84_4 = "MULTIPOLYGON(((25 0, 25 30, 40 30, 40 0, 25 0)))";

    private MultiPolygon WGS84_1;
    private MultiPolygon WGS84_2;
    private MultiPolygon WGS84_3;
    private MultiPolygon WGS84_4;

    public AclIntegrationTestSupport support;

    private AclResourceAccessManager accessManager;

    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/applicationContext-test.xml");
    }

    @Before
    public void setUp() throws ParseException {
        support = new AclIntegrationTestSupport(() -> GeoServerSystemTestSupport.applicationContext);
        support.before();
        accessManager = applicationContext.getBean(AclResourceAccessManager.class);
        // reset default config
        accessManager.initDefaults();
        // add rule to grant access to all to everything with a very low priority
        support.addRule(9999, ALLOW, null, null, null, null, null, null);

        WGS84_1 = parse(WKT_WGS84_1);
        WGS84_2 = parse(WKT_WGS84_2);
        WGS84_3 = parse(WKT_WGS84_3);
        WGS84_4 = parse(WKT_WGS84_4);
    }

    private MultiPolygon parse(String mpWkt) throws ParseException {
        return (MultiPolygon) new WKTReader().read(mpWkt);
    }

    @After
    public void clearRules() {
        support.after();
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);
    }

    protected Catalog getRawCatalog() {
        return support.getRawCatalog();
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsEnlargement() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, if one of the container
        // LayerGroup doesn't have an allowed area, there will be no allowedArea in the final
        // filter.
        Catalog catalog = getRawCatalog();
        LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

        LayerGroupInfo group1 = support.createLayerGroup("group21", NAMED, null, places, forests);
        LayerGroupInfo group2 = support.createLayerGroup("group22", NAMED, null, places, forests);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(0, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group21");

        // limit rule for anonymousUser on LayerGroup group2
        support.addRule(1, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group22");

        // add allowed Area only to the first layer group
        support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, places);

        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsRestrictAccess() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one LayerGroup, the allowedArea
        // applied to the filter is the intersection of the allowed area of each LayerGroup
        // if the groups' rules were defined for the same role.
        Catalog catalog = getRawCatalog();
        LayerInfo bridges = catalog.getLayerByName(getLayerId(MockData.BRIDGES));
        LayerInfo buildings = catalog.getLayerByName(getLayerId(MockData.BUILDINGS));
        LayerGroupInfo group1 = support.createLayerGroup("group1", NAMED, null, bridges, buildings);
        LayerGroupInfo group2 = support.createLayerGroup("group2", NAMED, null, bridges, buildings);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(2, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group1");
        // add allowed Area
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(3, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group2");

        // add allowed Area to layer groups rules
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, bridges);

        // intersects the allowed areas for test
        MultiPolygon allowedArea1 = toJts(limits1);
        MultiPolygon allowedArea2 = toJts(limits2);
        MultiPolygon intersectionArea = intersect(allowedArea1, allowedArea2);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        Intersects intersects2 = (Intersects) vl.getWriteFilter();
        MultiPolygon writeFilterArea = intersects2.getExpression2().evaluate(null, MultiPolygon.class);
        intersectionArea.normalize();
        // normalize geometries to avoids assertion failures for
        // a different internal order of the polygons
        readFilterArea.normalize();
        writeFilterArea.normalize();
        assertTrue(intersectionArea.equalsExact(readFilterArea, 10.0E-15));
        assertTrue(intersectionArea.equalsExact(writeFilterArea, 10.0E-15));
    }

    @Test
    public void testAllowedAreaLayerInTwoGroupsEnlargement2() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to more then one group the two areas are applied.

        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);
        LayerGroupInfo group2 = support.createLayerGroup("group32", NAMED, null, lakes, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(4, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group31");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(5, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group32");

        Rule r3 = support.addRule(4, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group32");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r4 = support.addRule(5, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group31");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);
        RuleLimits limits3 = support.setRuleLimits(r3, HIDE, WKT_WGS84_3, 4326);
        RuleLimits limits4 = support.setRuleLimits(r4, HIDE, WKT_WGS84_3, 4326);

        // Merge the allowed areas
        MultiPolygon allowedArea1 = toJts(limits1);
        MultiPolygon allowedArea2 = toJts(limits2);
        MultiPolygon allowedArea3 = toJts(limits3);
        MultiPolygon intersectOne = intersect(allowedArea1, allowedArea3);
        MultiPolygon intersectTwo = intersect(allowedArea2, allowedArea3);
        MultiPolygon unionedArea = Converters.convert(intersectOne.union(intersectTwo), MultiPolygon.class);
        unionedArea.normalize();
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        assertThat(vl.getReadFilter(), instanceOf(Intersects.class));
        Intersects readFilter = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = readFilter.getExpression2().evaluate(null, MultiPolygon.class);
        readFilterArea.normalize();
        Intersects writeFilter = (Intersects) vl.getWriteFilter();
        MultiPolygon writeFilterArea = writeFilter.getExpression2().evaluate(null, MultiPolygon.class);
        writeFilterArea.normalize();
        assertTrue(unionedArea.equalsExact(readFilterArea, 10.0E-15));
        assertTrue(unionedArea.equalsExact(writeFilterArea, 10.0E-15));
    }

    @Test
    public void testAllowedAreaSRIDIsPreserved() throws Exception {
        // test that when adding an allowed area with a SRID different from
        // the layerGroup one, the final filter has been reprojected to the correct CRS
        Catalog catalog = getRawCatalog();
        LayerInfo basicPolygons = catalog.getLayerByName(getLayerId(MockData.BASIC_POLYGONS));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group41", NAMED, null, basicPolygons, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(7, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group41");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 3857);
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, basicPolygons);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon allowedArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        allowedArea.normalize();

        MultiPolygon geom = toJts(limits1);
        MathTransform mt = CRS.findMathTransform(
                CRS.decode("EPSG:3857"), basicPolygons.getResource().getCRS(), true);
        MultiPolygon reproj = (MultiPolygon) JTS.transform(geom, mt);
        reproj.normalize();
        assertTrue(allowedArea.equalsExact(reproj, 10.0E-15));
    }

    @Test
    public void testLayerGroupsAllowedAreaWithDifferentSRIDS() throws Exception {
        // tests that when having a Layer directly accessed for WMS request
        // belonging to two LayerGroups each with a different CRS for the allowed area
        // the resulting geometry filter has a geometry that is a union of the two areas
        // in the correct CRS.
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo namedPlaces = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
        LayerGroupInfo group1 = support.createLayerGroup("group51", NAMED, null, lakes, namedPlaces);
        LayerGroupInfo group2 = support.createLayerGroup("group52", NAMED, null, lakes, namedPlaces);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(8, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group51");

        // limit rule for anonymousUser on LayerGroup group1
        Rule r2 = support.addRule(9, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group52");

        // limit rule for anonymousUser on LayerGroup group1
        Rule r3 = support.addRule(8, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group52");

        // limit rule for anonymousUser on LayerGroup group1
        Rule r4 = support.addRule(9, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group51");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        String wkt3857 = reprojectWkt(WKT_WGS84_2, CRS.decode("EPSG:4326"), CRS.decode("EPSG:3857"), 3857);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, wkt3857, 3857);
        RuleLimits limits3 = support.setRuleLimits(r3, HIDE, WKT_WGS84_3, 4326);
        RuleLimits limits4 = support.setRuleLimits(r4, HIDE, WKT_WGS84_4, 4326);

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon allowedArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        allowedArea.normalize();

        MultiPolygon geom1 = toJts(limits1);
        Geometry geom2 = reproject(toJts(limits2), 3857, 4326);
        MultiPolygon geom3 = toJts(limits3);
        MultiPolygon geom4 = toJts(limits4);
        Geometry intersect1 = geom1.intersection(geom3);
        Geometry intersect2 = geom2.intersection(geom4);
        MultiPolygon union = Converters.convert(intersect1.union(intersect2), MultiPolygon.class);
        union.normalize();

        assertTrue(allowedArea.equalsExact(union, 10.0E-12));
    }

    @Test
    public void testLayerGroupsClipAndIntersectsSpatialFilterUnion() throws Exception {
        // test that when having two layergroups each with two rules with clip and intersects
        // allowed area, the geometries are correctly merged
        Catalog catalog = getRawCatalog();
        LayerInfo droutes = catalog.getLayerByName(getLayerId(MockData.DIVIDED_ROUTES));
        LayerInfo ponds = catalog.getLayerByName(getLayerId(MockData.PONDS));
        LayerGroupInfo group1 = support.createLayerGroup("group61", NAMED, null, droutes, ponds);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(10, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group61");

        Rule r2 = support.addRule(11, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group61");

        LayerGroupInfo group2 = support.createLayerGroup("group62", NAMED, null, droutes, ponds);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r3 = support.addRule(12, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, "group62");

        Rule r4 = support.addRule(13, LIMIT, "anonymousUser", "ROLE_ANONYMOUS2", "WMS", null, null, "group62");

        String areaWKT1 =
                "MultiPolygon (((-97.48185823120911664 0.02172899055096349, -97.4667765271758384 0.02148629646307176, -97.46795532703131926 0.01663241470523705, -97.48165020770520073 0.01607768536148451, -97.48185823120911664 0.02172899055096349)))";
        String areaWKT2 =
                "MultiPolygon (((-97.48109547836145339 0.026374848804891, -97.46934215039070182 0.02672155464473634, -97.46993155031843514 0.02294246099042217, -97.48102613719348142 0.02294246099042217, -97.48109547836145339 0.026374848804891)))";
        String areaWKT3 =
                "MultiPolygon (((-97.48119949011341134 0.00914356856457779, -97.46941149155865958 0.00973296849231486, -97.46955017389460352 0.00605788658995429, -97.48182356062513065 0.00581519250206256, -97.48119949011341134 0.00914356856457779)))";
        String areaWKT4 =
                "MultiPolygon (((-97.48161553712121474 0.00449771031065027, -97.46889143279889822 0.00435902797471214, -97.46948083272663155 0.00127334600008865, -97.48178889004114467 0.00120400483211958, -97.48161553712121474 0.00449771031065027)))";
        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, areaWKT1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, areaWKT2, 4326, CLIP);
        RuleLimits limits3 = support.setRuleLimits(r3, HIDE, areaWKT3, 4326);
        RuleLimits limits4 = support.setRuleLimits(r4, HIDE, areaWKT4, 4326, CLIP);
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, droutes);
        Geometry intersectsArea = vl.getIntersectVectorFilter();
        Geometry clipArea = vl.getClipVectorFilter();
        intersectsArea.normalize();
        clipArea.normalize();

        // union of the allowed area where the 3857 is reprojected to 4326
        MultiPolygon geom1 = toJts(limits1);
        MultiPolygon geom2 = toJts(limits2);
        MultiPolygon geom3 = toJts(limits3);
        MultiPolygon geom4 = toJts(limits4);

        Geometry intersectIntersection = geom1.intersection(geom3);
        intersectIntersection.normalize();

        Geometry clipIntersection = geom2.intersection(geom4);
        clipIntersection.normalize();

        assertTrue(intersectsArea.equalsExact(intersectIntersection, 10.0E-15));
        assertTrue(clipArea.equalsExact(clipIntersection, 10.0E-15));
    }

    @Test
    public void testCiteCannotWriteOnWorkspace() {
        accessManager.setGrantWriteToWorkspacesToAuthenticatedUsers(false);
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

        support.addAdminRule(1, null, citeAdminRole, cite.getName(), ADMIN);
        support.addAdminRule(2, null, citeUserRole, cite.getName(), USER);
        support.addAdminRule(3, null, sfAdminRole, sf.getName(), ADMIN);
        support.addAdminRule(4, null, sfUserRole, sf.getName(), USER);

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
        Catalog catalog = getRawCatalog();

        LayerInfo places = catalog.getLayerByName(getLayerId(MockData.NAMED_PLACES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));

        LayerGroupInfo group1 = support.createLayerGroup("group21", SINGLE, null, places, forests);
        LayerGroupInfo group2 = support.createLayerGroup("group22", NAMED, null, places, forests);
        // limit rule for anonymousUser on LayerGroup group1
        Rule limit1 = support.addRule(0, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, group1.getName());

        // limit rule for anonymousUser on LayerGroup group2
        Rule limit2 = support.addRule(1, LIMIT, "anonymousUser", "ROLE_ANONYMOUS", "WMS", null, null, group2.getName());

        // add allowed Area only to the first layer group
        support.setRuleLimits(limit1.getId(), HIDE, WKT_WGS84_1, 4326);

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, places);

        assertEquals(Filter.INCLUDE, vl.getReadFilter());
        assertEquals(Filter.INCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testLayerInGroupAreaEnlargement() throws Exception {
        // tests that when a Layer is directly accessed in a WMS request if it is belonging to a
        // group and both groups and layer have rules with allowed areas, if the areas are for
        // different roles, union is applied.
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "group31");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(2, LIMIT, null, "ROLE_ANONYMOUS2", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);

        // Merge the allowed areas
        MultiPolygon allowedArea1 = toJts(limits1);
        MultiPolygon allowedArea2 = toJts(limits2);
        MultiPolygon unionedArea = Converters.convert(allowedArea1.union(allowedArea2), MultiPolygon.class);
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        unionedArea.normalize();
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        readFilterArea.normalize();
        Intersects intersects2 = (Intersects) vl.getWriteFilter();
        MultiPolygon writeFilterArea = intersects2.getExpression2().evaluate(null, MultiPolygon.class);
        writeFilterArea.normalize();
        assertTrue(unionedArea.equalsExact(readFilterArea, 10.0E-15));
        assertTrue(unionedArea.equalsExact(writeFilterArea, 10.0E-15));
    }

    @Test
    public void testLayerInGroupAreaRestriction() throws Exception {
        // tests that when a Layer is directly accessed for WMS request
        // if it is belonging to a group and both the group and the layer have rule with allowed
        // area, if the areas are for same role, intersection is applied.

        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "group31");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(2, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);

        // Merge the allowed areas
        MultiPolygon allowedArea1 = toJts(limits1);
        MultiPolygon allowedArea2 = toJts(limits2);
        MultiPolygon intersectedArea = Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
        intersectedArea.normalize();
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        readFilterArea.normalize();
        Intersects intersects2 = (Intersects) vl.getWriteFilter();
        MultiPolygon writeFilterArea = intersects2.getExpression2().evaluate(null, MultiPolygon.class);
        writeFilterArea.normalize();
        assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
        assertTrue(intersectedArea.equalsExact(writeFilterArea, 10.0E-15));
    }

    @Test
    public void testLayerDeniedInGroup() throws Exception {
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);

        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, null, "group31");

        // limit rule for anonymousUser on LayerGroup group2
        support.addRule(2, DENY, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes, List.of(group1));
        assertEquals(Filter.EXCLUDE, vl.getReadFilter());
        assertEquals(Filter.EXCLUDE, vl.getWriteFilter());
    }

    @Test
    public void testLayerInGroupDirectAccessLimitResolutionByRole() throws Exception {
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);

        // match
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_ONE", "WMS", null, null, "group31");
        // no match
        Rule r2 = support.addRule(2, LIMIT, null, "ROLE_THREE", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);

        // Merge the allowed areas
        MultiPolygon allowedArea1 = toJts(limits1);
        allowedArea1.normalize();

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("aUser", "", "ROLE_ONE", "ROLE_TWO");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        readFilterArea.normalize();
        assertTrue(allowedArea1.equalsExact(readFilterArea, 10.0E-15));
    }

    @Test
    public void testLayerInGroupLimitResolutionByRole() throws Exception {
        assertTrue("preflight", WGS84_1.intersects(WGS84_2));

        final MultiPolygon roleArea = WGS84_1;
        final MultiPolygon lakesArea = WGS84_2;

        Catalog catalog = getRawCatalog();

        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));

        LayerGroupInfo container = support.createLayerGroup("group31", NAMED, null, lakes, fifteen);

        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_GROUP", "WMS", null, null, "group31");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(2, LIMIT, null, "ROLE_LAKES", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        RuleLimits roleLimits = support.setRuleLimits(r1, HIDE, roleArea, 4326);
        RuleLimits lakesLimits = support.setRuleLimits(r2, HIDE, lakesArea, 4326);

        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        testContainerLimitResolutionByRole(lakes, container, lakesArea, "ROLE_LAKES");
        testContainerLimitResolutionByRole(lakes, container, roleArea, "ROLE_GROUP");

        MultiPolygon union = Converters.convert(roleArea.union(lakesArea), MultiPolygon.class);
        testContainerLimitResolutionByRole(lakes, container, union, "ROLE_GROUP", "ROLE_LAKES");
    }

    private void testContainerLimitResolutionByRole(
            LayerInfo layer, LayerGroupInfo container, MultiPolygon expected, String... roles) {

        Authentication user = getUser("aUser", "", roles);
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, layer, List.of(container));
        Intersects intersects = (Intersects) vl.getReadFilter();
        MultiPolygon readFilterArea = intersects.getExpression2().evaluate(null, MultiPolygon.class);
        readFilterArea.normalize();
        expected.normalize();
        assertTrue(expected.equalsExact(readFilterArea, 10.0E-15));
    }

    @Test
    public void testLayerInGroupAreaRestrictionRulesByUser() throws Exception {
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("group71", NAMED, null, lakes, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(20, LIMIT, "user1", null, "WMS", null, null, "group71");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(21, LIMIT, "user1", null, "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        RuleLimits limits1 = support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326);
        RuleLimits limits2 = support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326);

        // Merge the allowed areas
        MultiPolygon intersectedArea = intersect(toJts(limits1), toJts(limits2));
        intersectedArea.normalize();
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("user1", "", "ROLE1");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        Intersects readFilter = (Intersects) vl.getReadFilter();
        Intersects writeFilter = (Intersects) vl.getWriteFilter();

        MultiPolygon readFilterArea = readFilter.getExpression2().evaluate(null, MultiPolygon.class);
        MultiPolygon writeFilterArea = writeFilter.getExpression2().evaluate(null, MultiPolygon.class);

        readFilterArea.normalize();
        writeFilterArea.normalize();

        assertTrue(intersectedArea.equalsExact(readFilterArea, 10.0E-15));
        assertTrue(intersectedArea.equalsExact(writeFilterArea, 10.0E-15));
    }

    @Test
    public void testLayerBothAreas() throws Exception {
        Catalog catalog = getRawCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo fifteen = catalog.getLayerByName(getLayerId(MockData.FIFTEEN));
        LayerGroupInfo group1 = support.createLayerGroup("groupTree31", NAMED, null, lakes, fifteen);
        // limit rule for anonymousUser on LayerGroup group1
        Rule r1 = support.addRule(3, LIMIT, null, "ROLE_ANONYMOUS", "WMS", null, "cite", "Lakes");

        // limit rule for anonymousUser on LayerGroup group2
        Rule r2 = support.addRule(2, LIMIT, null, "ROLE_ANONYMOUS2", "WMS", null, "cite", "Lakes");

        // add allowed Area to layer groups rules
        support.setRuleLimits(r1, HIDE, WKT_WGS84_1, 4326, CLIP);
        support.setRuleLimits(r2, HIDE, WKT_WGS84_2, 4326, INTERSECT);
        // mock a WMS request to check contained layers direct access
        support.setDispatcherRequest("WMS", "GetMap");

        Authentication user = getUser("anonymousUser", "", "ROLE_ANONYMOUS", "ROLE_ANONYMOUS2");
        VectorAccessLimits vl = (VectorAccessLimits) accessManager.getAccessLimits(user, lakes);
        assertNotNull(vl.getClipVectorFilter());
        assertNotNull(vl.getIntersectVectorFilter());
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

    private String reprojectWkt(
            String srcWKT, CoordinateReferenceSystem srcCRS, CoordinateReferenceSystem targetCRS, int targetSRID)
            throws Exception {

        Geometry geometry = new WKTReader().read(srcWKT);
        Geometry transformed = reproject(geometry, srcCRS, targetCRS, targetSRID);
        return new WKTWriter().write(transformed);
    }

    private Geometry reproject(Geometry geometry, int srcCRS, int targetCRS)
            throws FactoryException, TransformException {
        CoordinateReferenceSystem src = CRS.decode("EPSG:%d".formatted(srcCRS));
        CoordinateReferenceSystem target = CRS.decode("EPSG:%d".formatted(targetCRS));
        return reproject(geometry, src, target, targetCRS);
    }

    private Geometry reproject(
            Geometry geometry, CoordinateReferenceSystem srcCRS, CoordinateReferenceSystem targetCRS, int targetSRID)
            throws FactoryException, TransformException {
        MathTransform mt = CRS.findMathTransform(srcCRS, targetCRS, true);
        Geometry transformed = JTS.transform(geometry, mt);
        transformed.setSRID(targetSRID);
        return transformed;
    }

    private MultiPolygon toJts(RuleLimits limits) {
        return org.geolatte.geom.jts.JTS.to(limits.getAllowedArea());
    }

    private MultiPolygon intersect(MultiPolygon allowedArea1, MultiPolygon allowedArea2) {
        return Converters.convert(allowedArea1.intersection(allowedArea2), MultiPolygon.class);
    }
}
