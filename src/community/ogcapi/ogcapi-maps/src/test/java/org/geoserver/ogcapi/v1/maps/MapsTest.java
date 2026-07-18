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
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
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
