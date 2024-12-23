/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE;
import static org.geoserver.ogcapi.v1.features.JSONFGFeaturesResponse.COORD_REF_SYS;
import static org.hamcrest.CoreMatchers.both;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.closeTo;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
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
import org.geoserver.wfs.WFSInfo;
import org.geotools.api.referencing.FactoryException;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class FeatureTest extends FeaturesTestSupport {

    private static final String WEB_MERCATOR_URI = "http://www.opengis.net/def/crs/EPSG/0/3857";

    @Test
    public void testContentDisposition() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response =
                getAsServletResponse("ogc/features/v1/collections/" + roadSegments + "/items");
        assertEquals(200, response.getStatus());
        assertEquals("inline; filename=RoadSegments.json", response.getHeader("Content-Disposition"));
    }

    @Test
    public void testGetLayerAsGeoJson() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response =
                getAsMockHttpServletResponse("ogc/features/v1/collections/" + roadSegments + "/items", 200);
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
                startsWith("http://localhost:8080/geoserver/ogc/features/v1/collections/" + roadSegments + "?"));
    }

    @Test
    @SuppressWarnings("unchecked") // matchers make for generic varargs
    public void testGetLayerAsGeoJsonReproject() throws Exception {
        String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
        MockHttpServletResponse response = getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + roadSegments + "/items?crs=" + FeatureService.CRS_PREFIX + "3857",
                200);
        assertEquals(
                "<http://www.opengis.net/def/crs/EPSG/0/3857>",
                response.getHeader(HttpHeaderContentCrsAppender.CRS_RESPONSE_HEADER));
        DocumentContext json = getAsJSONPath(response);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(5, (int) json.read("features.length()", Integer.class));
        // get ordinates of RoadSegments.1107532045091, returns array[array[array[double]]]
        List<List<List<Double>>> result =
                readSingle(json, "features[?(@.id=='RoadSegments.1107532045091')].geometry.coordinates");
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
        DocumentContext json = getAsJSONPath(
                MockData.ROAD_SEGMENTS.getPrefix() + "/ogc/features/v1/collections/" + roadSegments + "/items", 200);
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
                startsWith("http://localhost:8080/geoserver/cite/ogc/features/v1/collections/" + roadSegments + "?"));
    }

    @Test
    public void testBBoxFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?bbox=35,0,60,3", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
    }

    @Test
    public void testBBoxCRSFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(35, 60, 0, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?" + bboxCrsQueryParameters(wmBox), 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
    }

    private String bboxCrsQueryParameters(ReferencedEnvelope re) throws FactoryException {
        String boxValue = bboxQueryParameter(re);
        String crsValue = crsQueryParameter(re);
        return "bbox=" + boxValue + "&bbox-crs=" + crsValue;
    }

    private String bboxQueryParameter(ReferencedEnvelope re) {
        return re.getMinX() + "," + re.getMinY() + "," + re.getMaxX() + "," + re.getMaxY();
    }

    private String crsQueryParameter(ReferencedEnvelope re) throws FactoryException {
        return CRS.equalsIgnoreMetadata(re.getCoordinateReferenceSystem(), DefaultGeographicCRS.WGS84)
                ? FeatureService.DEFAULT_CRS
                : FeatureService.CRS_PREFIX + CRS.lookupEpsgCode(re.getCoordinateReferenceSystem(), true);
    }

    @Test
    public void testBBOXOGCAuthority() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?"
                        + "bbox=35,0,60,3"
                        + "&bbox-crs=http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return those two features only
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
    }

    @Test
    public void testInvalidBBOXCRS() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?"
                        + "bbox=35,0,60,3"
                        + "&bbox-crs=http://www.opengis.net/def/crs/OGC/1.3/INVALID",
                400);
        assertEquals(APIException.INVALID_PARAMETER_VALUE, json.read("code", String.class));
        assertEquals("Invalid CRS: http://www.opengis.net/def/crs/OGC/1.3/INVALID", json.read("description"));
    }

    @Test
    public void testBBoxDatelineCrossingFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?bbox=170,0,60,3", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f002 and f003
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
    }

    @Test
    public void testCqlFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?filter=name='name-f001'", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    @Test
    public void testCql2JsonFilter() throws Exception {
        // enable cql2-json
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        CQL2Conformance cql2Conformance = CQL2Conformance.configuration(wfsInfo);
        cql2Conformance.setJSON(true);
        getGeoServer().save(wfsInfo);

        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?filter=%7B%22op%22%3A%22%3D%22%2C%22args%22%3A%5B%7B%22property%22%3A%22name%22%7D%2C%22name-f001%22%5D%7D" // {"op":"=","args":[{"property":"name"},"name-f001"]}
                        + "&filter-lang=cql2-json",
                200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    @Test
    public void testCqlSpatialFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?filter=BBOX(pointProperty,38,1,40,3)&filter-lang=cql-text",
                200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    @Test
    public void testCql2JsonSpatialFilter() throws Exception {
        // enable cql2-json
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        CQL2Conformance cql2Conformance = CQL2Conformance.configuration(wfsInfo);
        cql2Conformance.setJSON(true);
        getGeoServer().save(wfsInfo);

        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?filter=%7B%22op%22%3A%22s_intersects%22%2C%22args%22%3A%5B%7B%22property%22%3A%22pointProperty%22%7D%2C%7B%22bbox%22%3A%5B38%2C1%2C40%2C3%5D%7D%5D%7D" //
                        + "&filter-lang=cql2-json",
                200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    @Test
    public void testCqlSpatialFilterWithFilterCrs() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(38, 40, 1, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);

        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?" + filterCrsQueryParameters(wmBox), 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    private String filterCrsQueryParameters(ReferencedEnvelope re) throws FactoryException {
        String boxValue = "BBOX(pointProperty," + bboxQueryParameter(re) + ")";
        String crsValue = crsQueryParameter(re);
        return "filter=" + boxValue + "&filter-crs=" + crsValue + "&filter-lang=cql-text";
    }

    @Test
    public void testCqlFilterInvalidFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);

        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?filter=THIS IS NOT A FILTER", 400);
        assertEquals("InvalidParameterValue", json.read("code", String.class));
        assertThat(json.read("description", String.class), Matchers.containsString("THIS IS NOT A FILTER"));
    }

    @Test
    public void testCqlFilterInvalidCrs() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);

        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
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
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?filter=name='name-f001'&filter-lang=foo-bar",
                400);
        assertEquals("InvalidParameterValue", json.read("code", String.class));
        assertThat(json.read("description", String.class), Matchers.containsString("foo-bar"));
    }

    @Test
    public void testTimeFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?datetime=2006-10-25", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        // should return only f001
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                        .size());
    }

    @Test
    public void testTimeRangeFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?datetime=2006-09-01/2006-10-23", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class)
                        .size());
    }

    @Test
    public void testTimeDurationFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/" + collectionName + "/items?datetime=2006-09-01/P1M23DT12H31M12S", 200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f003')]", List.class)
                        .size());
    }

    @Test
    public void testCombinedSpaceTimeFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + collectionName
                        + "/items?datetime=2006-09-01/2006-10-23&bbox=35,0,60,3",
                200);
        assertEquals("FeatureCollection", json.read("type", String.class));
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        assertEquals(
                1,
                json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                        .size());
    }

    @Test
    public void testSortByWithDefaultSortOrder() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            DocumentContext json =
                    getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?sortby=name&limit=2", 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(null, json.read("features[0].properties.name", String.class));
            assertEquals("name-f001", json.read("features[1].properties.name", String.class));
        } finally {
            featureServiceInfo.setSortBy(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSortByWithAscendingSortOrder() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            DocumentContext json = getAsJSONPath(
                    "ogc/features/v1/collections/" + collectionName + "/items?sortby=%2Bname&limit=2", 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(null, json.read("features[0].properties.name", String.class));
            assertEquals("name-f001", json.read("features[1].properties.name", String.class));
        } finally {
            featureServiceInfo.setSortBy(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSortByWithDescendingSortOrder() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            DocumentContext json =
                    getAsJSONPath("ogc/features/v1/collections/" + collectionName + "/items?sortby=-name&limit=2", 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals("name-f008", json.read("features[0].properties.name", String.class));
            assertEquals("name-f003", json.read("features[1].properties.name", String.class));
        } finally {
            featureServiceInfo.setSortBy(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSortByMultipleProperties() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            DocumentContext json = getAsJSONPath(
                    "ogc/features/v1/collections/" + collectionName + "/items?sortby=booleanProperty,-intProperty",
                    200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(5, (int) json.read("features.length()", Integer.class));
            assertEquals(null, json.read("features[0].properties.name", String.class));
            assertEquals("name-f002", json.read("features[1].properties.name", String.class));
            assertEquals("name-f008", json.read("features[2].properties.name", String.class));
            assertEquals("name-f003", json.read("features[3].properties.name", String.class));
            assertEquals("name-f001", json.read("features[4].properties.name", String.class));
        } finally {
            featureServiceInfo.setSortBy(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testIdsFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setIDs(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
            DocumentContext json = getAsJSONPath(
                    "ogc/features/v1/collections/"
                            + roadSegments
                            + "/items?ids=RoadSegments.1107532045088,RoadSegments.1107532045091",
                    200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals("RoadSegments.1107532045088", json.read("features[0].id", String.class));
            assertEquals("RoadSegments.1107532045091", json.read("features[1].id", String.class));
        } finally {
            featureServiceInfo.setIDs(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchCql2JsonFilter() throws Exception {
        // enable search
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        // enable cql2-json
        CQL2Conformance cql2Conformance = CQL2Conformance.configuration(wfsInfo);
        cql2Conformance.setJSON(true);

        getGeoServer().save(wfsInfo);

        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\n"
                    + "  \"filter\": {\"op\":\"=\",\"args\":[{\"property\":\"name\"},\"name-f001\"]},"
                    + "  \"filter-lang\": \"cql2-json\"\n"
                    + "}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f001
            assertEquals(1, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchCql2TextFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        String request = "{\n"
                + "  \"filter\": \"BBOX(pointProperty,38,1,40,3)\",\n"
                + "  \"filter-lang\": \"cql-text\"\n"
                + "}";

        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f001
            assertEquals(1, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchCql2TextFilterWithFilterCrs() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(38, 40, 1, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);
        String crsValue = crsQueryParameter(wmBox);

        String request = "{\n"
                + "  \"filter\": \"BBOX(pointProperty,"
                + bboxQueryParameter(wmBox)
                + ")\",\n"
                + " \"filter-lang\": \"cql-text\"\n,"
                + " \"filter-crs\": \""
                + crsValue
                + "\"\n}";

        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f001
            assertEquals(1, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchBBoxJsonFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\"bbox\":[35, 0, 60, 3]}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f002 and f003
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchBBoxTextFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\"bbox\":\"35,0,60,3\"}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f002 and f003
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchBBoxCRSFilter() throws Exception {
        String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
        ReferencedEnvelope bbox = new ReferencedEnvelope(35, 60, 0, 3, DefaultGeographicCRS.WGS84);
        ReferencedEnvelope wmBox = bbox.transform(CRS.decode("EPSG:3857", true), true);
        String boxValue = bboxQueryParameter(wmBox);
        String bboxCrsValue = crsQueryParameter(wmBox);
        String request = "{\"bbox\":\"" + boxValue + "\",\"bbox-crs\":\"" + bboxCrsValue + "\"}";

        // check this is disabled by default
        DocumentContext notFound =
                postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 404);
        assertEquals("NotFound", notFound.read("code", String.class));

        // enable
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true);
        getGeoServer().save(wfsInfo);
        try {
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f002 and f003
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f002')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    @SuppressWarnings("unchecked")
    public void testSearchCRSFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String roadSegments = ResponseUtils.urlEncode(getLayerId(MockData.ROAD_SEGMENTS));
            String crs = FeatureService.CRS_PREFIX + "3857";
            String request = "{\"crs\":\"" + crs + "\"}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/search", request, 200);

            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(5, (int) json.read("features.length()", Integer.class));
            // get ordinates of RoadSegments.1107532045091, returns array[array[array[double]]]
            List<List<List<Double>>> result =
                    readSingle(json, "features[?(@.id=='RoadSegments.1107532045091')].geometry.coordinates");
            // original feature:
            // RoadSegments.1107532045091=MULTILINESTRING ((-0.0014 -0.0024, -0.0014 0.0002))|
            //                            106|Dirt Road by Green Forest
            List<Double> ordinate0 = result.get(0).get(0);
            List<Double> ordinate1 = result.get(0).get(1);
            assertThat(ordinate0, contains(closeTo(-156, 1), closeTo(-267, 1)));
            assertThat(ordinate1, contains(closeTo(-156, 1), closeTo(22, 1)));
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchIdsJsonFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        featureServiceInfo.setIDs(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
            String request = "{\"ids\":[\"RoadSegments.1107532045088\",\"RoadSegments.1107532045091\"]}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals("RoadSegments.1107532045088", json.read("features[0].id", String.class));
            assertEquals("RoadSegments.1107532045091", json.read("features[1].id", String.class));
        } finally {
            featureServiceInfo.setSearch(null); // default
            featureServiceInfo.setIDs(null); // enable
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchIdsTextFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        featureServiceInfo.setIDs(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
            String request = "{\"ids\":\"RoadSegments.1107532045088,RoadSegments.1107532045091\"}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals("RoadSegments.1107532045088", json.read("features[0].id", String.class));
            assertEquals("RoadSegments.1107532045091", json.read("features[1].id", String.class));
        } finally {
            featureServiceInfo.setSearch(null); // default
            featureServiceInfo.setIDs(null); // enable
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchDatetimeFilter() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\"datetime\":\"2006-10-25\"}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            // should return only f001
            assertEquals(1, (int) json.read("features.length()", Integer.class));
            assertEquals(
                    1,
                    json.read("features[?(@.id == 'PrimitiveGeoFeature.f001')]", List.class)
                            .size());
        } finally {
            featureServiceInfo.setSearch(null); // default
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchSortByJson() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\"sortby\":[\"name\"],\"limit\":2}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(null, json.read("features[0].properties.name", String.class));
            assertEquals("name-f001", json.read("features[1].properties.name", String.class));
        } finally {
            featureServiceInfo.setSearch(null); // default
            featureServiceInfo.setSortBy(null); // enable
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSearchSortByText() throws Exception {
        WFSInfo wfsInfo = getGeoServer().getService(WFSInfo.class);
        FeatureConformance featureServiceInfo = FeatureConformance.configuration(wfsInfo);
        featureServiceInfo.setSearch(true); // enable
        featureServiceInfo.setSortBy(true); // enable
        getGeoServer().save(wfsInfo);
        try {
            String collectionName = getLayerId(MockData.PRIMITIVEGEOFEATURE);
            String request = "{\"sortby\":\"name\",\"limit\":2}";
            DocumentContext json =
                    postAsJSONPath("ogc/features/v1/collections/" + collectionName + "/search", request, 200);
            assertEquals("FeatureCollection", json.read("type", String.class));
            assertEquals(2, (int) json.read("features.length()", Integer.class));
            assertEquals(null, json.read("features[0].properties.name", String.class));
            assertEquals("name-f001", json.read("features[1].properties.name", String.class));
        } finally {
            featureServiceInfo.setSearch(null); // default
            featureServiceInfo.setSortBy(null); // enable
            getGeoServer().save(wfsInfo);
        }
    }

    @Test
    public void testSingleFeatureAsGeoJson() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json =
                getAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/items/RoadSegments.1107532045088", 200);
        assertEquals("Feature", json.read("type", String.class));
        // check self link
        String geoJsonLinkPath = "links[?(@.type == 'application/geo+json')]";
        List selfRels = json.read(geoJsonLinkPath + ".rel");
        assertEquals(1, selfRels.size());
        assertEquals("self", selfRels.get(0));
        String href = (String) ((List) json.read(geoJsonLinkPath + "href")).get(0);
        String expected = "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments"
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
                getAsMockHttpServletResponse("ogc/features/v1/collections/" + roadSegments + "/items?limit=3", 200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(1));
        assertEquals(links.get(0), "<" + expectedNextURL + ">; rel=\"next\"; type=\"application/geo+json\"");

        DocumentContext json = getAsJSONPath(response);
        assertEquals(3, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(json.read("$.links[?(@.rel=='prev')].href"), Matchers.empty());
        assertThat(json.read("$.links[?(@.rel=='next')].href", JSONArray.class).get(0), equalTo(expectedNextURL));
    }

    @Test
    public void testMiddlePage() throws Exception {
        String expectedPrevURL =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=2&limit=1";
        String expectedNextURL =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=4&limit=1";

        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse response = getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + roadSegments + "/items?startIndex=3&limit=1", 200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(2));
        assertEquals("<" + expectedPrevURL + ">; rel=\"prev\"; type=\"application/geo+json\"", links.get(0));
        assertEquals("<" + expectedNextURL + ">; rel=\"next\"; type=\"application/geo+json\"", links.get(1));

        DocumentContext json = getAsJSONPath(response);
        assertEquals(1, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(json.read("$.links[?(@.rel=='prev')].href", JSONArray.class).get(0), equalTo(expectedPrevURL));
        assertThat(json.read("$.links[?(@.rel=='next')].href", JSONArray.class).get(0), equalTo(expectedNextURL));
    }

    @Test
    public void testLastPage() throws Exception {
        String expectedPrevLink =
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/items?startIndex=0&limit=3";

        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        MockHttpServletResponse response = getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + roadSegments + "/items?startIndex=3&limit=3", 200);
        List<String> links = response.getHeaders("Link");
        assertThat(links, Matchers.hasSize(1));
        assertEquals(links.get(0), "<" + expectedPrevLink + ">; rel=\"prev\"; type=\"application/geo+json\"");

        DocumentContext json = getAsJSONPath(response);
        assertEquals(2, (int) json.read("features.length()", Integer.class));
        // check the paging link is there
        assertThat(json.read("$.links[?(@.rel=='prev')].href", JSONArray.class).get(0), equalTo(expectedPrevLink));
        assertThat(json.read("$.links[?(@.rel=='next')].href"), Matchers.empty());
    }

    @Test
    public void testErrorHandling() throws Exception {
        String roadSegments = getLayerId(MockData.ROAD_SEGMENTS);
        DocumentContext json = getAsJSONPath("ogc/features/v1/collections/" + roadSegments + "/items?limit=abc", 400);
        assertEquals("InvalidParameterValue", json.read("code"));
        assertThat(json.read("description"), both(containsString("limit")).and(containsString("abc")));
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
                "106",
                document.select("td:matches(RoadSegments\\.1107532045091) + td").text());
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
        String expectedBase = "http://localhost:8080/geoserver/ogc/features/v1/collections/" + roadSegments + "/items";

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
        FeatureTypeInfo genericEntity = getCatalog().getFeatureTypeByName(getLayerId(MockData.GENERICENTITY));
        genericEntity.setName("EntitéGénérique");
        getCatalog().save(genericEntity);
        try {
            String encodedLocalName = URLEncoder.encode(genericEntity.getName(), StandardCharsets.UTF_8.name());
            String typeName = URLEncoder.encode(genericEntity.prefixedName(), StandardCharsets.UTF_8.name());
            String encodedFeatureId = encodedLocalName + ".f004";
            DocumentContext json =
                    getAsJSONPath("ogc/features/v1/collections/" + typeName + "/items/" + encodedFeatureId, 200);

            assertEquals("Feature", json.read("type", String.class));
            // check self link
            String geoJsonLinkPath = "links[?(@.type == 'application/geo+json')]";
            List selfRels = json.read(geoJsonLinkPath + ".rel");
            assertEquals(1, selfRels.size());
            assertEquals("self", selfRels.get(0));
            String href = (String) ((List) json.read(geoJsonLinkPath + "href")).get(0);
            String expected = "http://localhost:8080/geoserver/ogc/features/v1/collections/"
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
        MockHttpServletResponse response = getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + primitiveLayer + "/items/PrimitiveGeoFeature.f002", 200);
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
                startsWith("http://localhost:8080/geoserver/ogc/features/v1/collections/" + primitiveLayer + "?"));
    }

    @Test
    public void testGetItemAsGeoJsonWithCRS() throws Exception {
        String primitiveLayer = ResponseUtils.urlEncode(getLayerId(MockData.PRIMITIVEGEOFEATURE));
        MockHttpServletResponse response = getAsMockHttpServletResponse(
                "ogc/features/v1/collections/" + primitiveLayer + "/items/PrimitiveGeoFeature.f002" + "?crs=CRS:84",
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
                startsWith("http://localhost:8080/geoserver/ogc/features/v1/collections/" + primitiveLayer + "?"));
    }

    @Test
    public void testJSONFGSingleFeatureCRS84() throws Exception {
        String bridges = ResponseUtils.urlEncode(getLayerId(MockData.BRIDGES));
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + bridges
                        + "/items/Bridges.1107531599613"
                        + "?crs=CRS:84&f="
                        + ResponseUtils.urlEncode(JSONFGFeaturesResponse.MIME_TYPE),
                200);

        assertEquals("Feature", json.read("type", String.class));
        // the coord ref sys is not included in the response, as it's the default one
        assertThrows(PathNotFoundException.class, () -> json.read(COORD_REF_SYS, String.class));
        // we have geometry, but not place
        assertThrows(PathNotFoundException.class, () -> json.read("place"));
        assertEquals("Point", json.read("geometry.type"));
        assertArrayEquals(new double[] {2E-4, 7E-4}, json.read("geometry.coordinates", double[].class), 1e-4);
    }

    @Test
    public void testJSONFGSingleFeatureWebMercator() throws Exception {
        String bridges = ResponseUtils.urlEncode(getLayerId(MockData.BRIDGES));
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + bridges
                        + "/items/Bridges.1107531599613"
                        + "?crs=EPSG:3857&f="
                        + ResponseUtils.urlEncode(JSONFGFeaturesResponse.MIME_TYPE),
                200);

        assertEquals("Feature", json.read("type", String.class));
        // the coord ref sys is not included in the response, as it's the default one
        assertEquals(WEB_MERCATOR_URI, json.read(COORD_REF_SYS, String.class));
        // we have place, but not geometry
        assertThrows(PathNotFoundException.class, () -> json.read("geometry"));
        assertEquals("Point", json.read("place.type"));
        assertArrayEquals(new double[] {22, 78}, json.read("place.coordinates", double[].class), 1d);
        // check the link to the type information for single features
        DocumentContext typeLink = readSingleContext(json, "links[?(@.rel == 'type')]");
        assertEquals(SCHEMA_TYPE_VALUE, typeLink.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ABridges/schemas/fg/feature.json",
                typeLink.read("href"));
    }

    /**
     * JSON-FG output respect axis order as mandated by the authority
     *
     * @throws Exception
     */
    @Test
    public void testJSONFGSingleFeatureETRS89() throws Exception {
        String bridges = ResponseUtils.urlEncode(getLayerId(MockData.BRIDGES));
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + bridges
                        + "/items/Bridges.1107531599613"
                        + "?crs=http://www.opengis.net/def/crs/EPSG/0/4258&f="
                        + ResponseUtils.urlEncode(JSONFGFeaturesResponse.MIME_TYPE),
                200);

        assertEquals("Feature", json.read("type", String.class));
        // the coord ref sys is not included in the response, as it's the default one
        assertEquals("http://www.opengis.net/def/crs/EPSG/0/4258", json.read(COORD_REF_SYS, String.class));
        // we have place, but not geometry
        assertThrows(PathNotFoundException.class, () -> json.read("geometry"));
        assertEquals("Point", json.read("place.type"));
        // mind that axis flip
        assertArrayEquals(new double[] {7E-4, 2E-4}, json.read("place.coordinates", double[].class), 1e-4);
    }

    @Test
    public void testJSONFG_CRS84() throws Exception {
        String bridges = ResponseUtils.urlEncode(getLayerId(MockData.BRIDGES));
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + bridges
                        + "/items"
                        + "?crs=CRS:84&f="
                        + ResponseUtils.urlEncode(JSONFGFeaturesResponse.MIME_TYPE),
                200);

        assertEquals("FeatureCollection", json.read("type", String.class));
        // the coord ref sys is not included in the response, as it's the default one
        assertThrows(PathNotFoundException.class, () -> json.read(COORD_REF_SYS, String.class));
        // other basic meta information
        assertEquals(0, json.read("geometryDimension", Integer.class).intValue());
        assertEquals("cite:Bridges", json.read("featureType"));
        // we have geometry, but not place
        DocumentContext feature = readContext(json, "features[0]");
        assertThrows(PathNotFoundException.class, () -> feature.read("place"));
        assertEquals("Point", feature.read("geometry.type"));
        assertArrayEquals(new double[] {2E-4, 7E-4}, feature.read("geometry.coordinates", double[].class), 1e-4);
        assertEquals("Point", feature.read("geometry.type"));
        // check the link to the type information for collections
        DocumentContext typeLink = readSingleContext(json, "links[?(@.rel == 'type')]");
        assertEquals(SCHEMA_TYPE_VALUE, typeLink.read("type"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ABridges/schemas/fg/collection.json",
                typeLink.read("href"));
    }

    @Test
    public void testJSONFGWebMercator() throws Exception {
        String bridges = ResponseUtils.urlEncode(getLayerId(MockData.BRIDGES));
        DocumentContext json = getAsJSONPath(
                "ogc/features/v1/collections/"
                        + bridges
                        + "/items"
                        + "?crs=EPSG:3857&f="
                        + ResponseUtils.urlEncode(JSONFGFeaturesResponse.MIME_TYPE),
                200);

        assertEquals("FeatureCollection", json.read("type", String.class));
        // the coord ref sys is included in the response, not the default
        assertEquals(WEB_MERCATOR_URI, json.read(COORD_REF_SYS, String.class));
        // we have place, but not geometry
        DocumentContext feature = readContext(json, "features[0]");
        assertThrows(PathNotFoundException.class, () -> feature.read("geometry"));
        assertEquals("Point", feature.read("place.type"));
        assertArrayEquals(new double[] {22, 78}, feature.read("place.coordinates", double[].class), 1d);
    }
}
