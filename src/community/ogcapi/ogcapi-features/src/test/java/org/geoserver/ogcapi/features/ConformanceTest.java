/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConformanceTest extends FeaturesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(1, (int) json.read("$.length()", Integer.class));
        assertEquals(6, (int) json.read("$.conformsTo.length()", Integer.class));
        assertEquals(FeatureService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(FeatureService.OAS30, json.read("$.conformsTo[1]", String.class));
        assertEquals(FeatureService.HTML, json.read("$.conformsTo[2]", String.class));
        assertEquals(FeatureService.GEOJSON, json.read("$.conformsTo[3]", String.class));
        assertEquals(FeatureService.GMLSF0, json.read("$.conformsTo[4]", String.class));
        assertEquals(FeatureService.CQL_TEXT, json.read("$.conformsTo[5]", String.class));
    }

    @Test
    @Ignore
    public void testConformanceXML() throws Exception {
        Document dom = getAsDOM("ogc/features?f=application/xml");
        print(dom);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/conformance?f=text/html");
        assertEquals("GeoServer OGC API Features Conformance", document.select("#title").text());
        assertEquals(FeatureService.CORE, document.select("#content li:eq(0)").text());
        assertEquals(FeatureService.OAS30, document.select("#content li:eq(1)").text());
        assertEquals(FeatureService.HTML, document.select("#content li:eq(2)").text());
        assertEquals(FeatureService.GEOJSON, document.select("#content li:eq(3)").text());
        assertEquals(FeatureService.GMLSF0, document.select("#content li:eq(4)").text());
        assertEquals(FeatureService.CQL_TEXT, document.select("#content li:eq(5)").text());
    }
}
