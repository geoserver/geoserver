/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.data.test.MockData;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class NamespaceTest extends CatalogRESTTestSupport {

    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/namespaces.xml");
        assertEquals( catalog.getNamespaces().size() , 
            dom.getElementsByTagName( "namespace").getLength() );
    }
    
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/namespaces.json");
        assertTrue( json instanceof JSONObject );
        
        JSONArray namespaces = ((JSONObject)json).getJSONObject("namespaces").getJSONArray("namespace");
        assertNotNull( namespaces );
        
        assertEquals( catalog.getNamespaces().size() , namespaces.size() ); 
    }
    
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/namespaces.html" );
        
        List<NamespaceInfo> namespaces = catalog.getNamespaces();
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( namespaces.size(), links.getLength() );
        
        for ( int i = 0; i < namespaces.size(); i++ ){
            NamespaceInfo ws = namespaces.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( ws.getPrefix() + ".html") );
        }
    }
    
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse( "/rest/namespaces" ).getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse( "/rest/namespaces").getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/namespaces/sf.xml");
        assertEquals( "namespace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "prefix" ).getLength() );
        
        Element prefix = (Element) dom.getElementsByTagName( "prefix" ).item(0);
        assertEquals( "sf", prefix.getFirstChild().getTextContent() );
        
        Element name = (Element) dom.getElementsByTagName( "uri" ).item(0);
        assertEquals( MockData.SF_URI, name.getFirstChild().getTextContent() );
    }
    
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/namespaces/sf.html");

        List<ResourceInfo> resources = catalog.getResourcesByNamespace("sf",ResourceInfo.class); 
        NodeList listItems = xp.getMatchingNodes("//html:li", dom );
        assertEquals( resources.size(), listItems.getLength() );
        
        for ( int i = 0; i < resources.size(); i++ ){
            ResourceInfo resource = resources.get( i );
            Element listItem = (Element) listItems.item( i );
            
            assertTrue( listItem.getFirstChild().getNodeValue().endsWith( resource.getName() ) );
        }
    }
    
    public void testGetNonExistant() throws Exception {
        assertEquals( 404, getAsServletResponse( "/rest/namespaces/none").getStatusCode() );
    }
    
    public void testPostAsXML() throws Exception {
        String xml = 
            "<namespace>" + 
              "<prefix>foo</prefix>" +
              "<uri>http://foo.com</uri>" +
            "</namespace>";
        MockHttpServletResponse response = postAsServletResponse( "/rest/namespaces", xml, "text/xml" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/namespaces/foo" ) );
        
        NamespaceInfo ws = getCatalog().getNamespaceByPrefix( "foo" );
        assertNotNull(ws);
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/namespaces/sf.json");
        JSONObject namespace = ((JSONObject) json).getJSONObject( "namespace") ;
        assertEquals( "sf", namespace.get( "prefix" ) );
        assertEquals( MockData.SF_URI, namespace.get( "uri" ) );
    }
    
    public void testPostAsJSON() throws Exception {
        String json = "{'namespace':{ 'prefix':'foo', 'uri':'http://foo.com' }}";
        
        MockHttpServletResponse response = postAsServletResponse( "/rest/namespaces", json, "text/json" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/namespaces/foo" ) );
        
        
        NamespaceInfo ws = getCatalog().getNamespaceByPrefix( "foo" );
        assertNotNull(ws);
    }
    
    public void testPostToResource() throws Exception {
        String xml = 
            "<namespace>" +
              "<name>changed</name>" + 
            "</namespace>";
        
        MockHttpServletResponse response = 
            postAsServletResponse("/rest/namespaces/gs", xml, "text/xml" );
        assertEquals( 405, response.getStatusCode() );
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/namespaces/newExistant").getStatusCode() );
    }
    
    public void testDelete() throws Exception {
        String xml = 
            "<namespace>" +
              "<prefix>foo</prefix>" + 
              "<uri>http://foo.com</uri>" +
            "</namespace>";
        post( "/rest/namespaces", xml );
        
        Document dom = getAsDOM( "/rest/namespaces/foo.xml");
        assertEquals( "namespace", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 200, deleteAsServletResponse( "/rest/namespaces/foo" ).getStatusCode() );
        assertEquals( 404, getAsServletResponse( "/rest/namespaces/foo.xml" ).getStatusCode() );
    }
    
    public void testDeleteNonEmpty() throws Exception {
        assertEquals( 401, deleteAsServletResponse("/rest/namespaces/sf").getStatusCode() );
    }
    
    public void testPut() throws Exception {
        String xml = 
            "<namespace>" +
              "<uri>http://changed</uri>" + 
            "</namespace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/namespaces/gs", xml, "text/xml" );
        assertEquals( 200, response.getStatusCode() );
        
        Document dom = getAsDOM( "/rest/namespaces/gs.xml" );
        assertXpathEvaluatesTo("1", "count(//namespace/uri[text()='http://changed'])", dom );
    }
    
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<namespace>" +
              "<name>changed</name>" + 
            "</namespace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/namespaces/nonExistant", xml, "text/xml" );
        assertEquals( 404, response.getStatusCode() );
    }
    
    public void testGetDefaultNamespace() throws Exception {
        Document dom = getAsDOM( "/rest/namespaces/default.xml");
        
        assertEquals( "namespace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "prefix" ).getLength() );
        assertEquals( 1, dom.getElementsByTagName( "uri" ).getLength() );
    }
    
    public void testPutDefaultNamespace() throws Exception {
        NamespaceInfo def = getCatalog().getDefaultNamespace();
        assertEquals( "gs", def.getPrefix() );
        
        String json = "{'namespace':{ 'prefix':'sf' }}";
        put( "/rest/namespaces/default", json, "text/json");
        
        def = getCatalog().getDefaultNamespace(); 
        assertEquals( "sf", def.getPrefix() );
    }
}
