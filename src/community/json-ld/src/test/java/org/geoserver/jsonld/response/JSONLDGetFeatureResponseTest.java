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
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class JSONLDGetFeatureResponseTest extends AbstractAppSchemaTestSupport {
    Catalog catalog;
    FeatureTypeInfo typeInfo;
    FeatureTypeInfo typeInfo2;
    GeoServerDataDirectory dd;

    @Before
    public void before() {
        catalog = getCatalog();

        typeInfo = catalog.getFeatureTypeByName("st_gml31", "Station_gml31");
        typeInfo2 = catalog.getFeatureTypeByName("st_gml32", "Station_gml32");
        dd = (GeoServerDataDirectory) applicationContext.getBean("dataDirectory");
    }

    public void setUp() throws IOException {

        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/st_gml31/"
                                        + typeInfo.getStore().getName()
                                        + "/"
                                        + typeInfo.getName(),
                                typeInfo.getName() + ".json");
        dd.getResourceLoader().copyFromClassPath("Station_gml31.json", file, getClass());
    }

    public void setUpInvalid() throws IOException {

        File file =
                dd.getResourceLoader()
                        .createFile(
                                "workspaces/st_gml32/"
                                        + typeInfo2.getStore().getName()
                                        + "/"
                                        + typeInfo2.getName(),
                                typeInfo2.getName() + ".json");
        dd.getResourceLoader().copyFromClassPath("Station_gml32_invalid.json", file, getClass());
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

    @Test
    public void testJsonLdResponseWithoutTemplate() throws Exception {
        setUp();
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=ms_gml31:Measurement_gml31&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(
                response.getContentAsString()
                        .contains("No Json-Ld template found for feature type"));
    }

    @Test
    public void testJsonLdQueryWithGET() throws Exception {
        setUp();
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=st_gml31:Station_gml31&outputFormat=")
                        .append("application%2Fld%2Bjson")
                        .append("&cql_filter=features.name IS NULL");
        JSONObject result = (JSONObject) getJsonLd(sb.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.isEmpty());
    }

    @Test
    public void testJsonLdQueryWithPOST() throws Exception {
        setUp();
        StringBuilder xml =
                new StringBuilder("<wfs:GetFeature ")
                        .append(" service=\"WFS\" ")
                        .append(" outputFormat=\"application/ld+json\" ")
                        .append(" version=\"1.0.0\" ")
                        .append(" xmlns:st_gml31=\"http://www.stations_gml31.org/1.0\" ")
                        .append(" xmlns:wfs=\"http://www.opengis.net/wfs\" ")
                        .append(" xmlns:ogc=\"http://www.opengis.net/ogc\" ")
                        .append(">")
                        .append(" <wfs:Query typeName=\"st_gml31:Station_gml31\">")
                        .append(" <ogc:Filter><ogc:PropertyIsNull> ")
                        .append("<ogc:PropertyName>features.name</ogc:PropertyName>")
                        .append("</ogc:PropertyIsNull></ogc:Filter></wfs:Query>")
                        .append("</wfs:GetFeature>");
        JSONObject result = (JSONObject) postJsonLd(xml.toString());
        JSONObject context = (JSONObject) result.get("@context");
        assertNotNull(context);
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.isEmpty());
    }

    @Test
    public void testInvalidTemplateResponse() throws Exception {
        setUpInvalid();
        StringBuilder sb = new StringBuilder("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=st_gml32:Station_gml32&outputFormat=");
        sb.append("application%2Fld%2Bjson");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertTrue(response.getContentAsString().contains("Failed to validate json-ld template"));
    }

    protected JSON getJsonLd(String path) throws Exception {
        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    protected JSON postJsonLd(String xml) throws Exception {
        MockHttpServletResponse response = postAsServletResponse("wfs", xml);
        assertEquals(response.getContentType(), "application/ld+json");
        return json(response);
    }

    @Override
    protected AbstractAppSchemaMockData createTestData() {
        return new StationsMockData();
    }

    @After
    public void cleanup() {
        dd.getResourceLoader()
                .get(
                        "workspaces/st_gml31/"
                                + typeInfo.getStore().getName()
                                + "/"
                                + typeInfo.getName()
                                + "/"
                                + typeInfo.getName()
                                + ".json")
                .delete();
        dd.getResourceLoader()
                .get(
                        "workspaces/st_gml32/"
                                + typeInfo2.getStore().getName()
                                + "/"
                                + typeInfo2.getName()
                                + "/"
                                + typeInfo2.getName()
                                + ".json")
                .delete();
        dd = null;
        catalog = null;
        typeInfo = null;
    }
}
