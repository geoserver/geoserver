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
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
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
