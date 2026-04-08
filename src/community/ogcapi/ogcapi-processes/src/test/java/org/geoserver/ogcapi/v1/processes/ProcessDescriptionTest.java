/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import net.minidev.json.JSONArray;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.junit.Test;

public class ProcessDescriptionTest extends OGCApiTestSupport {

    @Test
    public void testBufferProcess() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/processes/v1/processes/JTS:buffer", 200);

        // basics
        assertEquals("JTS:buffer", doc.read("id"));
        assertEquals("Buffer", doc.read("title"));
        assertEquals(
                "Returns a polygonal geometry representing the input geometry enlarged by a given distance around its"
                        + " exterior.",
                doc.read("description"));
        assertEquals(List.of("sync-execute", "async-execute"), doc.read("jobControlOptions"));

        // geometry (complex input)
        DocumentContext geom = readContext(doc, "inputs.geom");
        assertEquals("Input geometry", geom.read("description"));
        assertEquals(1, geom.read("minOccurs", Integer.class).intValue());
        assertEquals(1, geom.read("maxOccurs", Integer.class).intValue());
        JSONArray geomSchema = readContext(geom, "schema.oneOf").json();
        checkGeometrySchema(geomSchema);

        // distance (floating point, no restrictions)
        DocumentContext distance = readContext(doc, "inputs.distance");
        assertEquals(
                "Distance to buffer the input geometry, in the units of the geometry", distance.read("description"));
        assertEquals(1, distance.read("minOccurs", Integer.class).intValue());
        assertEquals(1, distance.read("maxOccurs", Integer.class).intValue());
        assertEquals("number", distance.read("schema.type"));

        // quadrantSegments, optional
        DocumentContext quadrants = readContext(doc, "inputs.quadrantSegments");
        assertEquals(
                "Number determining the style and smoothness of buffer corners. Positive numbers create round corners"
                        + " with that number of segments per quarter-circle, 0 creates flat corners.",
                quadrants.read("description"));
        assertEquals(0, quadrants.read("minOccurs", Integer.class).intValue());
        assertEquals(1, quadrants.read("maxOccurs", Integer.class).intValue());
        assertEquals("integer", quadrants.read("schema.type"));

        // capStyle, optional, enumerated
        DocumentContext capStyle = readContext(doc, "inputs.capStyle");
        assertEquals(
                "Style for the buffer end caps. Values are: Round - rounded ends (default), Flat - flat ends; Square "
                        + "- square ends.",
                capStyle.read("description"));
        assertEquals(0, capStyle.read("minOccurs", Integer.class).intValue());
        assertEquals(1, capStyle.read("maxOccurs", Integer.class).intValue());
        assertEquals("string", capStyle.read("schema.type"));
        assertEquals(List.of("Round", "Flat", "Square"), capStyle.read("schema.enum"));

        // output
        DocumentContext result = readContext(doc, "outputs.result");
        assertEquals("Buffered geometry", result.read("description"));
        JSONArray resultSchema = readContext(geom, "schema.oneOf").json();
        checkGeometrySchema(resultSchema);

        // check the links
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes/JTS:buffer?f=application%2Fjson",
                readSingle(doc, "links[?(@.rel == 'self')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes/JTS:buffer?f=text%2Fhtml",
                readSingle(doc, "links[?(@.rel == 'alternate' && @.type == 'text/html')].href"));
    }

    @SuppressWarnings({"unchecked", "PMD.ForLoopCanBeForeach"})
    private static void checkGeometrySchema(JSONArray geomSchema) {
        Set<String> expectedTypes =
                new HashSet<>(Set.of("application/json", "application/gml-2.1.2", "application/gml-3.1.1"));
        for (int i = 0; i < geomSchema.size(); i++) {
            Map<String, String> schema = (Map) geomSchema.get(i);
            String type = schema.get("type");
            String format = schema.get("format");
            String mediaType = schema.get("contentMediaType");
            assertEquals("string", type);
            if (expectedTypes.remove(mediaType)) assertNull(format); // no one of them is actually binary
        }
        assertEquals("Could not find some expected type: " + expectedTypes, 0, expectedTypes.size());
    }

    @Test
    public void testBufferProcessHTML() throws Exception {
        Document doc = getAsJSoup("ogc/processes/v1/processes/JTS:buffer?f=html");

        // 1. Title check
        String pageTitle = doc.title();
        assertEquals("JTS:buffer", pageTitle);

        // 2. Process title and description
        assertEquals("Buffer", doc.getElementById("JTS__buffer_title").text());
        assertEquals(
                "Returns a polygonal geometry representing the input geometry enlarged by a given distance around its"
                        + " exterior.",
                doc.getElementById("JTS__buffer_description").text());

        // 3. Inputs table
        Element inputsTable = doc.select("div.card:has(h2:contains(Process inputs)) table.function-table")
                .first();
        assertNotNull("Inputs table not found", inputsTable);

        Elements inputRows = inputsTable.select("tr[id]");
        assertEquals(4, inputRows.size());

        // Check individual input rows (order should be preserved by the factory, uses LinkedHashMap internally)
        Set<String> geometryFormats = Set.of(
                "application/json",
                "application/wkt",
                "application/ewkt",
                "application/gml-2.1.2",
                "application/gml-3.1.1");
        checkInputRow(inputRows.get(0), "geom", "Geometry", "1/1", "Input geometry", geometryFormats);
        checkInputRow(
                inputRows.get(1),
                "distance",
                "number",
                "1/1",
                "Distance to buffer the input geometry, in the units of the geometry",
                Set.of());
        checkInputRow(
                inputRows.get(2),
                "quadrantSegments",
                "integer",
                "0/1",
                "Number determining the style and smoothness of buffer corners. Positive numbers create round corners"
                        + " with that number of segments per quarter-circle, 0 creates flat corners.",
                Set.of());
        checkInputRow(
                inputRows.get(3),
                "capStyle",
                "string",
                "0/1",
                "Style for the buffer end caps. Values are: Round - rounded ends (default), Flat - flat ends; Square "
                        + "- square ends.",
                Set.of());

        // 4. Outputs table
        Element outputsTable = doc.select("div.card:has(h2:contains(Process outputs)) table.function-table")
                .first();
        assertNotNull("Outputs table not found", outputsTable);

        Elements outputRows = outputsTable.select("tr[id]");
        assertEquals(1, outputRows.size());

        checkOutputRow(outputRows.get(0), "result", "Geometry", "Buffered geometry", geometryFormats);
    }

    private void checkInputRow(
            Element row,
            String expectedId,
            String expectedType,
            String expectedOccurrences,
            String expectedDesc,
            Set<String> expectedFormats) {
        Elements cols = row.select("td");
        assertEquals(5, cols.size());
        assertEquals(expectedId, cols.get(0).text());
        assertEquals(expectedType, cols.get(1).text());
        assertEquals(expectedOccurrences, cols.get(2).text());
        assertEquals(expectedDesc, cols.get(3).text());
        Set<String> actualFormats =
                cols.get(4).select("span").stream().map(e -> e.text()).collect(Collectors.toSet());
        if (expectedFormats.isEmpty()) {
            assertTrue("No formats expected, but found: " + actualFormats, actualFormats.isEmpty());
        } else {
            assertEquals("Expected formats do not match", expectedFormats, actualFormats);
        }
    }

    private void checkOutputRow(
            Element row, String expectedId, String expectedType, String expectedDesc, Set<String> expectedFormats) {
        Elements cols = row.select("td");
        assertEquals(4, cols.size());
        assertEquals(expectedId, cols.get(0).text());
        assertEquals(expectedType, cols.get(1).text());
        assertEquals(expectedDesc, cols.get(2).text());
        Set<String> actualFormats =
                cols.get(3).select("span").stream().map(e -> e.text()).collect(Collectors.toSet());
        if (expectedFormats.isEmpty()) {
            assertTrue("No formats expected, but found: " + actualFormats, actualFormats.isEmpty());
        } else {
            assertEquals("Expected formats do not match", expectedFormats, actualFormats);
        }
    }

    @Test
    public void testEchoProcess() throws Exception {
        DocumentContext doc = getAsJSONPath("ogc/processes/v1/processes/gs:Echo", 200);
        assertEquals("gs:Echo", doc.read("id"));
        assertEquals("Echo", doc.read("title"));
        assertEquals("Echoes back the input parameters provided to the process.", doc.read("description"));
        assertEquals(List.of("sync-execute", "async-execute"), doc.read("jobControlOptions"));
        // Check inputs
        assertEquals(5, doc.read("inputs.size()", Integer.class).intValue());
        // string input
        DocumentContext stringInput = readContext(doc, "inputs.stringInput");
        assertEquals("stringInput", stringInput.read("title"));
        assertEquals("string", stringInput.read("schema.type"));
        assertEquals(0, stringInput.read("minOccurs", Integer.class).intValue());
        assertEquals(1, stringInput.read("maxOccurs", Integer.class).intValue());
        // double input
        DocumentContext doubleInput = readContext(doc, "inputs.doubleInput");
        assertEquals("doubleInput", doubleInput.read("title"));
        assertEquals("number", doubleInput.read("schema.type"));
        assertEquals("double", doubleInput.read("schema.format"));
        assertEquals(0, doubleInput.read("minOccurs", Integer.class).intValue());
        assertEquals(1, doubleInput.read("maxOccurs", Integer.class).intValue());
        // bbox input
        DocumentContext boundingBoxInput = readContext(doc, "inputs.boundingBoxInput");
        assertEquals("boundingBoxInput", boundingBoxInput.read("title"));
        assertEquals(AbstractProcessIO.FORMAT_OGC_BBOX, boundingBoxInput.read("schema.allOf[0].format"));
        assertEquals(0, boundingBoxInput.read("minOccurs", Integer.class).intValue());
        assertEquals(1, boundingBoxInput.read("maxOccurs", Integer.class).intValue());
        // image input
        DocumentContext imageInput = readContext(doc, "inputs.imageInput");
        assertEquals("imageInput", imageInput.read("title"));
        DocumentContext imageSchemas = readContext(imageInput, "schema.oneOf");
        for (int i = 0; i < 2; i++) {
            DocumentContext schema = readContext(imageSchemas, "[%d]".formatted(i));
            assertEquals("string", schema.read("type"));
            assertEquals("binary", schema.read("format"));
            String format = schema.read("contentMediaType");
            assertTrue(format.equals("image/png") || format.equals("image/jpeg"));
        }
        // pause input
        DocumentContext pauseInput = readContext(doc, "inputs.pause");
        assertEquals("pause", pauseInput.read("title"));
        assertEquals("integer", pauseInput.read("schema.type"));
        assertEquals(0, pauseInput.read("minOccurs", Integer.class).intValue());
        assertEquals(1, pauseInput.read("maxOccurs", Integer.class).intValue());
    }
}
