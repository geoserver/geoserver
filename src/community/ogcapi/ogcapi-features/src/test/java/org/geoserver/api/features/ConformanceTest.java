/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

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
}
