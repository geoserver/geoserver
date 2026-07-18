/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.maps;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.geoserver.ogcapi.ConformanceClass;
import org.junit.Test;

public class ConformanceTest extends MapsTestSupport {

    @Test
    public void testConformanceJson() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/maps/v1/conformance", 200);
        checkConformance(json);
    }

    private void checkConformance(DocumentContext json) {
        assertEquals(2, (int) json.read("$.length()", Integer.class));
        List<String> classes = json.read("$.conformsTo");
        assertThat(
                classes,
                hasItems(
                        ConformanceClass.CORE,
                        ConformanceClass.COLLECTIONS,
                        MapsConformance.CORE.getId(),
                        MapsConformance.COLLECTION_MAP.getId(),
                        MapsConformance.STYLED_MAP.getId(),
                        MapsConformance.HTML.getId(),
                        MapsConformance.API_OPERATIONS.getId(),
                        MapsConformance.PNG.getId(),
                        MapsConformance.JPEG.getId(),
                        MapsConformance.SPATIAL_SUBSETTING.getId(),
                        MapsConformance.SCALING.getId(),
                        MapsConformance.DISPLAY_RESOLUTION.getId(),
                        MapsConformance.DATETIME.getId(),
                        MapsConformance.CRS.getId(),
                        MapsConformance.BACKGROUND.getId(),
                        MapsConformance.ORIENTATION.getId(),
                        MapsConformance.FEATURE_INFO.getId()));
        // the pre-1.0.0 draft URIs must be gone
        assertThat(
                classes,
                not(hasItems(
                        "http://www.opengis.net/spec/ogcapi-maps-1/1.0/conf/geodata",
                        "http://www.opengis.net/spec/ogcapi-maps-2/1.0/conf/bbox",
                        "http://www.opengis.net/spec/ogcapi-maps-2/1.0/conf/crs")));
    }

    @Test
    public void testConformanceYaml() throws Exception {
        String yaml = getAsString("ogc/maps/v1/conformance/?f=application/yaml");
        checkConformance(convertYamlToJsonPath(yaml));
    }

    @Test
    public void testConformanceHTML() throws Exception {
        org.jsoup.nodes.Document document = getAsJSoup("ogc/maps/v1/conformance?f=text/html");
        assertEquals(
                "GeoServer OGC API Maps Conformance", document.select("#title").text());
        String content = document.select("#content").text();
        assertThat(content, containsString(MapsConformance.CORE.getId()));
        assertThat(content, containsString(MapsConformance.FEATURE_INFO.getId()));
    }

    @Test
    public void testReflectivePropertyResolution() {
        // hyphenated conformance ids must still resolve to their Java field through the reflective accessors
        MapsConformance conf = new MapsConformance();
        conf.setEnabled(MapsConformance.SPATIAL_SUBSETTING, Boolean.FALSE);
        conf.setEnabled(MapsConformance.DISPLAY_RESOLUTION, Boolean.TRUE);
        assertEquals(Boolean.FALSE, conf.isEnabled(MapsConformance.SPATIAL_SUBSETTING));
        assertEquals(Boolean.TRUE, conf.isEnabled(MapsConformance.DISPLAY_RESOLUTION));
    }

    @Test
    public void testFeatureInfoDisabled() throws Exception {
        withConformance(MapsConformance::setFeatureInfo, false, () -> {
            List<String> classes = getAsJSONPath("ogc/maps/v1/conformance", 200).read("$.conformsTo");
            assertThat(classes, not(hasItems(MapsConformance.FEATURE_INFO.getId())));
        });
    }
}
