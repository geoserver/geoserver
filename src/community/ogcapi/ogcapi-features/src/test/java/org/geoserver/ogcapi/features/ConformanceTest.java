/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.features;

import static org.geoserver.ogcapi.ConformanceClass.FEATURES_FILTER;
import static org.geoserver.ogcapi.ConformanceClass.FILTER;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_ARITHMETIC;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_CQL_JSON;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_CQL_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_FUNCTIONS;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_SPATIAL_OPS;
import static org.geoserver.ogcapi.ConformanceClass.FILTER_TEMPORAL;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
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
        assertThat(json.read("$.conformsTo"), containsInAnyOrder(getExpectedConformanceClasses()));
    }

    private String[] getExpectedConformanceClasses() {
        return new String[] {
            FeatureService.CORE,
            FeatureService.OAS30,
            FeatureService.HTML,
            FeatureService.GEOJSON,
            FeatureService.GMLSF0,
            FEATURES_FILTER,
            FILTER,
            FILTER_SPATIAL_OPS,
            FILTER_TEMPORAL,
            FILTER_FUNCTIONS,
            FILTER_ARITHMETIC,
            FILTER_CQL_TEXT,
            FILTER_CQL_JSON
        };
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
        List<String> classes =
                document.select("#content li").stream()
                        .map(e -> e.text())
                        .collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }
}
