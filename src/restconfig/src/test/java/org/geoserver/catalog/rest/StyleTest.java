/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.styling.Style;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class StyleTest extends CatalogRESTTestSupport {
    
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/styles.xml" );
        
        List<StyleInfo> styles = catalog.getStyles();
        assertXpathEvaluatesTo(""+styles.size(), "count(//style)", dom);
    }
    
    public void testGetAllASJSON() throws Exception {
        JSON json = getAsJSON("/rest/styles.json");
        
        List<StyleInfo> styles = catalog.getStyles();
        assertEquals( styles.size(), 
            ((JSONObject) json).getJSONObject("styles").getJSONArray("style").size());
    }
    
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/styles.html");
        
        List<StyleInfo> styles = catalog.getStyles();
        NodeList links = xp.getMatchingNodes("//html:a", dom);

        for ( int i = 0; i < styles.size(); i++ ) {
            StyleInfo s = (StyleInfo) styles.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( s.getName()+ ".html"));
        }
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/styles/Ponds.xml" );
        
        assertEquals( "style", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("Ponds", "/style/name", dom);
        assertXpathEvaluatesTo("Ponds.sld", "/style/filename", dom);
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/styles/Ponds.json");
        
        JSONObject style =  ((JSONObject)json).getJSONObject("style"); 
        assertEquals( "Ponds", style.get( "name") );
        assertEquals( "Ponds.sld", style.get( "filename") );
    }
    
    public void testGetAsSLD() throws Exception {
        Document dom = getAsDOM( "/rest/styles/Ponds.sld");
        
        assertEquals( "sld:StyledLayerDescriptor", dom.getDocumentElement().getNodeName() );
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
    public void testPostAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, StyleResource.MEDIATYPE_SLD.toString());
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/foo" ) );
        
        assertNotNull( catalog.getStyleByName( "foo" ) );
    }
    
    public void testPostAsSLDWithName() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles?name=bar", xml, StyleResource.MEDIATYPE_SLD.toString());
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/styles/bar" ) );
        
        assertNotNull( catalog.getStyleByName( "bar" ) );
    }
    
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
        assertEquals( 200, response.getStatusCode() );
        
        style = catalog.getStyleByName( "Ponds");
        assertEquals( "Forests.sld", style.getFilename() );
    }
    
    public void testPutAsSLD() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/styles/Ponds", xml, StyleResource.MEDIATYPE_SLD.toString());
        assertEquals( 200, response.getStatusCode() );
        
        Style s = catalog.getStyleByName( "Ponds" ).getStyle();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        new SLDFormat().write(s, out);
        
        xml = new String(out.toByteArray());
        assertTrue(xml.contains("<sld:Name>foo</sld:Name>"));
    }
    
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
        assertEquals( 200, response.getStatusCode() );
        
        assertNull( catalog.getStyleByName( "dummy" ) );
    }
    
    public void testDeleteWithLayerReference() throws Exception {
        assertNotNull( catalog.getStyleByName( "Ponds" ) );
        
        MockHttpServletResponse response = 
            deleteAsServletResponse("/rest/styles/Ponds");
        assertEquals( 403, response.getStatusCode() );
         
        assertNotNull( catalog.getStyleByName( "Ponds" ) );
    }

    public void testDeleteWithoutPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, StyleResource.MEDIATYPE_SLD.toString());
        assertNotNull( catalog.getStyleByName( "foo" ) );
        
        //ensure the style not deleted on disk
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
        
        response = deleteAsServletResponse("/rest/styles/foo");
        assertEquals( 200, response.getStatusCode() );
        
        //ensure the style not deleted on disk
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
    }
    
    public void testDeleteWithPurge() throws Exception {
        String xml = newSLDXML();

        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/styles", xml, StyleResource.MEDIATYPE_SLD.toString());
        assertNotNull( catalog.getStyleByName( "foo" ) );
        
        //ensure the style not deleted on disk
        assertTrue(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
        
        response = deleteAsServletResponse("/rest/styles/foo?purge=true");
        assertEquals( 200, response.getStatusCode() );
        
        //ensure the style not deleted on disk
        assertFalse(new File(getDataDirectory().findStyleDir(), "foo.sld").exists());
    }
    
    public void testGetAllByLayer() throws Exception {
        Document dom = getAsDOM( "/rest/layers/cite:BasicPolygons/styles.xml");
        LayerInfo layer = catalog.getLayerByName( "cite:BasicPolygons" );
        
        assertXpathEvaluatesTo(layer.getStyles().size()+"", "count(//style)", dom );
    }
    
    public void testPostByLayer() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:BasicPolygons" );
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles", xml, "text/xml");
        assertEquals( 201, response.getStatusCode() );
        
        LayerInfo l2 = catalog.getLayerByName( "cite:BasicPolygons" );
        assertEquals( nstyles+1, l2.getStyles().size() );
        
        assertTrue( l2.getStyles().contains( catalog.getStyleByName( "Ponds") ) );
    }
    
    public void testPostByLayerWithDefault() throws Exception {
        LayerInfo l = catalog.getLayerByName( "cite:BasicPolygons" );
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles?default=true", xml, "text/xml");
        assertEquals( 201, response.getStatusCode() );
        
        LayerInfo l2 = catalog.getLayerByName( "cite:BasicPolygons" );
        assertEquals( nstyles+1, l2.getStyles().size() );
        assertEquals( catalog.getStyleByName( "Ponds"), l2.getDefaultStyle() );
    }
    
    public void testPostByLayerExistingWithDefault() throws Exception {
        testPostByLayer();
        
        LayerInfo l = catalog.getLayerByName("cite:BasicPolygons");
        int nstyles = l.getStyles().size();
        
        String xml = 
            "<style>" + 
              "<name>Ponds</name>" + 
            "</style>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/layers/cite:BasicPolygons/styles?default=true", xml, "text/xml");
        assertEquals( 201, response.getStatusCode() );
        
        LayerInfo l2 = catalog.getLayerByName("cite:BasicPolygons");
        assertEquals( nstyles, l2.getStyles().size() );
        assertEquals( catalog.getStyleByName( "Ponds"), l2.getDefaultStyle() );
    }
}
