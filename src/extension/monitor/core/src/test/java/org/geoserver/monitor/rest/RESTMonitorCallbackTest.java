/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import static org.junit.Assert.assertEquals;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RESTMonitorCallbackTest extends GeoServerSystemTestSupport {

    RequestData data;

    @Before
    public void setUpListener() throws Exception {
        Monitor monitor = GeoServerExtensions.bean(Monitor.class);
        monitor.addRequestDataListener(new RequestDataListener() {
            @Override
            public void requestStarted(RequestData rd) {}

            @Override
            public void requestUpdated(RequestData rd) {}

            @Override
            public void requestCompleted(RequestData rd) {
                RESTMonitorCallbackTest.this.data = rd;
            }

            @Override
            public void requestPostProcessed(RequestData rd) {}
        });
    }

    @Test
    public void testURLEncodedRequestPathInfo() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/foo");
        assertEquals(404, response.getStatus());

        assertEquals("foo", data.getResources().get(1));

        response = getAsServletResponse(RestBaseController.ROOT_PATH + "/layers/acme:foo");
        assertEquals(404, response.getStatus());

        assertEquals("acme:foo", data.getResources().get(1));
    }
}
