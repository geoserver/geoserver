/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.styles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ConformanceTest extends StylesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertEquals(6, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(StylesService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(StylesService.HTML, json.read("$.conformsTo[1]", String.class));
        assertEquals(StylesService.JSON, json.read("$.conformsTo[2]", String.class));
        assertEquals(StylesService.MAPBOX, json.read("$.conformsTo[3]", String.class));
        assertEquals(StylesService.SLD10, json.read("$.conformsTo[4]", String.class));
        assertEquals(StylesService.SLD11, json.read("$.conformsTo[5]", String.class));
        // check the others as they get implemented
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/styles/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/styles/v1/conformance?f=text/html");
        assertEquals("GeoServer OGC API Styles Conformance", document.select("#title").text());
        assertEquals(StylesService.CORE, document.select("#content li:eq(0)").text());
        assertEquals(StylesService.HTML, document.select("#content li:eq(1)").text());
        assertEquals(StylesService.JSON, document.select("#content li:eq(2)").text());
        assertEquals(StylesService.MAPBOX, document.select("#content li:eq(3)").text());
        assertEquals(StylesService.SLD10, document.select("#content li:eq(4)").text());
        assertEquals(StylesService.SLD11, document.select("#content li:eq(5)").text());
    }
}
