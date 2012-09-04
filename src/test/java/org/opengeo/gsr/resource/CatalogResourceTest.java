package org.opengeo.gsr.resource;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.LayerGroupInfo;

public class CatalogResourceTest extends ResourceTest {

    public void testServiceException() throws Exception {
        if (baseURL != null) {
            JSON json = getAsJSON(baseURL + "?f=xxx");
            assertTrue(json instanceof JSONObject);
            JSONObject jsonObject = (JSONObject) json;
            JSONObject error = (JSONObject) jsonObject.get("error");
            assertTrue(error instanceof JSONObject);
            int code = (Integer) error.get("code");
            assertEquals(400, code);
            String message = (String) error.get("message");
            assertEquals("Output format not supported", message);
            JSONArray details = (JSONArray) error.get("details");
            assertTrue(details instanceof JSONArray);
            assertEquals("Format xxx is not supported", details.getString(0));
        }
    }

    /**
     * A.1.1 catalog/request, catalog/parameters
     * @throws Exception
     */
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

    public void testCatalogResponseSchema() throws Exception {
        JSON json = getAsJSON(baseURL + "?f=json");
        assertTrue(json instanceof JSONObject);
        String jsonString = json.toString();
        System.out.println(jsonString);
        assertTrue(validateJSON(jsonString, "gsr-cs/1.0/catalog.json"));
    }
    
    // TODO: Set up ResourceTest to include Virtual Services and test that folders contain those virtual services
    // and that they validate as Catalog endpoints

    /**
     * A.1.2 catalog/uniqueServiceNames
     */
    public void testUniqueServiceNames() {
        boolean thrown = false;
        try {
            CatalogFactory catalogFactory = catalog.getFactory();
            LayerGroupInfo layerGroup = catalogFactory.createLayerGroup();
            layerGroup.setName(catalog.getLayerGroups().get(0).getName());
            catalog.add(layerGroup);
        } catch (IllegalArgumentException e) {
            thrown = true;
        }
        assertTrue(thrown);
    }
    
    
}
