package org.opengeo.gsr.resource;

import static org.junit.Assert.fail;

import org.junit.Ignore;

public class LegendResourceTest extends ResourceTest {
    @Ignore
    public void testStreamsLegend() throws Exception {
        String result = getAsString(baseURL + "cite/MapServer/legend?f=json");
        fail(result);
    }
}
