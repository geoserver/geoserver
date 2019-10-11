/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasProperty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import javax.xml.namespace.QName;
import org.geoserver.catalog.*;
import org.geoserver.data.test.CiteTestData;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.CoverageAccessLimits;
import org.geoserver.security.DataAccessLimits;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.TestResourceAccessManager;
import org.geoserver.security.impl.AbstractUserGroupService;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.geowebcache.service.ve.VEConverter;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.io.WKTReader;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Performs integration tests using a mock {@link ResourceAccessManager}
 *
 * @author Niels Charlier, Scitus Development
 */
public class GWCDataSecurityTest extends WMSTestSupport {

    static final Logger LOGGER = Logging.getLogger(GWCDataSecurityTest.class);

    private static final String SECURITY_ERROR_TYPE = "text/plain";
    private static final String NOT_FOUND_ERROR_TYPE = "text/html";

    @Before
    public void cleanUpCrsHints() {
        // Some other tests leave hints affecting axis order which disrupts this test
        CRS.cleanupThreadLocals();
    }

    /** Add the test resource access manager in the spring context */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath:/org/geoserver/wms/ResourceAccessManagerContext.xml");
    }
    /** Enable the Spring Security auth filters */
    @Override
    protected List<javax.servlet.Filter> getFilters() {
        return Collections.singletonList(
                (javax.servlet.Filter) GeoServerExtensions.bean("filterChainProxy"));
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GWC.get().getConfig().setSecurityEnabled(true);

        testData.addStyle("raster", "raster.sld", SystemTestData.class, getCatalog());
        Map properties = new HashMap();
        properties.put(LayerProperty.STYLE, "raster");
        testData.addRasterLayer(
                new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                properties,
                SystemTestData.class,
                getCatalog());

        testData.addRasterLayer(
                new QName(MockData.SF_URI, "Mosaic2", MockData.SF_PREFIX),
                "raster-filter-test.zip",
                null,
                properties,
                SystemTestData.class,
                getCatalog());

        CoverageInfo sfMosaicCI = getCatalog().getCoverageByName("sf:mosaic");
        sfMosaicCI.setNativeBoundingBox(CiteTestData.DEFAULT_LATLON_ENVELOPE);
        getCatalog().save(sfMosaicCI);

        GeoServerUserGroupStore ugStore =
                getSecurityManager()
                        .loadUserGroupService(AbstractUserGroupService.DEFAULT_NAME)
                        .createStore();

        ugStore.addUser(ugStore.createUserObject("cite", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_mosaic2", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_nomosaic", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_cropmosaic", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_filtermosaic", "cite", true));
        ugStore.addUser(ugStore.createUserObject("cite_nogroup", "cite", true));
        ugStore.store();

        GeoServerRoleStore roleStore = getSecurityManager().getActiveRoleService().createStore();
        GeoServerRole role = roleStore.createRoleObject("ROLE_DUMMY");
        roleStore.addRole(role);
        roleStore.associateRoleToUser(role, "cite");
        roleStore.associateRoleToUser(role, "cite_mosaic2");
        roleStore.associateRoleToUser(role, "cite_nogroup");
        roleStore.associateRoleToUser(role, "cite_nomosaic");
        roleStore.associateRoleToUser(role, "cite_cropmosaic");
        roleStore.associateRoleToUser(role, "cite_filtermosaic");
        roleStore.store();

        // populate the access manager
        Catalog catalog = getCatalog();
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");

        CoverageInfo coverage = catalog.getCoverageByName("sf:mosaic");
        CoverageInfo coverage2 = catalog.getCoverageByName("sf:Mosaic2");

        // set permissions on layer coverage
        tam.putLimits(
                "cite_mosaic2", coverage, new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE));
        tam.putLimits("cite", coverage, new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE));

        // set permissions on layer coverage2
        tam.putLimits(
                "cite", coverage2, new DataAccessLimits(CatalogMode.CHALLENGE, Filter.EXCLUDE));
        tam.putLimits(
                "cite_mosaic2",
                coverage2,
                new DataAccessLimits(CatalogMode.CHALLENGE, Filter.INCLUDE));

        // layer disable
        tam.putLimits(
                "cite_nomosaic",
                coverage,
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE, null, null));

        // image cropping setup
        WKTReader wkt = new WKTReader();
        MultiPolygon cropper =
                (MultiPolygon)
                        wkt.read("MULTIPOLYGON(((140 -50, 150 -50, 150 -30, 140 -30, 140 -50)))");
        tam.putLimits(
                "cite_cropmosaic",
                coverage,
                new CoverageAccessLimits(CatalogMode.HIDE, Filter.INCLUDE, cropper, null));

        // filter setup
        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        Filter filter = ff.contains(ff.property("geometry"), ff.literal(cropper));
        tam.putLimits(
                "cite_filtermosaic",
                coverage,
                new CoverageAccessLimits(CatalogMode.HIDE, filter, null, null));

        // System.out.println(coverage.boundingBox());
    }

    @Test
    public void testNoMosaic() throws Exception {
        GWC.get().getConfig().setSecurityEnabled(true);

        // first to cache
        setRequestAuth("cite", "cite");
        String path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());

        // try again, now should be cached
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());

        // try now as different user
        setRequestAuth("cite_nomosaic", "cite");
        response = getAsServletResponse(path);
        assertEquals(NOT_FOUND_ERROR_TYPE, response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(
                str.contains("org.geotools.ows.ServiceException: Could not find layer sf:mosaic"));
    }

    @Test
    public void testPermissionMosaicTileWmts() throws Exception {
        doPermissionMosaicTileTest(
                (layer) ->
                        String.format(
                                "gwc/service/wmts?LAYER=%s&FORMAT=image/png&SERVICE=WMTS&VERSION=1.0.0"
                                        + "&REQUEST=GetTile&TILEMATRIXSET=EPSG:900913&TILEMATRIX=EPSG:900913:0&TILECOL=0&TILEROW=0",
                                layer),
                SECURITY_ERROR_TYPE,
                NOT_FOUND_ERROR_TYPE);
    }

    Matcher<MockHttpServletResponse> hasBody(Matcher<String> matcher) {
        return new org.hamcrest.BaseMatcher<MockHttpServletResponse>() {

            @Override
            public boolean matches(Object item) {
                try {
                    return matcher.matches(
                            string(getBinaryInputStream((MockHttpServletResponse) item)));
                } catch (Exception e) {
                    return false;
                }
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("HTTP Response with body ").appendDescriptionOf(matcher);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                description.appendText("body was: \n");
                try {
                    description.appendValue(
                            string(getBinaryInputStream((MockHttpServletResponse) item)));
                } catch (Exception e) {
                    description.appendText("\tcould not read body ").appendValue(e.getMessage());
                }
            }
        };
    }

    Matcher<MockHttpServletResponse> addBodyOnFail(
            Matcher<? extends MockHttpServletResponse> matcher) {
        return new org.hamcrest.BaseMatcher<MockHttpServletResponse>() {

            @Override
            public boolean matches(Object item) {
                return matcher.matches(item);
            }

            @Override
            public void describeTo(Description description) {
                matcher.describeTo(description);
            }

            @Override
            public void describeMismatch(Object item, Description description) {
                matcher.describeMismatch(item, description);
                String body;
                if (((MockHttpServletResponse) item).getContentType().startsWith("image")) {
                    description.appendText("\n  was an image");
                }
                try {
                    body = string(getBinaryInputStream((MockHttpServletResponse) item));
                    description.appendText("\n  body:").appendValue(body);
                } catch (Exception e) {
                    description.appendText("\n  could not get body:").appendValue(e.getMessage());
                }
            }
        };
    }

    enum TestGridset {
        GlobalCRS84Geometric("EPSG:4326", new long[] {7489, 1245, 12}, new long[] {4096, 2048, 12}),
        GoogleMapsCompatible(
                "EPSG:900913", new long[] {7489, 3237, 13}, new long[] {4096, 4096, 13});

        public final String name;
        public final long[] tileInBounds;
        public final long[] tileOutOfBounds;

        private TestGridset(String name, long[] tileInBounds, long[] tileOutOfBounds) {
            this.name = name;
            this.tileInBounds = tileInBounds;
            this.tileOutOfBounds = tileOutOfBounds;
        }
    }

    protected void doPermissionCropTileTest(
            BiFunction<String, long[], String> pathForLayer, String failFormat, TestGridset grid)
            throws Exception {
        final String tileFormat = "image/png";
        //      final String pathInBounds = pathForLayer.apply("sf:mosaic", new
        // long[]{4073,4118,13});
        //      final String pathOutOfBounds = pathForLayer.apply("sf:mosaic", new
        // long[]{4073,4117,13});
        final String pathInBounds = pathForLayer.apply("sf:mosaic", grid.tileInBounds);
        final String pathOutOfBounds = pathForLayer.apply("sf:mosaic", grid.tileOutOfBounds);
        GWC.get().getConfig().setSecurityEnabled(true);

        setRequestAuth("cite", "cite");

        // Add out of bounds tile to the cache
        MockHttpServletResponse response = getAsServletResponse(pathOutOfBounds);
        assertThat(
                response,
                addBodyOnFail(
                        hasProperty(
                                "contentType",
                                equalTo(tileFormat)))); // If this was unsuccessful the test is
        // invalid

        setRequestAuth("cite_cropmosaic", "cite");

        // Request out of bounds should fail
        response = getAsServletResponse(pathOutOfBounds);
        assertThat(
                response,
                allOf(
                        hasProperty("contentType", equalTo(failFormat)),
                        hasBody(Matchers.containsString("Not Authorized"))));

        // Request within bounds should work
        response = getAsServletResponse(pathInBounds);
        assertThat(response, addBodyOnFail(hasProperty("contentType", equalTo(tileFormat))));
    }

    protected void doPermissionMosaicTileTest(
            Function<String, String> pathForLayer, String secFailFormat, String otherFailFormat)
            throws Exception {
        final String tileFormat = "image/png";

        final String path = pathForLayer.apply("sf:mosaic");
        final String path2 = pathForLayer.apply("sf:Mosaic2");

        GWC.get().getConfig().setSecurityEnabled(true);

        // First make sure the tiles are cached
        setRequestAuth("cite", "cite");
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(tileFormat, response.getContentType());

        // try again, now should be cached
        response = getAsServletResponse(path);
        assertEquals(tileFormat, response.getContentType());

        // The second layer
        setRequestAuth("cite_mosaic2", "cite");
        response = getAsServletResponse(path2);
        assertEquals(tileFormat, response.getContentType());

        // try again, now should be cached
        response = getAsServletResponse(path2);
        assertEquals(tileFormat, response.getContentType());

        // permission must be denied to cite user

        setRequestAuth("cite", "cite");
        response = getAsServletResponse(path2);
        assertEquals(secFailFormat, response.getContentType());
        // mode challenge
        assertThat(string(getBinaryInputStream(response)), containsString("Not Authorized"));

        // try now as cite_mosaic2 user permission on sf:mosaic must be denied
        setRequestAuth("cite_mosaic2", "cite");
        response = getAsServletResponse(path);
        assertEquals(otherFailFormat, response.getContentType());

        // mode hide
        assertThat(
                string(getBinaryInputStream(response)),
                containsString("Could not find layer sf:mosaic"));
    }

    @Test
    public void testPermissionMosaicTileGmaps() throws Exception {
        doPermissionMosaicTileTest(
                (layer) ->
                        String.format(
                                "gwc/service/gmaps?LAYERS=%s&FORMAT=image/png&ZOOM=0&X=0&Y=0",
                                layer),
                SECURITY_ERROR_TYPE,
                NOT_FOUND_ERROR_TYPE);
    }

    @Test
    public void testPermissionMosaicTileMGmaps() throws Exception {
        doPermissionMosaicTileTest(
                (layer) ->
                        String.format(
                                "gwc/service/mgmaps?LAYERS=%s&FORMAT=image/png&ZOOM=17&X=0&Y=0",
                                layer),
                SECURITY_ERROR_TYPE,
                NOT_FOUND_ERROR_TYPE);
    }

    @Test
    public void testPermissionMosaicTileTms() throws Exception {
        doPermissionMosaicTileTest(
                (layer) ->
                        String.format("gwc/service/tms/1.0.0/%s@EPSG:900913@png/0/0/0.png", layer),
                SECURITY_ERROR_TYPE,
                NOT_FOUND_ERROR_TYPE);
    }

    @Test
    public void testPermissionMosaicTileKml() throws Exception {
        doPermissionMosaicTileTest(
                (layer) -> String.format("gwc/service/kml/%s/x0y0z0.png", layer),
                SECURITY_ERROR_TYPE,
                NOT_FOUND_ERROR_TYPE);
    }

    @Test
    public void testPermissionCropTileTms() throws Exception {
        doPermissionCropTileTest(
                (layer, index) ->
                        String.format(
                                "gwc/service/tms/1.0.0/%s@EPSG:900913@png/%d/%d/%d.png",
                                layer, index[2], index[0], index[1]),
                SECURITY_ERROR_TYPE,
                TestGridset.GoogleMapsCompatible);
    }

    @Test
    public void testPermissionCropTileWmts() throws Exception {

        //        System.out.println(
        //                Arrays.toString(
        //                        GWC.get()
        //                                .getTileLayerByName("sf:mosaic")
        //                                .getGridSubset("EPSG:900913")
        //                                .getCoverage(13)));
        //        System.out.println(
        //                GWC.get()
        //                        .getTileLayerByName("sf:mosaic")
        //                        .getGridSubset("EPSG:900913")
        //                        .getOriginalExtent());
        //        System.out.println(
        //                ((LayerInfo)
        //                                ((GeoServerTileLayer)
        // (GWC.get().getTileLayerByName("sf:mosaic")))
        //                                        .getPublishedInfo())
        //                        .getResource()
        //                        .getLatLonBoundingBox());
        //        System.out.println(
        //
        // getCatalog().getLayerByName("sf:mosaic").getResource().getLatLonBoundingBox());
        doPermissionCropTileTest(
                (layer, index) ->
                        String.format(
                                "gwc/service/wmts?LAYER=%s&FORMAT=image/png&SERVICE=WMTS&VERSION=1.0.0"
                                        + "&REQUEST=GetTile&TILEMATRIXSET=EPSG:900913&TILEMATRIX=EPSG:900913:%d&TILECOL=%d&TILEROW=%d",
                                layer, index[2], index[0], (1 << index[2]) - index[1] - 1),
                SECURITY_ERROR_TYPE,
                TestGridset.GoogleMapsCompatible);
    }

    @Test
    public void testPermissionCropTileGmaps() throws Exception {
        doPermissionCropTileTest(
                (layer, index) ->
                        String.format(
                                "gwc/service/gmaps?LAYERS=%s&FORMAT=image/png&ZOOM=%d&X=%d&Y=%d",
                                layer, index[2], index[0], (1 << index[2]) - index[1] - 1),
                SECURITY_ERROR_TYPE,
                TestGridset.GoogleMapsCompatible);
    }

    @Test
    public void testPermissionCropTileMGmaps() throws Exception {
        doPermissionCropTileTest(
                (layer, index) ->
                        String.format(
                                "gwc/service/mgmaps?LAYERS=%s&FORMAT=image/png&ZOOM=%d&X=%d&Y=%d",
                                layer, 17 - index[2], index[0], (1 << index[2]) - index[1] - 1),
                SECURITY_ERROR_TYPE,
                TestGridset.GoogleMapsCompatible);
    }

    @Test
    public void testPermissionCropTileKml() throws Exception {
        doPermissionCropTileTest(
                (layer, index) ->
                        String.format(
                                "gwc/service/kml/%s/x%dy%dz%d.png",
                                layer, index[0], index[1], index[2]),
                SECURITY_ERROR_TYPE,
                TestGridset.GlobalCRS84Geometric);
    }

    @Test
    public void testPermissionCropTileBing() throws Exception {
        doPermissionCropTileTest(
                (layer, index) -> {
                    long col = index[0];
                    long row = (1 << index[2]) - index[1] - 1;
                    long zoom = index[2];
                    long key = 0;
                    for (int i = 0; i < zoom; i++) {
                        key |= (col & (1 << i)) != 0 ? (1 << (i * 2)) : 0;
                        key |= (row & (1 << i)) != 0 ? (1 << (i * 2 + 1)) : 0;
                    }
                    String quadKey = Long.toString(key, 4);
                    assertThat(
                            VEConverter.convert(quadKey),
                            equalTo(index)); // Check that the test key is correct. Failure means
                    // the test is broken
                    return String.format(
                            "gwc/service/ve?layers=%s&format=image/png&quadKey=%s", layer, quadKey);
                },
                SECURITY_ERROR_TYPE,
                TestGridset.GoogleMapsCompatible);
    }

    protected void doPermissionKmlOverlay(
            Function<String, String> pathForLayer, String overlayFormat) throws Exception {
        final String failFormat = "text/html";

        final String path = pathForLayer.apply("sf:mosaic");
        final String path2 = pathForLayer.apply("sf:Mosaic2");

        GWC.get().getConfig().setSecurityEnabled(true);

        // Test that we have access when we should
        setRequestAuth("cite", "cite");
        MockHttpServletResponse response = getAsServletResponse(path);
        assertThat(response, addBodyOnFail(hasProperty("contentType", equalTo(overlayFormat))));

        setRequestAuth("cite_mosaic2", "cite");
        response = getAsServletResponse(path2);
        assertThat(response, addBodyOnFail(hasProperty("contentType", equalTo(overlayFormat))));

        // try now as cite_mosaic2 user permission on sf:mosaic must be denied
        setRequestAuth("cite_mosaic2", "cite");
        response = getAsServletResponse(path);
        assertThat(
                response,
                allOf(
                        hasProperty("contentType", equalTo(failFormat)),
                        hasBody(Matchers.containsString("Could not find layer sf:mosaic"))));

        // TODO: Test that the overlays don't contain references to inaccessible tiles.
    }

    @Test
    public void testPermissionMosaicKmlRasterSuperOverlay() throws Exception {
        doPermissionKmlOverlay(
                (layer) -> String.format("gwc/service/kml/%s.png.kmz", layer),
                "application/vnd.google-earth.kmz");
    }

    @Test
    public void testPermissionMosaicKmlVectorSuperOverlay() throws Exception {
        doPermissionKmlOverlay(
                (layer) -> String.format("gwc/service/kml/%s.kml.kmz", layer),
                "application/vnd.google-earth.kmz");
    }

    @Test
    public void testCroppedMosaic() throws Exception {
        // first to cache
        setRequestAuth("cite", "cite");
        String path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());

        // this should fail
        setRequestAuth("cite_cropmosaic", "cite");

        path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals(SECURITY_ERROR_TYPE, response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(str.contains("Not Authorized"));

        // but this should be fine
        path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=143.4375,-42.1875,146.25,-39.375&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testFilterMosaic() throws Exception {
        // first to cache
        setRequestAuth("cite", "cite");
        String path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());

        // this should fail
        setRequestAuth("cite_filtermosaic", "cite");

        path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals(SECURITY_ERROR_TYPE, response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertThat(str, containsString("Not Authorized"));

        // but this should be fine
        path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=143.4375,-42.1875,146.25,-39.375&WIDTH=256&HEIGHT=256&transparent=false";
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testLayerGroup() throws Exception {
        // no auth, it should work
        setRequestAuth(null, null);
        String path =
                "gwc/service/wms?bgcolor=0x000000&LAYERS="
                        + NATURE_GROUP
                        + "&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());

        // now setup auth for the group
        TestResourceAccessManager tam =
                (TestResourceAccessManager) applicationContext.getBean("testResourceAccessManager");
        LayerInfo lakes = getCatalog().getLayerByName(getLayerId(MockData.LAKES));
        // LayerInfo forests = getCatalog().getLayerByName(getLayerId(MockData.FORESTS));
        tam.putLimits(
                "cite_nogroup", lakes, new DataAccessLimits(CatalogMode.HIDE, Filter.EXCLUDE));
        tam.putLimits("cite", lakes, new DataAccessLimits(CatalogMode.HIDE, Filter.INCLUDE));
        //        tam.putLimits("cite_nogroup", forests, new DataAccessLimits(CatalogMode.HIDE,
        // Filter.EXCLUDE));
        //        tam.putLimits("cite", forests, new DataAccessLimits(CatalogMode.HIDE,
        // Filter.INCLUDE));

        // this one cannot get the image, one layer in the group is not accessible
        setRequestAuth("cite_nogroup", "cite");
        response = getAsServletResponse(path);
        assertEquals(NOT_FOUND_ERROR_TYPE, response.getContentType());
        String str = string(getBinaryInputStream(response));
        assertTrue(
                str.contains(
                        "org.geotools.ows.ServiceException: Could not find layer " + NATURE_GROUP));

        // but this can access it all
        setRequestAuth("cite", "cite");
        response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }

    @Test
    public void testWorkspacedLayerGroup() throws Exception {
        Catalog catalog = getCatalog();
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        WorkspaceInfo ws = lakes.getResource().getStore().getWorkspace();
        LayerGroupInfo workspacedLayerGroup = getCatalog().getFactory().createLayerGroup();
        workspacedLayerGroup.setWorkspace(ws);
        workspacedLayerGroup.setName("citeGroup");
        workspacedLayerGroup.getLayers().add(lakes);
        workspacedLayerGroup.getStyles().add(null);
        catalog.add(workspacedLayerGroup);
        // enable direct WMS integration
        GWC.get().getConfig().setDirectWMSIntegrationEnabled(true);

        // no auth, it should work
        setRequestAuth(null, null);
        String path =
                ws.getName()
                        + "/wms?bgcolor=0x000000&LAYERS="
                        + workspacedLayerGroup.prefixedName()
                        + "&STYLES=&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1"
                        + "&REQUEST=GetMap&SRS=EPSG:4326&BBOX=0,-90,180,90&WIDTH=256&HEIGHT=256&transparent=false&tiled=true";
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals("image/png", response.getContentType());
    }
}
