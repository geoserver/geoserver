/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.geoserver.ogcapi.v1.processes.ProcessesService.CONF_CLASS_HTML;
import static org.geoserver.ogcapi.v1.processes.ProcessesService.CONF_CLASS_JSON;
import static org.geoserver.ogcapi.v1.processes.ProcessesService.CONF_CLASS_PROCESSES_CORE;
import static org.geoserver.ogcapi.v1.processes.ProcessesService.CONF_CLASS_PROCESS_DESCRIPTION;
import static org.geoserver.ogcapi.v1.processes.ProcessesService.CONF_KVP_EXECUTE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.junit.Test;

public class ConformanceTest extends OGCApiTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/processes/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertThat(json.read("$.conformsTo"), containsInAnyOrder(getExpectedConformanceClasses()));
    }

    private String[] getExpectedConformanceClasses() {
        // TODO: add conformance classes and check the contents accordingly
        return new String[] {
            ConformanceClass.CORE,
            ConformanceClass.OAS3,
            CONF_CLASS_PROCESSES_CORE,
            CONF_CLASS_PROCESS_DESCRIPTION,
            CONF_CLASS_HTML,
            CONF_CLASS_JSON,
            CONF_KVP_EXECUTE
        };
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/processes/v1/conformance/?f=application/yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/processes/v1/conformance?f=text/html");
        assertEquals(
                "GeoServer OGC API Processes Conformance",
                document.select("#title").text());
        List<String> classes =
                document.select("#content li").stream().map(e -> e.text()).collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }
}
