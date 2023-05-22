/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.proxybase.ext;

import static org.geoserver.ows.util.ResponseUtils.buildURL;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import org.geoserver.ows.URLMangler;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/** Integration test for {@link ProxyBaseExtUrlMangler} that tests the actual mangling of URLs in */
public class ProxyBaseExtensionIntegrationTest extends ProxyBaseExtensionTestSupport {
    private static final String BASEURL = "http://localhost:8080/";

    @Test
    public void testMangleWithHeader() throws Exception {
        MockHttpServletRequest request =
                createRequest("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader("fixedCollection", "exampleCollection");
        MockHttpServletResponse response = dispatch(request);
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        assertEquals(
                "https://wfs.example.com/exampleCollection?request=GetCapabilities",
                dom.getElementsByTagName("Get")
                        .item(0)
                        .getAttributes()
                        .getNamedItem("onlineResource")
                        .getNodeValue());
    }

    @Test
    public void testMangleWithNotEnoughHeaders() throws Exception {
        MockHttpServletRequest request =
                createRequest("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader("fixedCollection", "exampleCollection");
        MockHttpServletResponse response = dispatch(request);
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        Element root = dom.getDocumentElement();
        assertEquals(
                "http://www.opengis.net/wfs http://localhost:8080/geoserver/schemas/wfs/1.0.0/WFS-capabilities.xsd",
                root.getAttribute("xsi:schemaLocation"));
    }

    @Test
    public void testMangleWithEnoughHeaders() throws Exception {
        MockHttpServletRequest request =
                createRequest("wfs?service=WFS&version=1.0.0&request=getCapabilities");
        request.setMethod("GET");
        request.setContent(new byte[] {});
        request.addHeader("fixedCollection", "exampleCollection");
        request.addHeader("fixedFeature", "exampleFeature");
        MockHttpServletResponse response = dispatch(request);
        Document dom = dom(new ByteArrayInputStream(response.getContentAsString().getBytes()));
        Element root = dom.getDocumentElement();
        assertEquals(
                "http://www.opengis.net/wfs https://wfs.example.com/exampleCollection/exampleFeature",
                root.getAttribute("xsi:schemaLocation"));
    }

    @Test
    public void testBasic() {
        String url = buildURL(BASEURL, "geoserver/basic", null, URLMangler.URLType.SERVICE);
        assertEquals("https://basic.example.com/", url);
    }
}
