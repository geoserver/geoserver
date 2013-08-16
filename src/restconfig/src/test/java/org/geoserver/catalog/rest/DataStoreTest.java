/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import net.sf.json.JSON;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.CascadeDeleteVisitor;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DataStoreTest extends CatalogRESTTestSupport {

    @Before
    public void addDataStores() throws IOException {
        // the store configuration gets ruined by tests in more than one way, let's recreate it
        DataStoreInfo sfStore = getCatalog().getDataStoreByName("sf");
        if(sfStore != null) {
            CascadeDeleteVisitor remover = new CascadeDeleteVisitor(getCatalog());
            remover.visit(sfStore);
        }
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, catalog);
        getTestData().addVectorLayer(SystemTestData.AGGREGATEGEOFEATURE, catalog);
        getTestData().addVectorLayer(SystemTestData.GENERICENTITY, catalog);
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores.xml");
        assertEquals( catalog.getStoresByWorkspace( "sf", DataStoreInfo.class ).size(), 
            dom.getElementsByTagName( "dataStore").getLength() );
    }

    @Test
    public void testGetAllAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/datastores.json");
        assertTrue( json instanceof JSONObject );
        
        Object datastores = ((JSONObject)json).getJSONObject("dataStores").get("dataStore");
        assertNotNull( datastores );
        
        if( datastores instanceof JSONArray ) {
            assertEquals( catalog.getDataStoresByWorkspace("sf").size() , ((JSONArray)datastores).size() );    
        }
        else {
            assertEquals( 1, catalog.getDataStoresByWorkspace("sf").size() );
        }
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores.html");
        List<DataStoreInfo> datastores = catalog.getDataStoresByWorkspace("sf"); 
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( datastores.size(), links.getLength() );
        
        for ( int i = 0; i < datastores.size(); i++ ){
            DataStoreInfo ds = datastores.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( ds.getName() + ".html") );
        }
    }

    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/sf/datastores").getStatusCode() );
    }

    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/sf/datastores").getStatusCode() );
    }

    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf.xml");
        assertEquals( "dataStore", dom.getDocumentElement().getNodeName() );
        assertEquals( "sf", xp.evaluate( "/dataStore/name", dom) );
        assertEquals( "sf", xp.evaluate( "/dataStore/workspace/name", dom) );
        assertXpathExists( "/dataStore/connectionParameters", dom );
    }

    @Test
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf.html");
        
        DataStoreInfo ds = catalog.getDataStoreByName( "sf" );
        List<FeatureTypeInfo> featureTypes = catalog.getFeatureTypesByDataStore( ds );
        
        NodeList links = xp.getMatchingNodes("//html:a", dom );
        assertEquals( featureTypes.size(), links.getLength() );
        
        for ( int i = 0; i < featureTypes.size(); i++ ){
            FeatureTypeInfo ft = featureTypes.get( i );
            Element link = (Element) links.item( i );
            
            assertTrue( link.getAttribute("href").endsWith( ft.getName() + ".html") );
        }
    }
    
    File setupNewDataStore() throws Exception {
        Properties props = new Properties();
        props.put( "_", "name:StringpointProperty:Point");
        props.put( "NewDataStore.0", "'zero'|POINT(0 0)");
        props.put( "NewDataStore.1", "'one'|POINT(1 1)");
        
        File dir = new File( "./target/nds" );
        dir.mkdir();
        
        File file = new File( dir, "newDataStore.properties");
        file.deleteOnExit();
        dir.deleteOnExit();
        
        props.store( new FileOutputStream( file ), null );
        return dir;
    }

    @Test
    public void testPostAsXML() throws Exception {
        
        File dir = setupNewDataStore();
        String xml =
            "<dataStore>" +
              "<name>newDataStore</name>" +
              "<connectionParameters>" +
                 "<namespace><string>sf</string></namespace>" + 
                 "<directory>" +
                  "<string>" + dir.getAbsolutePath() + "</string>" +
                 "</directory>" + 
              "</connectionParameters>" + 
              "<workspace>sf</workspace>" + 
            "</dataStore>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/datastores", xml, "text/xml" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/datastores/newDataStore" ) );

        DataStoreInfo newDataStore = catalog.getDataStoreByName( "newDataStore" );
        assertNotNull( newDataStore );
        
        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
    }

    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/datastores/sf.json" );
        
        JSONObject dataStore = ((JSONObject)json).getJSONObject("dataStore");
        assertNotNull(dataStore);
        
        assertEquals( "sf", dataStore.get( "name") );
        assertEquals( "sf", dataStore.getJSONObject( "workspace").get( "name" ) );
        assertNotNull( dataStore.get( "connectionParameters") );
    }

    @Test
    public void testPostAsJSON() throws Exception {
        removeStore("sf", "newDataStore");
        File dir = setupNewDataStore();
        String json = 
            "{'dataStore':{" +
               "'connectionParameters': {" +
                   "'namespace': {'string':'sf'}," +
                   "'directory': {'string':'" + dir.getAbsolutePath().replace('\\','/')  + "'}" +
                "}," +
                "'workspace':'sf'," +
                "'name':'newDataStore'," +
              "}" +
            "}";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/datastores", json, "text/json" );
        
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/datastores/newDataStore" ) );
        
        DataStoreInfo newDataStore = catalog.getDataStoreByName( "newDataStore" );
        assertNotNull( newDataStore );
        
        DataStore ds = (DataStore) newDataStore.getDataStore(null);
        assertNotNull(ds);
    }

    @Test
    public void testPostToResource() throws Exception {
        String xml = 
        "<dataStore>" + 
         "<name>sf</name>" + 
         "<enabled>false</enabled>" + 
        "</dataStore>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/datastores/sf", xml, "text/xml");
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
    public void testPut2() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/datastores/sf.xml");
        assertXpathEvaluatesTo("2", "count(//dataStore/connectionParameters/*)", dom );
        
        String xml = 
        "<dataStore>" + 
         "<name>sf</name>" + 
         "<connectionParameters>" +  
          "<one>" + 
           "<string>1</string>" + 
         "</one>" + 
         "<two>"+ 
           "<string>2</string>" + 
         "</two>" + 
        "</connectionParameters>"+ 
        "</dataStore>";
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/sf/datastores/sf", xml, "text/xml");
        assertEquals( 200, response.getStatusCode() );
        
        DataStoreInfo ds = catalog.getDataStoreByName( "sf", "sf" );
        assertEquals( 2, ds.getConnectionParameters().size() );
        assertTrue( ds.getConnectionParameters().containsKey( "one" ) );
        assertTrue( ds.getConnectionParameters().containsKey( "two" ) );
    }

    @Test
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<dataStore>" + 
            "<name>changed</name>" + 
            "</dataStore>";

        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/datastores/nonExistant", xml, "text/xml" );
        assertEquals( 404, response.getStatusCode() );
    }

    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404, deleteAsServletResponse("/rest/workspaces/sf/datastores/nonExistant").getStatusCode() );
    }

    @Test
    public void testDelete() throws Exception {
        removeStore("sf", "newDataStore");
        File dir = setupNewDataStore();
        String xml =
            "<dataStore>" +
              "<name>newDataStore</name>" +
              "<connectionParameters>" +
                "<entry>" + 
                  "<string>namespace</string>" +
                  "<string>sf</string>" +
                "</entry>" + 
                "<entry>" + 
                  "<string>directory</string>" +
                  "<string>" + dir.getAbsolutePath() + "</string>" +
                "</entry>" + 
              "</connectionParameters>" + 
              "<workspace>sf</workspace>" + 
            "</dataStore>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/datastores", xml, "text/xml" );
        assertEquals( 201, response.getStatusCode() );
        assertNotNull( catalog.getDataStoreByName("sf", "newDataStore"));
        
        assertEquals( 200, deleteAsServletResponse("/rest/workspaces/sf/datastores/newDataStore").getStatusCode());
        assertNull( catalog.getDataStoreByName("sf", "newDataStore"));
    }

    @Test
    public void testDeleteNonEmptyForbidden() throws Exception {
        assertEquals( 403, deleteAsServletResponse("/rest/workspaces/sf/datastores/sf").getStatusCode());
    }

    @Test
    public void testDeleteRecursive() throws Exception {
        assertNotNull(catalog.getDataStoreByName("sf", "sf"));
        MockHttpServletResponse response =
            deleteAsServletResponse("/rest/workspaces/sf/datastores/sf?recurse=true");
        assertEquals(200, response.getStatusCode());

        assertNull(catalog.getDataStoreByName("sf", "sf"));
        
        for (FeatureTypeInfo ft : catalog.getFeatureTypes()) {
            if (ft.getStore().getName().equals("sf")) {
                fail();
            }
        }
    }

    @Test
    public void testPutNameChangeForbidden() throws Exception {
        getTestData().addVectorLayer(SystemTestData.PRIMITIVEGEOFEATURE, getCatalog());
        String xml = "<dataStore>" +
            "<name>newName</name>" + 
            "</dataStore>";
        assertEquals( 403, putAsServletResponse("/rest/workspaces/sf/datastores/sf", xml, "text/xml").getStatusCode());
    }

    @Test
    public void testPutWorkspaceChangeForbidden() throws Exception {
        String xml = "<dataStore>" +
        "<workspace>gs</workspace>" + 
        "</dataStore>";
        assertEquals( 403, putAsServletResponse("/rest/workspaces/sf/datastores/sf", xml, "text/xml").getStatusCode());
    }
}
