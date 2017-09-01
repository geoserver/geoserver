/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.*;

import java.io.*;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.*;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.TestData;
import org.geoserver.platform.resource.Resource;
import org.geoserver.rest.format.MediaTypes;
import org.geotools.data.DataUtilities;
import org.geotools.styling.Style;
import org.junit.Before;
import org.junit.Test;
import org.restlet.data.MediaType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import org.springframework.mock.web.MockHttpServletResponse;

public class StyleTest extends CatalogRESTTestSupport {

    @Before
    public void removeStyles() throws IOException {
        removeStyle("gs", "foo");
        removeStyle(null, "foo");
    }

    @Before
    public void addPondsStyle() throws IOException {
       getTestData().addStyle(SystemTestData.PONDS.getLocalPart(), getCatalog());
    }
    
    @Before
    public void restoreLayers() throws IOException {
        revertLayer(SystemTestData.BASIC_POLYGONS);
    }

    /**
     * Register a {@link StyleHandler} whose format and extension collide with the extension-based content negotiation, in order to verify that the
     * conflicting StyleHandler does *not* take precedence for requests with that extension. In particular, this will register a
     * {@link TestStyleHandlerExtensionConflict} handler with a file extension of ".xml", which is specifically created to conflict with the ".xml"
     * extension in the REST API.
     */
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        springContextLocations.add("testStyleHandlerExtensionConflict.xml");
        super.setUpSpring(springContextLocations);        
    }
    
    /**
     * Verify that the {@link MediaTypes} registry correctly maps the "xml" extension to the "application/xml" mime type, even though there is a
     * {@link StyleHandler} with the same file extension (but a different mime type).
     */
    @Test
    public void styleFinderConflictingExtensionTest() {
        MediaType mt = MediaTypes.getMediaTypeForExtension("xml");
        assertEquals("application/xml", mt.getName());
    }
    
    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/styles.xml" );
        
        List<StyleInfo> styles = catalog.getStyles();
        assertXpathEvaluatesTo(""+styles.size(), "count(//style)", dom);
    }
    
    @Test
    public void testGetAllASJSON() throws Exception {
        JSON json = getAsJSON("/rest/styles.json");
        
        List<StyleInfo> styles = catalog.getStyles();
        assertEquals( styles.size(), 
            ((JSONObject) json).getJSONObject("styles").getJSONArray("style").size());
    }
    
    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/styles.html");
        print(dom);
        
        List<StyleInfo> styles = catalog.getStylesByWorkspace(CatalogFacade.NO_WORKSPACE);
        NodeList links = xp.getMatchingNodes("//html:a", dom);

        for ( int i = 0; i < styles.size(); i++ ) {
            StyleInfo s = (StyleInfo) styles.get( i );
            Element link = (Element) links.item( i );
            
            final String href = link.getAttribute("href");
            assertTrue("Expected href to bed with " + s.getName() + ".html but was " + href,
                    href.endsWith(s.getName() + ".html"));
        }
    }

    @Test
    public void testGetAllFromWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/gs/styles.xml" );
        assertEquals("styles", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("0", "count(//style)", dom);

        addStyleToWorkspace("foo");

        dom = getAsDOM( "/rest/workspaces/gs/styles.xml" );
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
        Document dom = getAsDOM( "/rest/styles/Ponds.xml" );
        
        assertEquals( "style", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("Ponds", "/style/name", dom);
        assertXpathEvaluatesTo("Ponds.sld", "/style/filename", dom);
    }
    
    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/styles/Ponds.json");
        
        JSONObject style =  ((JSONObject)json).getJSONObject("style"); 
        assertEquals( "Ponds", style.get( "name") );
        assertEquals( "Ponds.sld", style.get( "filename") );
    }

    @Test
    public void testGetWrongStyle() throws Exception {
        // Parameters for the request
        String ws = "gs";
        String style = "foooooo";
        // Request path
        String requestPath = "/rest/styles/" + style + ".html";
        String requestPath2 = "/rest/workspaces/" + ws + "/styles/" + style + ".html";
        // Exception path
        String exception = "No such style: " + style;
        String exception2 = "No such style "+ style +" in workspace " + ws;
        
        // CASE 1: No workspace set
        
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(
                exception));
        
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(
                exception));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
        
        // CASE 2: workspace set
        
        // First request should thrown an exception
        response = getAsServletResponse(requestPath2);
        assertEquals(404, response.getStatus());
        assertTrue(response.getContentAsString().contains(
                exception2));
        
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath2 + "?quietOnNotFound=true");
        assertEquals(404, response.getStatus());
        assertFalse(response.getContentAsString().contains(
                exception2));
        // No exception thrown
        assertTrue(response.getContentAsString().isEmpty());
    }
    
    @Test
    public void testGetAsSLD() throws Exception {
        Document dom = getAsDOM( "/rest/styles/Ponds.sld");

        assertEquals( "StyledLayerDescriptor", dom.getDocumentElement().getNodeName() );
    }

    @Test
    public void testGetFromWorkspace() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/workspaces/gs/styles/foo.xml"); 
        assertEquals(404, resp.getStatus());

        addStyleToWorkspace("foo");

        resp = getAsServletResponse("/rest/workspaces/gs/styles/foo.xml");
        assertEquals(200, resp.getStatus());

        Document dom = getAsDOM("/rest/workspaces/gs/styles/foo.xml");
        assertXpathEvaluatesTo("foo", "/style/name", dom);
        assertXpathEvaluatesTo("gs", "/style/workspace/name", dom);
    }

    //GEOS-8080
    @Test
    public void testGetGlobalWithDuplicateInDefaultWorkspace() throws Exception {
        Catalog cat = getCatalog();
        String styleName = "foo";
        String wsName = cat.getDefaultWorkspace().getName();

        StyleInfo s = cat.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(styleName + ".sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName(styleName);
        s.setFilename(styleName + ".sld");
        s.setWorkspace(cat.getDefaultWorkspace());
        cat.add(s);

        Document dom = getAsDOM("/rest/styles/foo.xml");
        assertXpathEvaluatesTo(styleName, "/style/name", dom);
        assertXpathEvaluatesTo("", "/style/workspace/name", dom);

        dom = getAsDOM("/rest/workspaces/" + wsName + "/styles/foo.xml");
        assertXpathEvaluatesTo(styleName, "/style/name", dom);
        assertXpathEvaluatesTo(wsName, "/style/workspace/name", dom);
    }

    String newSLDXML() {
        return 
             "<sld:StyledLayerDescriptor xmlns:sld='http://www.opengis.net/sld'>"+
                "<sld:NamedLayer>"+
                "<sld:Name>foo</sld:Name>"+
                "<sld:UserStyle>"+
                  "<sld:Name>foo</sld:Name>"+
                  "<sld:FeatureTypeStyle>"+
                     "<sld:Name>foo</sld:Name>"+
                  "</sld:FeatureTypeStyle>" + 
                "</sld:UserStyle>" + 
              "</sld:NamedLayer>" + 
            "</sld:StyledLayerDescriptor>";
    }
    
    @Test
    public void testPostAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, SLDHandler.MIMETYPE_10);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/foo" ) );
        
        assertNotNull( catalog.getStyleByName( "foo" ) );
    }
    
    @Test
    public void testPostExternalEntityAsSLD() throws Exception {
        String xml = IOUtils.toString(TestData.class.getResource("externalEntities.sld"), "UTF-8");

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, SLDHandler.MIMETYPE_10);
        assertEquals( 500, response.getStatus() );
        String message = response.getContentAsString();
        assertThat(message, containsString("Entity resolution disallowed"));
        assertThat(message, containsString("/this/file/does/not/exist"));
    }
    
    @Test
    public void testPostAsSLDToWorkspace() throws Exception {
        assertNull( catalog.getStyleByName( "gs", "foo" ) );
        
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/gs/styles", xml, SLDHandler.MIMETYPE_10);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/gs/styles/foo" ) );
        
        assertNotNull( catalog.getStyleByName( "gs", "foo" ) );

        GeoServerResourceLoader rl = getResourceLoader();
        assertNotNull(rl.find("workspaces", "gs", "styles", "foo.sld"));
    }

    @Test
    public void testPostAsSLDWithName() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles?name=bar", xml, SLDHandler.MIMETYPE_10);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/bar" ) );
        
        assertNotNull( catalog.getStyleByName( "bar" ) );
    }

    @Test
    public void testPostToWorkspace() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("gs", "foo"));

        String xml = 
            "<style>" +
              "<name>foo</name>" +
              "<filename>foo.sld</filename>" + 
            "</style>";
        MockHttpServletResponse response =
            postAsServletResponse("/rest/workspaces/gs/styles", xml);
        assertEquals(201, response.getStatus());
        assertNotNull(cat.getStyleByName("gs", "foo"));
    }

    @Test
    public void testPut() throws Exception {
        StyleInfo style = catalog.getStyleByName( "Ponds");
        assertEquals( "Ponds.sld", style.getFilename() );
        
        String xml = 
            "<style>" +
              "<name>Ponds</name>" +
              "<filename>Forests.sld</filename>" + 
            "</style>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/styles/Ponds", xml.getBytes(), "text/xml");
        assertEquals( 200, response.getStatus() );
        
        style = catalog.getStyleByName( "Ponds");
        assertEquals( "Forests.sld", style.getFilename() );
    }
    
    @Test
    public void testPutAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/styles/Ponds", xml, SLDHandler.MIMETYPE_10);
        assertEquals( 200, response.getStatus() );
        
        Style s = catalog.getStyleByName( "Ponds" ).getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        new StyleFormat(SLDHandler.MIMETYPE_10, SLDHandler.VERSION_10, false, new SLDHandler(), null, getCatalog().getResourcePool().getEntityResolver()).write(s, out);
        
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }

    @Test
    public void testPutToWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertEquals("foo.sld", cat.getStyleByName("gs","foo").getFilename());

        String xml = 
            "<style>" +
              "<filename>bar.sld</filename>" +
            "</style>";
        
        MockHttpServletResponse response =
            putAsServletResponse("/rest/workspaces/gs/styles/foo", xml, "application/xml");
        assertEquals(200, response.getStatus());
        assertEquals("bar.sld", cat.getStyleByName("gs","foo").getFilename());
    }

    @Test
    public void testPutToWorkspaceChangeWorkspace() throws Exception {
        testPostToWorkspace();

        String xml = 
                "<style>" +
                  "<workspace>cite</workspace>" + 
                "</style>";
            
        MockHttpServletResponse response =
            putAsServletResponse("/rest/workspaces/gs/styles/foo", xml, "application/xml");
        assertEquals(403, response.getStatus());
    }
    @Test
    public void testStyleNotFoundGloballyWhenInWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertEquals("foo.sld", cat.getStyleByName("gs","foo").getFilename());

        String xml = 
            "<style>" +
              "<filename>bar.sld</filename>" +
            "</style>";
        
        MockHttpServletResponse response =
            putAsServletResponse("/rest/workspaces/gs/styles/foo", xml, "application/xml");
        assertEquals(200, response.getStatus());
        response = putAsServletResponse("/rest/styles/gs:foo", xml, "application/xml");
        assertEquals(404, response.getStatus());
    }
    @Test
    public void testDelete() throws Exception {
        String xml = 
            "<style>" +
              "<name>dummy</name>" + 
              "<filename>dummy.sld</filename>" + 
            "</style>";
        post( "/rest/styles", xml, "text/xml");
        assertNotNull( catalog.getStyleByName( "dummy" ) );
        
        MockHttpServletResponse response = 
            deleteAsServletResponse("/rest/styles/dummy");
        assertEquals( 200, response.getStatus() );
        
        assertNull( catalog.getStyleByName( "dummy" ) );
    }
    
    @Test
    public void testDeleteWithLayerReference() throws Exception {
        assertNotNull( catalog.getStyleByName( "Ponds" ) );
        
        MockHttpServletResponse response = 
            deleteAsServletResponse("/rest/styles/Ponds");
        assertEquals( 403, response.getStatus() );
         
        assertNotNull( catalog.getStyleByName( "Ponds" ) );
    }

    @Test
    public void testDeleteWithLayerReferenceAndRecurse() throws Exception {
        assertNotNull( catalog.getStyleByName( "Ponds" ) );

        MockHttpServletResponse response =
                deleteAsServletResponse("/rest/styles/Ponds?recurse=true");
        assertEquals( 200, response.getStatus() );

        assertNull( catalog.getStyleByName( "Ponds" ) );
    }

    @Test
    public void testDeleteWithoutPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, SLDHandler.MIMETYPE_10);
        assertNotNull( catalog.getStyleByName( "foo" ) );
        
        //ensure the style not deleted on disk
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
        
        response = deleteAsServletResponse("/rest/styles/foo");
        assertEquals( 200, response.getStatus() );
        
        //ensure the style deleted on disk but backed up
        assertFalse(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld.bak").exists());
    }
    
    @Test
    public void testDeleteWithPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, SLDHandler.MIMETYPE_10);
        assertNotNull( catalog.getStyleByName( "foo" ) );
        
        //ensure the style not deleted on disk
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
        
        response = deleteAsServletResponse("/rest/styles/foo?purge=true");
        assertEquals( 200, response.getStatus() );
        
        //ensure the style not deleted on disk
        assertFalse(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
    }

    @Test
    public void testDeleteFromWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("gs", "foo"));
        
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/gs/styles/foo");
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
        
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/gs/styles/foo?purge=true");
        assertEquals(200, response.getStatus());

        assertNull(cat.getStyleByName("gs", "foo"));
        assertNull(rl.find("workspaces", "gs", "styles", "foo.sld"));
    }

    @Test
    public void testGetAllByLayer() throws Exception {
        Document dom = getAsDOM( "/rest/layers/cite:BasicPolygons/styles.xml");
        LayerInfo layer = catalog.getLayerByName( "cite:BasicPolygons" );
        
        assertXpathEvaluatesTo(layer.getStyles().size()+"", "count(//style)", dom );
    }
    
    @Test
    public void testPostByLayer() throws Exception {

        LayerInfo l = catalog.getLayerByName( "cite:BasicPolygons" );
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles", xml, "text/xml");
        assertEquals( 201, response.getStatus() );
        
        LayerInfo l2 = catalog.getLayerByName( "cite:BasicPolygons" );
        assertEquals( nstyles+1, l2.getStyles().size() );
        
        assertTrue( l2.getStyles().contains( catalog.getStyleByName( "Ponds") ) );
    }
    
    @Test
    public void testPostByLayerWithDefault() throws Exception {
        getTestData().addVectorLayer(SystemTestData.BASIC_POLYGONS, getCatalog());
        LayerInfo l = catalog.getLayerByName( "cite:BasicPolygons" );
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles?default=true", xml, "text/xml");
        assertEquals( 201, response.getStatus() );
        
        LayerInfo l2 = catalog.getLayerByName( "cite:BasicPolygons" );
        assertEquals( nstyles+1, l2.getStyles().size() );
        assertEquals( catalog.getStyleByName( "Ponds"), l2.getDefaultStyle() );
    }
    
    @Test
    public void testPostByLayerExistingWithDefault() throws Exception {
        getTestData().addVectorLayer(SystemTestData.BASIC_POLYGONS, getCatalog());
        testPostByLayer();
        
        LayerInfo l = catalog.getLayerByName("cite:BasicPolygons");
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles?default=true", xml, "text/xml");
        assertEquals( 201, response.getStatus() );
        
        LayerInfo l2 = catalog.getLayerByName("cite:BasicPolygons");
        assertEquals( nstyles, l2.getStyles().size() );
        assertEquals( catalog.getStyleByName( "Ponds"), l2.getDefaultStyle() );
    }

    @Test
    public void testPostAsPSL() throws Exception {
        Properties props = new Properties();
        props.put("type", "point");
        props.put("color", "ff0000");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
                postAsServletResponse( "/rest/styles?name=foo", out.toString(), PropertyStyleHandler.MIMETYPE);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/foo" ) );

        assertNotNull( catalog.getStyleByName( "foo" ) );

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();

        props = new Properties();
        try {
            props.load(in);
            assertEquals("point", props.getProperty("type"));
        }
        finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out);
            assertFalse(out.toString().startsWith("#comment!"));
        }
        finally {
            in.close();
        }
    }

    @Test
    public void testPostAsPSLRaw() throws Exception {
        Properties props = new Properties();
        props.put("type", "point");
        props.put("color", "ff0000");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
            postAsServletResponse( "/rest/styles?name=foo&raw=true", out.toString(), PropertyStyleHandler.MIMETYPE);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/foo" ) );

        // check style on disk to ensure the exact contents was preserved
        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out);
            assertTrue(out.toString().startsWith("#comment!"));
        }
        finally {
            in.close();
        }
    }

    @Test
    public void testGetAsPSL() throws Exception {
        Properties props = new Properties();
        props.load(get("/rest/styles/Ponds.properties"));

        assertEquals("polygon", props.getProperty("type"));
    }

    @Test
    public void testPutAsPSL() throws Exception {
        testPostAsPSL();

        Properties props = new Properties();
        props.put("type", "line");
        props.put("color", "00ff00");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
            putAsServletResponse( "/rest/styles/foo", out.toString(), PropertyStyleHandler.MIMETYPE);
        assertEquals( 200, response.getStatus() );

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();
        try {
            props = new Properties();
            props.load(in);
            assertEquals("line", props.getProperty("type"));
        }
        finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out);
            assertFalse(out.toString().startsWith("#comment!"));
        }
        finally {
            in.close();
        }
    }

    @Test
    public void testPutAsPSLRaw() throws Exception {
        testPostAsPSL();

        Properties props = new Properties();
        props.put("type", "line");
        props.put("color", "00ff00");

        StringWriter out = new StringWriter();
        props.store(out, "comment!");

        MockHttpServletResponse response =
            putAsServletResponse( "/rest/styles/foo?raw=true", out.toString(), PropertyStyleHandler.MIMETYPE);
        assertEquals( 200, response.getStatus() );

        Resource style = getDataDirectory().style(getCatalog().getStyleByName("foo"));
        InputStream in = style.in();
        try {
            props = new Properties();
            props.load(in);
            assertEquals("line", props.getProperty("type"));
        }
        finally {
            in.close();
        }

        in = style.in();
        try {
            out = new StringWriter();
            IOUtils.copy(in, out);
            assertTrue(out.toString().startsWith("#comment!"));
        }
        finally {
            in.close();
        }
    }

    @Test
    public void testPostAsSE() throws Exception {
        String xml =
            "<StyledLayerDescriptor xmlns=\"http://www.opengis.net/sld\" " +
            "       xmlns:se=\"http://www.opengis.net/se\" version=\"1.1.0\"> "+
            " <NamedLayer> "+
            "  <UserStyle> "+
            "   <se:Name>UserSelection</se:Name> "+
            "   <se:FeatureTypeStyle> "+
            "    <se:Rule> "+
            "     <se:PolygonSymbolizer> "+
            "      <se:Fill> "+
            "       <se:SvgParameter name=\"fill\">#FF0000</se:SvgParameter> "+
            "      </se:Fill> "+
            "     </se:PolygonSymbolizer> "+
            "    </se:Rule> "+
            "   </se:FeatureTypeStyle> "+
            "  </UserStyle> "+
            " </NamedLayer> "+
            "</StyledLayerDescriptor>";

        MockHttpServletResponse response =
                postAsServletResponse( "/rest/styles?name=foo", xml, SLDHandler.MIMETYPE_11);
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/foo" ) );

        StyleInfo style = catalog.getStyleByName("foo");
        assertNotNull(style);

        assertEquals("sld", style.getFormat());
        assertEquals(SLDHandler.VERSION_11, style.getFormatVersion());
    }

    @Test
    public void testPostToWorkspaceSLDPackage() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("gs", "foo"));

        URL zip = getClass().getResource( "test-data/foo.zip" );
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse( "/rest/workspaces/gs/styles", bytes, "application/zip");
        assertEquals( 201, response.getStatus() );
        assertNotNull(cat.getStyleByName("gs", "foo"));

        Document d = getAsDOM("/rest/workspaces/gs/styles/foo.sld");

        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element)list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/foo.sld"));
    }
    
    @Test
    public void testPostWithExternalEntities() throws Exception {
        URL zip = getClass().getResource( "test-data/externalEntities.zip" );
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse( "/rest/workspaces/gs/styles", bytes, "application/zip");
        // expecting a failure with explanation
        assertEquals(400, response.getStatus() );
        final String content = response.getContentAsString();
        assertThat(content, containsString("Entity resolution disallowed"));
        assertThat(content, containsString("/this/file/does/not/exist"));
    }


    @Test
    public void testPutToWorkspaceSLDPackage() throws Exception {
        testPostAsSLDToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("gs", "foo"));

        URL zip = getClass().getResource( "test-data/foo.zip" );
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse( "/rest/workspaces/gs/styles/foo.zip", bytes, "application/zip");
        assertEquals( 200, response.getStatus() );
        assertNotNull(cat.getStyleByName("gs", "foo"));

        Document d = getAsDOM("/rest/workspaces/gs/styles/foo.sld");

        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element)list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/gs/styles/foo.sld"));
    }

    @Test
    public void testPostSLDPackage() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getStyleByName("foo"));

        URL zip = getClass().getResource( "test-data/foo.zip" );
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response =
                postAsServletResponse( "/rest/styles", bytes, "application/zip");
        assertEquals( 201, response.getStatus() );
        assertNotNull(cat.getStyleByName("foo"));

        Document d = getAsDOM("/rest/styles/foo.sld");

        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element)list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/foo.sld"));
    }

    @Test
    public void testPutSLDPackage() throws Exception {
        testPostAsSLD();

        Catalog cat = getCatalog();
        assertNotNull(cat.getStyleByName("foo"));

        URL zip = getClass().getResource( "test-data/foo.zip" );
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response =
                putAsServletResponse( "/rest/styles/foo.zip", bytes, "application/zip");
        assertEquals( 200, response.getStatus() );
        assertNotNull(cat.getStyleByName("foo"));

        Document d = getAsDOM("/rest/styles/foo.sld");

        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element)list.item(0);
        assertEquals("gear.png", onlineResource.getAttribute("xlink:href"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/gear.png"));
        assertNotNull(getCatalog().getResourceLoader().find("styles/foo.sld"));
    }
}
