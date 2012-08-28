//package org.opengeo.gsr.resource;
//
//import net.sf.json.JSON;
//import net.sf.json.JSONArray;
//import net.sf.json.JSONObject;
//
//import org.opengeo.gsr.service.ServiceType;
//
//public class CatalogResourceTest extends ResourceTest {
//
//    
//    public void testGetCatalogRootURI() throws Exception {
//        JSON json = getAsJSON("/gsr/services/?f=json");
//        assertTrue(json instanceof JSONObject);
//        JSONObject jsonObject = (JSONObject) json;
//        assertEquals("1.0", jsonObject.get("specVersion"));
//        assertEquals("OpenGeo Suite Enterprise Edition", jsonObject.get("productName"));
//        assertEquals("2.4.4", jsonObject.get("currentVersion"));
//        assertEquals(ServiceType.CatalogServer.toString(), jsonObject.get("type"));
//        JSONArray folders = (JSONArray) jsonObject.get("folders");
//        assertEquals(0, folders.size());
//        JSONArray services = (JSONArray) jsonObject.get("services");
//        assertEquals(1, services.size());
//    }
//    
////    @Override
////    public void testServiceException() throws Exception {
////        String baseURL = "/gsr/services/";
////        this.baseURL = baseURL;
////        super.testServiceException();
////    }
//}
