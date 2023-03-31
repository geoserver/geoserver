package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;

import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class HTMLSimpleFeaturesResponseAPITest extends GeoServerSystemTestSupport {

    @Test
    public void testNotOverrideHTMLResponseOnNullTemplate() throws Exception {
        StringBuilder sb =
                new StringBuilder("ogc/features/v1/collections/")
                        .append("cite:NamedPlaces")
                        .append("/items?f=text/html");
        MockHttpServletResponse response = getAsServletResponse(sb.toString());
        assertEquals(200, response.getStatus());
    }
}
