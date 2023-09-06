package org.geoserver.featurestemplating.expressions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geotools.api.filter.FilterFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public class RequestFunctionsTest {

    static final FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    @BeforeClass
    public static void setDispatcherRequest() {
        MockHttpServletRequest req = new MockHttpServletRequest();
        req.setScheme("http");
        req.setServerPort(8080);
        req.setContextPath("/geoserver");
        req.setPathInfo("/wfs");
        req.addParameter("testParameter", "testParameterValue");
        req.addHeader("testHeader", "testHeaderValue");
        Request request = new Request();
        request.setOutputFormat("application/json");
        request.setHttpRequest(req);
        Dispatcher.REQUEST.set(request);
    }

    @Test
    public void testRequestParamFunction() {
        String result =
                (String)
                        FF.function("requestParam", FF.literal("testParameter"))
                                .evaluate(Dispatcher.REQUEST.get());
        assertEquals("testParameterValue", result);
    }

    @Test
    public void testRequestHeaderFunction() {
        String result =
                (String)
                        FF.function("header", FF.literal("testHeader"))
                                .evaluate(Dispatcher.REQUEST.get());
        assertEquals("testHeaderValue", result);
    }

    @Test
    public void testMimeTypeFunction() {
        String result = (String) FF.function("mimeType").evaluate(Dispatcher.REQUEST.get());
        assertEquals("application/json", result);
    }

    @Test
    public void testRequestRegexMatch() {
        Boolean result =
                (Boolean)
                        FF.function("requestMatchRegex", FF.literal("^.*wfs.*$"))
                                .evaluate(Dispatcher.REQUEST.get());
        assertTrue(result.booleanValue());
        result =
                (Boolean)
                        FF.function("requestMatchRegex", FF.literal("^.*wms.*$"))
                                .evaluate(Dispatcher.REQUEST.get());
        assertFalse(result.booleanValue());
    }
}
