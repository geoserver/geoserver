/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.junit.Assert.*;

import java.io.ByteArrayOutputStream;
import java.util.HashMap;
import java.util.Map;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.RestBaseController;
import org.geoserver.security.AccessMode;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.ysld.YsldHandler;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class YsldStyleControllerTest extends GeoServerSystemTestSupport {

    protected static Catalog catalog;
    protected static XpathEngine xp;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        addLayerAccessRule("*", "*", AccessMode.READ, "*");
        addLayerAccessRule("*", "*", AccessMode.WRITE, "*");

        catalog = getCatalog();

        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("html", "http://www.w3.org/1999/xhtml");
        namespaces.put("sld", "http://www.opengis.net/sld");
        namespaces.put("ogc", "http://www.opengis.net/ogc");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
        xp = XMLUnit.newXpathEngine();
    }

    @Before
    public void login() throws Exception {
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void testRawPutYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String xml =
                "<style>"
                        + "<name>foo</name>"
                        + "<format>ysld</format>"
                        + "<filename>foo.yaml</filename>"
                        + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("foo"));

        String content = newYSLD();
        response = putAsServletResponse("/rest/styles/foo?raw=true", content, YsldHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

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
    public void testPostYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull("foo not available", cat.getStyleByName("foo"));

        String content = newYSLD();
        MockHttpServletResponse response =
                postAsServletResponse("/rest/styles?name=foo", content, YsldHandler.MIMETYPE);
        assertEquals(201, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/foo.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

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
    public void testPutYSLD() throws Exception {
        // step 1 create style info with correct format
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("bar"));

        String xml =
                "<style>"
                        + "<name>bar</name>"
                        + "<format>ysld</format>"
                        + "<filename>bar.yaml</filename>"
                        + "</style>";

        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("bar"));

        String content = newYSLD();
        response = putAsServletResponse("/rest/styles/bar", content, YsldHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        GeoServerResourceLoader resources = catalog.getResourceLoader();

        Resource resource = resources.get("/styles/bar.yaml");

        String definition = new String(resource.getContents());
        assertTrue("is yaml", definition.contains("stroke-color: '#FF0000'"));

        StyleInfo styleInfo = catalog.getStyleByName("bar");
        Style s = styleInfo.getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        content = new String(out.toByteArray());
        assertTrue(content.contains("<sld:Name>bar</sld:Name>"));

        catalog.remove(styleInfo);
    }

    String newYSLD() {
        return "title: valid ysld\n"
                + "symbolizers:\n"
                + "- line:\n"
                + "    stroke-width: 1.0\n"
                + "    stroke-color: '#FF0000'";
    }
}
