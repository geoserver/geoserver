/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.expressions;

import static org.junit.Assert.assertEquals;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Function;
import org.springframework.mock.web.MockHttpServletRequest;

public class ServiceLinkFunctionTest {

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @BeforeClass
    public static void setDispatcherRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerPort(8080);
        req.setContextPath("/geoserver");
        Request request = new Request();
        request.setHttpRequest(req);
        Dispatcher.REQUEST.set(request);
    }

    @AfterClass
    public static void clearDispatcherRequest() {
        Dispatcher.REQUEST.remove();
    }

    @Test
    public void testNoParams() {
        Function f = FF.function("serviceLink", FF.literal("oseo/search"));
        String url = f.evaluate(null, String.class);
        assertEquals("http://localhost:8080/geoserver/oseo/search", url);
    }

    @Test
    public void testPathParams() {
        Function f =
                FF.function(
                        "serviceLink",
                        FF.literal("ogc/features/collections/%s/items/%s"),
                        FF.literal("states"),
                        FF.literal("states.1"));
        String url = f.evaluate(null, String.class);
        assertEquals(
                "http://localhost:8080/geoserver/ogc/features/collections/states/items/states.1",
                url);
    }

    @Test
    public void testKvpParams() {
        Function f =
                FF.function(
                        "serviceLink",
                        FF.literal("ows?service=WFS&version=1.0.0&request=GetFeature&featureId=%s"),
                        FF.literal("myType.1"));
        String url = f.evaluate(null, String.class);
        assertEquals(
                "http://localhost:8080/geoserver/ows?service=WFS&version=1.0.0&request=GetFeature&featureId=myType.1",
                url);
    }
}
