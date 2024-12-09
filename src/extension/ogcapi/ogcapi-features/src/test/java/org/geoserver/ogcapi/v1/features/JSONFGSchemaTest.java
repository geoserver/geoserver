/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.JSONSchemaMessageConverter.SCHEMA_TYPE_VALUE;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.MockData;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONFGSchemaTest extends FeaturesTestSupport {

    @Test
    public void collection() throws Exception {
        basicSchemaTest("collection");
    }

    @Test
    public void coordrefsys() throws Exception {
        basicSchemaTest("coordrefsys");
    }

    @Test
    public void featuretype() throws Exception {
        basicSchemaTest("featuretype");
    }

    @Test
    public void geometry() throws Exception {
        basicSchemaTest("geometry");
    }

    @Test
    public void geometryObjects() throws Exception {
        basicSchemaTest("geometry-objects");
    }

    @Test
    public void link() throws Exception {
        basicSchemaTest("link");
    }

    @Test
    public void place() throws Exception {
        basicSchemaTest("place");
    }

    @Test
    public void time() throws Exception {
        basicSchemaTest("time");
    }

    public void basicSchemaTest(String schemaId) throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/features/v1/collections/cite:Buildings/schemas/fg/"
                                + schemaId
                                + ".json");
        assertEquals(200, response.getStatus());
        assertEquals(SCHEMA_TYPE_VALUE, response.getContentType());
        String expected = getFixture(schemaId);
        assertEquals(expected, response.getContentAsString());
    }

    private String getFixture(String schemaId) throws IOException {
        try (InputStream is = getClass().getResourceAsStream("schema/" + schemaId + ".json")) {
            return IOUtils.toString(is, StandardCharsets.UTF_8);
        }
    }

    @Test
    public void featureBuildings() throws Exception {
        DocumentContext json = basicFeatureChecks(getLayerId(MockData.BUILDINGS), "MultiPolygon");

        // no actual properties to check
        assertEquals("null", json.read("properties.properties.oneOf[0].type"));
        assertEquals("object", json.read("properties.properties.oneOf[1].type"));
    }

    @Test
    public void featureLakes() throws Exception {
        DocumentContext json = basicFeatureChecks(getLayerId(MockData.LAKES), "MultiPolygon");

        // no actual properties to check
        assertEquals("null", json.read("properties.properties.oneOf[0].type"));
        DocumentContext props = readContext(json, "properties.properties.oneOf[1]");
        assertEquals("object", props.read("type"));
        assertEquals("string", props.read("FID.type"));
        assertEquals("string", props.read("NAME.type"));
    }

    @Test
    public void featurePrimitives() throws Exception {
        DocumentContext json =
                basicFeatureChecks(getLayerId(MockData.PRIMITIVEGEOFEATURE), "Polygon");

        // no actual properties to check
        assertEquals("null", json.read("properties.properties.oneOf[0].type"));
        DocumentContext props = readContext(json, "properties.properties.oneOf[1]");
        assertEquals("object", props.read("type"));
        assertEquals("string", props.read("description.type"));
        assertEquals("string", props.read("name.type"));
        assertEquals("integer", props.read("intProperty.type"));
        assertEquals("string", props.read("uriProperty.type"));
        assertEquals("string", props.read("dateTimeProperty.type"));
        assertEquals("date-time", props.read("dateTimeProperty.format"));
        assertEquals("string", props.read("dateProperty.type"));
        assertEquals("date", props.read("dateProperty.format"));
        assertEquals("number", props.read("decimalProperty.type"));
        assertEquals("boolean", props.read("booleanProperty.type"));
    }

    private DocumentContext basicFeatureChecks(String typeName, String geometryType)
            throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        "ogc/features/v1/collections/" + typeName + "/schemas/fg/feature.json");
        assertEquals(200, response.getStatus());
        assertEquals(SCHEMA_TYPE_VALUE, response.getContentType());
        DocumentContext json = getAsJSONPath(response);

        // geometry and place updated to match only MultiPolygon
        assertEquals("null", json.read("properties.geometry.oneOf[0].type"));
        assertEquals(
                "geometry-objects.json#/$defs/" + geometryType,
                json.read("properties.geometry.oneOf[1].$ref"));
        assertEquals("null", json.read("properties.place.oneOf[0].type"));
        assertEquals(
                "geometry-objects.json#/$defs/" + geometryType,
                json.read("properties.place.oneOf[1].$ref"));
        return json;
    }
}
