/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.dggs;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;

public class ConformanceTest extends DGGSTestSupport {

    @Test
    public void testConformance() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(1, (int) json.read("$.length()", Integer.class));
        assertEquals(1, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(DGGSService.CORE, json.read("$.conformsTo[0]", String.class));
    }

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/dggs/conformance?f=json", 200);
        checkConformance(json);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/dggs/conformance?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/dggs/conformance?f=text/html");
        assertEquals("GeoServer DGGS Conformance", document.select("#title").text());
        assertEquals(DGGSService.CORE, document.select("#content li:eq(0)").text());
    }
}
