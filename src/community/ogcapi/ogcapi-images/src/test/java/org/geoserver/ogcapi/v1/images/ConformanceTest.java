/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.images;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.ogcapi.ConformanceClass;
import org.junit.Test;

public class ConformanceTest extends ImagesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/images/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertEquals(4, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(ConformanceClass.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(ConformanceClass.COLLECTIONS, json.read("$.conformsTo[1]", String.class));
        assertEquals(ImagesService.IMAGES_CORE, json.read("$.conformsTo[2]", String.class));
        assertEquals(
                ImagesService.IMAGES_TRANSACTIONAL, json.read("$.conformsTo[3]", String.class));
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/images/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/images/v1/conformance?f=text/html");
        assertEquals("GeoServer OGC API Images Conformance", document.select("#title").text());
        assertEquals(ConformanceClass.CORE, document.select("#content li:eq(0)").text());
        assertEquals(ConformanceClass.COLLECTIONS, document.select("#content li:eq(1)").text());
        assertEquals(ImagesService.IMAGES_CORE, document.select("#content li:eq(2)").text());
        assertEquals(
                ImagesService.IMAGES_TRANSACTIONAL, document.select("#content li:eq(3)").text());
    }
}
