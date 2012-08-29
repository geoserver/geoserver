package org.opengeo.gsr.resource;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

public class CatalogResourceTest extends ResourceTest {

    public void testServiceException() throws Exception {
        if (baseURL != null) {
            JSON json = getAsJSON(baseURL + "?f=xxx");
            assertTrue(json instanceof JSONObject);
            JSONObject jsonObject = (JSONObject) json;
            JSONObject error = (JSONObject) jsonObject.get("error");
            assertTrue(error instanceof JSONObject);
            String code = (String) error.get("code");
            assertEquals("400", code);
            String message = (String) error.get("message");
            assertEquals("Output format not supported", message);
            JSONArray details = (JSONArray) error.get("details");
            assertTrue(details instanceof JSONArray);
            assertEquals("Format xxx is not supported", details.getString(0));
        }
    }

    public void testCatalogResponse() throws Exception {
        JSON json = getAsJSON(baseURL + "?f=json");
        assertTrue(json instanceof JSONObject);
        JSONObject jsonObject = (JSONObject) json;
        JSONArray services = (JSONArray) jsonObject.get("services");
        JSONObject mapService = services.getJSONObject(0);
        assertEquals("layerGroup1", mapService.get("name"));
        assertEquals("MapServer", mapService.get("type"));
        JSONObject geometryService = services.getJSONObject(1);
        assertEquals("Geometry", geometryService.get("name"));
        assertEquals("GeometryServer", geometryService.get("type"));
    }
}
