/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ConformanceTest extends STACTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/conformance", 200);
        checkConformance(json);
    }

    public static void checkConformance(DocumentContext json) {
        assertEquals(1, (int) json.read("$.length()", Integer.class));
        assertEquals(8, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(STACService.FEATURE_CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(STACService.FEATURE_OAS30, json.read("$.conformsTo[1]", String.class));
        assertEquals(STACService.FEATURE_HTML, json.read("$.conformsTo[2]", String.class));
        assertEquals(STACService.FEATURE_GEOJSON, json.read("$.conformsTo[3]", String.class));
        assertEquals(STACService.FEATURE_CQL_TEXT, json.read("$.conformsTo[4]", String.class));
        assertEquals(STACService.STAC_CORE, json.read("$.conformsTo[5]", String.class));
        assertEquals(STACService.STAC_FEATURES, json.read("$.conformsTo[6]", String.class));
        assertEquals(STACService.STAC_SEARCH, json.read("$.conformsTo[7]", String.class));
        // check the others as they get implemented
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/stac/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/stac/conformance?f=text/html");
        assertEquals(
                "GeoServer SpatioTemporal Asset Catalog Conformance",
                document.select("#title").text());
        assertEquals(STACService.FEATURE_CORE, document.select("#content li:eq(0)").text());
        assertEquals(STACService.FEATURE_OAS30, document.select("#content li:eq(1)").text());
        assertEquals(STACService.FEATURE_HTML, document.select("#content li:eq(2)").text());
        assertEquals(STACService.FEATURE_GEOJSON, document.select("#content li:eq(3)").text());
        assertEquals(STACService.FEATURE_CQL_TEXT, document.select("#content li:eq(4)").text());
        assertEquals(STACService.STAC_CORE, document.select("#content li:eq(5)").text());
        assertEquals(STACService.STAC_FEATURES, document.select("#content li:eq(6)").text());
        assertEquals(STACService.STAC_SEARCH, document.select("#content li:eq(7)").text());
        // check the others as they get implemented
    }
}
