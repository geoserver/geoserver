package org.opengeo.gsr.resource;

import static org.junit.Assert.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.junit.Test;
import org.opengeo.gsr.JsonSchemaTest;

public class LegendResourceTest extends ResourceTest {
    @Test
    public void testStreamsLegend() throws Exception {
        String result = getAsString(baseURL + "cite/MapServer/legend?f=json");
        fail(result);
    }
}
