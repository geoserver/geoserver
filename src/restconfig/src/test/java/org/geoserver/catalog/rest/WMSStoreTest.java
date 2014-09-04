/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.*;

import java.util.List;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;


public class WMSStoreTest extends CatalogRESTTestSupport {
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        // we need to add a wms store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        catalog.add(wms);
    } 

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores.xml");
        assertEquals("wmsStores", dom.getDocumentElement().getNodeName());
        assertEquals( catalog.getStoresByWorkspace( "sf", WMSStoreInfo.class ).size(), 
            dom.getElementsByTagName( "wmsStore").getLength() );
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/wmsstores.json");
        assertTrue( json instanceof JSONObject );
        
        Object stores = ((JSONObject)json).getJSONObject("wmsStores").get("wmsStore");
        assertNotNull( stores );
        
        if( stores instanceof JSONArray ) {
            assertEquals( catalog.getStoresByWorkspace("sf", WMSStoreInfo.class).size() , ((JSONArray)stores).size() );    
        } else {
            assertEquals( 1, catalog.getStoresByWorkspace("sf", WMSStoreInfo.class).size() );
        }
    }
    
    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores.html");
        List<WMSStoreInfo> stores = catalog.getStoresByWorkspace("sf", WMSStoreInfo.class);
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( stores.size(), links.getLength() );
        
        for ( int i = 0; i < stores.size(); i++ ){
            WMSStoreInfo store = stores.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( store.getName() + ".html") );
        }
    }
    
    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/sf/wmsstores").getStatusCode() );
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/sf/wmsstores").getStatusCode() );
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores/demo.xml");
        assertEquals( "wmsStore", dom.getDocumentElement().getNodeName() );
        assertEquals( "demo", xp.evaluate( "/wmsStore/name", dom) );
        assertEquals( "sf", xp.evaluate( "/wmsStore/workspace/name", dom) );
        assertXpathExists( "/wmsStore/capabilitiesURL", dom );
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores/demo.html");
        
        WMSStoreInfo wms = catalog.getStoreByName( "demo", WMSStoreInfo.class );
        List<WMSLayerInfo> wmsLayers = catalog.getResourcesByStore( wms, WMSLayerInfo.class );
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( wmsLayers.size(), links.getLength() );
        
        for ( int i = 0; i < wmsLayers.size(); i++ ){
            WMSLayerInfo wl = wmsLayers.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( wl.getName() + ".html") );
        }
    }
   
    @Test
    public void testPostAsXML() throws Exception {
        
        String xml =
            "<wmsStore>" +
              "<name>newWMSStore</name>" +
              "<capabilitiesURL>http://somehost/wms?</capabilitiesURL>" +
              "<workspace>sf</workspace>" + 
            "</wmsStore>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/wmsstores", xml, "text/xml" );
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/wmsstores/newWMSStore" ) );

        WMSStoreInfo newStore = catalog.getStoreByName( "newWMSStore", WMSStoreInfo.class );
        assertNotNull( newStore );
        
        assertEquals("http://somehost/wms?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/wmsstores/demo.json" );
        
        JSONObject store = ((JSONObject)json).getJSONObject("wmsStore");
        assertNotNull(store);
        
        assertEquals( "demo", store.get( "name") );
        assertEquals( "sf", store.getJSONObject( "workspace").get( "name" ) );
        assertEquals( "http://demo.opengeo.org/geoserver/wms?", store.getString( "capabilitiesURL") );
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("sf", "newWMSStore");
        String json = 
            "{'wmsStore':{" +
               "'capabilitiesURL': 'http://somehost/wms?'," +
                "'workspace':'sf'," +
                "'name':'newWMSStore'," +
              "}" +
            "}";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/wmsstores", json, "text/json" );
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/wmsstores/newWMSStore" ) );

        WMSStoreInfo newStore = catalog.getStoreByName( "newWMSStore", WMSStoreInfo.class );
        assertNotNull( newStore );
        
        assertEquals("http://somehost/wms?", newStore.getCapabilitiesURL());
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = 
        "<wmsStore>" + 
         "<name>demo</name>" + 
         "<enabled>false</enabled>" + 
        "</wmsStore>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/wmsstores/demo", xml, "text/xml");
        assertEquals( 405, response.getStatusCode() );
    }

    @Test
    public void testPut() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("true", "/dataStore/enabled", dom );
        
        String xml = 
        "<dataStore>" + 
         "<name>sf</name>" + 
         "<enabled>false</enabled>" + 
        "</dataStore>";
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );

        dom = getAsDOM( "/rest/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("false", "/dataStore/enabled", dom );
        
        assertFalse( catalog.getDataStoreByName( "sf", "sf").isEnabled() );
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<wmsStore>" + 
            "<name>changed</name>" + 
            "</wmsStore>";

        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/wmsstores/nonExistant", xml, "text/xml" );
        assertEquals( 404, response.getStatusCode() );
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/workspaces/sf/datastores/nonExistant").getStatusCode() );
    }

    @Test
    public void testDelete() throws Exception {
        removeStore("sf", "newWMSStore");
        testPostAsXML();
        assertNotNull( catalog.getStoreByName("sf", "newWMSStore", WMSStoreInfo.class));
        
        assertEquals( 200, deleteAsServletResponse("/rest/workspaces/sf/wmsstores/newWMSStore").getStatusCode());
        assertNull( catalog.getStoreByName("sf", "newWMSStore", WMSStoreInfo.class));
    }
    
//    public void testDeleteNonEmptyForbidden() throws Exception {
//        assertEquals( 403, deleteAsServletResponse("/rest/workspaces/sf/datastores/sf").getStatusCode());
//    }
    
    @Test
    public void testPutNameChangeForbidden() throws Exception {
        String xml = "<wmsStore>" +
            "<name>newName</name>" + 
            "</wmsStore>";
        assertEquals( 403, putAsServletResponse("/rest/workspaces/sf/wmsstores/demo", xml, "text/xml").getStatusCode());
    }

    @Test
    public void testPutWorkspaceChangeForbidden() throws Exception {
        String xml = "<wmsStore>" +
        "<workspace>gs</workspace>" + 
        "</wmsStore>";
        assertEquals( 403, putAsServletResponse("/rest/workspaces/sf/wmsstores/demo", xml, "text/xml").getStatusCode());
    }
}
