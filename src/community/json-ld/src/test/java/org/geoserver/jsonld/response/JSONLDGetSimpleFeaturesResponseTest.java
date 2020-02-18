/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.response;

import static org.junit.Assert.*;

import java.io.File;
import java.io.IOException;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.data.test.MockData;
import org.geoserver.jsonld.configuration.JsonLdConfiguration;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDGetSimpleFeaturesResponseTest extends GeoServerSystemTestSupport {
    Catalog catalog;
    FeatureTypeInfo typeInfo;
    GeoServerDataDirectory dd;

    @Before
    public void before() throws IOException {
        catalog = getCatalog();
        typeInfo =
                catalog.getFeatureTypeByName(
                        MockData.CITE_PREFIX, MockData.NAMED_PLACES.getLocalPart());
        dd = (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");
        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/cite/"
                                        + typeInfo.getStore().getName()
                                        + "/"
                                        + typeInfo.getName(),
                                JsonLdConfiguration.JSON_LD_NAME);
        dd.getResourceLoader().copyFromClassPath("NamedPlaces.json", file, getClass());
    }

    @Test
    public void testJsonLdResponse() throws Exception {
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=cite:NamedPlaces&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 2);
        checkFeature((JSONObject) features.get(0));
    }

    @Test
    public void testJsonLdResponseOGCAPI() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 2);
        checkFeature((JSONObject) features.get(0));
    }

    @Test
    public void testJsonLdQueryPointingToExpr() throws Exception {
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter= features.geometry.wkt IS NULL ");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 0);
    }

    @Test
    public void testJsonLdResponseWithFilter() throws Exception {
        StringBuilder path =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=cite:NamedPlaces")
                        .append("&outputFormat=application%2Fld%2Bjson")
                        .append("&cql_filter= features.id = '118'");
        JSONObject result = (JSONObject) getJsonLd(path.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(features.size(), 1);
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals(feature.getString("id"), "118");
        assertNotNull(feature.getString("name"), "Goose Island");
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("@type"), "MultiPolygon");
        assertNotNull(geometry.getString("wkt"));
    }

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    @After
    public void cleanup() {
        dd.getResourceLoader()
                .get(
                        "workspaces/cite/"
                                + typeInfo.getStore().getName()
                                + "/"
                                + typeInfo.getName()
                                + "/"
                                + JsonLdConfiguration.JSON_LD_NAME)
                .delete();
    }

    private void checkFeature(JSONObject feature) {
        assertNotNull(feature.getString("id"));
        assertNotNull(feature.getString("name"));
        JSONObject geometry = (JSONObject) feature.get("geometry");
        assertEquals(geometry.getString("@type"), "MultiPolygon");
        assertNotNull(geometry.getString("wkt"));
    }
}
