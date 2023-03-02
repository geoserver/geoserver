/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class SortablesTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
    }

    @Test
    public void testSearchSortables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/sortables", 200);
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/sortables", json.read("$.$id"));
        checkSortableProperties(json);
    }

    @Test
    public void testCollectionSortables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/sortables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/sortables",
                json.read("$.$id"));

        // check a couple properties, more in depth tests are found in STACSortableMapperTest
        checkSortableProperties(json);
    }

    @Test
    public void testCollectionNotExists() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/NOT_THERE/sortables", 404);
        assertEquals(APIException.NOT_FOUND, json.read("code"));
        assertEquals("Collection not found: NOT_THERE", json.read("description"));
    }

    @Test
    public void testSearchSortablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/v1/sortables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testCollectionSortablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/v1/collections/SENTINEL2/sortables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testLandsat8Sortables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/sortables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/LANDSAT8/sortables",
                json.read("$.$id"));

        // checks the common properties
        checkSortableProperties(json);

        // check the one custom sortables added in LANDSAT8 template
        DocumentContext orbit = readContext(json, "properties.landsat:orbit");
        assertEquals("integer", orbit.read("type"));
        assertEquals("integer", orbit.read("description"));
    }

    private void checkSortableProperties(DocumentContext json) {
        // check a couple properties, more in depth tests are found in STACSortablesMapperTest
        assertEquals("string", json.read("properties.id.type"));
        assertEquals("string", json.read("properties.datetime.type"));
        assertEquals("date-time", json.read("properties.datetime.format"));
    }
}
