/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest;

import static org.easymock.EasyMock.anyObject;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.junit.Assert.*;

import org.easymock.EasyMock;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class RESTDispatcherCallbackTest extends GeoServerSystemTestSupport {

    DispatcherCallback callback;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // no data needed for this test
    }

    @Before
    public void prepareCallback() throws Exception {
        callback = EasyMock.createMock(DispatcherCallback.class);
        GeoServerExtensionsHelper.init(applicationContext);
        GeoServerExtensionsHelper.singleton("testCallback", callback, DispatcherCallback.class);
    }

    @Test
    public void testCallback() throws Exception {
        callback.init(anyObject(), anyObject());
        expectLastCall();
        callback.dispatched(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.finished(anyObject(), anyObject());
        expectLastCall();
        replay(callback);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/index.html");
        assertEquals(200, response.getStatus());
        verify(callback);
    }

    @Test
    public void testCallbackException() throws Exception {
        callback.init(anyObject(), anyObject());
        expectLastCall();
        callback.dispatched(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.exception(anyObject(), anyObject(), anyObject());
        expectLastCall();
        callback.finished(anyObject(), anyObject());
        expectLastCall();
        replay(callback);

        getAsServletResponse(RestBaseController.ROOT_PATH + "/exception?code=400&message=error");
        verify(callback);
    }
}
