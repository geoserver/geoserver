/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.coverages;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.ConformanceClass;
import org.junit.Test;

public class ConformanceTest extends CoveragesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/coverages/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertThat(json.read("$.conformsTo"), containsInAnyOrder(getExpectedConformanceClasses()));
    }

    private String[] getExpectedConformanceClasses() {
        return new String[] {
            ConformanceClass.CORE,
            ConformanceClass.COLLECTIONS,
            ConformanceClass.HTML,
            ConformanceClass.JSON,
            ConformanceClass.OAS3,
            ConformanceClass.GEODATA,
            CoveragesService.CONF_CLASS_COVERAGE,
            CoveragesService.CONF_CLASS_GEOTIFF,
            CoveragesService.CONF_CLASS_SUBSET
        };
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/coverages/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/coverages/v1/conformance?f=text/html");
        assertEquals("GeoServer OGC API Coverages Conformance", document.select("#title").text());
        List<String> classes =
                document.select("#content li").stream()
                        .map(e -> e.text())
                        .collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }
}
