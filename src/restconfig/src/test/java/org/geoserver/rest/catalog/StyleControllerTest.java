/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.endsWith;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.*;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.rest.RestBaseController;
import org.geotools.styling.Style;
import org.geotools.util.URLs;
import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/** Just ripped these tests from the existing rest test */
public class StyleControllerTest extends CatalogRESTTestSupport {

    @Before
    public void removeStyles() throws IOException {
        removeStyle("gs", "foo");
        removeStyle(null, "foo");
        removeStyle(getCatalog().getDefaultWorkspace().getName(), "foo");
    }

    @Before
    public void addPondsStyle() throws IOException {
        getTestData().addStyle(SystemTestData.PONDS.getLocalPart(), getCatalog());
    }

    @Before
    public void restoreLayers() throws IOException {
        revertLayer(SystemTestData.BASIC_POLYGONS);
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.xml");

        List<StyleInfo> styles = catalog.getStyles();
        assertXpathEvaluatesTo("" + styles.size(), "count(//style)", dom);
    }

    @Test
    public void testGetAllASJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/styles.json");

        List<StyleInfo> styles = catalog.getStyles();
        assertEquals(
                styles.size(),
                ((JSONObject) json).getJSONObject("styles").getJSONArray("style").size());
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles.html");
        print(dom);

        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        NodeList links = xp.getMatchingNodes("//html:a", dom);

        for (int i = 0; i < styles.size(); i++) {
            StyleInfo s = styles.get(i);
            Element link = (Element) links.item(i);

            final String href = link.getAttribute("href");
            assertTrue(
                    "Expected href to bed with " + s.getName() + ".html but was " + href,
                    href.endsWith(s.getName() + ".html"));
        }
    }

    @Test
    public void testGetAllFromWorkspace() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs/styles.xml");
        assertEquals("styles", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("0", "count(//style)", dom);

        addStyleToWorkspace("foo");

        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs/styles.xml");
        assertEquals("styles", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("1", "count(//style)", dom);
        assertXpathExists("//style/name[text() = 'foo']", dom);
    }

    void addStyleToWorkspace(String name) {
        Catalog cat = getCatalog();
        StyleInfo s = cat.getFactory().createStyle();
        s.setName(name);
        s.setFilename(name + ".sld");
        s.setWorkspace(cat.getWorkspaceByName("gs"));
        cat.add(s);
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/Ponds.xml");

        assertEquals("style", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("Ponds", "/style/name", dom);
        assertXpathEvaluatesTo("Ponds.sld", "/style/filename", dom);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/styles/Ponds.json");

        JSONObject style = ((JSONObject) json).getJSONObject("style");
        assertEquals("Ponds", style.get("name"));
        assertEquals("Ponds.sld", style.get("filename"));
    }

    @Test
    public void testGetWrongStyle() throws Exception {
        // Parameters for the request
        String ws = "gs";
        String style = "foooooo";
        // Request path
        String requestPath = RestBaseController.ROOT_PATH + "/styles/" + style + ".html";
        String requestPath2 =
                RestBaseController.ROOT_PATH + "/workspaces/" + ws + "/styles/" + style + ".html";
        // Exception path
        String exception = "No such style: " + style;
        String exception2 = "No such style " + style + " in workspace " + ws;

        // CASE 1: No workspace set

        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());

        // CASE 2: workspace set

        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(exception2));

        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(exception2));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }

    @Test
    public void testGetAsSLD() throws Exception {
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/Ponds.sld");

        assertEquals("StyledLayerDescriptor", dom.getDocumentElement().getNodeName());
    }

    @Test
    public void testGetFromWorkspace() throws Exception {
        MockHttpServletResponse resp =
                getAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.xml");
        assertEquals(404, resp.getStatus());

        addStyleToWorkspace("foo");

        resp = getAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.xml");
        assertEquals(200, resp.getStatus());

        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.xml");
        assertXpathEvaluatesTo("foo", "/style/name", dom);
        assertXpathEvaluatesTo("gs", "/style/workspace/name", dom);
    }

    // GEOS-8080
    @Test
    public void testGetGlobalWithDuplicateInDefaultWorkspace() throws Exception {
        Catalog cat = getCatalog();
        String styleName = "foo";
        String wsName = cat.getDefaultWorkspace().getName();

        // Add a workspace style
        StyleInfo s = cat.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(styleName + ".sld");
        s.setWorkspace(cat.getDefaultWorkspace());
        cat.add(s);

        // Verify this style cannot retrieved by a non-workspaced GET
        MockHttpServletResponse resp =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/styles/foo.xml");
        assertEquals(404, resp.getStatus());

        // Add a global style
        s = cat.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(styleName + ".sld");
        cat.add(s);

        // Verify the global style is returned by a non-workspaced GET
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/foo.xml");
        assertXpathEvaluatesTo("foo", "/style/name", dom);
        assertXpathEvaluatesTo("", "/style/workspace/name", dom);

        // Verify the workspaced style is returned by a workspaced GET
        dom = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/" + wsName + "/styles/foo.xml");
        assertXpathEvaluatesTo("foo", "/style/name", dom);
        assertXpathEvaluatesTo(wsName, "/style/workspace/name", dom);
    }

    String newSLDXML() {
        return "<sld:StyledLayerDescriptor xmlns:sld='http://www.opengis.net/sld'>"
                + "<sld:NamedLayer>"
                + "<sld:Name>foo</sld:Name>"
                + "<sld:UserStyle>"
                + "<sld:Name>foo</sld:Name>"
                + "<sld:FeatureTypeStyle>"
                + "<sld:Name>foo</sld:Name>"
                + "</sld:FeatureTypeStyle>"
                + "</sld:UserStyle>"
                + "</sld:NamedLayer>"
                + "</sld:StyledLayerDescriptor>";
    }

    String newSLD11XML() {
        return "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                + "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" xmlns:ogc=\"http://www.opengis"
                + ".net/ogc\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" version=\"1.1.0\" "
                + "xmlns:xlink=\"http://www.w3.org/1999/xlink\" xsi:schemaLocation=\"http://www.opengis.net/sld"
                + " http://schemas.opengis.net/sld/1.1.0/StyledLayerDescriptor.xsd\" xmlns:se=\"http://www"
                + ".opengis.net/se\">\n"
                + "  <NamedLayer>\n"
                + "    <se:Name>foo</se:Name>\n"
                + "    <UserStyle>\n"
                + "      <se:Name>foo</se:Name>\n"
                + "      <se:FeatureTypeStyle>\n"
                + "        <se:Rule>\n"
                + "          <se:Name>Single symbol</se:Name>\n"
                + "          <se:LineSymbolizer>\n"
                + "            <se:Stroke>\n"
                + "              <se:SvgParameter name=\"stroke\">#aa6e8e</se:SvgParameter>\n"
                + "              <se:SvgParameter name=\"stroke-width\">2</se:SvgParameter>\n"
                + "              <se:SvgParameter name=\"stroke-linejoin\">round</se:SvgParameter>\n"
                + "              <se:SvgParameter name=\"stroke-linecap\">round</se:SvgParameter>\n"
                + "            </se:Stroke>\n"
                + "          </se:LineSymbolizer>\n"
                + "        </se:Rule>\n"
                + "      </se:FeatureTypeStyle>\n"
                + "    </UserStyle>\n"
                + "  </NamedLayer>\n"
                + "</StyledLayerDescriptor>";
    }

    @Test
    public void testPostAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", xml, SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/foo"));

        assertNotNull(catalog.getStyleByName("foo"));
    }

    @Test
    public void testPostAsSLD11() throws Exception {
        String xml = newSLD11XML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", xml, SLDHandler.MIMETYPE_11);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/foo"));

        final StyleInfo style = catalog.getStyleByName("foo");
        assertNotNull(style);
        assertNotNull(style.getSLD());
    }

    @Test
    public void testPostExternalEntityAsSLD() throws Exception {
        String xml = IOUtils.toString(TestData.class.getResource("externalEntities.sld"), "UTF-8");

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", xml, SLDHandler.MIMETYPE_10);
        assertEquals(500, response.getStatus());
        String message = response.getContentAsString();
        assertThat(message, containsString("Entity resolution disallowed"));
        assertThat(message, containsString("/this/file/does/not/exist"));
    }

    @Test
    public void testPostAsSLDToWorkspace() throws Exception {
        assertNull(catalog.getStyleByName("gs", "foo"));

        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/gs/styles/foo"));

        assertNotNull(catalog.getStyleByName("gs", "foo"));
        assertNotNull(catalog.getStyleByName("gs", "foo").getDateCreated());

        GeoServerResourceLoader rl = getResourceLoader();
        assertNotNull(rl.find("workspaces", "gs", "styles", "foo.sld"));
    }

    @Test
    public void testPostAsSLD11ToWorkspace() throws Exception {
        assertNull(catalog.getStyleByName("gs", "foo"));

        String xml = newSLD11XML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles",
                        xml,
                        SLDHandler.MIMETYPE_11);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/workspaces/gs/styles/foo"));

        assertNotNull(catalog.getStyleByName("gs", "foo"));
        assertNotNull(catalog.getStyleByName("gs", "foo").getDateCreated());

        GeoServerResourceLoader rl = getResourceLoader();
        assertNotNull(rl.find("workspaces", "gs", "styles", "foo.sld"));

        final StyleInfo styleInfo = catalog.getStyleByName("gs:foo");
        assertNotNull(styleInfo);
        assertNotNull(styleInfo.getSLD());
    }

    @Test
    public void testPostAsSLDWithName() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles?name=bar",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/bar"));

        assertNotNull(catalog.getStyleByName("bar"));
        assertNotNull(catalog.getStyleByName("bar").getDateCreated());
    }

    @Test
    public void testStyleWithSpaceInName() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles?name=Default%20Styler",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertThat(response.getHeader("Location"), endsWith("/styles/Default%20Styler"));

        assertNotNull(catalog.getStyleByName("Default Styler"));

        // now delete it, using a + instead of %20, the old code supported it
        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/Default+Styler");
        assertEquals(200, response.getStatus());
    }

    @Test
    public void testPostToWorkspace() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("gs", "foo"));

        String xml = "<style>" + "<name>foo</name>" + "<filename>foo.sld</filename>" + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(RestBaseController.ROOT_PATH + "/workspaces/gs/styles", xml);
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));
        assertNotNull(cat.getStyleByName("gs", "foo"));
        assertNotNull(cat.getStyleByName("gs", "foo").getDateCreated());
    }

    @Test
    public void testPut() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        assertEquals("Ponds.sld", style.getFilename());

        String xml =
                "<style>" + "<name>Ponds</name>" + "<filename>Forests.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds", xml.getBytes(), "text/xml");
        assertEquals(200, response.getStatus());

        style = catalog.getStyleByName("Ponds");
        assertEquals("Forests.sld", style.getFilename());
        assertNotNull(style.getDateModified());
    }

    /** Test for getting style with metadataMap value via Rest GET, XML format. */
    @Test
    public void testGetWithMetadataAsXML() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        style.getMetadata().put("cacheAgeMax", "300");
        catalog.save(style);
        Document dom = getAsDOM(RestBaseController.ROOT_PATH + "/styles/Ponds.xml");

        assertEquals("style", dom.getDocumentElement().getNodeName());
        assertXpathEvaluatesTo("Ponds", "/style/name", dom);
        assertXpathEvaluatesTo("Ponds.sld", "/style/filename", dom);
        assertXpathEvaluatesTo("300", "/style/metadata/entry[@key='cacheAgeMax']", dom);
    }

    /** Test for getting style with metadataMap value via Rest GET, JSON format. */
    @Test
    public void testGetWithMetadataAsJSON() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        style.getMetadata().put("cacheAgeMax", "300");
        style.getMetadata().put("surename", "test1");
        catalog.save(style);
        JSON json = getAsJSON(RestBaseController.ROOT_PATH + "/styles/Ponds.json");

        JSONObject styleJson = ((JSONObject) json).getJSONObject("style");
        assertEquals("Ponds", styleJson.get("name"));
        assertEquals("Ponds.sld", styleJson.get("filename"));
        Collection<JSONObject> entryCollection =
                JSONArray.toCollection(
                        styleJson.getJSONObject("metadata").getJSONArray("entry"),
                        JSONObject.class);
        assertTrue(
                entryCollection
                        .stream()
                        .anyMatch(
                                j ->
                                        "cacheAgeMax".equals(j.getString("@key"))
                                                && "300".equals(j.getString("$"))));
        assertTrue(
                entryCollection
                        .stream()
                        .anyMatch(
                                j ->
                                        "surename".equals(j.getString("@key"))
                                                && "test1".equals(j.getString("$"))));
    }

    /** Checks saving an style with metadataMap value via Rest PUT. */
    @Test
    public void testPutWithMetadata() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        assertEquals("Ponds.sld", style.getFilename());

        String xml =
                "<style>"
                        + "<name>Ponds</name>"
                        + "<filename>Forests.sld</filename>"
                        + "<metadata> <entry key=\"cacheAgeMax\">300</entry> </metadata>"
                        + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds", xml.getBytes(), "text/xml");
        assertEquals(200, response.getStatus());

        style = catalog.getStyleByName("Ponds");
        assertEquals("Forests.sld", style.getFilename());
        MetadataMap metadata = style.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.size() == 1);
        assertEquals("300", metadata.get("cacheAgeMax"));
        assertNotNull(style.getDateModified());
    }

    /** Checks saving an style with metadataMap value via Rest PUT using JSON format. */
    @Test
    public void testPutJSONWithMetadata() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        assertEquals("Ponds.sld", style.getFilename());

        String json =
                "{\"style\": {\"name\":\"Ponds\",\"format\":\"sld\",\"languageVersion\":{\"version\":\"1.0.0\"},"
                        + "\"filename\":\"Ponds.sld\","
                        + "\"metadata\":{"
                        + "\"entry\":[{\"@key\":\"cacheAgeMax\",\"$\":\"300\"}"
                        + ",{\"@key\":\"surename\",\"$\":\"test1\"}]}}"
                        + "}";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds.json",
                        json.getBytes(),
                        "application/json");
        assertEquals(200, response.getStatus());

        style = catalog.getStyleByName("Ponds");
        assertEquals("Ponds.sld", style.getFilename());
        MetadataMap metadata = style.getMetadata();
        assertNotNull(metadata);
        assertTrue(metadata.size() == 2);
        assertEquals("300", metadata.get("cacheAgeMax"));
        assertEquals("test1", metadata.get("surename"));
        assertNotNull(style.getDateModified());
    }

    @Test
    public void testPutAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(200, response.getStatus());

        Style s = catalog.getStyleByName("Ponds").getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }

    @Test
    public void testPutAsSLDWithCharset() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds",
                        xml,
                        SLDHandler.MIMETYPE_10 + "; charset=utf-8");
        assertEquals(200, response.getStatus());

        Style s = catalog.getStyleByName("Ponds").getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }

    @Test
    public void testPutAsSLD11Raw() throws Exception {
        String xml = newSLD11XML();

        // check it's version 1.0 before
        StyleInfo infoBefore = catalog.getStyleByName("Ponds");
        assertThat(infoBefore.getFormatVersion(), equalTo(SLDHandler.VERSION_10));

        // put a SLD 1.1 in raw mode (the content type is ignored in raw mode, but mimicking was
        // gsconfig does here)
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds?raw=true",
                        xml,
                        "application/vnd.ogc.se+xml");
        assertEquals(200, response.getStatus());

        // now it should have been "upgraded" to 1.1
        StyleInfo infoAfter = catalog.getStyleByName("Ponds");
        assertThat(infoAfter.getFormatVersion(), equalTo(SLDHandler.VERSION_11));
        assertNotNull(infoAfter.getDateModified());
    }

    @Test
    public void testPutVersionBackAndForth() throws Exception {
        // go from 1.0 to 1.1
        testPutAsSLD11Raw();

        String xml = newSLDXML();

        // put a SLD 1.0 in raw mode (the content type is ignored in raw mode, but mimicking was
        // gsconfig does here)
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds?raw=true",
                        xml,
                        "application/vnd.ogc.sld+xml");
        assertEquals(200, response.getStatus());

        // now it should have been modified back to 1.0
        StyleInfo infoAfter = catalog.getStyleByName("Ponds");
        assertThat(infoAfter.getFormatVersion(), equalTo(SLDHandler.VERSION_10));
        assertNotNull(infoAfter.getDateModified());
    }

    @Test
    public void testPutAsSLDWithExtension() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds.sld",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(200, response.getStatus());

        Style s = catalog.getStyleByName("Ponds").getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }

    @Test
    public void testRawPutAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds?raw=true",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(200, response.getStatus());

        Style s = catalog.getStyleByName("Ponds").getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SLDHandler handler = new SLDHandler();
        handler.encode(Styles.sld(s), SLDHandler.VERSION_10, false, out);
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }

    @Test
    public void testRawPutAsInvalidSLD() throws Exception {
        String xml = "This is not valid SLD";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds?raw=true",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(200, response.getStatus());

        StyleInfo styleInfo = catalog.getStyleByName("Ponds");
        String fileName = styleInfo.getFilename();

        GeoServerResourceLoader resources = getGeoServer().getCatalog().getResourceLoader();

        Resource resource = resources.get("styles/" + fileName);
        String content = new String(resource.getContents());

        assertFalse("replaced", content.contains("<sld:Name>foo</sld:Name>"));
        assertTrue("replaced", content.contains("not valid"));
    }

    @Test
    public void testPutToWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertEquals("foo.sld", cat.getStyleByName("gs", "foo").getFilename());

        String xml = "<style>" + "<filename>bar.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo",
                        xml,
                        "application/xml");
        assertEquals(200, response.getStatus());
        assertEquals("bar.sld", cat.getStyleByName("gs", "foo").getFilename());
        assertNotNull(cat.getStyleByName("gs", "foo").getDateModified());
    }

    @Test
    public void testPutToWorkspaceChangeWorkspace() throws Exception {
        testPostToWorkspace();

        String xml = "<style>" + "<workspace>cite</workspace>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo",
                        xml,
                        "application/xml");
        assertEquals(200, response.getStatus());
        assertNotNull("cite", getCatalog().getStyleByName("cite", "foo"));
        assertNotNull("cite", getCatalog().getStyleByName("cite", "foo").getWorkspace());
        assertEquals("cite", getCatalog().getStyleByName("cite", "foo").getWorkspace().getName());
        assertNotNull(getCatalog().getStyleByName("cite", "foo").getDateModified());
    }

    @Test
    public void testPutToWorkspaceChangeWorkspaceBackToGlobal() throws Exception {
        testPostToWorkspace();

        String xml = "<style>" + "<workspace></workspace>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo",
                        xml,
                        "application/xml");
        assertEquals(200, response.getStatus());
        assertNotNull("no workspace", getCatalog().getStyleByName("foo"));
        assertNull("no workspace", getCatalog().getStyleByName("foo").getWorkspace());
        assertNotNull(getCatalog().getStyleByName("foo").getDateModified());
    }

    @Test
    public void testPutRenameDefault() throws Exception {
        StyleInfo style = catalog.getStyleByName("Ponds");
        assertEquals("Ponds.sld", style.getFilename());

        String xml =
                "<style>" + "<name>Ponds</name>" + "<filename>Forests.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/line", xml.getBytes(), "text/xml");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testPutAsSLDNamedLayer() throws Exception {
        String xml =
                "<StyledLayerDescriptor version='1.0.0' "
                        + " xsi:schemaLocation='http://www.opengis.net/sld StyledLayerDescriptor.xsd' "
                        + " xmlns='http://www.opengis.net/sld' "
                        + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>"
                        + "  <NamedLayer>\n"
                        + "    <Name>Streams</Name>\n"
                        + // Reference the Streams layer
                        "  </NamedLayer>\n"
                        + "  <NamedLayer>\n"
                        + "    <Name>RoadSegments</Name>\n"
                        + // 2nd, valid layer
                        "  </NamedLayer>\n"
                        + "</StyledLayerDescriptor>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(200, response.getStatus());

        assertNotNull(catalog.getStyleByName("Ponds"));
        assertNotNull(catalog.getStyleByName("Ponds").getDateModified());
    }

    @Test
    public void testPutAsSLDNamedLayerInvalid() throws Exception {
        String xml =
                "<StyledLayerDescriptor version='1.0.0' "
                        + " xsi:schemaLocation='http://www.opengis.net/sld StyledLayerDescriptor.xsd' "
                        + " xmlns='http://www.opengis.net/sld' "
                        + " xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance'>"
                        + "  <NamedLayer>\n"
                        + "    <Name>Stream</Name>\n"
                        + // invalid layer
                        "  </NamedLayer>\n"
                        + "  <NamedLayer>\n"
                        + "    <Name>Streams</Name>\n"
                        + // valid layer
                        "  </NamedLayer>\n"
                        + "</StyledLayerDescriptor>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds",
                        xml,
                        SLDHandler.MIMETYPE_10);
        assertEquals(400, response.getStatus());
        assertEquals(
                "Invalid style:No layer or layer group named 'Stream' found in the catalog",
                response.getContentAsString());
    }

    @Test
    public void testStyleNotFoundGloballyWhenInWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertEquals("foo.sld", cat.getStyleByName("gs", "foo").getFilename());

        String xml = "<style>" + "<filename>bar.sld</filename>" + "</style>";

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo",
                        xml,
                        "application/xml");
        assertEquals(200, response.getStatus());
        response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/gs:foo", xml, "application/xml");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDelete() throws Exception {
        String xml =
                "<style>" + "<name>dummy</name>" + "<filename>dummy.sld</filename>" + "</style>";
        post(RestBaseController.ROOT_PATH + "/styles", xml, "text/xml");
        assertNotNull(catalog.getStyleByName("dummy"));

        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/dummy");
        assertEquals(200, response.getStatus());

        assertNull(catalog.getStyleByName("dummy"));
    }

    @Test
    public void testDeleteDefault() throws Exception {

        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/line");
        assertEquals(500, response.getStatus());
    }

    @Test
    public void testDeleteWithLayerReference() throws Exception {
        assertNotNull(catalog.getStyleByName("Ponds"));

        MockHttpServletResponse response =
                deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/Ponds");
        assertEquals(403, response.getStatus());

        assertNotNull(catalog.getStyleByName("Ponds"));
    }

    @Test
    public void testDeleteWithLayerReferenceAndRecurse() throws Exception {
        assertNotNull(catalog.getStyleByName("Ponds"));

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/Ponds?recurse=true");
        assertEquals(200, response.getStatus());

        assertNull(catalog.getStyleByName("Ponds"));
    }

    @Test
    public void testDeleteWithoutPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", xml, SLDHandler.MIMETYPE_10);
        assertNotNull(catalog.getStyleByName("foo"));

        // ensure the style not deleted on disk
        assertTrue(
                new File(Resources.directory(getDataDirectory().getStyles()), "foo.sld").exists());

        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/foo");
        assertEquals(200, response.getStatus());

        // ensure the style deleted on disk but backed up
        assertFalse(
                new File(Resources.directory(getDataDirectory().getStyles()), "foo.sld").exists());
        assertTrue(
                new File(Resources.directory(getDataDirectory().getStyles()), "foo.sld.bak")
                        .exists());
    }

    @Test
    public void testDeleteWithPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", xml, SLDHandler.MIMETYPE_10);
        assertNotNull(catalog.getStyleByName("foo"));

        // ensure the style not deleted on disk
        assertTrue(
                new File(Resources.directory(getDataDirectory().getStyles()), "foo.sld").exists());

        response = deleteAsServletResponse(RestBaseController.ROOT_PATH + "/styles/foo?purge=true");
        assertEquals(200, response.getStatus());

        // ensure the style not deleted on disk
        assertFalse(
                new File(Resources.directory(getDataDirectory().getStyles()), "foo.sld").exists());
    }

    @Test
    public void testDeleteFromWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("gs", "foo"));

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.xml");
        assertEquals(200, response.getStatus());

        assertNull(cat.getStyleByName("gs", "foo"));
    }

    @Test
    public void testDeleteFromWorkspaceWithPurge() throws Exception {
        testPostAsSLDToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("gs", "foo"));

        GeoServerResourceLoader rl = getResourceLoader();
        assertNotNull(rl.find("workspaces", "gs", "styles", "foo.sld"));

        MockHttpServletResponse response =
                deleteAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo?purge=true");
        assertEquals(200, response.getStatus());

        assertNull(cat.getStyleByName("gs", "foo"));
        assertNull(rl.find("workspaces", "gs", "styles", "foo.sld"));
    }

    @Test
    public void testGetAllByLayer() throws Exception {
        Document dom =
                getAsDOM(RestBaseController.ROOT_PATH + "/layers/cite:BasicPolygons/styles.xml");
        LayerInfo layer = catalog.getLayerByName("cite:BasicPolygons");

        assertXpathEvaluatesTo(layer.getStyles().size() + "", "count(//style)", dom);
    }

    @Test
    public void testPostByLayer() throws Exception {

        LayerInfo l = catalog.getLayerByName("cite:BasicPolygons");
        int nstyles = l.getStyles().size();

        String xml = "<style>" + "<name>Ponds</name>" + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/layers/cite:BasicPolygons/styles",
                        xml,
                        "text/xml");
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));

        LayerInfo l2 = catalog.getLayerByName("cite:BasicPolygons");
        assertEquals(nstyles + 1, l2.getStyles().size());

        assertTrue(l2.getStyles().contains(catalog.getStyleByName("Ponds")));
    }

    @Test
    public void testPostByLayerWithDefault() throws Exception {
        getTestData().addVectorLayer(SystemTestData.BASIC_POLYGONS, getCatalog());
        LayerInfo l = catalog.getLayerByName("cite:BasicPolygons");
        int nstyles = l.getStyles().size();

        String xml = "<style>" + "<name>Ponds</name>" + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/layers/cite:BasicPolygons/styles?default=true",
                        xml,
                        "text/xml");
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));

        LayerInfo l2 = catalog.getLayerByName("cite:BasicPolygons");
        assertEquals(nstyles + 1, l2.getStyles().size());
        assertEquals(catalog.getStyleByName("Ponds"), l2.getDefaultStyle());
        assertNotNull(l2.getDefaultStyle().getDateModified());
    }

    @Test
    public void testPostByLayerExistingWithDefault() throws Exception {
        getTestData().addVectorLayer(SystemTestData.BASIC_POLYGONS, getCatalog());
        testPostByLayer();

        LayerInfo l = catalog.getLayerByName("cite:BasicPolygons");
        int nstyles = l.getStyles().size();

        String xml = "<style>" + "<name>Ponds</name>" + "</style>";
        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH
                                + "/layers/cite:BasicPolygons/styles?default=true",
                        xml,
                        "text/xml");
        assertEquals(201, response.getStatus());
        assertThat(response.getContentType(), CoreMatchers.startsWith(MediaType.TEXT_PLAIN_VALUE));

        LayerInfo l2 = catalog.getLayerByName("cite:BasicPolygons");
        assertEquals(nstyles, l2.getStyles().size());
        assertEquals(catalog.getStyleByName("Ponds"), l2.getDefaultStyle());
        assertNotNull(l2.getDefaultStyle().getDateModified());
    }

    @Test
    @Ignore
    public void testPostAsPSL() throws Exception {
        Properties props = new Properties();
        props.put("type", "point");
        props.put("color", "ff0000");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles?name=foo",
                        out.toString(),
                        PropertyStyleHandler.MIMETYPE);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/foo"));

        assertNotNull(catalog.getStyleByName("foo"));
        assertNotNull(catalog.getStyleByName("foo").getDateCreated());

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();

        props = new Properties();
        try {
            props.load(in);
            assertEquals("point", props.getProperty("type"));
        } finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out, "UTF-8");
            assertFalse(out.toString().startsWith("#comment!"));
        } finally {
            in.close();
        }
    }

    @Test
    @Ignore
    public void testPostAsPSLRaw() throws Exception {
        Properties props = new Properties();
        props.put("type", "point");
        props.put("color", "ff0000");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles?name=foo&raw=true",
                        out.toString(),
                        PropertyStyleHandler.MIMETYPE);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/foo"));

        // check style on disk to ensure the exact contents was preserved
        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        try (InputStream in = style.in()) {
            out = new StringWriter();
            IOUtils.copy(in, out, "UTF-8");
            assertTrue(out.toString().startsWith("#comment!"));
        }
    }

    @Test
    @Ignore
    public void testGetAsPSL() throws Exception {
        Properties props = new Properties();
        props.load(get(RestBaseController.ROOT_PATH + "/styles/Ponds.properties"));

        assertEquals("polygon", props.getProperty("type"));
    }

    @Test
    @Ignore
    public void testPutAsPSL() throws Exception {
        testPostAsPSL();

        Properties props = new Properties();
        props.put("type", "line");
        props.put("color", "00ff00");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/foo",
                        out.toString(),
                        PropertyStyleHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();
        try {
            props = new Properties();
            props.load(in);
            assertEquals("line", props.getProperty("type"));
        } finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out, "UTF-8");
            assertFalse(out.toString().startsWith("#comment!"));
        } finally {
            in.close();
        }
    }

    @Test
    @Ignore
    public void testPutAsPSLRaw() throws Exception {
        testPostAsPSL();

        Properties props = new Properties();
        props.put("type", "line");
        props.put("color", "00ff00");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/foo?raw=true",
                        out.toString(),
                        PropertyStyleHandler.MIMETYPE);
        assertEquals(200, response.getStatus());

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();
        try {
            props = new Properties();
            props.load(in);
            assertEquals("line", props.getProperty("type"));
        } finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out, "UTF-8");
            assertTrue(out.toString().startsWith("#comment!"));
        } finally {
            in.close();
        }
    }

    @Test
    public void testPostAsSE() throws Exception {
        String xml =
                "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" "
                        + "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "
                        + " <NamedLayer> "
                        + "  <UserStyle> "
                        + "   <se:Name>UserSelection</se:Name> "
                        + "   <se:FeatureTypeStyle> "
                        + "    <se:Rule> "
                        + "     <se:PolygonSymbolizer> "
                        + "      <se:Fill> "
                        + "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "
                        + "      </se:Fill> "
                        + "     </se:PolygonSymbolizer> "
                        + "    </se:Rule> "
                        + "   </se:FeatureTypeStyle> "
                        + "  </UserStyle> "
                        + " </NamedLayer> "
                        + "</StyledLayerDescriptor>";

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles?name=foo",
                        xml,
                        SLDHandler.MIMETYPE_11);
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(response.getHeader("Location"));
        assertTrue(response.getHeader("Location").endsWith("/styles/foo"));

        StyleInfo style = catalog.getStyleByName("foo");
        assertNotNull(style);

        assertEquals("sld", style.getFormat());
        assertEquals(SLDHandler.VERSION_11, style.getFormatVersion());
        assertNotNull(style.getDateCreated());
    }

    @Test
    public void testPostToWorkspaceSLDPackage() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("gs", "foo"));

        URL zip = getClass().getResource("test-data/foo.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles",
                        bytes,
                        "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(cat.getStyleByName("gs", "foo"));
        assertNotNull(cat.getStyleByName("gs", "foo").getDateCreated());

        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.sld");

        assertEquals("StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list =
                engine.getMatchingNodes(
                        "//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource",
                        d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element) list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/foo.sld"));
    }

    @Test
    public void testPostWithExternalEntities() throws Exception {
        URL zip = getClass().getResource("test-data/externalEntities.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles",
                        bytes,
                        "application/zip");
        // expecting a failure with explanation
        assertEquals(400, response.getStatus());
        final String content = response.getContentAsString();
        assertThat(content, containsString("Entity resolution disallowed"));
        assertThat(content, containsString("/this/file/does/not/exist"));
    }

    @Test
    public void testPutToWorkspaceSLDPackage() throws Exception {
        testPostAsSLDToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("gs", "foo"));

        URL zip = getClass().getResource("test-data/foo.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo",
                        bytes,
                        "application/zip");
        assertEquals(200, response.getStatus());
        assertNotNull(cat.getStyleByName("gs", "foo"));
        assertNotNull(cat.getStyleByName("gs", "foo").getDateCreated());

        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/workspaces/gs/styles/foo.sld");

        assertEquals("StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list =
                engine.getMatchingNodes(
                        "//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource",
                        d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element) list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/foo.sld"));
    }

    @Test
    public void testPostSLDPackage() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));

        URL zip = getClass().getResource("test-data/foo.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles", bytes, "application/zip");
        assertEquals(201, response.getStatus());
        assertEquals(MediaType.TEXT_PLAIN_VALUE, response.getContentType());
        assertNotNull(cat.getStyleByName("foo"));
        assertNotNull(cat.getStyleByName("foo").getDateCreated());

        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/styles/foo.sld");

        assertEquals("StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list =
                engine.getMatchingNodes(
                        "//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource",
                        d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element) list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/foo.sld"));
    }

    @Test
    public void testPutSLDPackage() throws Exception {
        testPostAsSLD();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("foo"));

        URL zip = getClass().getResource("test-data/foo.zip");
        byte[] bytes = FileUtils.readFileToByteArray(URLs.urlToFile(zip));

        // @TODO i had to change this from foo.zip to just foo. see the long comments below
        MockHttpServletResponse response =
                putAsServletResponse(
                        RestBaseController.ROOT_PATH + "/styles/foo", bytes, "application/zip");
        assertEquals(200, response.getStatus());
        assertNotNull(cat.getStyleByName("foo"));
        assertNotNull(cat.getStyleByName("foo").getDateModified());

        Document d = getAsDOM(RestBaseController.ROOT_PATH + "/styles/foo.sld");

        assertEquals("StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list =
                engine.getMatchingNodes(
                        "//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource",
                        d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element) list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/foo.sld"));
    }

    /**
     * TODO I had to put this here BECAUSE:
     *
     * <p>- The testPutSLDPackage test uses a *.zip URL - BUT, put style does not support ZIP
     * responses - Spring interprets the .zip extension on the path as being a request for a zip
     * response - This fails, because there is no actual handler for a zip response on a style
     * endpoint - Unfortunately Spring only considers one of the Accept header or the path - So the
     * handler is never found
     *
     * <p>this leaves us with a few options
     *
     * <p>1) Configure spring to prefer the accept header over the path. This would:
     *
     * <p>- Force future clients who depended on put/posting to zip endpoints to make sure their
     * Accept header is correct. - Maybe more importantly it could potentially break other end
     * points that depend on preferring the path extension.
     *
     * <p>2) Continue letting Spring prefer the path (which is really the right behavior for a REST
     * api)
     *
     * <p>- Future clients would not be able to use an endpoint like .zip - But this is more REST-y
     *
     * <p>3) Write our own content negotiation strategy that allows for both.
     *
     * <p>- This is a pain in the ass. - Potentially difficult to recreate all default behavior +
     * behavior needed to fix this test case
     */
    protected MockHttpServletResponse putAsServletResponse(
            String path, byte[] body, String contentType, String accepts) throws Exception {

        MockHttpServletRequest request = createRequest(path);
        request.setMethod("PUT");
        request.setContentType(contentType);
        request.setContent(body);
        request.addHeader("Accept", accepts);
        request.addHeader("Content-type", contentType);

        return dispatch(request);
    }
}
