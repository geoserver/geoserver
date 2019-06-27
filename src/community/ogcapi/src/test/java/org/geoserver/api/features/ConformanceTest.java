/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.features;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.junit.Test;
import org.w3c.dom.Document;

public class ConformanceTest extends FeaturesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/conformance", 200);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(
                "http://www.opengis.net/spec/wfs-1/3.0/req/core",
                json.read("$.conformsTo[0]", String.class));
        assertEquals(
                "http://www.opengis.net/spec/wfs-1/3.0/req/oas30",
                json.read("$.conformsTo[1]", String.class));
        assertEquals(
                "http://www.opengis.net/spec/wfs-1/3.0/req/geojson",
                json.read("$.conformsTo[2]", String.class));
        assertEquals(
                "http://www.opengis.net/spec/wfs-1/3.0/req/gmlsf0",
                json.read("$.conformsTo[3]", String.class));
    }

    @Test
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
