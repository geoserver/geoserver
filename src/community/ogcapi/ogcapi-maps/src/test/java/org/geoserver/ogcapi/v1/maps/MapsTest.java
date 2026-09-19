/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.fail;

import com.jayway.jsonpath.DocumentContext;
import java.awt.image.BufferedImage;
import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
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

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        // tighten the forests bounds, so the group extent matches the actual data instead of the world
        Catalog catalog = getCatalog();
        LayerInfo forests = layer(MockData.FORESTS);
        new CatalogBuilder(catalog).setupBounds(forests.getResource());
        catalog.save(forests.getResource());
        addNatureGroup(NATURE_GROUP);
    }

    @Test
    public void testDefaultMap() throws Exception {
        BufferedImage image = getAsPNG("ogc/maps/v1/collections/Lakes/map?f=image/png");
        File expectedImage = new File("src/test/resources/expected/mapsDefault.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapWidth() throws Exception {
        BufferedImage image = getAsPNG("ogc/maps/v1/collections/Lakes/map?f=image/png&width=100");
        File expectedImage = new File("src/test/resources/expected/mapsDefaultW100.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapHeight() throws Exception {
        BufferedImage image = getAsPNG("ogc/maps/v1/collections/Lakes/map?f=image/png&height=50");
        File expectedImage = new File("src/test/resources/expected/mapsDefaultH50.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultMapLayerGroup() throws Exception {
        // default map on a layer group goes through the null-styleInfo branch: it must still render
        BufferedImage image = getAsPNG("ogc/maps/v1/collections/" + NATURE_GROUP + "/map?f=image/png");
        File expectedImage = new File("src/test/resources/expected/mapsGroup.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    @Test
    public void testDefaultStyledMap() throws Exception {
        BufferedImage image = getAsPNG("ogc/maps/v1/collections/cite:Lakes/styles/red/map?f=image/png");
        File expectedImage = new File("src/test/resources/expected/mapsRed.png");
        ImageAssert.assertEquals(expectedImage, image, 0);
    }

    /** A numeric parameter that is not a number is a client error, not a server one. */
    @Test
    public void testInvalidNumber() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/collections/Lakes/map?f=image/png&width=abcd", 400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("type"));
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
    public void testContentHeaders() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse("ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("-1.0,-1.0,1.0,1.0", response.getHeader("Content-Bbox"));
        // the map is delivered in CRS84, the default response CRS when the collection declares no storageCrs, so
        // /req/core/map-response part C leaves it out of the header and part D reads the bbox in it
        assertNull(response.getHeader("Content-Crs"));
    }

    @Test
    public void testOutputCrsBareCode() throws Exception {
        // the bare EPSG:4326 code is a GeoServer extension over the URI and SafeCURIE forms of the spec, and like
        // everywhere else in GeoServer it is longitude-first, which makes it CRS84: no header, no axis flip
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-2,-1,2,1&crs=EPSG:4326&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("-2.0,-1.0,2.0,1.0", response.getHeader("Content-Bbox"));
        assertNull(response.getHeader("Content-Crs"));
    }

    @Test
    public void testSubsetToBBox() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Lon(1:3),Lat(0:2)&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertNull(response.getHeader("Content-Crs"));
        assertEquals("1.0,0.0,3.0,2.0", response.getHeader("Content-Bbox"));
    }

    @Test
    public void testBBoxCrsSafeCurie() throws Exception {
        // [EPSG:4326] is the authority latitude/longitude order. The same area the default CRS84 bbox expresses
        // as -2,-1,2,1 (lon,lat) is here -1,-2,1,2 (lat,lon), and yields the identical delivered extent.
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-2,1,2&bbox-crs=[EPSG:4326]&crs=EPSG:4326&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("-2.0,-1.0,2.0,1.0", response.getHeader("Content-Bbox"));
    }

    @Test
    public void testOutputCrsSafeCurie() throws Exception {
        // the SafeCURIE form must be accepted for the output crs, and deliver the same map as the bare code
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-2,-1,2,1&crs=[EPSG:4326]&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("-1.0,-2.0,1.0,2.0", response.getHeader("Content-Bbox"));
    }

    @Test
    public void testSubsetCrsSafeCurie() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Lon(0:2),Lat(0:2)&subset-crs=[EPSG:4326]&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("0.0,0.0,2.0,2.0", response.getHeader("Content-Bbox"));
    }

    @Test
    public void testCenterCrsAxisOrder() throws Exception {
        // center in CRS84 (lon,lat) and the same point in authority [EPSG:4326] (lat,lon) must define the same extent
        String base = "ogc/maps/v1/collections/Lakes/map?f=image/png&scale-denominator=1000000&width=50&height=50";
        MockHttpServletResponse lonLat = getAsServletResponse(base + "&center=0.5,1");
        MockHttpServletResponse latLon = getAsServletResponse(base + "&center=1,0.5&center-crs=[EPSG:4326]");
        assertEquals(200, lonLat.getStatus());
        assertEquals(200, latLon.getStatus());
        assertEquals(lonLat.getHeader("Content-Bbox"), latLon.getHeader("Content-Bbox"));
    }

    @Test
    public void testOutputCrsReprojection() throws Exception {
        // the output crs must actually reproject: the delivered CRS is EPSG:3395, not the layer native EPSG:4326
        MockHttpServletResponse response = getAsServletResponse(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-1,-2,1,2&crs=EPSG:3395&width=50&height=50");
        assertEquals(200, response.getStatus());
        assertEquals("<http://www.opengis.net/def/crs/EPSG/0/3395>", response.getHeader("Content-Crs"));
        // the bbox is now in metres, and the taller box tells the easting from the northing: one degree of
        // longitude is 111319 m at the equator, two degrees of latitude 221194 m on the World Mercator ellipsoid
        String[] ordinates = response.getHeader("Content-Bbox").split(",");
        assertEquals(111319.49, Double.parseDouble(ordinates[2]), 1.0);
        assertEquals(221194.85, Double.parseDouble(ordinates[3]), 1.0);
    }

    /** A map is flat: the vertical range of a six ordinate bbox is dropped, the map is the horizontal one. */
    @Test
    public void testBBox3D() throws Exception {
        String base = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50&bbox=";
        BufferedImage flat = getAsPNG(base + "-1,-1,1,1");
        assertEquals(flat.getRGB(25, 25), getAsPNG(base + "-1,-1,0,1,1,100").getRGB(25, 25));

        // a 3-dimensional bbox-crs is accepted too, and renders its horizontal part
        assertEquals(
                flat.getRGB(25, 25),
                getAsPNG(base + "-1,-1,0,1,1,100&bbox-crs=EPSG:4979").getRGB(25, 25));
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
        assertEquals("170.0,-50.0,190.0,-30.0", response.getHeader("Content-Bbox"));
    }

    @Test
    public void testDatelineSubsetSupported() throws Exception {
        // a subset whose low longitude is greater than the high one crosses the antimeridian too, just like the
        // bbox form: the extent is 170..190, not the whole world minus that strip
        String base = "ogc/maps/v1/collections/" + getLayerId(MockData.WORLD) + "/map?f=image/png&width=50&height=50&";
        BufferedImage wrapping = getAsPNG(base + "subset=Lon(170:-170),Lat(-5:5)");
        // pinned against the same area written without the inversion, which does not take the wrapping branch
        ImageAssert.assertEquals(getAsPNG(base + "subset=Lon(170:190),Lat(-5:5)"), wrapping, 0);
        ImageAssert.assertEquals(getAsPNG(base + "bbox=170,-5,-170,5"), wrapping, 0);
    }

    @Test
    public void testWideCartesianBboxTolerated() throws Exception {
        // silly clients send world-spanning cartesian boxes; these are a single envelope and must not error
        BufferedImage image =
                getAsPNG("ogc/maps/v1/collections/Lakes/map?f=image/png&bbox=-520,-80,520,80&width=50&height=50");
        assertEquals(50, image.getWidth());
        assertEquals(50, image.getHeight());
    }

    @Test
    public void testBboxWiderThanTheWorldKeptWhole() throws Exception {
        // an OpenLayers preview fitting the whole world asks for a bit more than a full turn; the longitudes must
        // not be rolled into a sliver around the antimeridian, the extent is the one requested
        String world =
                "ogc/maps/v1/collections/" + getLayerId(MockData.WORLD) + "/map?f=image/png&width=370&height=190";
        MockHttpServletResponse response = getAsServletResponse(world + "&bbox=-185,-95,185,95");
        assertEquals(200, response.getStatus());
        assertEquals("-185.0,-95.0,185.0,95.0", response.getHeader("Content-Bbox"));

        // the same box written in the lat-first order of the CRS it names
        response = getAsServletResponse(world + "&bbox=-95,-185,95,185&bbox-crs=%5BEPSG:4326%5D");
        assertEquals(200, response.getStatus());
        assertEquals("-185.0,-95.0,185.0,95.0", response.getHeader("Content-Bbox"));

        // one degree per pixel either way, so the origin sits 185 pixels from the left and 95 from the top, where
        // the plain world map has it in its own middle
        BufferedImage wide = getAsPNG(world + "&bbox=-185,-95,185,95");
        BufferedImage whole = getAsPNG("ogc/maps/v1/collections/" + getLayerId(MockData.WORLD)
                + "/map?f=image/png&width=360&height=180&bbox=-180,-90,180,90");
        assertEquals(whole.getRGB(180, 90), wide.getRGB(185, 95));
        assertEquals(whole.getRGB(0, 0), wide.getRGB(5, 5));
    }

    /** A projected bbox names the same area as its geographic equivalent, whatever axis order its CRS declares. */
    @Test
    public void testProjectedBbox() throws Exception {
        String map = "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50&crs=CRS:84";
        // roughly 0.001 degrees around the origin, in web mercator meters
        BufferedImage projected = getAsPNG(map + "&bbox=-111.32,-111.32,111.32,111.32&bbox-crs=EPSG:3857");
        BufferedImage geographic = getAsPNG(map + "&bbox=-0.001,-0.001,0.001,0.001");
        ImageAssert.assertEquals(geographic, projected, 100);
    }

    @Test
    public void testInvalidSubsetExpressionRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&subset=Lon10:20&width=50&height=50", "subset");
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

    /** A group whose members all lack a time dimension has nothing to apply the time to, layer group or not. */
    @Test
    public void testDatetimeOnLayerGroupWithoutTimeDimensionRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/" + NATURE_GROUP
                        + "/map?f=image/png&bbox=-1,-1,1,1&width=50&height=50&datetime=2012-01-01",
                "time dimension");
    }

    @Test
    public void testDatetimeOnLayerWithoutTimeDimensionRejected() throws Exception {
        assertBadRequestMentions(
                "ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50&datetime=2012-01-01",
                "time dimension");
    }

    @Test
    public void testInvalidDatetimeRejected() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        // a comma-separated list is neither a single instant nor an interval
        assertBadRequestMentions(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/styles/Default/map?f=image/png&width=50&height=50&datetime=2012-02-11,2012-02-12",
                "datetime");
    }

    /**
     * A datetime the parser cannot read is a client error, not a server one: the values below are all outside the
     * syntax of /req/datetime/datetime-definition, "now" and "current" being WMS keywords the time parser also knows.
     */
    @Test
    public void testInvalidDatetimeSyntaxRejected() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        String base = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50&datetime=";
        for (String datetime : new String[] {
            "not-a-date", "..", "now", "current", "now,2012-02-11T00:00:00Z", "2012-02-11T00:00:00-04:00"
        }) {
            MockHttpServletResponse response = getAsServletResponse(base + datetime);
            assertEquals(datetime, 400, response.getStatus());
            DocumentContext json = getAsJSONPath(response);
            assertEquals(datetime, APIException.INVALID_PARAMETER_VALUE, json.read("type"));
            assertThat(json.read("title", String.class), containsString(datetime));
        }
    }

    @Test
    public void testUnknownCollectionRejected() throws Exception {
        assertInvalidParameter(
                "ogc/maps/v1/collections/ThisDoesNotExist/map?f=image/png&width=50&height=50", "ThisDoesNotExist");
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
        assertEquals("a single instant leaves the second timestamp out", 0, alpha(instant));
        assertEquals("the interval covers both timestamps", 255, alpha(interval));
        // the same interval written as a datetime must render identically
        assertEquals(interval, northEastPixel("datetime=2012-02-11T00:00:00Z/2012-02-12T00:00:00Z"));

        // an asterisk stands for the earliest and the latest time of the layer own extent
        assertEquals(interval, northEastPixel("subset=time(*:*)"));
        assertEquals(instant, northEastPixel("subset=time(*:\"2012-02-11T00:00:00Z\")"));
    }

    /**
     * A layer with a time dimension reports the time actually rendered, also when the request did not ask for one, and
     * always as an RFC 3339 instant or interval.
     */
    @Test
    public void testContentDatetime() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        String base = "ogc/maps/v1/collections/sf:TimeWithStartEnd/map?f=image/png&width=50&height=50";
        // an instant is reported as an instant, not as the one second window the parser matches it against
        assertEquals(
                "2012-02-11T00:00:00Z",
                getAsServletResponse(base + "&datetime=2012-02-11T00:00:00Z").getHeader("Content-Datetime"));
        assertEquals(
                "2012-02-11T00:00:00Z/2012-02-12T00:00:00Z",
                getAsServletResponse(base + "&datetime=2012-02-11T00:00:00Z/2012-02-12T00:00:00Z")
                        .getHeader("Content-Datetime"));
        // the shortened forms the spec allows are reported as given too
        assertEquals("2012-02", getAsServletResponse(base + "&datetime=2012-02").getHeader("Content-Datetime"));
        // an open bound is not a datetime, so the instants actually rendered are reported instead, the open side being
        // the far away instant that leaves it unconstrained
        assertEquals(
                "2012-02-11T00:00:00Z/9999-12-31T23:59:59.999Z",
                getAsServletResponse(base + "&datetime=2012-02-11T00:00:00Z/..").getHeader("Content-Datetime"));
        // no datetime: the dimension default the renderer used, here the latest time of the layer
        assertEquals("2012-02-12T00:00:00Z", getAsServletResponse(base).getHeader("Content-Datetime"));
        // a layer with no time dimension has no temporal aspect to report
        assertNull(getAsServletResponse("ogc/maps/v1/collections/Lakes/map?f=image/png&width=50&height=50")
                .getHeader("Content-Datetime"));
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

    /**
     * The feature info reports the features of the very same map, so a subset restricts it too: the north-east quadrant
     * is empty at the first timestamp and holds one feature over the whole extent.
     */
    @Test
    public void testInfoHonorsSubset() throws Exception {
        setupStartEndTimeDimension(TIME_WITH_START_END, "time", "startTime", "endTime");
        assertEquals(0, northEastFeatures("subset=time(\"2012-02-11T00:00:00Z\")"));
        assertEquals(1, northEastFeatures("subset=time(*:*)"));

        // the spatial axes of a subset move the map too, so the pixel lands in the north west quadrant, which does
        // hold data at the first timestamp (the bbox is left out, it cannot be combined with a spatial subset)
        DocumentContext json = getAsJSONPath(
                "ogc/maps/v1/collections/sf:TimeWithStartEnd/map/info?f=application%2Fjson&width=40&height=40&i=30&j=10"
                        + "&subset=Lon(-180:0),Lat(0:90)&datetime=2012-02-11T00:00:00Z",
                200);
        assertEquals(Integer.valueOf(1), json.read("$.numberReturned", Integer.class));
    }

    /** Features the info resource reports at the north-east quadrant pixel of the time enabled layer. */
    private int northEastFeatures(String query) throws Exception {
        DocumentContext json = getAsJSONPath(quadrantInfoUrl(query.replace("\"", "%22")), 200);
        return json.read("$.numberReturned", Integer.class);
    }

    /**
     * ARGB pixel of the north-east quadrant of the time enabled layer, rendered under the given time query. The map is
     * requested transparent, so alpha 0 means the query selected no data for that quadrant.
     */
    private int northEastPixel(String timeQuery) throws Exception {
        return quadrantMap(timeQuery.replace("\"", "%22")).getRGB(NE[0], NE[1]);
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
        assertInvalidParameter(GROUP_INFO + "&limit=-1", "limit");
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
        // a time enabled collection with no datetime in the request: the template has no value to write out
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
