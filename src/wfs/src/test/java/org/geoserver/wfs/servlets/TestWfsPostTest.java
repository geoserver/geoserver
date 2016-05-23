/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.servlets;

import static org.junit.Assert.assertTrue;

import org.geoserver.ows.util.ResponseUtils;
import org.junit.Test;
import org.vfny.geoserver.wfs.servlets.TestWfsPost;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class TestWfsPostTest {

    @Test
    public void testEscapeXMLReservedChars() throws Exception {
        TestWfsPost servlet = new TestWfsPost();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setScheme("http");
        request.setServerName("localhost");
        request.setServerPort(8080);
        request.setContextPath("/geoserver/TestWfsPost");
        request.setRequestURI(ResponseUtils.stripQueryString(ResponseUtils.appendPath(
                    "/geoserver/TestWfsPost")));
        request.setQueryString(ResponseUtils.getQueryString("form_hf_0=&url=vjoce<>:garbage"));
        request.setRemoteAddr("127.0.0.1");
        request.addHeader("Host", "localhost:8080");
        request.setupAddParameter("url", "vjoce<>:garbage");
        request.setMethod("GET");
        MockHttpServletResponse response = new MockHttpServletResponse();
        servlet.service(request, response);
        // System.out.println(response.getContentAsString());
        // check xml chars have been escaped
        assertTrue(response.getOutputStreamContent().contains("java.net.MalformedURLException: no protocol: vjoce&lt;&gt;:garbage"));
    }
}
