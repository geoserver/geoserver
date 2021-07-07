/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.stac;

import static org.junit.Assert.assertEquals;

import com.jayway.jsonpath.DocumentContext;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.ServiceException;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class QueryablesTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
    }

    @Test
    public void testSearchQueryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/queryables", 200);
        assertEquals("http://localhost:8080/geoserver/ogc/stac/queryables", json.read("$.$id"));
        checkQueryableProperties(json);
    }

    @Test
    public void testCollectionQueryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/SENTINEL2/queryables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/SENTINEL2/queryables",
                json.read("$.$id"));

        // check a couple properties, more in depth tests are found in STACQueryablesBuilderTest
        checkQueryableProperties(json);
    }

    @Test
    public void testCollectionNotExists() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/NOT_THERE/queryables", 404);
        assertEquals(ServiceException.INVALID_PARAMETER_VALUE, json.read("code"));
        assertEquals("Collection not found: NOT_THERE", json.read("description"));
    }

    @Test
    public void testSearchQueryablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/queryables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testCollectionQueryablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/collections/SENTINEL2/queryables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testLandsat8Queryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/collections/LANDSAT8/queryables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/collections/LANDSAT8/queryables",
                json.read("$.$id"));

        // checks the common properties
        checkQueryableProperties(json);

        // check the one custom queryable added in LANDSAT8 template
        DocumentContext orbit = readContext(json, "properties.landsat:orbit");
        assertEquals("integer", orbit.read("type"));
        assertEquals("integer", orbit.read("description"));
    }

    private void checkQueryableProperties(DocumentContext json) {
        // check a couple properties, more in depth tests are found in STACQueryablesBuilderTest
        assertEquals(
                "https://geojson.org/schema/Polygon.json", json.read("properties.geometry.$ref"));

        DocumentContext datetime = readContext(json, "properties.datetime");
        assertEquals("string", datetime.read("type"));
        assertEquals("date-time", datetime.read("format"));
    }
}
