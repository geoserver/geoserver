/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.features;

import static org.geoserver.ogcapi.ConformanceClass.CQL2_ADVANCED;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_ARITHMETIC;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_BASIC;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_BASIC_SPATIAL;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_FUNCTIONS;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_PROPERTY_PROPERTY;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_SPATIAL;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.ECQL;
import static org.geoserver.ogcapi.ConformanceClass.ECQL_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.FEATURES_FILTER;
import static org.geoserver.ogcapi.ConformanceClass.FILTER;
import static org.geoserver.ogcapi.ConformanceClass.IDS;
import static org.geoserver.ogcapi.ConformanceClass.QUERYABLES;
import static org.geoserver.ogcapi.ConformanceClass.SEARCH;
import static org.geoserver.ogcapi.ConformanceClass.SORTBY;
import static org.geoserver.ogcapi.v1.features.FeatureService.CORE;
import static org.geoserver.ogcapi.v1.features.FeatureService.CRS_BY_REFERENCE;
import static org.geoserver.ogcapi.v1.features.FeatureService.GEOJSON;
import static org.geoserver.ogcapi.v1.features.FeatureService.HTML;
import static org.geoserver.ogcapi.v1.features.FeatureService.OAS30;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ConformanceTest extends FeaturesTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/features/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        assertThat(json.read("$.conformsTo"), containsInAnyOrder(getExpectedConformanceClasses()));
    }

    private String[] getExpectedConformanceClasses() {
        return new String[] {
            CORE,
            OAS30,
            HTML,
            GEOJSON,
            CRS_BY_REFERENCE,
            FILTER,
            QUERYABLES,
            FEATURES_FILTER,
            SEARCH,
            ECQL,
            ECQL_TEXT,
            CQL2_BASIC,
            CQL2_ADVANCED,
            CQL2_ARITHMETIC,
            CQL2_PROPERTY_PROPERTY,
            CQL2_BASIC_SPATIAL,
            CQL2_SPATIAL,
            CQL2_FUNCTIONS,
            CQL2_TEXT,
            SORTBY,
            IDS
        };
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/v1/conformance?f=text/html");
        assertEquals("GeoServer OGC API Features Conformance", document.select("#title").text());
        List<String> classes =
                document.select("#content li").stream()
                        .map(e -> e.text())
                        .collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }
}
