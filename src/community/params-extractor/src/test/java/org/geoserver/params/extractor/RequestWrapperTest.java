package org.geoserver.params.extractor;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

/** Tests for {@link RequestWrapper} methods. */
public class RequestWrapperTest {

    /** Tests getServletPath correct value. */
    @Test
    public void testServletPath() {
        UrlTransform urlTransform =
                new UrlTransform("/geoserver/it.geosolutions/wms/what", buildParameters());
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET", "http://127.0.0.1/geoserver/it.geosolutions/wms/what");
        RequestWrapper requestWrapper = new RequestWrapper(urlTransform, request);
        assertEquals("/geoserver", requestWrapper.getServletPath());
    }

    /** Tests getServletPath correct value. */
    @Test
    public void testServletPathWithQueryParams() {
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/it.geosolutions/wms/what?parameter=value", buildParameters());
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "http://127.0.0.1/geoserver/it.geosolutions/wms/what?parameter=value");
        RequestWrapper requestWrapper = new RequestWrapper(urlTransform, request);
        assertEquals("/geoserver", requestWrapper.getServletPath());
    }

    /** Tests getPathInfo correct value. */
    @Test
    public void testPathInfo() {
        UrlTransform urlTransform =
                new UrlTransform("/geoserver/it.geosolutions/wms/what", buildParameters());
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET", "http://127.0.0.1/geoserver/it.geosolutions/wms/what");
        RequestWrapper requestWrapper = new RequestWrapper(urlTransform, request);
        assertEquals("/it.geosolutions/wms/what", requestWrapper.getPathInfo());
    }

    /** Tests getPathInfo correct value. */
    @Test
    public void testPathInfoWithQueryParams() {
        UrlTransform urlTransform =
                new UrlTransform(
                        "/geoserver/it.geosolutions/wms/what/?parameter=value", buildParameters());
        MockHttpServletRequest request =
                new MockHttpServletRequest(
                        "GET",
                        "http://127.0.0.1/geoserver/it.geosolutions/wms/what/?parameter=value");
        RequestWrapper requestWrapper = new RequestWrapper(urlTransform, request);
        assertEquals("/it.geosolutions/wms/what", requestWrapper.getPathInfo());
    }

    private Map<String, String[]> buildParameters() {
        Map<String, String[]> params = new HashMap<>();
        params.put("test", new String[] {"1", "2"});
        return params;
    }
}
