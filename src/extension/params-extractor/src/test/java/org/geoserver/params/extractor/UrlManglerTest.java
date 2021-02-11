/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.params.extractor;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import javax.servlet.http.HttpServletRequestWrapper;
import org.geoserver.config.GeoServerDataDirectory;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.Request;
import org.geoserver.platform.GeoServerExtensions;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;

public final class UrlManglerTest extends TestSupport {
    protected static GeoServerDataDirectory getDataDirectory() {
        return (GeoServerDataDirectory) GeoServerExtensions.bean("dataDirectory");
    }

    @Override
    public void voidClean() throws IOException {
        super.voidClean();
        Dispatcher.REQUEST.remove();
    }

    @Test
    public void testSimpleWrappedRequest() {

        Map<String, String[]> params = new HashMap<String, String[]>();

        MockHttpServletRequest httpRequest =
                new MockHttpServletRequest(
                        "GET", "http://127.0.0.1/geoserver/it.geosolutions/param/wms");
        UrlTransform transform = new UrlTransform("/geoserver/workspace/param/wms", params);
        transform.removeMatch("/param");
        RequestWrapper wrapper = new RequestWrapper(transform, httpRequest);
        Request request = new Request();
        request.setHttpRequest(wrapper);
        request.setRequest("GetCapabilities");

        Dispatcher.REQUEST.set(request);
        UrlMangler mangler = new UrlMangler(getDataDirectory());
        StringBuilder path = new StringBuilder("/geoserver/workspace/param/wms");
        Map<String, String> kvp = new HashMap<String, String>();
        mangler.mangleURL(new StringBuilder(), path, kvp, null);
        assertEquals("workspace/param/wms", path.toString());
    }

    @Test
    public void testDoubleWrappedRequest() {

        Map<String, String[]> params = new HashMap<String, String[]>();

        MockHttpServletRequest httpRequest =
                new MockHttpServletRequest(
                        "GET", "http://127.0.0.1/geoserver/it.geosolutions/param/wms");
        UrlTransform transform = new UrlTransform("/geoserver/workspace/param/wms", params);
        transform.removeMatch("/param");
        RequestWrapper wrapper = new RequestWrapper(transform, httpRequest);
        HttpServletRequestWrapper secondWrapper = new HttpServletRequestWrapper(wrapper);
        Request request = new Request();
        request.setHttpRequest(secondWrapper);
        request.setRequest("GetCapabilities");

        Dispatcher.REQUEST.set(request);
        UrlMangler mangler = new UrlMangler(getDataDirectory());
        StringBuilder path = new StringBuilder("/geoserver/workspace/param/wms");
        Map<String, String> kvp = new HashMap<String, String>();
        mangler.mangleURL(new StringBuilder(), path, kvp, null);
        assertEquals("workspace/param/wms", path.toString());
    }
}
