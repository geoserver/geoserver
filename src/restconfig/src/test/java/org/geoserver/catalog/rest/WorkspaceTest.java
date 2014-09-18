/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class WorkspaceTest extends CatalogRESTTestSupport {

    @Before
    public void addWorkspaces() {
        getTestData().addWorkspace(SystemTestData.DEFAULT_PREFIX, SystemTestData.DEFAULT_URI, catalog);
        getTestData().addWorkspace(SystemTestData.SF_PREFIX, SystemTestData.SF_URI, catalog);
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces.xml");
        assertEquals( catalog.getNamespaces().size() , 
            dom.getElementsByTagName( "workspace").getLength() );
    }
    
    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces.json");
        assertTrue( json instanceof JSONObject );
        
        JSONArray workspaces = ((JSONObject)json).getJSONObject("workspaces").getJSONArray("workspace");
        assertNotNull( workspaces );
        
        assertEquals( catalog.getNamespaces().size() , workspaces.size() ); 
    }
    
    @Test
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
    
    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse( "/rest/workspaces" ).getStatusCode() );
    }
    
    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse( "/rest/workspaces").getStatusCode() );
    }
    
    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf.xml");
        assertEquals( "workspace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "name" ).getLength() );
        
        Element name = (Element) dom.getElementsByTagName( "name" ).item(0);
        assertEquals( "sf", name.getFirstChild().getTextContent() );
    }
    
    @Test
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
    
    @Test
    public void testGetWrongWorkspace() throws Exception {
        // Parameters for the request
        String workspace = "sfsssss";
        // Request path
        String requestPath = "/rest/workspaces/" + workspace + ".html";
        // Exception path
        String exception = "No such workspace: " + workspace;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getOutputStreamContent().contains(
                exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatusCode());
        assertFalse(response.getOutputStreamContent().contains(
                exception));
    }
    
    @Test
    public void testGetNonExistant() throws Exception {
        assertEquals( 404, getAsServletResponse( "/rest/workspaces/none").getStatusCode() );
    }
    
    @Test
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
    
    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf.json");
        JSONObject workspace = ((JSONObject) json).getJSONObject( "workspace") ;
        assertEquals( "sf", workspace.get( "name" ) );
    }
    
    @Test
    public void testPostAsJSON() throws Exception {
        removeWorkspace("foo");
        String json = "{'workspace':{ 'name':'foo' }}";
        
        MockHttpServletResponse response = postAsServletResponse( "/rest/workspaces", json, "text/json" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/foo" ) );
        
        
        WorkspaceInfo ws = getCatalog().getWorkspaceByName( "foo" );
        assertNotNull(ws);
    }
    
    @Test
    public void testPostToResource() throws Exception {
        String xml = 
            "<workspace>" +
              "<name>changed</name>" + 
            "</workspace>";
        
        MockHttpServletResponse response = 
            postAsServletResponse("/rest/workspaces/gs", xml, "text/xml" );
        assertEquals( 405, response.getStatusCode() );
    }
    
    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/workspaces/newExistant").getStatusCode() );
    }
    
    @Test
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
    
    @Test
    public void testDeleteNonEmptyForbidden() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
        assertEquals( 403, deleteAsServletResponse("/rest/workspaces/sf").getStatusCode() );
    }
    
    @Test
    public void testDeleteDefaultNotAllowed() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/default").getStatusCode() );
    }
    
    @Test
    public void testDeleteAllOneByOne() throws Exception {
        for(WorkspaceInfo ws : getCatalog().getWorkspaces()) {
            // empty the workspace otherwise we can't remove it
            CascadeDeleteVisitor visitor = new CascadeDeleteVisitor(getCatalog());
            for(StoreInfo store : getCatalog().getStoresByWorkspace(ws, StoreInfo.class)) {
                store.accept(visitor);
            }

            // actually go and remove the store
            String resource = "/rest/workspaces/" + ws.getName();
            assertEquals( 200, deleteAsServletResponse(resource).getStatusCode() );
            assertEquals( 404, getAsServletResponse(resource).getStatusCode() );
        }
        Document dom = getAsDOM( "/rest/workspaces.xml");
        assertEquals(0, dom.getElementsByTagName( "workspace").getLength() );
    }
    
    @Test
    public void testDeleteRecursive() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
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

    @Test
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
    
    @Test
    public void testPutNameChangeForbidden() throws Exception {
        String xml = 
            "<workspace>" +
              "<name>changed</name>" +
            "</workspace>";
        
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/gs", xml, "text/xml" );
        assertEquals( 403, response.getStatusCode() );
    }
    
    @Test
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
    
    @Test
    public void testGetDefaultWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/default.xml");
        
        assertEquals( "workspace", dom.getDocumentElement().getLocalName() );
        assertEquals( 1, dom.getElementsByTagName( "name" ).getLength() );
    }
    
    @Test
    public void testPutDefaultWorkspace() throws Exception {
        WorkspaceInfo def = getCatalog().getDefaultWorkspace();
        assertEquals( "gs", def.getName() );
        
        String json = "{'workspace':{ 'name':'sf' }}";
        put( "/rest/workspaces/default", json, "text/json");
        
        def = getCatalog().getDefaultWorkspace(); 
        assertEquals( "sf", def.getName() );
    }
}
