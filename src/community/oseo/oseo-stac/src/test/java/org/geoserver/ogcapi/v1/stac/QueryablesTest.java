/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.PathNotFoundException;
import java.util.Arrays;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.ogcapi.APIException;
import org.geoserver.opensearch.eo.OSEOInfo;
import org.jsoup.nodes.Document;
import org.junit.Test;

public class QueryablesTest extends STACTestSupport {

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        copyTemplate("/mandatoryLinks.json");
        copyTemplate("/items-LANDSAT8.json");
        copyTemplate("/items-SENTINEL2.json");
        // these 3 needed for SAS1 to work
        copyTemplate("/items-SAS1.json");
        copyTemplate("/box.json");
        copyTemplate("/parentLink.json");
    }

    @Test
    public void testSearchQueryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/queryables", 200);
        assertEquals("http://localhost:8080/geoserver/ogc/stac/v1/queryables", json.read("$.$id"));
        checkQueryableProperties(json);
    }

    @Test
    public void testCollectionQueryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/queryables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/queryables",
                json.read("$.$id"));

        // check a couple properties, more in depth tests are found in STACQueryablesBuilderTest
        checkQueryableProperties(json);

        // custom queryable declared in test data, but not found in the templates, not in the output
        DocumentContext properties = readContext(json, "properties");
        assertThrows(PathNotFoundException.class, () -> properties.read("mean_solar_azimuth"));

        // custom one that is there instead
        DocumentContext cc = readContext(properties, "eo:cloud_cover");
        assertEquals("integer", cc.read("type"));
        assertEquals("integer", cc.read("description"));

        // top level queryable
        DocumentContext kw = readContext(properties, "keywords");
        assertEquals("string", kw.read("type"));
        assertEquals("string", kw.read("description"));
    }

    /**
     * Same as above, but with global queriables configured as well
     *
     * @throws Exception
     */
    @Test
    public void testCollectionQueryablesWithGlobals() throws Exception {
        GeoServer gs = getGeoServer();
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        oseo.getGlobalQueryables().addAll(Arrays.asList("constellation", "view:sun_azimuth"));
        gs.save(oseo);

        try {
            DocumentContext json =
                    getAsJSONPath("ogc/stac/v1/collections/SENTINEL2/queryables", 200);
            assertEquals(
                    "http://localhost:8080/geoserver/ogc/stac/v1/collections/SENTINEL2/queryables",
                    json.read("$.$id"));

            // check a couple properties, more in depth tests are found in STACQueryablesBuilderTest
            checkQueryableProperties(json);

            // custom queryable declared in test data, but not found in the templates, not in the
            // output
            DocumentContext properties = readContext(json, "properties");
            assertThrows(PathNotFoundException.class, () -> properties.read("mean_solar_azimuth"));

            // custom one that is there instead
            DocumentContext cc = readContext(properties, "eo:cloud_cover");
            assertEquals("integer", cc.read("type"));
            assertEquals("integer", cc.read("description"));

            // check the custom global queryables as well
            DocumentContext constellation = readContext(json, "properties.constellation");
            assertEquals("string", constellation.read("type"));
            assertEquals("string", constellation.read("description"));

            DocumentContext sun_azimuth = readContext(json, "properties.view:sun_azimuth");
            assertEquals("number", sun_azimuth.read("type"));
            assertEquals("number", sun_azimuth.read("description"));
        } finally {
            oseo.getGlobalQueryables().clear();
            gs.save(oseo);
        }
    }

    @Test
    public void testCollectionNotExists() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/NOT_THERE/queryables", 404);
        assertEquals(APIException.NOT_FOUND, json.read("code"));
        assertEquals("Collection not found: NOT_THERE", json.read("description"));
    }

    @Test
    public void testSearchQueryablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/v1/queryables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testCollectionQueryablesHTML() throws Exception {
        Document html = getAsJSoup("ogc/stac/v1/collections/SENTINEL2/queryables?f=html");
        // TODO: add checks, for now it just verifies the template is producing HTML
    }

    @Test
    public void testLandsat8Queryables() throws Exception {
        DocumentContext json = getAsJSONPath("ogc/stac/v1/collections/LANDSAT8/queryables", 200);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/stac/v1/collections/LANDSAT8/queryables",
                json.read("$.$id"));

        // checks the common properties
        checkQueryableProperties(json);

        // check the one custom queryable added in LANDSAT8 template
        DocumentContext orbit = readContext(json, "properties.landsat:orbit");
        assertEquals("integer", orbit.read("type"));
        assertEquals("integer", orbit.read("description"));
    }

    @Test
    public void testCustomGlobalQueryables() throws Exception {
        GeoServer gs = getGeoServer();
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        oseo.getGlobalQueryables().addAll(Arrays.asList("constellation", "view:sun_azimuth"));
        gs.save(oseo);

        try {
            DocumentContext json = getAsJSONPath("ogc/stac/v1/queryables", 200);
            checkQueryableProperties(json);

            // check the custom queryables added above
            DocumentContext constellation = readContext(json, "properties.constellation");
            assertEquals("string", constellation.read("type"));
            assertEquals("string", constellation.read("description"));

            DocumentContext sun_azimuth = readContext(json, "properties.view:sun_azimuth");
            assertEquals("number", sun_azimuth.read("type"));
            assertEquals("number", sun_azimuth.read("description"));
        } finally {
            oseo.getGlobalQueryables().clear();
            gs.save(oseo);
        }
    }

    @Test
    public void testCustomCollectionQueryables() throws Exception {
        GeoServer gs = getGeoServer();
        OSEOInfo oseo = gs.getService(OSEOInfo.class);
        oseo.getGlobalQueryables().addAll(Arrays.asList("constellation", "view:sun_azimuth"));
        gs.save(oseo);

        try {
            DocumentContext json = getAsJSONPath("ogc/stac/v1/queryables", 200);
            checkQueryableProperties(json);

            // check the custom queryables added above
            DocumentContext constellation = readContext(json, "properties.constellation");
            assertEquals("string", constellation.read("type"));
            assertEquals("string", constellation.read("description"));

            DocumentContext sun_azimuth = readContext(json, "properties.view:sun_azimuth");
            assertEquals("number", sun_azimuth.read("type"));
            assertEquals("number", sun_azimuth.read("description"));
        } finally {
            oseo.getGlobalQueryables().clear();
            gs.save(oseo);
        }
    }

    private void checkQueryableProperties(DocumentContext json) {
        // check a couple properties, more in depth tests are found in STACQueryablesBuilderTest
        assertEquals(
                STACQueryablesBuilder.GEOMETRY_SCHEMA_REF, json.read("properties.geometry.$ref"));
        assertEquals(
                STACQueryablesBuilder.DATETIME_SCHEMA_REF, json.read("properties.datetime.$ref"));
    }
}
