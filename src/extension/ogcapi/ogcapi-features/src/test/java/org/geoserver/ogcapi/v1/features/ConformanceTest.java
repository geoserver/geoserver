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
import static org.geoserver.ogcapi.ConformanceClass.CQL2_JSON;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_PROPERTY_PROPERTY;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_SPATIAL;
import static org.geoserver.ogcapi.ConformanceClass.CQL2_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.ECQL_TEXT;
import static org.geoserver.ogcapi.ConformanceClass.FEATURES_FILTER;
import static org.geoserver.ogcapi.ConformanceClass.FILTER;
import static org.geoserver.ogcapi.ConformanceClass.QUERYABLES;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.ogcapi.ConformanceClass;
import org.geoserver.wfs.WFSInfo;
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
        // The following are not enabled by default:
        //   IDS
        //   SEARCH
        //   SORTBY
        //   PROPERTY_SELECTION
        //   GMLSF0
        //   GMLSF2
        return new String[] {
            FeatureConformance.CORE.getId(),
            FeatureConformance.OAS30.getId(),
            FeatureConformance.HTML.getId(),
            FeatureConformance.GEOJSON.getId(),
            FeatureConformance.GML321.getId(),
            FeatureConformance.CRS_BY_REFERENCE.getId(),
            FEATURES_FILTER,
            FILTER,
            QUERYABLES,
            ECQL_TEXT,
            CQL2_BASIC,
            CQL2_ADVANCED,
            CQL2_ARITHMETIC,
            CQL2_PROPERTY_PROPERTY,
            CQL2_BASIC_SPATIAL,
            CQL2_SPATIAL,
            CQL2_FUNCTIONS,
            CQL2_TEXT,
            CQL2_JSON
        };
    }

    @Test
    public void testCollectionsYaml() throws Exception {
        String yaml = getAsString("ogc/features/v1/conformance/?f=application/yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/features/v1/conformance?f=text/html");
        assertEquals(
                "GeoServer OGC API Features Conformance",
                document.select("#title").text());
        List<String> classes =
                document.select("#content li").stream().map(e -> e.text()).collect(Collectors.toList());
        assertThat(classes, containsInAnyOrder(getExpectedConformanceClasses()));
    }

    @Test
    public void testOptionalConformance() throws Exception {
        WFSInfo wfs = getGeoServer().getService(WFSInfo.class);
        FeatureConformance features = FeatureConformance.configuration(wfs);
        // enable a few optional extensions
        features.setIDs(true);
        features.setSortBy(true);
        features.setPropertySelection(true);
        getGeoServer().save(wfs);
        try {
            DocumentContext json = getAsJSONPath("ogc/features/v1/conformance", 200);
            assertEquals(2, (int) json.read("$.length()", Integer.class));
            // start with the basic ones
            List<String> expected = new ArrayList<>(Arrays.asList(getExpectedConformanceClasses()));
            // add the ones configured above
            expected.add(ConformanceClass.IDS);
            expected.add(ConformanceClass.SORTBY);
            expected.add(ConformanceClass.PROPERTY_SELECTION);
            assertThat(json.read("$.conformsTo"), containsInAnyOrder(expected.toArray(String[]::new)));
        } finally {
            // reset defaults
            features.setIDs(null);
            features.setSortBy(null);
            features.setPropertySelection(null);
            getGeoServer().save(wfs);
        }
    }
}
