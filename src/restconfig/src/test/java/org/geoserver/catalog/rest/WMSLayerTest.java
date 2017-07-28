/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;
import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.RemoteOWSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.w3c.dom.Document;

import org.springframework.mock.web.MockHttpServletResponse;

import java.util.List;

public class WMSLayerTest extends CatalogRESTTestSupport {
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        // we need to add a wms store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(catalog.getWorkspaceByName("sf"));
        WMSStoreInfo wms = cb.buildWMSStore("demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        catalog.add(wms);
        
        // and a wms layer as well (cannot use the builder, would turn this test into an online one
        addStatesWmsLayer();
    } 

    @Before
    public void addStatesWmsLayer() throws Exception {
        WMSLayerInfo wml = catalog.getResourceByName("sf", "states", WMSLayerInfo.class);
        if (wml == null) {
            wml = catalog.getFactory().createWMSLayer();
            wml.setName("states");
            wml.setNativeName("topp:states");
            wml.setStore(catalog.getStoreByName("demo", WMSStoreInfo.class));
            wml.setCatalog(catalog);
            wml.setNamespace(catalog.getNamespaceByPrefix("sf"));
            wml.setSRS("EPSG:4326");
            CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
            wml.setNativeCRS(wgs84);
            wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
            wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);
            
            catalog.add(wml);
        }
    }
    
    @After
    public void removeLayer() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "states"));
        if(l != null) {
            catalog.remove(l);
        }
    }

    @Before
    public void removeBugsites() throws Exception {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "bugsites"));
        if(l != null) {
            catalog.remove(l);
        }

        ResourceInfo r = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        if (r != null) {
            catalog.remove(r);
        }
    }
    @Test
    public void testGetAllByWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmslayers.xml");
        assertEquals( 
            catalog.getResourcesByNamespace( catalog.getNamespaceByPrefix( "sf"), WMSLayerInfo.class ).size(), 
            dom.getElementsByTagName( "wmsLayer").getLength() );
    }

    @Test
    public void testGetAllByWMSStore() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores/demo/wmslayers.xml");
        
        assertEquals( 1, dom.getElementsByTagName( "wmsLayer").getLength() );
        assertXpathEvaluatesTo( "1", "count(//wmsLayer/name[text()='states'])", dom );
    }
    
    @Test
    public void testGetAllAvailable() throws Exception {
        if(!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.warning("Skipping layer availability test as remote server is not available");
            return;
        }
        
        Document dom = getAsDOM( "/rest/workspaces/sf/wmsstores/demo/wmslayers.xml?list=available");
        // print(dom);
        
        // can't control the demo server enough to check the type names, but it should have something
        // more than just topp:states
        assertXpathEvaluatesTo("true", "count(//wmsLayerName) > 0", dom);
    }
    
    @Test
    public void testPutAllUnauthorized() throws Exception {
        assertEquals( 405, putAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers").getStatus() );
    }
    
    @Test
    public void testDeleteAllUnauthorized() throws Exception {
        assertEquals( 405, deleteAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers").getStatus() );
    }
 
    @Test
    public void testPostAsXML() throws Exception {
        if(!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.warning("Skipping layer posting test as remote server is not available");
            return;
        }
        
        assertNull(catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class));
        
        String xml = 
          "<wmsLayer>"+
            "<name>bugsites</name>"+
            "<nativeName>og:bugsites</nativeName>"+
            "<srs>EPSG:4326</srs>" + 
            "<nativeCRS>EPSG:4326</nativeCRS>" + 
            "<store>demo</store>" + 
          "</wmsLayer>";
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/", xml, "text/xml");
        
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/wmsstores/demo/wmslayers/bugsites" ) );
        
        WMSLayerInfo layer = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        assertNotNull(layer.getNativeBoundingBox());
    }
    
    @Test
    public void testPostAsJSON() throws Exception {
        if(!RemoteOWSTestSupport.isRemoteWMSStatesAvailable(LOGGER)) {
            LOGGER.warning("Skipping layer posting test as remote server is not available");
            return;
        }
        
        assertNull(catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class));
        
        String json = 
          "{" + 
           "'wmsLayer':{" + 
              "'name':'bugsites'," +
              "'nativeName':'og:bugsites'," +
              "'srs':'EPSG:4326'," +
              "'nativeCRS':'EPSG:4326'," +
              "'store':'demo'" +
             "}" +
          "}";
        MockHttpServletResponse response =  
            postAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/", json, "text/json");
        
        assertEquals( 201, response.getStatus() );
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/workspaces/sf/wmsstores/demo/wmslayers/bugsites" ) );
        
        WMSLayerInfo layer = catalog.getResourceByName("sf", "bugsites", WMSLayerInfo.class);
        assertNotNull(layer.getNativeBoundingBox());
    }
    
    @Test
    public void testPostToResource() throws Exception {
        String xml = 
            "<wmsLayer>"+
              "<name>og:restricted</name>"+
            "</wmsLayer>";
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/states", xml, "text/xml");
        assertEquals( 405, response.getStatus() );
    }
    
    @Test
    public void testGetAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmslayers/states.xml");
        
        assertEquals( "wmsLayer", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("states", "/wmsLayer/name", dom);
        assertXpathEvaluatesTo( "EPSG:4326", "/wmsLayer/srs", dom);
        assertEquals( CRS.decode( "EPSG:4326" ).toWKT(), xp.evaluate( "/wmsLayer/nativeCRS", dom ) );
        
        WMSLayerInfo wml = catalog.getResourceByName( "sf", "states", WMSLayerInfo.class );
        
        ReferencedEnvelope re = wml.getLatLonBoundingBox();
        assertXpathEvaluatesTo(  re.getMinX()+"" , "/wmsLayer/latLonBoundingBox/minx", dom );
        assertXpathEvaluatesTo(  re.getMaxX()+"" , "/wmsLayer/latLonBoundingBox/maxx", dom );
        assertXpathEvaluatesTo(  re.getMinY()+"" , "/wmsLayer/latLonBoundingBox/miny", dom );
        assertXpathEvaluatesTo(  re.getMaxY()+"" , "/wmsLayer/latLonBoundingBox/maxy", dom );
    }
    
    @Test
    public void testGetAsJSON() throws Exception {
        JSON json = getAsJSON( "/rest/workspaces/sf/wmslayers/states.json");
        JSONObject featureType = ((JSONObject)json).getJSONObject("wmsLayer");
        assertNotNull(featureType);
        
        assertEquals( "states", featureType.get("name") );
        assertEquals( CRS.decode("EPSG:4326").toWKT(), featureType.get( "nativeCRS") );
        assertEquals( "EPSG:4326", featureType.get( "srs") );
    }
    
    @Test
    public void testGetAsHTML() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/wmslayers/states.html");
        // print(dom);
    }


    @Test
    public void testGetInWorkspace() throws Exception {
        Catalog catalog = getCatalog();

        List<WorkspaceInfo> workspaces = catalog.getWorkspaces();

        //The default workspace
        WorkspaceInfo ws1 = catalog.getDefaultWorkspace();
        //Some other workspace, not the default
        WorkspaceInfo ws2 = workspaces.get(0);
        if (ws1.equals(ws2)) {
            ws2 = workspaces.get(1);
        }

        //Add an identically named store to each workspace

        // we need to add a wms store
        CatalogBuilder cb = new CatalogBuilder(catalog);
        cb.setWorkspace(ws1);
        WMSStoreInfo wms = cb.buildWMSStore("ws_demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        catalog.add(wms);

        // and a wms layer as well (cannot use the builder, would turn this test into an online one)
        WMSLayerInfo wml = catalog.getFactory().createWMSLayer();
        wml.setName("foo");
        wml.setNativeName("topp:states");
        wml.setStore(catalog.getStoreByName(ws1, "ws_demo", WMSStoreInfo.class));
        wml.setCatalog(catalog);
        wml.setNamespace(catalog.getNamespaceByPrefix(ws1.getName()));
        wml.setSRS("EPSG:4326");
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326");
        wml.setNativeCRS(wgs84);
        wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
        wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        catalog.add(wml);

        // repeat for ws2
        cb.setWorkspace(ws2);
        wms = cb.buildWMSStore("ws_demo");
        wms.setCapabilitiesURL("http://demo.opengeo.org/geoserver/wms?");
        catalog.add(wms);


        //use a different name for the layer
        wml = catalog.getFactory().createWMSLayer();
        wml.setName("bar");
        wml.setNativeName("topp:states");
        wml.setStore(catalog.getStoreByName(ws2, "ws_demo", WMSStoreInfo.class));
        wml.setCatalog(catalog);
        wml.setNamespace(catalog.getNamespaceByPrefix(ws2.getName()));
        wml.setSRS("EPSG:4326");
        wml.setNativeCRS(wgs84);
        wml.setLatLonBoundingBox(new ReferencedEnvelope(-110, 0, -60, 50, wgs84));
        wml.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

        catalog.add(wml);

        //Try to get the one in ws2 (the non-default workspace
        Document dom = getAsDOM( "/rest/workspaces/" + ws2.getName() + "/wmsstores/ws_demo/wmslayers.xml");

        //make sure we've got the one in the right workspace (not the default)
        assertXpathEvaluatesTo( "1", "count(//wmsLayer/name[text()='bar'])", dom );
        assertXpathEvaluatesTo( "0", "count(//wmsLayer/atom:link[contains(@href,'"+ws1.getName()+"')])", dom );
        assertXpathEvaluatesTo( "1", "count(//wmsLayer/atom:link[contains(@href,'"+ws2.getName()+"')])", dom );

    }

    @Test
    public void testGetWrongWMSLayer() throws Exception {
        // Parameters for the request
        String ws = "sf";
        String wms = "demo";
        String wl = "statessssss";
        // Request path
        String requestPath = "/rest/workspaces/" + ws + "/wmslayers/" + wl + ".html";
        String requestPath2 = "/rest/workspaces/" + ws + "/wmsstores/" + wms + "/wmslayers/" + wl + ".html";
        // Exception path
        String exception = "No such cascaded wms: "+ws+","+wl;
        String exception2 = "No such cascaded wms layer: "+ws+","+wms+","+wl;
        
        // CASE 1: No wmsstore set
        
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
        
        // CASE 2: wmsstore set
        
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
    public void testPut() throws Exception {
        String xml = 
          "<wmsLayer>" + 
            "<title>Lots of states here</title>" +  
          "</wmsLayer>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers/states", xml, "text/xml");
        assertEquals( 200, response.getStatus() );
        
        Document dom = getAsDOM("/rest/workspaces/sf/wmsstores/demo/wmslayers/states.xml");
        assertXpathEvaluatesTo("Lots of states here", "/wmsLayer/title", dom );
        
        WMSLayerInfo wli = catalog.getResourceByName( "sf", "states", WMSLayerInfo.class);
        assertEquals( "Lots of states here", wli.getTitle() );
    }
    
    @Test
    public void testPutNonDestructive() throws Exception {
        WMSLayerInfo wli = catalog.getResourceByName( "sf", "states", WMSLayerInfo.class);
        wli.setEnabled(true);
        catalog.save(wli);
        assertTrue(wli.isEnabled());
        boolean isAdvertised = wli.isAdvertised();
        
        String xml = 
          "<wmsLayer>" + 
            "<title>Lots of states here</title>" +  
          "</wmsLayer>";
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers/states", xml, "text/xml");
        assertEquals( 200, response.getStatus() );
        
        wli = catalog.getResourceByName( "sf", "states", WMSLayerInfo.class);
        assertTrue(wli.isEnabled());
        assertEquals(isAdvertised, wli.isAdvertised());
    }
    
    @Test
    public void testPutNonExistant() throws Exception {
        String xml = 
            "<wmsLayer>" + 
              "<title>new title</title>" +  
            "</wmsLayer>";
          MockHttpServletResponse response = 
              putAsServletResponse("/rest/workspaces/sf/wmsstores/demo/wmslayers/bugsites", xml, "text/xml");
          assertEquals( 404, response.getStatus() );
    }
   
    @Test
    public void testDelete() throws Exception {
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        assertEquals( 200,  
            deleteAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/states").getStatus());
        assertNull( catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
    }
    
    @Test
    public void testDeleteNonExistant() throws Exception {
        assertEquals( 404,  
            deleteAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/NonExistent").getStatus());
    }
    
    void addLayer() {
        LayerInfo l = catalog.getLayerByName(new NameImpl("sf", "states"));
        if (l == null) {
            l = catalog.getFactory().createLayer();
            l.setResource(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
            catalog.add(l);
        }
    }
    
    @Test
    public void testDeleteNonRecursive() throws Exception {
        addLayer();
        
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        assertEquals( 403,  
            deleteAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/states").getStatus());
    }
    
    @Test
    public void testDeleteRecursive() throws Exception {
        addLayer();
        
        assertNotNull(catalog.getLayerByName("sf:states"));
        assertNotNull(catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
        
        assertEquals( 200,  
            deleteAsServletResponse( "/rest/workspaces/sf/wmsstores/demo/wmslayers/states?recurse=true").getStatus());
        
        assertNull( catalog.getLayerByName("sf:states"));
        assertNull( catalog.getResourceByName("sf", "states", WMSLayerInfo.class));
    }
    
    @Test
    public void testResourceLink() throws Exception {
        addLayer();
         
        Document doc = getAsDOM( "/rest/layers/states.xml");
        
        XpathEngine xpath = XMLUnit.newXpathEngine();
        String resourceUrl = xpath.evaluate("//resource/atom:link/@href", doc);
        resourceUrl = resourceUrl.substring(resourceUrl.indexOf("/rest"));
        
        doc = getAsDOM(resourceUrl);
        assertXpathEvaluatesTo("states", "/wmsLayer/name", doc);
    }

}
