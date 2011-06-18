/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.StyleInfo;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class LayerGroupTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        LayerGroupInfo lg = catalog.getFactory().createLayerGroup();
        lg.setName( "sfLayerGroup" );
        lg.getLayers().add( catalog.getLayerByName( "sf:PrimitiveGeoFeature" ) );
        lg.getLayers().add( catalog.getLayerByName( "sf:AggregateGeoFeature" ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.getStyles().add( catalog.getStyleByName( StyleInfo.DEFAULT_POINT ) );
        lg.setBounds( new ReferencedEnvelope( -180,-90,180,90, CRS.decode( "EPSG:4326") ) );
        catalog.add( lg );
    }
    
    public void testGetAllAsXML() throws Exception {
        Document dom = getAsDOM( "/rest/layergroups.xml");
        assertEquals( catalog.getLayerGroups().size(), dom.getElementsByTagName( "layerGroup").getLength());
    }
    
    public void testGetAllAsHTML() throws Exception {
        getAsDOM( "/rest/layergroups.html");
    }
    
    public void testGetAsXML() throws Exception {
        print(get("/rest/layergroups/sfLayerGroup.xml"));
        Document dom = getAsDOM( "/rest/layergroups/sfLayerGroup.xml");
        assertEquals( "layerGroup", dom.getDocumentElement().getNodeName() );
        assertXpathEvaluatesTo("sfLayerGroup", "/layerGroup/name", dom );
        assertXpathEvaluatesTo( "2", "count(//layer)", dom );
        assertXpathEvaluatesTo( "2", "count(//style)", dom );
    }
    
    public void testGetAsHTML() throws Exception {
        getAsDOM( "/rest/layergroups/sfLayerGroup.html");
    }
    
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
    
    public void testDelete() throws Exception {
        MockHttpServletResponse response = deleteAsServletResponse( "/rest/layergroups/sfLayerGroup");
        assertEquals( 200, response.getStatusCode() );
        
        assertEquals( 0, catalog.getLayerGroups().size() );
    }
}
