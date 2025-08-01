/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wps.DeprecatedProcessFactory;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geotools.api.feature.type.Name;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.hamcrest.Matchers;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class ProcessesTest extends OGCApiTestSupport {

    @Test
    public void testProcessesJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/processes/v1/processes", 200);
        testProcessesJson(json, "application/json");
    }

    @Test
    public void testProcessesYaml() throws Exception {
        String yaml = getAsString("ogc/processes/v1/processes?f=yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testProcessesJson(json, "application/yaml");
    }

    private void testProcessesJson(DocumentContext json, String selfFormat) {
        // check the self link
        assertEquals(selfFormat, readSingle(json, "links[?(@.rel == 'self')].type"));
        String selfFormatEncoded = selfFormat.replace("/", "%2F");
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?f=" + selfFormatEncoded,
                readSingle(json, "links[?(@.rel == 'self')].href"));

        // grab a process and do a quick test
        DocumentContext disjoint = readSingleContext(json, "processes[?(@.id == 'JTS:disjoint')]");
        assertEquals("1.0.0", disjoint.read("version"));
        assertEquals("Disjoint Test", disjoint.read("title"));
        assertEquals("Tests if two geometries do not have any points in common.", disjoint.read("description"));

        // check there are "many" processes
        assertThat(json.read("processes.length()", Integer.class), Matchers.greaterThan(10));
    }

    @Test
    public void testProcessSelection() throws Exception {
        // disable all factories but the geoserver one
        List<ProcessGroupInfo> groups = Processors.getProcessFactories().stream()
                .map(pf -> {
                    ProcessGroupInfo group = new ProcessGroupInfoImpl();
                    group.setFactoryClass(pf.getClass());
                    group.setEnabled(!(pf instanceof DeprecatedProcessFactory)
                            && pf.getNames().stream().anyMatch(n -> "gs".equals(n.getNamespaceURI())));
                    return group;
                })
                .collect(Collectors.toList());

        GeoServer gs = getGeoServer();
        WPSInfo wps = gs.getService(WPSInfo.class);
        wps.getProcessGroups().clear();
        wps.getProcessGroups().addAll(groups);
        gs.save(wps);

        try {
            DocumentContext json = getAsJSONPath("ogc/processes/v1/processes", 200);
            // JTS processes are gone
            assertEquals(
                    0,
                    json.read("processes[?(@.id == 'JTS:disjoint')].length()", List.class)
                            .size());
            // but a GS one is found
            assertEquals(
                    1,
                    json.read("processes[?(@.id == 'gs:GeorectifyCoverage')].length()", List.class)
                            .size());
        } finally {
            wps.getProcessGroups().clear();
            gs.save(wps);
        }
    }

    @Test
    public void testProcessesHTML() throws Exception {
        Document document = getAsJSoup("ogc/processes/v1/processes?f=html");

        Name disjointName = new NameImpl("JTS", "disjoint");
        ProcessFactory jtsFactory = Processors.createProcessFactory(disjointName);

        // just test the disjoint process
        assertEquals(
                jtsFactory.getTitle(disjointName).toString(),
                document.select("#JTS__disjoint_title").text());
        assertEquals(
                jtsFactory.getDescription(disjointName).toString(),
                document.select("#JTS__disjoint_description").text());
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes/JTS:disjoint",
                document.selectFirst("a:contains(JTS:disjoint)").attr("href"));
    }
}
