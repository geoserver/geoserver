/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.geoserver.catalog.ResourceInfo.CUSTOM_DIMENSION_PREFIX;
import static org.geoserver.catalog.testreader.CustomFormat.CUSTOM_DIMENSION_NAME;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.xml.namespace.QName;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.junit.After;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Covers the OGC API - Maps general subsetting class: advertising the extent of dimensions beyond space and time
 * (elevation and custom, on vectors and coverages) and applying a {@code subset} on them.
 */
public class DimensionsTest extends MapsTestSupport {

    private static final QName WATTEMP = new QName(MockData.SF_URI, "watertemp", MockData.SF_PREFIX);
    private static final QName CUST_RASTER = new QName(MockData.SF_URI, "custwatertemp", MockData.SF_PREFIX);

    private static final String CRS_5030 = "http://www.opengis.net/def/crs/EPSG/0/5030";

    // sf:TimeWithStartEnd has three features keyed by startElevation, one per world quadrant:
    // f0 startElevation=1.0 -> NW, f1 startElevation=2.0 -> NE, f2 startElevation=1.0 -> SW. Pixels in a 50x50 map:
    private static final int[] NE = {37, 12};
    private static final int[] NW = {12, 12};
    private static final int[] SW = {12, 37};

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // raster with an elevation dimension, from the gs-main test-jar; covers the coverage domain read path
        testData.addRasterLayer(WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        // units carry a vertical CRS URI, so the extent reports it as vrs rather than the nil definition
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, CRS_5030, "m");
        // raster with a custom dimension (MY_DIMENSION), read by CustomFormat; covers the raster custom domain path
        testData.addRasterLayer(CUST_RASTER, "custwatertemp.zip", null, null, SystemTestData.class, getCatalog());
        CoverageInfo cust = getCatalog().getCoverageByName(CUST_RASTER.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setPresentation(DimensionPresentation.LIST);
        cust.getMetadata().put(CUSTOM_DIMENSION_PREFIX + CUSTOM_DIMENSION_NAME, di);
        getCatalog().save(cust);
    }

    /**
     * Each test configures the dimensions it needs on the shared vector layer, so clear them afterwards: a leftover
     * dimension applies its default value to the following requests and silently changes what they render.
     */
    @After
    public void clearVectorDimensions() {
        FeatureTypeInfo ft = getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        if (ft == null) return;
        ft.getMetadata()
                .keySet()
                .removeIf(k -> k.equals(ResourceInfo.TIME)
                        || k.equals(ResourceInfo.ELEVATION)
                        || k.startsWith(CUSTOM_DIMENSION_PREFIX));
        getCatalog().save(ft);
    }

    /** The units tests reconfigure the shared coverage, so put back the setup the other tests expect. */
    @After
    public void restoreRasterElevationUnits() {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, CRS_5030, "m");
    }

    @Test
    public void testVectorElevationExtentAdvertised() throws Exception {
        setupDimension(TIME_WITH_START_END, ResourceInfo.ELEVATION, "startElevation", DimensionPresentation.LIST, null);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:TimeWithStartEnd", 200);
        assertEquals(1.0, (double) json.read("$.extent.elevation.interval[0][0]"), 0d);
        assertEquals(2.0, (double) json.read("$.extent.elevation.interval[0][1]"), 0d);
        // list presentation -> irregular grid with the enumerated coordinates
        assertEquals(Integer.valueOf(2), json.read("$.extent.elevation.grid.cellsCount"));
        assertEquals(List.of(1.0, 2.0), json.read("$.extent.elevation.grid.coordinates"));
        // no CRS units, so the schema reference is the nil "unknown" definition, and no vrs/trs is emitted
        assertEquals(CollectionDocument.UNKNOWN_DEFINITION, json.read("$.extent.elevation.definition"));
        Map<String, Object> ext = json.read("$.extent.elevation");
        assertFalse(ext.containsKey("vrs"));
        assertFalse(ext.containsKey("trs"));
    }

    @Test
    public void testVectorCustomDimensionExtentAdvertised() throws Exception {
        setupDimension(TIME_WITH_START_END, "dim_custom", "startElevation", DimensionPresentation.LIST, null);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:TimeWithStartEnd", 200);
        assertEquals(1.0, (double) json.read("$.extent.custom.interval[0][0]"), 0d);
        assertEquals(2.0, (double) json.read("$.extent.custom.interval[0][1]"), 0d);
        assertEquals(List.of(1.0, 2.0), json.read("$.extent.custom.grid.coordinates"));
        assertEquals(CollectionDocument.UNKNOWN_DEFINITION, json.read("$.extent.custom.definition"));
    }

    @Test
    public void testRegularStepDimensionGrid() throws Exception {
        // discrete interval presentation with a configured step -> a regular grid described by resolution, no
        // coordinates
        setupDimension(TIME_WITH_START_END, "dim_step", "startElevation", DimensionPresentation.DISCRETE_INTERVAL, 0.5);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:TimeWithStartEnd", 200);
        assertEquals(1.0, (double) json.read("$.extent.step.interval[0][0]"), 0d);
        assertEquals(2.0, (double) json.read("$.extent.step.interval[0][1]"), 0d);
        assertEquals(0.5, ((Number) json.read("$.extent.step.grid.resolution")).doubleValue(), 0d);
        Map<String, Object> grid = json.read("$.extent.step.grid");
        assertFalse(grid.containsKey("coordinates"));
    }

    @Test
    public void testTimeLikeCustomDimensionAdvertisesTrs() throws Exception {
        // a custom dimension backed by a date attribute has Date domain values, so it gets a temporal reference system
        setupDimension(TIME_WITH_START_END, "dim_runtime", "startTime", DimensionPresentation.LIST, null);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:TimeWithStartEnd", 200);
        assertEquals(CollectionDocument.GREGORIAN_TRS, json.read("$.extent.runtime.trs"));
        assertEquals(Integer.valueOf(2), json.read("$.extent.runtime.grid.cellsCount"));
        Map<String, Object> ext = json.read("$.extent.runtime");
        assertFalse(ext.containsKey("definition"));
    }

    @Test
    public void testCoverageElevationExtentWithVrs() throws Exception {
        // reads the elevation domain from the coverage reader, and reports the CRS-URI units as vrs (not
        // unit/definition)
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:watertemp", 200);
        assertEquals(CRS_5030, json.read("$.extent.elevation.vrs"));
        assertEquals(0.0, (double) json.read("$.extent.elevation.interval[0][0]"), 0d);
        assertEquals(100.0, (double) json.read("$.extent.elevation.interval[0][1]"), 0d);
        assertEquals(List.of(0.0, 100.0), json.read("$.extent.elevation.grid.coordinates"));
        Map<String, Object> ext = json.read("$.extent.elevation");
        assertFalse(ext.containsKey("unit"));
        assertFalse(ext.containsKey("definition"));
    }

    /**
     * The elevation units carry a vertical CRS, and GeoServer configures it as an authority code: WMS advertises that
     * code as is, so the extent expands it to the URI the schema wants, from the very same configuration.
     */
    @Test
    public void testCoverageElevationVrsFromAuthorityCode() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, "EPSG:5030", "m");
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:watertemp", 200);
        assertEquals(CRS_5030, json.read("$.extent.elevation.vrs"));
        Map<String, Object> ext = json.read("$.extent.elevation");
        assertFalse(ext.containsKey("unit"));
        assertFalse(ext.containsKey("definition"));
    }

    /**
     * WMS capabilities fall back to {@link DimensionInfo#ELEVATION_UNITS} when the units are unset, but a vertical CRS
     * nobody configured is a guess, so the extent reports the nil definition instead of inventing one.
     */
    @Test
    public void testCoverageElevationUnsetUnitsGetNoVrs() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, null, null);
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:watertemp", 200);
        assertEquals(CollectionDocument.UNKNOWN_DEFINITION, json.read("$.extent.elevation.definition"));
        Map<String, Object> ext = json.read("$.extent.elevation");
        assertFalse(ext.containsKey("vrs"));
        assertFalse(ext.containsKey("unit"));
    }

    /** Elevation units naming no CRS are a plain unit of measure, and get no vrs invented for them. */
    @Test
    public void testCoverageElevationPlainUnitsGetNoVrs() throws Exception {
        setupRasterDimension(WATTEMP, ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, "meters", "m");
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:watertemp", 200);
        assertEquals("meters", json.read("$.extent.elevation.unit"));
        assertEquals(CollectionDocument.UNKNOWN_DEFINITION, json.read("$.extent.elevation.definition"));
        Map<String, Object> ext = json.read("$.extent.elevation");
        assertFalse(ext.containsKey("vrs"));
    }

    @Test
    public void testCoverageCustomDimensionExtentAdvertised() throws Exception {
        // reads the MY_DIMENSION domain (string values) from the coverage reader
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/sf:custwatertemp", 200);
        String dim = "$.extent." + CUSTOM_DIMENSION_NAME;
        assertEquals("CustomDimValueA", json.read(dim + ".interval[0][0]"));
        assertEquals("CustomDimValueC", json.read(dim + ".interval[0][1]"));
        assertEquals(CollectionDocument.UNKNOWN_DEFINITION, json.read(dim + ".definition"));
        assertEquals(Integer.valueOf(3), json.read(dim + ".grid.cellsCount"));
        assertEquals(
                List.of("CustomDimValueA", "CustomDimValueB", "CustomDimValueC"), json.read(dim + ".grid.coordinates"));
    }

    @Test
    public void testVectorElevationSubsetSelectsFeatures() throws Exception {
        setupDimension(TIME_WITH_START_END, ResourceInfo.ELEVATION, "startElevation", DimensionPresentation.LIST, null);
        // startElevation=2.0 exists only in the NE feature: only that quadrant renders
        BufferedImage ne = quadrantMap("subset=elevation(2.0)");
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);
        // startElevation=1.0 exists in the NW and SW features, not NE
        BufferedImage west = quadrantMap("subset=elevation(1.0)");
        assertOpaque(west, NW);
        assertOpaque(west, SW);
        assertTransparent(west, NE);
    }

    @Test
    public void testVectorCustomSubsetSelectsFeatures() throws Exception {
        setupDimension(TIME_WITH_START_END, "dim_custom", "startElevation", DimensionPresentation.LIST, null);
        // the custom axis reaches the WMS DIM_custom pipeline and filters exactly like a native dimension
        BufferedImage ne = quadrantMap("subset=custom(2.0)");
        assertOpaque(ne, NE);
        assertTransparent(ne, NW);
        assertTransparent(ne, SW);
    }

    @Test
    public void testCoverageCustomDimensionSubsetApplied() throws Exception {
        // subset=MY_DIMENSION(CustomDimValueA) reaches the WMS DIM_MY_DIMENSION pipeline and the coverage renders
        BufferedImage image = getAsImage(
                "ogc/maps/v1/collections/sf:custwatertemp/map?f=image/png&subset=" + CUSTOM_DIMENSION_NAME
                        + "(CustomDimValueA)&width=50&height=50",
                "image/png");
        assertEquals(50, image.getWidth());
        assertOpaque(image, new int[] {25, 25});
    }

    /**
     * /conf/general-subsetting/subset-definition D: a subset whose values fall entirely outside the ones valid for the
     * axis has nothing to return, so the answer is a 404 rather than an empty map. This holds for a vector dimension, a
     * coverage one and a custom one alike, WMS finding it out with an existence query as it applies the dimension.
     */
    @Test
    public void testSubsetOutsideDimensionValuesNotFound() throws Exception {
        setupDimension(TIME_WITH_START_END, ResourceInfo.ELEVATION, "startElevation", DimensionPresentation.LIST, null);
        // the vector elevation domain is 1.0 and 2.0
        assertNotFound("sf:TimeWithStartEnd", "subset=elevation(500:600)", "elevation");
        // the coverage elevation domain is 0 and 100
        assertNotFound("sf:watertemp", "subset=elevation(500:600)", "elevation");
        // and the coverage custom dimension only knows the CustomDimValue* strings
        assertNotFound("sf:custwatertemp", "subset=" + CUSTOM_DIMENSION_NAME + "(NotAValue)", CUSTOM_DIMENSION_NAME);
    }

    /** A value inside the domain still renders, so the 404 above is about the range and not about the axis. */
    @Test
    public void testSubsetInsideDimensionValuesRenders() throws Exception {
        setupDimension(TIME_WITH_START_END, ResourceInfo.ELEVATION, "startElevation", DimensionPresentation.LIST, null);
        assertEquals(
                200,
                getAsServletResponse(
                                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50&subset=elevation(1.0)")
                        .getStatus());
        assertEquals(
                200,
                getAsServletResponse(
                                "ogc/maps/v1/collections/sf:watertemp/map?f=image/png&width=50&height=50&subset=elevation(0)")
                        .getStatus());
    }

    /** /conf/datetime/subset-definition D: the same rule on the time axis, through both spellings of the parameter. */
    @Test
    public void testTimeOutsideDimensionValuesNotFound() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        assertNotFound("sf:TimeWithStartEnd", "datetime=1990-01-01T00:00:00Z", "time");
        assertNotFound("sf:TimeWithStartEnd", "subset=time(%221990-01-01T00:00:00Z%22)", "time");
        // a time the layer does have still renders
        assertEquals(
                200,
                getAsServletResponse(
                                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50&datetime=2012-02-11T00:00:00Z")
                        .getStatus());
    }

    /** The feature info resource answers on the same map, so it reports the missing dimension value the same way. */
    @Test
    public void testInfoOutsideDimensionValuesNotFound() throws Exception {
        setupDimension(TIME_WITH_START_END, ResourceInfo.ELEVATION, "startElevation", DimensionPresentation.LIST, null);
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?f=application%2Fjson&width=50&height=50"
                        + "&i=25&j=25&subset=elevation(500:600)");
        assertEquals(404, response.getStatus());
    }

    /** Asserts a map request is refused with a 404 naming the dimension that has no matching value. */
    private void assertNotFound(String collection, String query, String dimension) throws Exception {
        String url = "ogc/maps/v1/collections/" + collection + "/map?f=image/png&width=50&height=50&" + query;
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(query, 404, response.getStatus());
        assertEquals("application/json", getBaseMimeType(response.getContentType()));
        DocumentContext json = JsonPath.parse(response.getContentAsString());
        assertEquals("NotFound", json.read("type"));
        assertThat(json.read("title"), containsString(dimension));
    }

    /**
     * Nearest match is a GeoServer configuration option, applied by the WMS dimension handling the map request goes
     * through: a time the layer does not have snaps to the closest one it has, rather than being refused.
     */
    @Test
    public void testTimeNearestMatch() throws Exception {
        setupNearestTimeDimension(null);
        MockHttpServletResponse response = getAsServletResponse(nearestTimeMap());
        assertEquals(200, response.getStatus());
        // the two features starting on the first day of the domain are drawn, the one starting a day later is not
        BufferedImage image = renderedMap(response);
        assertOpaque(image, NW);
        assertOpaque(image, SW);
        assertTransparent(image, NE);
        assertEquals(
                List.of("99 Nearest value used: time=2012-02-11T00:00:00.000Z  (sf:TimeWithStartEnd)"),
                List.copyOf(response.getHeaders("Warning")));
        // the API header reports the time drawn, not the one asked for (OGC API - Maps, /req/core/map-response)
        assertEquals("2012-02-11T00:00:00Z", response.getHeader("Content-Datetime"));
    }

    /** The nearest value must fall inside the configured acceptable interval, otherwise nothing matches. */
    @Test
    public void testTimeNearestMatchOutsideAcceptableInterval() throws Exception {
        setupNearestTimeDimension("P1D");
        MockHttpServletResponse response = getAsServletResponse(nearestTimeMap());
        assertEquals(200, response.getStatus());
        BufferedImage image = renderedMap(response);
        assertTransparent(image, NW);
        assertTransparent(image, SW);
        assertTransparent(image, NE);
        assertEquals(
                List.of("99 No nearest value found on sf:TimeWithStartEnd: time"),
                List.copyOf(response.getHeaders("Warning")));
        // nothing was drawn, so there is no rendered time to report and the requested one stands
        assertEquals("1990-01-01T00:00:00Z", response.getHeader("Content-Datetime"));
    }

    /** The map in the response, checking it is a PNG: the nearest match tests need its headers and its pixels. */
    private BufferedImage renderedMap(MockHttpServletResponse response) throws Exception {
        assertEquals("image/png", getBaseMimeType(response.getContentType()));
        return ImageIO.read(new ByteArrayInputStream(response.getContentAsByteArray()));
    }

    /** A map request for a time twenty years before the layer domain, the case nearest match exists for. */
    private static String nearestTimeMap() {
        return "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50"
                + "&datetime=1990-01-01T00:00:00Z";
    }

    /** Enables the time dimension with nearest match on, and an optional acceptable interval around the request. */
    private void setupNearestTimeDimension(String acceptableInterval) {
        setupStartEndTimeDimension(TIME_WITH_START_END, ResourceInfo.TIME, "startTime", "endTime");
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(TIME_WITH_START_END.getLocalPart());
        DimensionInfo time = info.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        time.setNearestMatchEnabled(true);
        time.setAcceptableInterval(acceptableInterval);
        getCatalog().save(info);
    }

    @Test
    public void testUnknownSubsetAxisRejected() throws Exception {
        // Lakes has no such dimension; the subset parameter is known but the axis name is not a valid value, so the
        // spec mandates a 4xx (/req/general-subsetting/subset-definition)
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Depth(1:2)&width=50&height=50");
        assertEquals(400, response.getStatus());
        assertEquals("application/json", getBaseMimeType(response.getContentType()));
        DocumentContext json = JsonPath.parse(response.getContentAsString());
        assertEquals("InvalidParameterValue", json.read("type"));
        assertThat(json.read("title"), containsString("Depth"));
    }

    private BufferedImage quadrantMap(String subset) throws Exception {
        return getAsImage(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&" + subset + "&width=50&height=50",
                "image/png");
    }

    private static void assertOpaque(BufferedImage image, int[] xy) {
        assertNotEquals("expected rendered data at " + xy[0] + "," + xy[1], 0, image.getRGB(xy[0], xy[1]) >>> 24);
    }

    private static void assertTransparent(BufferedImage image, int[] xy) {
        assertEquals("expected no data at " + xy[0] + "," + xy[1], 0, image.getRGB(xy[0], xy[1]) >>> 24);
    }

    private void setupDimension(
            QName typeName, String key, String attribute, DimensionPresentation presentation, Double resolution) {
        FeatureTypeInfo info = getCatalog().getFeatureTypeByName(typeName.getLocalPart());
        DimensionInfo di = new DimensionInfoImpl();
        di.setEnabled(true);
        di.setAttribute(attribute);
        di.setPresentation(presentation);
        if (resolution != null) di.setResolution(BigDecimal.valueOf(resolution));
        info.getMetadata().put(key, di);
        getCatalog().save(info);
    }
}
