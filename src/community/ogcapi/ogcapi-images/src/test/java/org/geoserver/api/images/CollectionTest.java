/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api.images;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import com.jayway.jsonpath.DocumentContext;
import java.util.List;
import org.hamcrest.Matchers;
import org.junit.Test;

public class CollectionTest extends ImagesTestSupport {

    @Test
    public void testWaterTempCollectionJson() throws Exception {
        String waterTemp = getLayerId(WATER_TEMP);
        DocumentContext json = getAsJSONPath("ogc/images/collections/" + waterTemp, 200);

        testWaterTempCollectionJson(json);
    }

    @Test
    public void testWaterTempCollectionHTML() throws Exception {
        String waterTemp = getLayerId(WATER_TEMP);
        getAsJSoup("ogc/images/collections/" + waterTemp + "?f=text/html");
        // TODO: add ids in the elemnets and check contents using jSoup
    }

    public void testWaterTempCollectionJson(DocumentContext json) {
        assertEquals("sf:watertemp", json.read("$.id", String.class));
        assertEquals("Water temperature", json.read("$.title", String.class));
        assertEquals(0.23722069, json.read("$.extent.spatial.bbox[0][0]", Double.class), 1e-6d);
        assertEquals(40.56208, json.read("$.extent.spatial.bbox[0][1]", Double.class), 1e-6d);
        assertEquals(14.592757, json.read("$.extent.spatial.bbox[0][2]", Double.class), 1e-6d);
        assertEquals(44.558083, json.read("$.extent.spatial.bbox[0][3]", Double.class), 1e-6d);
        assertEquals(
                "http://www.opengis.net/def/crs/OGC/1.3/CRS84",
                json.read("$.extent.spatial.crs", String.class));

        // check the images link
        List<String> items = json.read("$.links[?(@.rel=='images')].href");
        assertThat(items.size(), Matchers.greaterThanOrEqualTo(2));
        assertThat(
                items,
                hasItems(
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images?f=application%2Fstac%2Bjson",
                        "http://localhost:8080/geoserver/ogc/images/collections/sf%3Awatertemp/images?f=text%2Fhtml"));
    }

    @Test
    public void testImagesCollectionYaml() throws Exception {
        String waterTemp = getLayerId(WATER_TEMP);
        String yaml = getAsString("ogc/images/collections/" + waterTemp + "?f=application/x-yaml");
        DocumentContext json = convertYamlToJsonPath(yaml);
        testWaterTempCollectionJson(json);
    }

    @Test
    public void testWorkspacedWaterTempCollectionJson() throws Exception {
        DocumentContext json =
                getAsJSONPath(
                        "gs/ogc/images/collections/" + WATER_TEMP_DEFAULT.getLocalPart(), 200);

        // check the images link
        List<String> items = json.read("$.links[?(@.rel=='images')].href");
        assertThat(items.size(), Matchers.greaterThanOrEqualTo(2));
        assertThat(
                items,
                hasItems(
                        "http://localhost:8080/geoserver/gs/ogc/images/collections/watertemp/images?f=application%2Fstac%2Bjson",
                        "http://localhost:8080/geoserver/gs/ogc/images/collections/watertemp/images?f=text%2Fhtml"));
    }
}
