/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.vector;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.List;
import net.minidev.json.JSONArray;
import no.ecc.vectortile.VectorTileDecoder;
import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSTestSupport;
import org.geoserver.wms.mapbox.MapBoxTileBuilderFactory;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class VectorTilesIntegrationTest extends WMSTestSupport {

    private static final List<String> ALL_ROAD_NAMES =
            List.of("Dirt Road by Green Forest", "Main Street", "Route 5", "Route 5", "Route 5");

    /** A rule drawing the roads with the given name. */
    private static final String NAME_RULE =
            """
            <Rule><ogc:Filter><ogc:PropertyIsEqualTo>
              <ogc:PropertyName>NAME</ogc:PropertyName><ogc:Literal>%s</ogc:Literal>
            </ogc:PropertyIsEqualTo></ogc:Filter><LineSymbolizer/></Rule>""";

    /** A style for the roads layer, with its layer name, rules and feature type style vendor options. */
    private static final String ROADS_SLD =
            """
            <StyledLayerDescriptor version="1.0.0" xmlns="http://www.opengis.net/sld"
                xmlns:ogc="http://www.opengis.net/ogc">
              <NamedLayer><Name>%s</Name><UserStyle><FeatureTypeStyle>%s%s</FeatureTypeStyle></UserStyle></NamedLayer>
            </StyledLayerDescriptor>""";

    protected DocumentContext getAsJSONPath(String path, int expectedHttpCode) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        if (!isQuietTests()) {
            LOGGER.info(response.getContentAsString());
        }

        assertEquals(expectedHttpCode, response.getStatus());
        assertThat(response.getContentType(), startsWith("application/json"));
        return JsonPath.parse(response.getContentAsString());
    }

    @Test
    public void testSimple() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(5, ((JSONArray) json.read("$.features")).size());
        assertEquals(5, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(3, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]")).size());
        assertEquals(
                1, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]")).size());
    }

    @Test
    public void testSimpleMVT() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.MIME_TYPE);
    }

    @Test
    public void testSimpleMVTLegacyMime() throws Exception {
        checkSimpleMVT(MapBoxTileBuilderFactory.LEGACY_MIME_TYPE);
    }

    public void checkSimpleMVT(String mimeType) throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&format="
                + mimeType;
        MockHttpServletResponse response = getAsServletResponse(request);
        // the standard mime type is returned
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList =
                decoder.decode(responseBytes).asList();
        assertEquals(5, featuresList.size());
        assertEquals(
                3,
                featuresList.stream()
                        .filter(f -> "Route 5".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals(
                1,
                featuresList.stream()
                        .filter(f -> "Main Street".equals(f.getAttributes().get("NAME")))
                        .count());
        assertEquals("Extent should be 12288", 12288, featuresList.get(0).getExtent());
    }

    @Test
    public void testCqlFilter() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&CQL_FILTER=NAME='Main Street'&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]")).size());
        assertEquals(
                0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]")).size());
    }

    @Test
    public void testCqlFilterNoMatch() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&CQL_FILTER=1=0&format="
                + MapBoxTileBuilderFactory.MIME_TYPE;
        MockHttpServletResponse response = getAsServletResponse(request);
        assertEquals(200, response.getStatus());
        assertEquals(MapBoxTileBuilderFactory.MIME_TYPE, response.getContentType());
        byte[] responseBytes = response.getContentAsByteArray();
        VectorTileDecoder decoder = new VectorTileDecoder();
        List<VectorTileDecoder.Feature> featuresList =
                decoder.decode(responseBytes).asList();
        assertEquals(0, featuresList.size());
    }

    @Test
    public void testFilterById() throws Exception {
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers="
                + getLayerId(MockData.ROAD_SEGMENTS)
                + "&styles=&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326"
                + "&featureId=RoadSegments.1107532045091&format=application%2Fjson%3Btype%3Dgeojson";
        DocumentContext json = getAsJSONPath(request, 200);
        // all features returned, with a geometry and a name attribute
        assertEquals(1, ((JSONArray) json.read("$.features")).size());
        assertEquals(1, ((JSONArray) json.read("$.features[*].geometry")).size());
        assertEquals(0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Route 5')]")).size());
        assertEquals(0, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Main Street')]")).size());
        assertEquals(
                1, ((JSONArray) json.read("$.features[?(@.properties.NAME == 'Dirt Road by Green Forest')]")).size());
    }

    /** Between 6 and MAX_FILTER_RULES rule filters: filtered in the query. */
    @Test
    public void testRuleFilters() throws Exception {
        List<String> names = getRoadNames(nameRules(6));
        assertEquals(List.of("Main Street", "Route 5", "Route 5", "Route 5"), names);
    }

    /** More rule filters than MAX_FILTER_RULES: filtered in memory. */
    @Test
    public void testRuleFiltersInMemory() throws Exception {
        List<String> names = getRoadNames(nameRules(25));
        assertEquals(List.of("Main Street", "Route 5", "Route 5", "Route 5"), names);
    }

    /** A rule out of the map scale adds no feature. */
    @Test
    public void testRuleOutOfScale() throws Exception {
        String[] rules = nameRules(25);
        rules[0] = rules[0].replace(
                "<LineSymbolizer/>", "<MaxScaleDenominator>1000</MaxScaleDenominator><LineSymbolizer/>");
        assertEquals(List.of("Route 5", "Route 5", "Route 5"), getRoadNames(rules));
    }

    /** An else rule draws every feature, so the tile holds them all. */
    @Test
    public void testElseRule() throws Exception {
        String[] rules = nameRules(6, "<Rule><ElseFilter/><LineSymbolizer/></Rule>");
        assertEquals(ALL_ROAD_NAMES, getRoadNames(rules));
    }

    /** A rule without filter draws every feature, so the tile holds them all. */
    @Test
    public void testRuleWithoutFilter() throws Exception {
        String[] rules = nameRules(25, "<Rule><LineSymbolizer/></Rule>");
        assertEquals(ALL_ROAD_NAMES, getRoadNames(rules));
    }

    /**
     * Vector tiles do not run rendering transformations, and the rules of a transformed style apply to the
     * transformation output, so the tile holds every source feature.
     */
    @Test
    public void testTransformation() throws Exception {
        String transformation =
                """
                <Transformation><ogc:Function name="env"><ogc:Literal>x</ogc:Literal></ogc:Function>
                </Transformation>""";
        for (int count : new int[] {2, 6, 25}) {
            List<String> rules = new ArrayList<>(List.of(transformation));
            rules.addAll(List.of(nameRules(count)));
            assertEquals("Rule count " + count, ALL_ROAD_NAMES, getRoadNames(rules.toArray(String[]::new)));
        }
    }

    /** Filtering in memory resolves env() in rule filters with the values of the request. */
    @Test
    public void testRuleFiltersEnv() throws Exception {
        String[] rules = nameRules(25);
        rules[0] = rules[0].replace(
                "<ogc:Literal>Main Street</ogc:Literal>",
                "<ogc:Function name=\"env\"><ogc:Literal>road</ogc:Literal></ogc:Function>");
        JSONArray names = getTileFeatures("", "&env=road:Dirt%20Road%20by%20Green%20Forest", rules)
                .read("$.features[*].properties.NAME");
        assertEquals(List.of("Dirt Road by Green Forest", "Route 5", "Route 5", "Route 5"), sorted(names));
    }

    /** Filtering in memory reads the attributes the rules need, and leaves them out of the tile. */
    @Test
    public void testRuleFiltersAttributes() throws Exception {
        String options = "<VendorOption name=\"vt-attributes\">FID</VendorOption>";
        JSONArray properties = getTileFeatures(options, nameRules(25)).read("$.features[*].properties");
        assertEquals(List.of("{FID=102}", "{FID=103}", "{FID=104}", "{FID=105}"), sorted(properties));
    }

    /** The label layer gets the same rule filter in memory as through the query. */
    @Test
    public void testRuleFiltersLabels() throws Exception {
        String options =
                """
                <VendorOption name="vt-attributes">FID</VendorOption>
                <VendorOption name="vt-labels">true</VendorOption>
                <VendorOption name="vt-label-attributes">FID</VendorOption>""";
        String path = "$.features[*].properties";
        List<String> inMemory = sorted(getTileFeatures(options, nameRules(25)).read(path));
        List<String> inQuery = sorted(getTileFeatures(options, nameRules(6)).read(path));
        List<String> expected =
                List.of("{FID=102}", "{FID=102}", "{FID=103}", "{FID=103}", "{FID=104}", "{FID=105}", "{FID=105}");
        assertEquals(expected, inQuery);
        assertEquals(expected, inMemory);
    }

    private static List<String> sorted(JSONArray properties) {
        return properties.stream().map(Object::toString).sorted().toList();
    }

    /**
     * Returns rules matching "Main Street" and "Route 5", plus rules matching no road, {@code count} in all, then the
     * extra rules.
     */
    private static String[] nameRules(int count, String... extraRules) {
        List<String> names = new ArrayList<>(List.of("Main Street", "Route 5"));
        while (names.size() < count) names.add("No road " + names.size());
        List<String> rules = new ArrayList<>();
        for (String name : names) rules.add(NAME_RULE.formatted(name));
        rules.addAll(List.of(extraRules));
        return rules.toArray(String[]::new);
    }

    /** Returns the sorted names of the roads in a GeoJSON tile drawn with a style made of the given rules. */
    private List<String> getRoadNames(String... rules) throws Exception {
        List<String> names = getTileFeatures("", rules).read("$.features[*].properties.NAME");
        return names.stream().sorted().toList();
    }

    /** Returns the GeoJSON tile of the roads drawn with a style made of the given vendor options and rules. */
    private DocumentContext getTileFeatures(String vendorOptions, String... rules) throws Exception {
        return getTileFeatures(vendorOptions, "", rules);
    }

    /** Returns the GeoJSON tile as above, with more request parameters. */
    private DocumentContext getTileFeatures(String vendorOptions, String parameters, String... rules) throws Exception {
        String layer = getLayerId(MockData.ROAD_SEGMENTS);
        String sld = ROADS_SLD.formatted(layer, String.join("", rules), vendorOptions);
        String request = "wms?service=WMS&version=1.1.0&request=GetMap&layers=" + layer
                + "&bbox=-1,-1,1,1&width=768&height=330&srs=EPSG:4326" + parameters
                + "&format=application%2Fjson%3Btype%3Dgeojson&SLD_BODY=" + URLEncoder.encode(sld, UTF_8);
        return getAsJSONPath(request, 200);
    }
}
