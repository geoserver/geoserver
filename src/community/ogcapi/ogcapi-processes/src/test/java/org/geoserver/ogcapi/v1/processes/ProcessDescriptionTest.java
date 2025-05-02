/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.jayway.jsonpath.DocumentContext;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
                "Returns a polygonal geometry representing the input geometry enlarged by a given distance around its exterior.",
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
                "Number determining the style and smoothness of buffer corners. Positive numbers create round corners with that number of segments per quarter-circle, 0 creates flat corners.",
                quadrants.read("description"));
        assertEquals(0, quadrants.read("minOccurs", Integer.class).intValue());
        assertEquals(1, quadrants.read("maxOccurs", Integer.class).intValue());
        assertEquals("integer", quadrants.read("schema.type"));

        // capStyle, optional, enumerated
        DocumentContext capStyle = readContext(doc, "inputs.capStyle");
        assertEquals(
                "Style for the buffer end caps. Values are: Round - rounded ends (default), Flat - flat ends; Square - square ends.",
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
    }

    @SuppressWarnings("unchecked")
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
                "Returns a polygonal geometry representing the input geometry enlarged by a given distance around its exterior.",
                doc.getElementById("JTS__buffer_description").text());

        // 3. Inputs table
        Element inputsTable = doc.select("div.card:has(h2:contains(Process inputs)) table.function-table")
                .first();
        assertNotNull("Inputs table not found", inputsTable);

        Elements inputRows = inputsTable.select("tr[id]");
        assertEquals(4, inputRows.size());

        // Check individual input rows (order should be preserved by the factory, uses LinkedHashMap internally)
        checkInputRow(inputRows.get(0), "geom", "geom", "Geometry", "Input geometry");
        checkInputRow(
                inputRows.get(1),
                "distance",
                "distance",
                "number",
                "Distance to buffer the input geometry, in the units of the geometry");
        checkInputRow(
                inputRows.get(2),
                "quadrantSegments",
                "quadrantSegments",
                "integer",
                "Number determining the style and smoothness of buffer corners. Positive numbers create round corners with that number of segments per quarter-circle, 0 creates flat corners.");
        checkInputRow(
                inputRows.get(3),
                "capStyle",
                "capStyle",
                "string",
                "Style for the buffer end caps. Values are: Round - rounded ends (default), Flat - flat ends; Square - square ends.");

        // 4. Outputs table
        Element outputsTable = doc.select("div.card:has(h2:contains(Process outputs)) table.function-table")
                .first();
        assertNotNull("Outputs table not found", outputsTable);

        Elements outputRows = outputsTable.select("tr[id]");
        assertEquals(1, outputRows.size());

        checkInputRow(outputRows.get(0), "result", "result", "Geometry", "Buffered geometry");
    }

    private void checkInputRow(
            Element row, String expectedId, String expectedTitle, String expectedType, String expectedDesc) {
        Elements cols = row.select("td");
        assertEquals(4, cols.size());
        assertEquals(expectedId, cols.get(0).text());
        assertEquals(expectedTitle, cols.get(1).text());
        assertEquals(expectedType, cols.get(2).text());
        assertEquals(expectedDesc, cols.get(3).text());
    }
}
