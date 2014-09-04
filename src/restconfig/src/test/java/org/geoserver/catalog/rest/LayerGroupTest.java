/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LayerGroupTest extends CatalogRESTTestSupport {

    @Before
    public void revertChanges() throws Exception {
        removeLayerGroup(null, "sfLayerGroup");
        removeLayerGroup("sf", "foo");
        removeLayerGroup(null, "newLayerGroup");
        removeLayerGroup(null, "newLayerGroupWithTypeCONTAINER");
        removeLayerGroup(null, "newLayerGroupWithTypeEO");
        removeLayerGroup(null, "nestedLayerGroupTest");
        
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName( "sfLayerGroup" );
        lg.getLayers().add( catalog.getLayerByName( "sf:PrimitiveGeoFeature" ) );
        lg.getLayers().add( catalog.getLayerByName( "sf:AggregateGeoFeature" ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.setBounds( new ReferencedEnvelope( -180,-90,180,90, CRS.decode( "EPSG:4326") ) );
        catalog.add( lg );
    }

    @Test
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layergroups.xml");
        assertEquals( catalog.getLayerGroups().size(), dom.getElementsByTagName( "layerGroup").getLength());
    }

    @Test
    public void testGetAllAsHTML() throws Exception {
        getAsDOM( "/rest/layergroups.html");
    }

    @Test
    public void testGetAllFromWorkspace() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/sf/layergroups.xml" );
        assertEquals("layerGroups", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("0", "count(//layerGroup)", dom);

        addLayerGroupToWorkspace("foo");

        dom = getAsDOM( "/rest/workspaces/sf/layergroups.xml" );
        assertEquals("layerGroups", dom.getDocumentElement().getNodeName());

        assertXpathEvaluatesTo("1", "count(//layerGroup)", dom);
        assertXpathExists("//layerGroup/name[text() = 'foo']", dom);
    }

    void addLayerGroupToWorkspace(String name) {
        Catalog cat = getCatalog();

        LayerGroupInfo lg = cat.getFactory().createLayerGroup();
        lg.setName("foo");
        lg.setWorkspace(cat.getWorkspaceByName("sf"));
        lg.getLayers().add(cat.getLayerByName("sf:PrimitiveGeoFeature"));
        lg.getStyles().add(null);
        cat.add(lg);
    }

    @Test
    public void testGetAsXML() throws Exception {
        print(get("/rest/layergroups/sfLayerGroup.xml"));
        Document dom = getAsDOM( "/rest/layergroups/sfLayerGroup.xml");
        assertEquals( "layerGroup", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("sfLayerGroup", "/layerGroup/name", dom );
        assertXpathEvaluatesTo( "2", "count(//published)", dom );
        assertXpathEvaluatesTo( "2", "count(//style)", dom );
    }

    @Test
    public void testGetAsHTML() throws Exception {
        getAsDOM( "/rest/layergroups/sfLayerGroup.html");
    }

    @Test
    public void testGetFromWorkspace() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/workspaces/sf/layergroups/foo.xml"); 
        assertEquals(404, resp.getStatusCode());

        addLayerGroupToWorkspace("foo");

        resp = getAsServletResponse("/rest/workspaces/sf/layergroups/foo.xml");
        assertEquals(200, resp.getStatusCode());

        Document dom = getAsDOM("/rest/workspaces/sf/layergroups/foo.xml");
        assertXpathEvaluatesTo("foo", "/layerGroup/name", dom);
        assertXpathEvaluatesTo("sf", "/layerGroup/workspace/name", dom);
    }

    @Test
    public void testPost() throws Exception {
        String xml = 
            "<layerGroup>" + 
                "<name>newLayerGroup</name>" +
                "<layers>" +
                  "<layer>Ponds</layer>" +
                  "<layer>Forests</layer>" +
                "</layers>" +
                "<styles>" +
                  "<style>polygon</style>" +
                  "<style>point</style>" +
                "</styles>" +
              "</layerGroup>";
        
        MockHttpServletResponse response = postAsServletResponse("/rest/layergroups", xml );
        assertEquals( 201, response.getStatusCode() );
        
        assertNotNull( response.getHeader( "Location") );
        assertTrue( response.getHeader("Location").endsWith( "/layergroups/newLayerGroup" ) );

        LayerGroupInfo lg = catalog.getLayerGroupByName( "newLayerGroup");
        assertNotNull( lg );
        
        assertEquals( 2, lg.getLayers().size() );
        assertEquals( "Ponds", lg.getLayers().get( 0 ).getName() );
        assertEquals( "Forests", lg.getLayers().get( 1 ).getName() );
        
        assertEquals( 2, lg.getStyles().size() );
        assertEquals( "polygon", lg.getStyles().get( 0 ).getName() );
        assertEquals( "point", lg.getStyles().get( 1 ).getName() );
        
        assertNotNull( lg.getBounds() );
    }

    @Test
    public void testPostWithNestedGroups() throws Exception {
        String xml = 
                "<layerGroup>" + 
                    "<name>nestedLayerGroupTest</name>" +
                    "<publishables>" +
                      "<published type=\"layer\">Ponds</published>" +
                      "<published type=\"layer\">Forests</published>" +
                      "<published type=\"layerGroup\">sfLayerGroup</published>" +
                    "</publishables>" +
                    "<styles>" +
                      "<style>polygon</style>" +
                      "<style>point</style>" +
                      "<style></style>" +                      
                    "</styles>" +
                  "</layerGroup>";
            
            MockHttpServletResponse response = postAsServletResponse("/rest/layergroups", xml );
            assertEquals( 201, response.getStatusCode() );
            
            assertNotNull( response.getHeader( "Location") );
            assertTrue( response.getHeader("Location").endsWith( "/layergroups/nestedLayerGroupTest" ) );

            LayerGroupInfo lg = catalog.getLayerGroupByName( "nestedLayerGroupTest");
            assertNotNull( lg );
            
            assertEquals( 3, lg.getLayers().size() );
            assertEquals( "Ponds", lg.getLayers().get( 0 ).getName() );
            assertEquals( "Forests", lg.getLayers().get( 1 ).getName() );
            assertEquals( "sfLayerGroup", lg.getLayers().get( 2 ).getName() );
            assertEquals( 3, lg.getStyles().size() );
            assertEquals( "polygon", lg.getStyles().get( 0 ).getName() );
            assertEquals( "point", lg.getStyles().get( 1 ).getName() );
            assertNull( lg.getStyles().get( 2 ) );
            
            assertNotNull( lg.getBounds() );
    }
    
    @Test
    public void testPostWithTypeContainer() throws Exception {
        String xml = 
            "<layerGroup>" + 
                "<name>newLayerGroupWithTypeCONTAINER</name>" +
                "<mode>CONTAINER</mode>" + 
                "<layers>" +
                  "<layer>Ponds</layer>" +
                  "<layer>Forests</layer>" +
                "</layers>" +
                "<styles>" +
                  "<style>polygon</style>" +
                  "<style>point</style>" +
                "</styles>" +
              "</layerGroup>";
        
        MockHttpServletResponse response = postAsServletResponse("/rest/layergroups", xml);
        assertEquals(201, response.getStatusCode());
        
        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroupWithTypeCONTAINER");
        assertNotNull(lg);
        
        assertEquals(LayerGroupInfo.Mode.CONTAINER, lg.getMode());
    }  
    
    @Test
    public void testPostWithTypeEO() throws Exception {
        String xml = 
            "<layerGroup>" + 
                "<name>newLayerGroupWithTypeEO</name>" +
                "<mode>EO</mode>" + 
                "<rootLayer>Ponds</rootLayer>" + 
                "<rootLayerStyle>polygon</rootLayerStyle>" + 
                "<layers>" +
                  "<layer>Forests</layer>" +
                "</layers>" +
                "<styles>" +
                  "<style>point</style>" +
                "</styles>" +
              "</layerGroup>";
        
        MockHttpServletResponse response = postAsServletResponse("/rest/layergroups", xml);
        assertEquals(201, response.getStatusCode());
        
        LayerGroupInfo lg = catalog.getLayerGroupByName("newLayerGroupWithTypeEO");
        assertNotNull(lg);
        
        assertEquals(LayerGroupInfo.Mode.EO, lg.getMode());
        assertEquals("Ponds", lg.getRootLayer().getName());
        assertEquals("polygon", lg.getRootLayerStyle().getName());
    }
    
    @Test
    public void testPostNoStyles() throws Exception {
        
        String xml = 
            "<layerGroup>" + 
                "<name>newLayerGroup</name>" +
                "<layers>" +
                  "<layer>Ponds</layer>" +
                  "<layer>Forests</layer>" +
                "</layers>" +
              "</layerGroup>";
        
        MockHttpServletResponse response = postAsServletResponse("/rest/layergroups", xml );
        assertEquals( 201, response.getStatusCode() );
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "newLayerGroup");
        assertNotNull( lg );
        
        assertEquals( 2, lg.getLayers().size() );
        assertEquals( "Ponds", lg.getLayers().get( 0 ).getName() );
        assertEquals( "Forests", lg.getLayers().get( 1 ).getName() );
        
        assertEquals( 2, lg.getStyles().size() );
        assertNull(lg.getStyles().get( 0 ));
        assertNull(lg.getStyles().get( 1 ));
    }

    @Test
    public void testPostToWorkspace() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getLayerGroupByName("sf", "foo"));

        String xml = 
            "<layerGroup>" + 
                "<name>foo</name>" +
                "<layers>" +
                  "<layer>PrimitiveGeoFeature</layer>" +
                "</layers>" +
              "</layerGroup>";
            
        MockHttpServletResponse response =
            postAsServletResponse("/rest/workspaces/sf/layergroups", xml);
        assertEquals(201, response.getStatusCode());
        assertNotNull(cat.getLayerGroupByName("sf", "foo"));
    }

    @Test
    public void testPut() throws Exception {
        String xml = 
            "<layerGroup>" + 
                "<name>sfLayerGroup</name>" +
                "<styles>" +
                  "<style>polygon</style>" +
                  "<style>line</style>" +
                "</styles>" +
              "</layerGroup>";
        
        MockHttpServletResponse response = putAsServletResponse("/rest/layergroups/sfLayerGroup", xml, "text/xml" );
        assertEquals( 200, response.getStatusCode() );
        
        LayerGroupInfo lg = catalog.getLayerGroupByName( "sfLayerGroup" );
        
        assertEquals( 2, lg.getLayers().size() );
        assertEquals( 2, lg.getStyles().size() );
        assertEquals( "polygon", lg.getStyles().get( 0 ).getName() );
        assertEquals( "line", lg.getStyles().get( 1 ).getName() );
    }

    @Test
    public void testPutToWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNull(cat.getLayerGroupByName("sf", "foo").getStyles().get(0));

        String xml = 
            "<layerGroup>" + 
                "<styles>" +
                  "<style>line</style>" +
                "</styles>" +
            "</layerGroup>";
        
        MockHttpServletResponse response =
            putAsServletResponse("/rest/workspaces/sf/layergroups/foo", xml, "application/xml");
        assertEquals(200, response.getStatusCode());
        assertEquals("line", cat.getLayerGroupByName("sf", "foo").getStyles().get(0).getName());
    }

    @Test
    public void testPutToWorkspaceChangeWorkspace() throws Exception {
        testPostToWorkspace();

        String xml = 
                "<layerGroup>" +
                  "<workspace>cite</workspace>" + 
                "</layerGroup>";
            
        MockHttpServletResponse response =
            putAsServletResponse("/rest/workspaces/sf/layergroups/foo", xml, "application/xml");
        assertEquals(403, response.getStatusCode());
    }

    @Test
    public void testDelete() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse( "/rest/layergroups/sfLayerGroup");
        assertEquals( 200, response.getStatusCode() );
        
        assertEquals( 0, catalog.getLayerGroups().size() );
    }

    @Test
    public void testDeleteFromWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        assertNotNull(cat.getLayerGroupByName("sf", "foo"));
        
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/sf/layergroups/foo");
        assertEquals(200, response.getStatusCode());

        assertNull(cat.getLayerGroupByName("sf", "foo"));
    }

    public void testLayersStylesInWorkspace() throws Exception {
        testPostToWorkspace();

        Catalog cat = getCatalog();
        StyleInfo s = cat.getFactory().createStyle();
        s.setName("s1");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("s1.sld");
        cat.add(s);

        s = cat.getFactory().createStyle();
        s.setName("s2");
        s.setWorkspace(cat.getWorkspaceByName("sf"));
        s.setFilename("s2.sld");
        cat.add(s);
        
        String xml = 
                "<layerGroup>" +
                    "<layers>" +
                      "<layer>PrimitiveGeoFeature</layer>" +
                      "<layer>AggregateGeoFeature</layer>" +
                    "</layers>" + 
                    "<styles>" +
                      "<style>" + 
                          "<name>s1</name>" + 
                          "<workspace>sf</workspace>" + 
                      "</style>" +
                      "<style>" + 
                          "<name>s2</name>" + 
                          "<workspace>sf</workspace>" + 
                      "</style>" +
                    "</styles>" +
                  "</layerGroup>";
            
        MockHttpServletResponse response = 
            putAsServletResponse("/rest/workspaces/sf/layergroups/foo", xml, "text/xml" );
        assertEquals( 200, response.getStatusCode() );
        
        LayerGroupInfo lg = cat.getLayerGroupByName("sf", "foo");
        assertEquals(2, lg.getLayers().size());
        assertEquals(2, lg.getStyles().size());

        assertEquals("PrimitiveGeoFeature", lg.getLayers().get(0).getName());
        assertEquals("AggregateGeoFeature", lg.getLayers().get(1).getName());

        assertEquals("s1", lg.getStyles().get(0).getName());
        assertNotNull(lg.getStyles().get(0).getWorkspace());
        assertEquals("sf", lg.getStyles().get(0).getWorkspace().getName());
        
        assertEquals("s2", lg.getStyles().get(1).getName());
        assertNotNull(lg.getStyles().get(1).getWorkspace());
        assertEquals("sf", lg.getStyles().get(1).getWorkspace().getName());

        Document dom = getAsDOM("/rest/workspaces/sf/layergroups/foo.xml");
        
        assertXpathEvaluatesTo("http://localhost/geoserver/rest/workspaces/sf/styles/s1.xml", 
           "//style[name = 's1']/atom:link/@href", dom );
        assertXpathEvaluatesTo("http://localhost/geoserver/rest/workspaces/sf/styles/s2.xml", 
                "//style[name = 's2']/atom:link/@href", dom );
    }
}
