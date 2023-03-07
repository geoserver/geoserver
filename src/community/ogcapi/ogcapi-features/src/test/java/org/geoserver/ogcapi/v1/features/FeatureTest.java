/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import net.minidev.json.JSONArray;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.APIException;
import org.geoserver.ows.util.KvpUtils;
import org.geoserver.ows.util.ResponseUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.springframework.mock.web.MockHttpServletResponse;

public class FeatureTest extends FeaturesTestSupport {

    @Test
    public void testContentDisposition() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response =
                getAsServletResponse("ogc/features/v1/collections/" + roadSegments + "/items");
        assertEquals(200, response.getStatus());
        assertEquals(
                "inline; filename=RoadSegments.json", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testGetLayerAsGeoJson() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/" + roadSegments + "/items", 200);
        assertEquals(
                "<http://www.opengis.net/def/crs/OGC/1.3/CRS84>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        // check self link
        List selfRels = json.read("links[?(@.type == 'application/geo+json')].rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(2, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
        assertEquals("collection", alternatefRels.get(1));
        // check collection link
        List selfLink = json.read("links[?(@.rel == 'collection')].href");
        assertThat(selfLink.size(), greaterThan(0));
        assertThat(
                (String) selfLink.get(0),
                startsWith(
                        "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                                + roadSegments
                                + "?"));
    }

    @Test
    @SuppressWarnings("unchecked") // matchers make for generic varargs
    public void testGetLayerAsGeoJsonReproject() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?crs="
                                + FeatureService.CRS_PREFIX
                                + "3857",
                        200);
        assertEquals(
                "<http://www.opengis.net/def/crs/EPSG/0/3857>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        // get ordinates of RoadSegments.1107532045091, returns array[array[array[double]]]
        List<List<List<Double>>> result =
                readSingle(
                        json,
                        "features[?(@.id=='RoadSegments.1107532045091')].geometry.coordinates");
        // original feature:
        // RoadSegments.1107532045091=MULTILINESTRING ((-0.0014 -0.0024, -0.0014 0.0002))|
        //                            106|Dirt Road by Green Forest
        List<Double> ordinate0 = result.get(0).get(0);
        List<Double> ordinate1 = result.get(0).get(1);
        assertThat(ordinate0, contains(closeTo(-156, 1), closeTo(-267, 1)));
        assertThat(ordinate1, contains(closeTo(-156, 1), closeTo(22, 1)));
    }

    @Test
    public void testWorkspaceQualified() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        DocumentContext json =
                getAsJSONPath(
                        MockData.ROAD_SEGMENTS.getPrefix()
                                + "/ogc/features/v1/collections/"
                                + roadSegments
                                + "/items",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        // check self link
        List selfRels = json.read("links[?(@.type == 'application/geo+json')].rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        // check json links
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(2, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
        assertEquals("collection", alternatefRels.get(1));
        // check collection link
        List selfLink = json.read("links[?(@.rel == 'collection')].href");
        assertThat(selfLink.size(), greaterThan(0));
        assertThat(
                (String) selfLink.get(0),
                startsWith(
                        "http://localhost:8080/geoserver/cite/ogc/features/v1/collections/"
                                + roadSegments
                                + "?"));
    }

    @Test
    public void testBBoxFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/" + roadSegments + "/items?bbox=35,0,60,3",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testBBoxCRSFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(35, 60, 0, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?"
                                + bboxCrsQueryParameters(wmBox),
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    private String bboxCrsQueryParameters(ReferencedEnvelope re) throws FactoryException {
        String boxValue =
                re.getMinX() + "," + re.getMinY() + "," + re.getMaxX() + "," + re.getMaxY();
        String crsValue =
                CRS.equalsIgnoreMetadata(
                                re.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)
                        ? FeatureService.DEFAULT_CRS
                        : FeatureService.CRS_PREFIX
                                + CRS.lookupEpsgCode(re.getCoordinateReferenceSystem(), true);
        return "bbox=" + boxValue + "&bbox-crs=" + crsValue;
    }

    @Test
    public void testBBOXOGCAuthority() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?"
                                + "bbox=35,0,60,3"
                                + "&bbox-crs=http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return those two features only
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testInvalidBBOXCRS() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?"
                                + "bbox=35,0,60,3"
                                + "&bbox-crs=http://www.opengis.net/def/crs/OGC/1.3/INVALID",
                        400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("code", String.class));
        assertEquals(
                "Invalid CRS: http://www.opengis.net/def/crs/OGC/1.3/INVALID",
                json.read("description"));
    }

    @Test
    public void testBBoxDatelineCrossingFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/" + roadSegments + "/items?bbox=170,0,60,3",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testCqlFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=name='name-f001'",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testCql2JsonFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=%7B%22op%22%3A%22%3D%22%2C%22args%22%3A%5B%7B%22property%22%3A%22name%22%7D%2C%22name-f001%22%5D%7D" // {"op":"=","args":[{"property":"name"},"name-f001"]}
                                + "&filter-lang=cql2-json",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testCqlSpatialFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=BBOX(pointProperty,38,1,40,3)&filter-lang=cql-text",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testCql2JsonSpatialFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=%7B%22op%22%3A%22s_intersects%22%2C%22args%22%3A%5B%7B%22property%22%3A%22pointProperty%22%7D%2C%7B%22bbox%22%3A%5B38%2C1%2C40%2C3%5D%7D%5D%7D" // {"op":"s_intersects","args":[{"property":"pointProperty"},{"bbox":[38,1,40,3]}]}
                                + "&filter-lang=cql2-json",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testCqlSpatialFilterWithFilterCrs() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(38, 40, 1, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);

        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?"
                                + filterCrsQueryParameters(wmBox),
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    private String filterCrsQueryParameters(ReferencedEnvelope re) throws FactoryException {
        String boxValue =
                "BBOX(pointProperty,"
                        + re.getMinX()
                        + ","
                        + re.getMinY()
                        + ","
                        + re.getMaxX()
                        + ","
                        + re.getMaxY()
                        + ")";
        String crsValue =
                CRS.equalsIgnoreMetadata(
                                re.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)
                        ? FeatureService.DEFAULT_CRS
                        : FeatureService.CRS_PREFIX
                                + CRS.lookupEpsgCode(re.getCoordinateReferenceSystem(), true);
        return "filter=" + boxValue + "&filter-crs=" + crsValue + "&filter-lang=cql-text";
    }

    @Test
    public void testCqlFilterInvalidCrs() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);

        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=BBOX(pointProperty,38,1,40,3)&"
                                + "filter-crs="
                                + FeatureService.CRS_PREFIX
                                + "0",
                        400);
        assertEquals("InvalidParameterValue", json.read("code", String.class));
        assertThat(json.read("description", String.class), Matchers.containsString("EPSG:0"));
    }

    @Test
    public void testCqlFilterInvalidLanguage() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?filter=name='name-f001'&filter-lang=foo-bar",
                        400);
        assertEquals("InvalidParameterValue", json.read("code", String.class));
        assertThat(json.read("description", String.class), Matchers.containsString("foo-bar"));
    }

    @Test
    public void testTimeFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?datetime=2006-10-25",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class).size());
    }

    @Test
    public void testTimeRangeFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?datetime=2006-09-01/2006-10-23",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class).size());
    }

    @Test
    public void testTimeDurationFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?datetime=2006-09-01/P1M23DT12H31M12S",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class).size());
    }

    @Test
    public void testCombinedSpaceTimeFilter() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?datetime=2006-09-01/2006-10-23&bbox=35,0,60,3",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1, json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class).size());
    }

    @Test
    public void testSortByWithDefaultSortOrder() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?sortby=name&limit=2",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(null, json.read("features[0].properties.name", String.class));
        assertEquals("name-f001", json.read("features[1].properties.name", String.class));
    }

    @Test
    public void testSortByWithAscendingSortOrder() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?sortby=%2Bname&limit=2",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(null, json.read("features[0].properties.name", String.class));
        assertEquals("name-f001", json.read("features[1].properties.name", String.class));
    }

    @Test
    public void testSortByWithDescendingSortOrder() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?sortby=-name&limit=2",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals("name-f008", json.read("features[0].properties.name", String.class));
        assertEquals("name-f003", json.read("features[1].properties.name", String.class));
    }

    @Test
    public void testSortByMultipleProperties() throws Exception {
        String roadSegments = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?sortby=booleanProperty,-intProperty",
                        200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        assertEquals(null, json.read("features[0].properties.name", String.class));
        assertEquals("name-f002", json.read("features[1].properties.name", String.class));
        assertEquals("name-f008", json.read("features[2].properties.name", String.class));
        assertEquals("name-f003", json.read("features[3].properties.name", String.class));
        assertEquals("name-f001", json.read("features[4].properties.name", String.class));
    }

    @Test
    public void testSingleFeatureAsGeoJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items/RoadSegments.1107532045088",
                        200);
        assertEquals("Feature", json.read("type", String.class));
        // check self link
        String geoJsonLinkPath = "links[?(@.type == 'application/geo+json')]";
        List selfRels = json.read(geoJsonLinkPath + ".rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        String href = (String) ((List) json.read(geoJsonLinkPath + "href")).get(0);
        String expected =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments"
                        + "/items/RoadSegments.1107532045088?f=application%2Fgeo%2Bjson";
        assertEquals(expected, href);
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertEquals(2, alternatefRels.size());
        assertEquals("alternate", alternatefRels.get(0));
        assertEquals("collection", alternatefRels.get(1));
    }

    @Test
    public void testFirstPage() throws Exception {
        String expectedNextURL =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?limit=3&startIndex=3";

        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/" + roadSegments + "/items?limit=3", 200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(1));
        assertEquals(
                links.get(0),
                "<" + expectedNextURL + ">; rel=\"next\"; type=\"application/geo+json\"");

        DocumentContext json = getAsJSONPath(response);
        assertEquals(3, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(json.read("$.links[?(@.rel=='prev')].href"), Matchers.empty());
        assertThat(
                json.read("$.links[?(@.rel=='next')].href", JSONArray.class).get(0),
                equalTo(expectedNextURL));
    }

    @Test
    public void testMiddlePage() throws Exception {
        String expectedPrevURL =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=2&limit=1";
        String expectedNextURL =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=4&limit=1";

        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?startIndex=3&limit=1",
                        200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(2));
        assertEquals(
                "<" + expectedPrevURL + ">; rel=\"prev\"; type=\"application/geo+json\"",
                links.get(0));
        assertEquals(
                "<" + expectedNextURL + ">; rel=\"next\"; type=\"application/geo+json\"",
                links.get(1));

        DocumentContext json = getAsJSONPath(response);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(
                json.read("$.links[?(@.rel=='prev')].href", JSONArray.class).get(0),
                equalTo(expectedPrevURL));
        assertThat(
                json.read("$.links[?(@.rel=='next')].href", JSONArray.class).get(0),
                equalTo(expectedNextURL));
    }

    @Test
    public void testLastPage() throws Exception {
        String expectedPrevLink =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=0&limit=3";

        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/"
                                + roadSegments
                                + "/items?startIndex=3&limit=3",
                        200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(1));
        assertEquals(
                links.get(0),
                "<" + expectedPrevLink + ">; rel=\"prev\"; type=\"application/geo+json\"");

        DocumentContext json = getAsJSONPath(response);
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(
                json.read("$.links[?(@.rel=='prev')].href", JSONArray.class).get(0),
                equalTo(expectedPrevLink));
        assertThat(json.read("$.links[?(@.rel=='next')].href"), Matchers.empty());
    }

    @Test
    public void testErrorHandling() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath(
                        "ogc/features/v1/collections/" + roadSegments + "/items?limit=abc", 400);
        assertEquals("InvalidParameterValue", json.read("code"));
        assertThat(
                json.read("description"), both(containsString("limit")).and(containsString("abc")));
    }

    @Test
    public void testGetLayerAsHTML() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        String url = "ogc/features/v1/collections/" + roadSegments + "/items?f=html";
        Document document = getAsJSoup(url);
        assertEquals(5, document.select("td:matches(RoadSegments\\..*)").size());
        // all elements expected are there
        // check the id of a known tag
        assertEquals(
                "106", document.select("td:matches(RoadSegments\\.1107532045091) + td").text());
    }

    @Test
    public void testGetLayerAsHTMLHeader() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        String url = "ogc/features/v1/collections/" + roadSegments + "/items?f=html";
        MockHttpServletResponse response = getAsMockHttpServletResponse(url, 200);
        assertEquals(
                "<http://www.opengis.net/def/crs/OGC/1.3/CRS84>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
    }

    @Test
    public void testGetLayerAsHTMLPagingLinks() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        String urlBase = "ogc/features/v1/collections/" + roadSegments + "/items?f=html";
        String expectedBase =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                        + roadSegments
                        + "/items";

        // first page, should only have next URL
        String firstPageURL = urlBase + "&limit=2";
        Document document = getAsJSoup(firstPageURL);
        assertNull(document.getElementById("prevPage"));
        assertNotNull(document.getElementById("nextPage"));
        String expectedSecondPageURL = expectedBase + "?f=html&limit=2&startIndex=2";
        assertURL(expectedSecondPageURL, document.getElementById("nextPage").attr("href"));

        // second page, should have both prev and next
        document = getAsJSoup(urlBase + "&limit=2&startIndex=2");
        assertNotNull(document.getElementById("prevPage"));
        assertNotNull(document.getElementById("nextPage"));
        String expectedThirdPageURL = expectedBase + "?f=html&limit=2&startIndex=4";
        assertURL(expectedThirdPageURL, document.getElementById("nextPage").attr("href"));

        // last page, only prev
        document = getAsJSoup(urlBase + "&limit=2&startIndex=4");
        assertNotNull(document.getElementById("prevPage"));
        assertNull(document.getElementById("nextPage"));
        assertURL(expectedSecondPageURL, document.getElementById("prevPage").attr("href"));
    }

    private void assertURL(String expectedURL, String actualURL) {
        String expectedPath = ResponseUtils.stripQueryString(expectedURL);
        String actualPath = ResponseUtils.stripQueryString(actualURL);
        assertEquals(expectedPath, actualPath);

        Map<String, Object> expectedKVP = KvpUtils.parseQueryString(expectedPath);
        Map<String, Object> actualKVP = KvpUtils.parseQueryString(expectedPath);
        assertEquals(expectedKVP, actualKVP);
    }

    @Test
    public void testSpecialCharsInTypeName() throws Exception {
        FeatureTypeInfo genericEntity =
                getCatalog().getFeatureTypeByName(getLayerId(MockData.GENERICENTITY));
        genericEntity.setName("EntitéGénérique");
        getCatalog().save(genericEntity);
        try {
            String encodedLocalName =
                    URLEncoder.encode(genericEntity.getName(), StandardCharsets.UTF_8.name());
            String typeName =
                    URLEncoder.encode(genericEntity.prefixedName(), StandardCharsets.UTF_8.name());
            String encodedFeatureId = encodedLocalName + ".f004";
            DocumentContext json =
                    getAsJSONPath(
                            "ogc/features/v1/collections/"
                                    + typeName
                                    + "/items/"
                                    + encodedFeatureId,
                            200);

            assertEquals("Feature", json.read("type", String.class));
            // check self link
            String geoJsonLinkPath = "links[?(@.type == 'application/geo+json')]";
            List selfRels = json.read(geoJsonLinkPath + ".rel");
            assertEquals(1, selfRels.size());
            assertEquals("self", selfRels.get(0));
            String href = (String) ((List) json.read(geoJsonLinkPath + "href")).get(0);
            String expected =
                    "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                            + typeName
                            + "/items/"
                            + encodedFeatureId
                            + "?f=application%2Fgeo%2Bjson";
            assertEquals(expected, href);
            // check alternate link
            List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
            assertEquals(2, alternatefRels.size());
            assertEquals("alternate", alternatefRels.get(0));
            assertEquals("collection", alternatefRels.get(1));
        } finally {
            genericEntity.setName(MockData.GENERICENTITY.getLocalPart());
            getCatalog().save(genericEntity);
        }
    }

    @Test
    public void testGetItemAsGeoJson() throws Exception {
        String primitiveLayer = ResponseUtils.urlEncode(getLayerId(MockData.PRIMITIVEGEOFEATURE));
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/"
                                + primitiveLayer
                                + "/items/PrimitiveGeoFeature.f002",
                        200);
        assertEquals(
                "<http://www.opengis.net/def/crs/OGC/1.3/CRS84>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("Feature", json.read("type", String.class));
        assertEquals("PrimitiveGeoFeature.f002", json.read("id", String.class));

        // check self link
        List selfRels = json.read("links[?(@.type == 'application/geo+json')].rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertTrue(alternatefRels.size() > 1);
        assertEquals("alternate", alternatefRels.get(0));
        assertEquals("collection", alternatefRels.get(1));
        // check collection link
        List selfLink = json.read("links[?(@.rel == 'collection')].href");
        assertThat(selfLink.size(), greaterThan(0));
        assertThat(
                (String) selfLink.get(0),
                startsWith(
                        "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                                + primitiveLayer
                                + "?"));
    }

    @Test
    public void testGetItemAsGeoJsonWithCRS() throws Exception {
        String primitiveLayer = ResponseUtils.urlEncode(getLayerId(MockData.PRIMITIVEGEOFEATURE));
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(
                        "ogc/features/v1/collections/"
                                + primitiveLayer
                                + "/items/PrimitiveGeoFeature.f002"
                                + "?crs=CRS:84",
                        200);
        assertEquals(
                "<http://www.opengis.net/def/crs/OGC/1.3/CRS84>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("Feature", json.read("type", String.class));
        assertEquals("PrimitiveGeoFeature.f002", json.read("id", String.class));

        // check self link
        List selfRels = json.read("links[?(@.type == 'application/geo+json')].rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        // check alternate link
        List alternatefRels = json.read("links[?(@.type == 'application/json')].rel");
        assertTrue(alternatefRels.size() > 1);
        assertEquals("alternate", alternatefRels.get(0));
        assertEquals("collection", alternatefRels.get(1));
        // check collection link
        List selfLink = json.read("links[?(@.rel == 'collection')].href");
        assertThat(selfLink.size(), greaterThan(0));
        assertThat(
                (String) selfLink.get(0),
                startsWith(
                        "http://localhost:8080/geoserver/ogc/features/v1/collections/"
                                + primitiveLayer
                                + "?"));
    }
}
