package org.opengeo.gsr.resource;

public class QueryResourceTest extends ResourceTest {
    private final String query(String params) {
        return baseURL + "cite/MapServer/Buildings/query" + params;
    }
    
    public void testServiceException() throws Exception {
        fail(getAsString(query("?f=xxx")));
//        JSON json = getAsJSON(query("?f=xxx"));
//        assertTrue(json instanceof JSONObject);
//        JSONObject jsonObject = (JSONObject) json;
//        JSONObject error = (JSONObject) jsonObject.get("error");
//        assertTrue(error instanceof JSONObject);
//        int code = (Integer) error.get("code");
//        assertEquals(400, code);
//        String message = (String) error.get("message");
//        assertEquals("Output format not supported", message);
//        JSONArray details = (JSONArray) error.get("details");
//        assertTrue(details instanceof JSONArray);
//        assertEquals("Format xxx is not supported", details.getString(0));
    }
}
