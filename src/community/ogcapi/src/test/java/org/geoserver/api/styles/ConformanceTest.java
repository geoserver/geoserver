/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.styles;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConformanceTest extends StylesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/styles/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(StylesService.CORE, json.read("$.conformsTo[0]", String.class));
        assertEquals(StylesService.HTML, json.read("$.conformsTo[1]", String.class));
        assertEquals(StylesService.JSON, json.read("$.conformsTo[2]", String.class));
        // check the others as they get implemented
    }

    @Test
    public void testConformanceXML() throws Exception {
        Document dom = getAsDOM("ogc/features?f=application/xml");
        print(dom);
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/styles/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }
}
