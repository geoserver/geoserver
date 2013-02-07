package org.opengeo.gsr.resource;

import org.opengeo.gsr.JsonSchemaTest;

import static org.junit.Assert.*;

public class MapRootResourceTest extends ResourceTest {
    private final String query(String service, String params) {
        return baseURL + service + "/MapServer" + params;
    }
    
    public void testBasicQuery() throws Exception {
        String result = getAsString(query("cite", "?f=json"));
        assertFalse(result.isEmpty());
        // TODO: Can't validate since ids are not integers.
         assertTrue("Root resource validates", JsonSchemaTest.validateJSON(result, "/gsr-ms/1.0/root.json"));
    }
}
