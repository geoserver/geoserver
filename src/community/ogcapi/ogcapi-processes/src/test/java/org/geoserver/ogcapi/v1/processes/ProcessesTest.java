/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.processes;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.config.GeoServer;
import org.geoserver.ogcapi.OGCApiTestSupport;
import org.geoserver.wps.DeprecatedProcessFactory;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.process.GeoServerProcessors;
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

    @Test
    public void testProcessPagingJSON() throws Exception {
        // GeoServer comes with 100+ processes, so we can test paging easily, but we need to know what's the last page
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        int count = pfs.stream().mapToInt(pf -> pf.getNames().size()).sum();

        // first page, has next link but not prev
        DocumentContext json = getAsJSONPath("ogc/processes/v1/processes?offset=0&limit=10", 200);
        assertEquals(10, (int) json.read("processes.length()", Integer.class));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=10&limit=10&f=application%2Fjson",
                readSingle(json, "links[?(@.rel == 'next' && @.type == 'application/json')].href"));
        assertThat(json.read("links[?(@.rel == 'prev')]"), Matchers.hasSize(0));

        // second page, has next and prev links
        json = getAsJSONPath("ogc/processes/v1/processes?offset=10&limit=10", 200);
        assertEquals(10, (int) json.read("processes.length()", Integer.class));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=20&limit=10&f=application%2Fjson",
                readSingle(json, "links[?(@.rel == 'next' && @.type == 'application/json')].href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=0&limit=10&f=application%2Fjson",
                readSingle(json, "links[?(@.rel == 'prev' && @.type == 'application/json')].href"));

        // last page
        int offset = (count % 10 == 0 ? count : (int) Math.ceil(count / 10d) * 10) - 10;
        json = getAsJSONPath("ogc/processes/v1/processes?offset=%d&limit=10".formatted(offset), 200);
        assertEquals(count - offset, (int) json.read("processes.length()", Integer.class));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=%d&limit=10&f=application%%2Fjson"
                        .formatted(offset - 10),
                readSingle(json, "links[?(@.rel == 'prev' && @.type == 'application/json')].href"));
        assertThat(json.read("links[?(@.rel == 'next')]"), Matchers.hasSize(0));
    }

    @Test
    public void testProcessPagingHTML() throws Exception {
        // GeoServer comes with 100+ processes, so we can test paging easily, but we need to know what's the last page
        Set<ProcessFactory> pfs = GeoServerProcessors.getProcessFactories();
        int count = pfs.stream().mapToInt(pf -> pf.getNames().size()).sum();

        // first page, has next link but not prev
        Document document = getAsJSoup("ogc/processes/v1/processes?offset=0&limit=10&f=html");
        assertEquals(10, document.select("div.card.h-100").size());
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=10&limit=10&f=text%2Fhtml",
                document.selectFirst("a#nextPage").attr("href"));
        assertTrue(document.selectFirst("a#prevPage").parent().hasClass("disabled"));

        // second page, has next and prev links
        document = getAsJSoup("ogc/processes/v1/processes?offset=10&limit=10&f=html");
        assertEquals(10, document.select("div.card.h-100").size());
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=20&limit=10&f=text%2Fhtml",
                document.selectFirst("a#nextPage").attr("href"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=0&limit=10&f=text%2Fhtml",
                document.selectFirst("a#prevPage").attr("href"));

        // last page, has prev link but not next
        int offset = (count % 10 == 0 ? count : (int) Math.ceil(count / 10d) * 10) - 10;
        document = getAsJSoup("ogc/processes/v1/processes?offset=%d&limit=10&f=html".formatted(offset));
        assertEquals(count - offset, document.select("div.card.h-100").size());
        assertTrue(document.selectFirst("a#nextPage").parent().hasClass("disabled"));
        assertEquals(
                "http://localhost:8080/geoserver/ogc/processes/v1/processes?offset=%d&limit=10&f=text%%2Fhtml"
                        .formatted(offset - 10),
                document.selectFirst("a#prevPage").attr("href"));
    }
}
