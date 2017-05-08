/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.*;

import java.util.Collections;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.community.mbstyle.MBStyleHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;

public class MBStyleControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;

    private static SimpleNamespaceContext namespaceContext;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        catalog = getCatalog();

        namespaceContext = new org.springframework.util.xml.SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("sld", "http://www.opengis.net/sld");
        namespaceContext.bindNamespaceUri("ogc", "http://www.opengis.net/ogc");

        testData.addStyle(catalog.getDefaultWorkspace(), "teststyle", "teststyle.json",
                MBStyleControllerTest.class, catalog,
                Collections.singletonMap(StyleProperty.FORMAT, MBStyleHandler.FORMAT));
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void getGetBodyAsJsonUsingAcceptHeader() throws Exception {
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/styles/teststyle");
        request.setMethod("GET");
        request.addHeader("Accept", MBStyleHandler.MIME_TYPE);
        MockHttpServletResponse response = dispatch(request);

        assertEquals(200, response.getStatus());
        assertEquals(MBStyleHandler.MIME_TYPE, response.getContentType());
        String responseContent = response.getContentAsString();
        String expected = IOUtils.toString(this.getClass().getResourceAsStream("teststyle.json"));
        assertEquals(expected, responseContent);
    }

    @Test
    public void getGetAsJsonUsingExtension() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/styles/teststyle.json");
        assertEquals(200, response.getStatus());
        assertEquals("application/json", response.getContentType());
        String responseContent = response.getContentAsString();
        assertEquals("{\"style\":{\"name\":\"teststyle\"," 
                               + "\"workspace\":{\"name\":\"gs\"},"
                               + "\"format\":\"json\"," + "\"languageVersion\":{\"version\":\"1.0.0\"},"
                               + "\"filename\":\"teststyle.json\"}}", responseContent);
    }

    @Test
    public void getGetAsSLDUsingAcceptHeader() throws Exception {
        MockHttpServletRequest request = createRequest(
                RestBaseController.ROOT_PATH + "/styles/teststyle");
        request.setMethod("GET");
        request.addHeader("Accept", SLDHandler.MIMETYPE_10);
        MockHttpServletResponse response = dispatch(request);

        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains("<sld:Name>test-layer</sld:Name>"));
    }

    @Test
    public void getGetAsSLDUsingExtension() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/styles/teststyle.sld");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains("<sld:Name>test-layer</sld:Name>"));
    }

    @Test
    public void getGetAsHTML() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(
                RestBaseController.ROOT_PATH + "/styles/teststyle.html");
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_VALUE, response.getContentType());
        String content = response.getContentAsString();
        assertTrue(content.contains(
                "<a href=\"http://localhost:8080/geoserver/rest/styles/teststyle.json\">teststyle.json</a>"));
    }

}
