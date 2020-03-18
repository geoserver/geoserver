/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasXPath;
import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Collections;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.community.css.web.CssHandler;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.StyleProperty;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.util.xml.SimpleNamespaceContext;
import org.w3c.dom.Document;

public class CssStyleControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    private static SimpleNamespaceContext namespaceContext;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        catalog = getCatalog();

        namespaceContext = new org.springframework.util.xml.SimpleNamespaceContext();
        namespaceContext.bindNamespaceUri("sld", "http://www.opengis.net/sld");
        namespaceContext.bindNamespaceUri("ogc", "http://www.opengis.net/ogc");

        testData.addStyle(
                catalog.getDefaultWorkspace(),
                "test",
                "test.css",
                CssStyleControllerTest.class,
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, "css"));

        testData.addStyle(
                catalog.getDefaultWorkspace(),
                "multilayer",
                "multilayer.css",
                CssStyleControllerTest.class,
                catalog,
                Collections.singletonMap(StyleProperty.FORMAT, "css"));
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void getGetAsCSS() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + catalog.getDefaultWorkspace().getName()
                                + "/styles/test.css");
        assertEquals(200, response.getStatus());
        assertEquals(CssHandler.MIME_TYPE, response.getContentType());
        String content = response.getContentAsString();
        assertEquals("* {stroke: red}", content);
    }

    @Test
    public void getGetAsSLD10() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + catalog.getDefaultWorkspace().getName()
                                + "/styles/test.sld");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        assertThat(
                dom,
                hasXPath(
                        "//sld:LineSymbolizer/sld:Stroke/sld:CssParameter",
                        namespaceContext,
                        equalTo("#ff0000")));
    }

    @Test
    public void getGetMultiLayerAsSLD10() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + catalog.getDefaultWorkspace().getName()
                                + "/styles/multilayer.sld");
        assertEquals(200, response.getStatus());
        assertEquals(SLDHandler.MIMETYPE_10, response.getContentType());
        Document dom = dom(new ByteArrayInputStream(response.getContentAsByteArray()));
        // css generates a single UserStyle with multiple feature type styles
        assertThat(
                dom,
                hasXPath(
                        "//sld:FeatureTypeStyle[1]/sld:FeatureTypeName",
                        namespaceContext,
                        equalTo("states")));
        assertThat(
                dom,
                hasXPath(
                        "//sld:FeatureTypeStyle[2]/sld:FeatureTypeName",
                        namespaceContext,
                        equalTo("roads")));
    }

    @Test
    public void getGetAsHTML() throws Exception {
        MockHttpServletResponse response =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/workspaces/"
                                + catalog.getDefaultWorkspace().getName()
                                + "/styles/test.html");
        assertEquals(200, response.getStatus());
        assertEquals(MediaType.TEXT_HTML_VALUE, response.getContentType());
        String content = response.getContentAsString();
        assertThat(
                content,
                containsString(
                        "<a href=\"http://localhost:8080/geoserver/rest/workspaces/"
                                + catalog.getDefaultWorkspace().getName()
                                + "/styles/test.css\">test.css</a>"));
    }

    @Test
    public void testRawPutCSS() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String xml =
                "<style>"
                        + "<name>foo</name>"
                        + "<format>css</format>"
                        + "<filename>foo.css</filename>"
                        + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("foo"));

        // step 2 define css
        String content = newCSS();
        response = putAsServletResponse("/rest/styles/foo?raw=true", content, CssHandler.MIME_TYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.css");

        String definition = new String(resource.getContents());
        assertTrue("is css", definition.contains("stroke: red"));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }

    @Test
    public void testPostCSS() throws Exception {
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String content = newCSS();
        MockHttpServletResponse response =
                postAsServletResponse("/rest/styles?name=foo", content, CssHandler.MIME_TYPE);
        assertEquals(201, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.css");

        String definition = new String(resource.getContents());
        assertTrue("is css", definition.contains("stroke: red"));

        StyleInfo styleInfo = catalog.getStyleByName("foo");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>foo</sld:Name>"));
        catalog.remove(styleInfo);
    }

    @Test
    public void testPutCSS() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("bar"));

        String xml =
                "<style>"
                        + "<name>bar</name>"
                        + "<format>css</format>"
                        + "<filename>bar.css</filename>"
                        + "</style>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("bar"));

        // step 2 define css
        String content = newCSS();
        response = putAsServletResponse("/rest/styles/bar", content, CssHandler.MIME_TYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/bar.css");

        String definition = new String(resource.getContents());
        assertTrue("is css", definition.contains("stroke: red"));

        StyleInfo styleInfo = catalog.getStyleByName("bar");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>bar</sld:Name>"));

        // step 3 validate css
        content = "* { outline: red}";
        response = putAsServletResponse("/rest/styles/bar", content, CssHandler.MIME_TYPE);
        assertEquals(400, response.getStatus());

        catalog.remove(styleInfo);
    }

    String newCSS() {
        return "* { stroke: red}";
    }
}
