/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.geotools.image.test.ImageAssert;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class MapsTest extends MapsTestSupport {

    static final String NATURE_GROUP = "nature";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // a layer group with default styling, to exercise the default map on a group; tighten the
        // forests bounds too, so the group extent matches the actual data instead of the world
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(catalog);
        LayerInfo lakes = catalog.getLayerByName(getLayerId(MockData.LAKES));
        LayerInfo forests = catalog.getLayerByName(getLayerId(MockData.FORESTS));
        cb.setupBounds(forests.getResource());
        catalog.save(forests.getResource());
        LayerGroupInfo group = catalog.getFactory().createLayerGroup();
        group.setName(NATURE_GROUP);
        group.getLayers().add(lakes);
        group.getLayers().add(forests);
        group.getStyles().add(null);
        group.getStyles().add(null);
        cb.calculateLayerGroupBounds(group);
        catalog.add(group);
    }

    @Test
    public void testDefaultMap() throws Exception {
        BufferedImage image = getAsImage("ogc/maps/v1/collections/Lakes/map?f=image/png", "image/png");
        File expectedImage = new File("src/test/resources/expected/mapsDefault.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapWidth() throws Exception {
        BufferedImage image = getAsImage("ogc/maps/v1/collections/Lakes/map?f=image/png&width=100", "image/png");
        File expectedImage = new File("src/test/resources/expected/mapsDefaultW100.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapHeight() throws Exception {
        BufferedImage image = getAsImage("ogc/maps/v1/collections/Lakes/map?f=image/png&height=50", "image/png");
        File expectedImage = new File("src/test/resources/expected/mapsDefaultH50.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapLayerGroup() throws Exception {
        // default map on a layer group goes through the null-styleInfo branch: it must still render
        BufferedImage image = getAsImage("ogc/maps/v1/collections/" + NATURE_GROUP + "/map?f=image/png", "image/png");
        File expectedImage = new File("src/test/resources/expected/mapsGroup.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultStyledMap() throws Exception {
        BufferedImage image = getAsImage("ogc/maps/v1/collections/cite:Lakes/styles/red/map?f=image/png", "image/png");
        File expectedImage = new File("src/test/resources/expected/mapsRed.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDatetimeJson() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        Integer[] values = {1, 1, 1, 1, 0, 1};
        // work with different time resolutions
        String[] dates = {
            "2012", "2012-02", "2012-02-11", "2012-02-11T00:00:00Z", "2012-02-14T00:00:00.000Z", "2012-02-12T00:00:00Z"
        };
        for (int i = 0; i < 6; i++) {
            DocumentContext json = getAsJSONPath(
                    "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map/info?i=50&j=50&f=application%2Fjson&datetime="
                            + dates[i],
                    200);
            assertEquals(values[i], json.read("$.numberReturned", Integer.class));
        }
    }

    @Test
    public void testSubsetToBBox() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Lon(1:3),Lat(0:2)&width=50&height=50");
        assertEquals(200, response.getStatus());
    }

    /** A map is flat: the vertical range of a six ordinate bbox is dropped, the map is the horizontal one. */
    @Test
    public void testBBox3D() throws Exception {
        String base = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50&bbox=";
        BufferedImage flat = getAsImage(base + "-1,-1,1,1", "image/png");
        assertEquals(
                flat.getRGB(25, 25),
                getAsImage(base + "-1,-1,0,1,1,100", "image/png").getRGB(25, 25));

        // a 3-dimensional bbox-crs is accepted too, and renders its horizontal part
        assertEquals(
                flat.getRGB(25, 25),
                getAsImage(base + "-1,-1,0,1,1,100&bbox-crs=EPSG:4979", "image/png")
                        .getRGB(25, 25));
    }

    @Test
    public void testOrientation() throws Exception {
        BufferedImage image = getAsImage(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50&orientation=45",
                "image/png");
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    public void testDisabledClassParameterIgnored() throws Exception {
        // orientation off: the parameter is ignored, not rejected, and the map still renders
        withConformance(MapsConformance::setOrientation, false, () -> {
            MockHttpServletResponse response = getAsServletResponse(
                    "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50&orientation=45");
            assertEquals(200, response.getStatus());
        });
    }

    @Test
    public void testWidthWorksWithScalingDisabled() throws Exception {
        // width/height are provided by the spatial subsetting class too, so they survive with scaling off
        withConformance(MapsConformance::setScaling, false, () -> {
            BufferedImage image = getAsImage("ogc/maps/v1/collections/Lakes/map?f=image/png&width=100", "image/png");
            assertEquals(100, image.getWidth());
        });
    }

    /** A viewport of zero or negative pixels is meaningless (Scaling, width/height requirement C). */
    @Test
    public void testNonPositiveSizeRejected() throws Exception {
        String base = "ogc/maps/v1/collections/Lakes/map?f=image/png";
        for (String size : new String[] {"width=0", "width=-10", "height=0", "height=-10"}) {
            DocumentContext json = getAsJSONPath(base + "&" + size, 400);
            assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
            assertThat(json.read("title"), containsString(size.split("=")[0] + " must be a positive number"));
        }
        // the size the map is actually rendered at stays acceptable
        assertEquals(1, getAsImage(base + "&width=1&height=1", "image/png").getWidth());
    }

    /** Only one extent per request: bbox, center and the spatial axes of a subset are three ways to say it. */
    @Test
    public void testCombinedExtentsRejected() throws Exception {
        String base = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50";
        for (String extents : new String[] {
            "&bbox=-1,-1,1,1&subset=Lon(0:2),Lat(0:2)",
            "&bbox=-1,-1,1,1&center=0,0",
            "&subset=Lon(0:2),Lat(0:2)&center=0,0"
        }) {
            DocumentContext json = getAsJSONPath(base + extents, 400);
            assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
            assertThat(json.read("title"), containsString("all define the map extent"));
        }

        // a subset on the time axis alone says nothing about the extent, so it combines with a bbox
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        MockHttpServletResponse response =
                getAsServletResponse("ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50"
                        + "&bbox=-1,-1,1,1&subset=time(\"2012-02-11T00:00:00Z\")");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testScaleDenominatorWithSizeAndBboxRejected() throws Exception {
        // scale-denominator + explicit width/height + a spatial extent is over-constrained (Scaling req E)
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-1,1,1&width=100&height=100&scale-denominator=1000",
                "scale-denominator");
    }

    @Test
    public void testScaleDenominatorWithSizeNoSubsettingRejected() throws Exception {
        // scale-denominator + width/height without the spatial subsetting class (Scaling req D)
        withConformance(
                MapsConformance::setSpatialSubsetting,
                false,
                () -> assertBadRequestMentions(
                        "ogc/maps/v1/collections/Lakes/map?f=image/png&width=100&scale-denominator=1000",
                        "scale-denominator"));
    }

    @Test
    public void testUnsupportedCrsRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50&crs=EPSG:299999",
                "CRS");
    }

    @Test
    public void testDatelineBboxSupported() throws Exception {
        // New Zealand area crosses the antimeridian (low longitude > high one); it must render, not be rejected,
        // as a single continuous extent extending past 180 (170..190)
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=170,-50,-170,-30&width=50&height=50");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testDatelineSubsetSupported() throws Exception {
        // a subset whose low longitude is greater than the high one crosses the antimeridian too, just like the
        // bbox form: the extent is 170..190, not the whole world minus that strip
        String base = "ogc/maps/v1/collections/" + getLayerId(MockData.WORLD) + "/map?f=image/png&width=50&height=50&";
        BufferedImage wrapping = getAsImage(base + "subset=Lon(170:-170),Lat(-5:5)", "image/png");
        // pinned against the same area written without the inversion, which does not take the wrapping branch
        ImageAssert.assertEquals(getAsImage(base + "subset=Lon(170:190),Lat(-5:5)", "image/png"), wrapping, 0);
        ImageAssert.assertEquals(getAsImage(base + "bbox=170,-5,-170,5", "image/png"), wrapping, 0);
    }

    @Test
    public void testWideCartesianBboxTolerated() throws Exception {
        // silly clients send world-spanning cartesian boxes; these are a single envelope and must not error
        BufferedImage image = getAsImage(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-520,-80,520,80&width=50&height=50", "image/png");
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    public void testInvalidSubsetExpressionRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Lon10:20&width=50&height=50", "subset");
    }

    @Test
    public void testUnknownSubsetAxisRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Depth(1:2)&width=50&height=50", "subset");
    }

    @Test
    public void testCenterWithoutSizeRejected() throws Exception {
        assertBadRequestMentions("ogc/maps/v1/collections/Lakes/map?f=image/png&center=0,0", "center");
    }

    @Test
    public void testCenterTooFewOrdinatesRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&center=0&width=50&height=50&scale-denominator=1000",
                "center");
    }

    @Test
    public void testInvalidStyleRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/cite:Lakes/styles/NotAStyle/map?f=image/png&width=50&height=50", "style");
    }

    @Test
    public void testTimeSubsetOnLayerGroupRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/" + NATURE_GROUP
                        + "/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50&datetime=2012-01-01",
                "layer group");
    }

    @Test
    public void testDatetimeOnLayerWithoutTimeDimensionRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50&datetime=2012-01-01",
                "Time dimension");
    }

    @Test
    public void testInvalidDatetimeRejected() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // a comma-separated list is neither a single instant nor an interval
        assertBadRequestMentions(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=image/png&width=50&height=50&datetime=2012-02-11,2012-02-12",
                "datetime");
    }

    @Test
    public void testUnknownCollectionRejected() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/maps/v1/collections/ThisDoesNotExist/map?f=image/png&width=50&height=50");
        assertThat(response.getStatus(), greaterThanOrEqualTo(400));
        assertThat(response.getContentAsString(), containsString("ThisDoesNotExist"));
    }

    /** Asserts the request returns a 400 whose error body names the offending parameter. */
    private void assertBadRequestMentions(String url, String parameter) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(url);
        assertEquals(400, response.getStatus());
        assertThat(response.getContentAsString(), containsString(parameter));
    }

    /**
     * The time axis of a subset takes an interval with the {@code low:high} separator, and an asterisk for the earliest
     * or latest available time (/req/datetime/subset-definition). The north-east quadrant of the layer only holds data
     * from the second timestamp on, so it tells an interval apart from the single instant that starts it.
     */
    @Test
    public void testSubsetTimeInterval() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        int instant = northEastPixel("subset=time(\"2012-02-11T00:00:00Z\")");
        int interval = northEastPixel("subset=time(\"2012-02-11T00:00:00Z\":\"2012-02-12T00:00:00Z\")");
        assertEquals("a single instant leaves the second timestamp out", 0, instant >>> 24);
        assertEquals("the interval covers both timestamps", 255, interval >>> 24);
        // the same interval written as a datetime must render identically
        assertEquals(interval, northEastPixel("datetime=2012-02-11T00:00:00Z/2012-02-12T00:00:00Z"));

        // an asterisk stands for the earliest and the latest time of the layer own extent
        assertEquals(interval, northEastPixel("subset=time(*:*)"));
        assertEquals(instant, northEastPixel("subset=time(*:\"2012-02-11T00:00:00Z\")"));
    }

    /** The datetime parameter takes the same open-ended intervals, written with the OGC double dot. */
    @Test
    public void testDatetimeOpenInterval() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        int instant = northEastPixel("datetime=2012-02-11T00:00:00Z");
        int wholeExtent = northEastPixel("datetime=2012-02-11T00:00:00Z/2012-02-12T00:00:00Z");
        assertEquals(wholeExtent, northEastPixel("datetime=../.."));
        assertEquals(wholeExtent, northEastPixel("datetime=2012-02-11T00:00:00Z/.."));
        // an open start resolves to the beginning of the extent, which is the first timestamp
        assertEquals(instant, northEastPixel("datetime=../2012-02-11T00:00:00Z"));
    }

    /** Pixel of the north-east quadrant of the time enabled layer, rendered under the given time query. */
    private int northEastPixel(String timeQuery) throws Exception {
        String path = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=40&height=40"
                + "&bbox=-180,-90,180,90&transparent=true&" + timeQuery.replace("\"", "%22");
        return getAsImage(path, "image/png").getRGB(30, 10);
    }

    @Test
    public void testSubsetTimeAppliedWhenSpatialSubsettingDisabled() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // spatial subsetting off: the subset spatial axes are ignored, but its time axis (datetime class) still applies
        withConformance(MapsConformance::setSpatialSubsetting, false, () -> {
            DocumentContext json = getAsJSONPath(
                    "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map/info?i=50&j=50&f=application%2Fjson&subset=time(\"2012-02-11T00:00:00Z\")",
                    200);
            assertEquals(Integer.valueOf(1), json.read("$.numberReturned", Integer.class));
        });
    }

    @Test
    public void testInfoDatetimeConformanceIgnored() throws Exception {
        // the info endpoint handles the parameter conformance classes like the map endpoint: a disabled class
        // means the parameter is ignored, not rejected. With datetime enabled the query would fail on Lakes
        // (no time dimension); with the class disabled the datetime is dropped and the request succeeds.
        withConformance(MapsConformance::setDatetime, false, () -> {
            MockHttpServletResponse response = getAsServletResponse(
                    "ogc/maps/v1/collections/Lakes/map/info?i=5&j=5&f=application%2Fjson&datetime=2012");
            assertEquals(200, response.getStatus());
        });
    }

    @Test
    public void testDefaultStyleInfo() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // no styleId segment: exercises the default-style binding on the info endpoint
        DocumentContext json = getAsJSONPath(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?i=50&j=50&f=application%2Fjson&datetime=2012-02-11",
                200);
        assertEquals(Integer.valueOf(1), json.read("$.numberReturned", Integer.class));
        // the info payload is GeoJSON: verify the actual feature, not just the count
        assertEquals("FeatureCollection", json.read("$.type"));
        assertEquals("Polygon", json.read("$.features[0].geometry.type"));
        assertEquals(1.0, json.read("$.features[0].properties.startElevation", Double.class), 0.0);
    }

    // a pixel inside Blue Lake, which also falls inside Green Forest, so both layers of the group answer
    private static final String GROUP_INFO =
            "ogc/maps/v1/collections/nature/map/info?f=application%2Fjson&bbox=-0.002,-0.003,0.005,0.002"
                    + "&width=100&height=100&i=50&j=64";

    @Test
    public void testInfoLimitDefaultsToOneFeature() throws Exception {
        DocumentContext json = getAsJSONPath(GROUP_INFO, 200);
        assertEquals(Integer.valueOf(1), json.read("$.features.length()"));
    }

    @Test
    public void testInfoLimitReturnsAllGroupLayers() throws Exception {
        DocumentContext json = getAsJSONPath(GROUP_INFO + "&limit=10", 200);
        assertEquals(Integer.valueOf(2), json.read("$.features.length()"));
        assertEquals("Blue Lake", json.read("$.features[0].properties.NAME"));
        assertEquals("Green Forest", json.read("$.features[1].properties.NAME"));
    }

    @Test
    public void testInfoInvalidLimitRejected() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(GROUP_INFO + "&limit=-1");
        assertEquals(400, response.getStatus());
        assertEquals("application/json", getBaseMimeType(response.getContentType()));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("InvalidParameterValue", json.read("type"));
        assertThat(json.read("title"), containsString("limit"));
    }

    @Test
    public void testInfoDisabled() throws Exception {
        withConformance(MapsConformance::setFeatureInfo, false, () -> {
            MockHttpServletResponse response =
                    getAsServletResponse("ogc/maps/v1/collections/Lakes/map/info?i=5&j=5&f=application%2Fjson");
            assertEquals(404, response.getStatus());
        });
    }

    @Test
    public void testDatetimeHTMLMapsFormat() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        Document document = getAsJSoup(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html&datetime=2012-02-12T00:00:00Z");
        assertEquals("2012-02-12T00:00:00Z", getParameterValue(document, "datetime"));
    }

    @Test
    public void testHTMLNoDatetime() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // failed here when no datetime provided, FTL processing error, null on js_string
        Document document = getAsJSoup("ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=html");
        assertNull(getParameterValue(document, "datetime"));
    }

    private static String getParameterValue(Document document, String key) {
        Elements parameters = document.select("input[type='hidden'][title='" + key + "']");
        if (parameters.isEmpty()) return null;
        if (parameters.size() > 1) fail("Found more than one element with key " + key + ": " + parameters);
        Element parameter = parameters.first();
        return parameter.attr("value");
    }
}
