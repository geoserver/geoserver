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

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WorkspaceTest extends CatalogRESTTestSupport {

    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces.xml");
        assertEquals( catalog.getNamespaces().size() , 
            dom.getElementsByTagName( "workspace").getLength() );
    }
    
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces.json");
        assertTrue( json instanceof JSONObject );
        
        JSONArray workspaces = ((JSONObject)json).getJSONObject("workspaces").getJSONArray("workspace");
        assertNotNull( workspaces );
        
        assertEquals( catalog.getNamespaces().size() , workspaces.size() ); 
    }
    
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces.html" );
        
        List<WorkspaceInfo> workspaces = catalog.getWorkspaces(); 
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( workspaces.size(), links.getLength() );
        
        for ( int i = 0; i < workspaces.size(); i++ ){
            WorkspaceInfo ws = workspaces.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( ws.getName() + ".html") );
        }
    }
    
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse( "/rest/workspaces" ).getStatusCode() );
    }
    
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse( "/rest/workspaces").getStatusCode() );
    }
    
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf.xml");
        assertEquals( "workspace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "name" ).getLength() );
        
        Element name = (Element) dom.getElementsByTagName( "name" ).item(0);
        assertEquals( "sf", name.getFirstChild().getTextContent() );
    }
    
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf.html");

        List<StoreInfo> stores = catalog.getStoresByWorkspace("sf",StoreInfo.class); 
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( stores.size(), links.getLength() );
        
        for ( int i = 0; i < stores.size(); i++ ){
            StoreInfo store = stores.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( store.getName() + ".html") );
        }
    }
    
    public void testGetNonExistant() throws Exception {
        assertEquals( 404, getAsServletResponse( "/rest/workspaces/none").getStatusCode() );
    }
    
    public void testPostAsXML() throws Exception {
        String xml = 
            "<workspace>" + 
              "<name>foo</name>" + 
            "</workspace>";
        MockHttpServletResponse response = postAsServletResponse( "/rest/workspaces", xml, "text/xml" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/foo" ) );
        
        WorkspaceInfo ws = getCatalog().getWorkspaceByName( "foo" );
        assertNotNull(ws);
    }
    
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf.json");
        JSONObject workspace = ((JSONObject) json).getJSONObject( "workspace") ;
        assertEquals( "sf", workspace.get( "name" ) );
    }
    
    public void testPostAsJSON() throws Exception {
        String json = "{'workspace':{ 'name':'foo' }}";
        
        MockHttpServletResponse response = postAsServletResponse( "/rest/workspaces", json, "text/json" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/foo" ) );
        
        
        WorkspaceInfo ws = getCatalog().getWorkspaceByName( "foo" );
        assertNotNull(ws);
    }
    
    public void testPostToResource() throws Exception {
        String xml = 
            "<workspace>" +
              "<name>changed</name>" + 
            "</workspace>";
        
        MockHttpServletResponse response = 
            postAsServletResponse("/rest/workspaces/gs", xml, "text/xml" );
        assertEquals( 405, response.getStatusCode() );
    }
    
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/workspaces/newExistant").getStatusCode() );
    }
    
    public void testDelete() throws Exception {
        String xml = 
            "<workspace>" +
              "<name>foo</name>" +
            "</workspace>";
        post( "/rest/workspaces", xml);
        
        Document dom = getAsDOM( "/rest/workspaces/foo.xml");
        assertEquals( "workspace", dom.getDocumentElement().getNodeName() );
        
        assertEquals( 200, deleteAsServletResponse( "/rest/workspaces/foo" ).getStatusCode() );
        assertEquals( 404, getAsServletResponse( "/rest/workspaces/foo.xml" ).getStatusCode() );
    }
    
    public void testDeleteNonEmptyForbidden() throws Exception {
        assertEquals( 403, deleteAsServletResponse("/rest/workspaces/sf").getStatusCode() );
    }
    
    public void testDeleteDefaultNotAllowed() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/default").getStatusCode() );
    }
    
    public void testDeleteAllOneByOne() throws Exception {
        for(WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            // empty the workspace otherwise we can't remove it
            CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(getCatalog());
            for(StoreInfo store : getCatalog().getStoresByWorkspace(ws, StoreInfo.class)) {
                store.accept(visitor);
            }

            // actually go and remove the store
            String resource = "/rest/workspaces/" + ws.getName();
            System.out.println(resource);
            assertEquals( 200, deleteAsServletResponse(resource).getStatusCode() );
            assertEquals( 404, getAsServletResponse(resource).getStatusCode() );
        }
        Document dom = getAsDOM( "/rest/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName( "workspace").getLength() );
    }
    
    public void testDeleteRecursive() throws Exception {
        List<StoreInfo> stores = catalog.getStoresByWorkspace("sf", StoreInfo.class); 
        assertFalse(stores.isEmpty());

        MockHttpServletResponse response =
            deleteAsServletResponse("/rest/workspaces/sf?recurse=true");
        assertEquals(200, response.getStatusCode());

        assertNull(catalog.getWorkspaceByName("sf"));
        assertNull(catalog.getNamespaceByPrefix("sf"));
        
        for (StoreInfo s : stores) {
            assertNull(catalog.getStoreByName(s.getName(), StoreInfo.class));
        }
        
    }

    public void testPut() throws Exception {
        String xml = 
            "<workspace>" +
              "<metadata>" +
                "<foo>" +
                   "<string>bar</string>" + 
                "</foo>" + 
              "</metadata>" +
            "</workspace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/gs", xml, "text/xml" );
        assertEquals( 200, response.getStatusCode() );
        
        Document dom = getAsDOM( "/rest/workspaces/gs.xml" );
        assertXpathEvaluatesTo("1", "count(//name[text()='gs'])", dom );
        assertXpathEvaluatesTo("1", "count(//entry[@key='foo' and text()='bar'])", dom );
        
    }
    
    public void testPutNameChangeForbidden() throws Exception {
        String xml = 
            "<workspace>" +
              "<name>changed</name>" +
            "</workspace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/gs", xml, "text/xml" );
        assertEquals( 403, response.getStatusCode() );
    }
    
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<workspace>" +
            "<metadata>" +
            "<entry>" +
               "<string>foo</string>" +
               "<string>bar</string>" + 
            "</entry>" + 
          "</metadata>" +
            "</workspace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/nonExistant", xml, "text/xml" );
        assertEquals( 404, response.getStatusCode() );
    }
    
    public void testGetDefaultWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/default.xml");
        
        assertEquals( "workspace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "name" ).getLength() );
    }
    
    public void testPutDefaultWorkspace() throws Exception {
        WorkspaceInfo def = getCatalog().getDefaultWorkspace();
        assertEquals( "gs", def.getName() );
        
        String json = "{'workspace':{ 'name':'sf' }}";
        put( "/rest/workspaces/default", json, "text/json");
        
        def = getCatalog().getDefaultWorkspace(); 
        assertEquals( "sf", def.getName() );
    }
}
