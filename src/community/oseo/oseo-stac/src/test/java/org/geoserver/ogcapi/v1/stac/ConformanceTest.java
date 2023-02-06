/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

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
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.Test;

public class ConformanceTest extends STACTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/conformance", 200);
        checkConformance(json);
    }

    public static void checkConformance(DocumentContext json) {
        assertThat(json.read("$.conformsTo"), containsInAnyOrder(getExpectedConformanceClasses()));
    }

    private static String[] getExpectedConformanceClasses() {
        return new String[] {
            STACService.FEATURE_CORE,
            STACService.FEATURE_OAS30,
            STACService.FEATURE_HTML,
            STACService.FEATURE_GEOJSON,
            STACService.STAC_CORE,
            STACService.STAC_FEATURES,
            STACService.STAC_SEARCH,
            STACService.STAC_SEARCH_FILTER,
            STACService.STAC_SEARCH_SORT,
            STACService.STAC_SEARCH_FIELDS,
            FEATURES_FILTER,
            FILTER,
            ECQL,
            ECQL_TEXT,
            CQL2_BASIC,
            CQL2_ADVANCED,
            CQL2_ARITHMETIC,
            CQL2_PROPERTY_PROPERTY,
            CQL2_BASIC_SPATIAL,
            CQL2_SPATIAL,
            CQL2_FUNCTIONS,
            CQL2_TEXT
        };
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/stac/v1/conformance/?f=application/x-yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/stac/v1/conformance?f=text/html");
        assertEquals(
                "GeoServer SpatioTemporal Asset Catalog Conformance",
                document.select("#title").text());
        List<String> classes =
                document.select("#content li").stream()
                        .map(e -> e.text())
                        .collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }
}
