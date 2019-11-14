/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jsonld.response;

import static org.junit.Assert.*;

import java.io.*;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.test.*;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDGetFeatureResponseTest extends AbstractAppSchemaTestSupport {
    Catalog catalog;
    FeatureTypeInfo typeInfo;

    private void setUp() throws IOException {
        catalog = getCatalog();

        typeInfo = catalog.getFeatureTypeByName("st_gml31", "Station_gml31");
        GeoServerDataDirectory dd =
                (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");
        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/st_gml31/"
                                        + typeInfo.getStore().getName()
                                        + "/"
                                        + typeInfo.getName(),
                                typeInfo.getName() + ".json");
        dd.getResourceLoader()
                .copyFromClassPath("../configuration/Station_gml31.json", file, getClass());
    }

    @Test
    public void testJsonLdResponse() throws Exception {
        setUp();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=st_gml31:Station_gml31&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals(feature.get("@id"), "st.1");
        assertEquals(feature.get("name"), "station1");
        JSONObject geom = (JSONObject) feature.get("geometry");
        assertEquals(geom.get("@type"), "Point");
        assertEquals(geom.get("wkt"), "POINT (1 -1)");
        JSONArray measurements = (JSONArray) feature.get("st_gml31:measurements");
        JSONObject first = (JSONObject) measurements.get(0);
        JSONObject second = (JSONObject) measurements.get(1);
        assertEquals(first.get("name"), "temperature");
        assertEquals(second.get("name"), "wind");
        JSONObject firstGeom = (JSONObject) first.get("stillThePoint");
        JSONObject secondGeom = (JSONObject) second.get("stillThePoint");
        assertEquals(firstGeom.get("@type"), "Point");
        assertEquals(firstGeom.get("wkt"), "POINT (1 -1)");
        assertEquals(secondGeom.get("@type"), "Point");
        assertEquals(secondGeom.get("wkt"), "POINT (1 -1)");
    }

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new StationsMockData();
    }
}
