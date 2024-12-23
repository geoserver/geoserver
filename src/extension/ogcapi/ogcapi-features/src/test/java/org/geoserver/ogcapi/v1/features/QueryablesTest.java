/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi.v1.features;

import static org.geoserver.data.test.CiteTestData.ROAD_SEGMENTS;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.github.erosb.jsonsKema.JsonParser;
import com.github.erosb.jsonsKema.JsonValue;
import com.github.erosb.jsonsKema.SchemaLoader;
import com.jayway.jsonpath.DocumentContext;
import java.io.UnsupportedEncodingException;
import org.geoserver.data.test.MockData;
import org.geoserver.ogcapi.Queryables;
import org.hamcrest.Matchers;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

public class QueryablesTest extends FeaturesTestSupport {

    @Test
    public void testDefaultFormat() throws Exception {
        MockHttpServletResponse response = getAsMockHttpServletResponse(roadSegmentQueryables(), 200);
        assertEquals("application/schema+json", response.getContentType());

        checkRoadSegmentsQueryables(response);
    }

    @Test
    public void testFullFormatParameter() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(roadSegmentQueryables() + "?f=application/schema%2Bjson", 200);
        assertEquals("application/schema+json", response.getContentType());

        checkRoadSegmentsQueryables(response);
    }

    private String roadSegmentQueryables() {
        return "ogc/features/v1/collections/" + getLayerId(ROAD_SEGMENTS) + "/queryables";
    }

    @Test
    public void testAcceptHeader() throws Exception {
        MockHttpServletRequest request = createRequest(roadSegmentQueryables());
        request.setMethod("GET");
        request.addHeader("Accept", "application/schema+json");

        MockHttpServletResponse response = dispatch(request, null);
        assertEquals(200, response.getStatus());
        checkRoadSegmentsQueryables(response);
    }

    private void checkRoadSegmentsQueryables(MockHttpServletResponse response) throws UnsupportedEncodingException {
        DocumentContext json = getAsJSONPath(response);
        assertEquals("geometry-multilinestring", json.read("properties.the_geom.format"));
        assertEquals("string", json.read("properties.FID.type"));
        assertEquals("string", json.read("properties.FID.title"));
        assertEquals("string", json.read("properties.NAME.type"));
        assertEquals("string", json.read("properties.NAME.title"));
        assertEquals(Queryables.JSON_SCHEMA_DRAFT_2020_12, readSingle(json, ".$schema"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/v1/collections/cite%3ARoadSegments/queryables",
                readSingle(json, ".$id"));
        assertEquals("object", json.read("type"));

        // JSON schema can be represented only by the schema encoder, the YAML one, and HTML one.
        assertEquals(Integer.valueOf(3), json.read("$..links.length()", Integer.class));
        DocumentContext selfLink = readSingleContext(json, "links[?(@.rel=='self')]");
        assertEquals("application/schema+json", selfLink.read("type"));
        DocumentContext htmlLink = readSingleContext(json, "links[?(@.rel=='alternate' && @.type=='text/html')]");
        assertThat(htmlLink.read("href"), Matchers.endsWith("queryables?f=text%2Fhtml"));
        DocumentContext yamlLink =
                readSingleContext(json, "links[?(@.rel=='alternate' && @.type=='application/yaml')]");
        assertThat(yamlLink.read("href"), Matchers.endsWith("queryables?f=application%2Fyaml"));
    }

    @Test
    public void testQueryablesHTML() throws Exception {
        String roadSegments = MockData.ROAD_SEGMENTS.getLocalPart();
        org.jsoup.nodes.Document document =
                getAsJSoup("cite/ogc/features/v1/collections/" + roadSegments + "/queryables?f=html");
        assertEquals(
                "the_geom: MultiLineString",
                document.select("#queryables li:eq(0)").text());
    }

    @Test
    public void queryablesSchema() throws Exception {
        MockHttpServletResponse response =
                getAsMockHttpServletResponse(roadSegmentQueryables() + "?f=application/schema%2Bjson", 200);

        // check the response can be read as a valid JSON schema (will fail with an exception if
        // not)
        JsonValue schemaJSON = new JsonParser(response.getContentAsString()).parse();
        new SchemaLoader(schemaJSON).load();
    }
}
