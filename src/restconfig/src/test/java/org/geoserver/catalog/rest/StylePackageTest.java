/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static junit.framework.Assert.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.IOException;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.catalog.StyleInfo;
import org.geotools.data.DataUtilities;
import org.junit.After;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class StylePackageTest extends CatalogRESTTestSupport {
    
    @After
    public void tearDown() throws IOException {
        StyleInfo si = getCatalog().getStyleByName("sf", "sldpackage");
        if(si != null) {
            getCatalog().remove(si);
            File imageFile = getCatalog().getResourceLoader().find("workspaces/sf/styles/sldpackage.png");
            if(imageFile != null) {
                imageFile.delete();
            }
            imageFile = getCatalog().getResourceLoader().find("workspaces/sf/styles/otherimage.png");
            if(imageFile != null) {
                imageFile.delete();
            }
            File sldFile = getCatalog().getResourceLoader().find("workspaces/sf/styles/sldpackage.sld");
            if(sldFile != null) {
                sldFile.delete();
            }
        }
    }
    
    @Test
    public void testPackageUploadAndCreateStyle() throws Exception {
        URL zip = getClass().getResource( "test-data/sldpackage.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/sld/sf/sldpackage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        Document d = getAsDOM("/rest/workspaces/sf/styles/sldpackage.xml");
        
        assertEquals( "style", d.getDocumentElement().getNodeName());
        
        d = getAsDOM("/rest/workspaces/sf/styles/sldpackage.sld");
        
        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertTrue(list.getLength() == 1);
        Element onlineResource = (Element)list.item(0);
        assertEquals("sldpackage.png", onlineResource.getAttribute("xlink:href"));
        
        StyleInfo si = getCatalog().getStyleByName("sf", "sldpackage");
        assertNotNull(si);
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/sf/styles/sldpackage.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/sf/styles/sldpackage.sld"));
    }
    
    @Test
    public void testUpdateStyle() throws Exception {
        URL zip = getClass().getResource("test-data/sldpackage.zip");
        byte[] bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));

        MockHttpServletResponse response = postAsServletResponse("/rest/sld/sf/sldpackage", bytes,
                "application/zip");
        assertEquals(201, response.getStatusCode());

        zip = getClass().getResource("test-data/sldpackage2.zip");
        bytes = FileUtils.readFileToByteArray(DataUtilities.urlToFile(zip));
        response = putAsServletResponse("/rest/sld/sf/sldpackage", bytes, "application/zip");
        assertEquals(200, response.getStatusCode());
        
        Document d = getAsDOM("/rest/workspaces/sf/styles/sldpackage.sld");
        
        assertEquals( "StyledLayerDescriptor", d.getDocumentElement().getNodeName());
        
        XpathEngine engine = XMLUnit.newXpathEngine();
        NodeList list = engine.getMatchingNodes("//sld:StyledLayerDescriptor/sld:NamedLayer/sld:UserStyle/sld:FeatureTypeStyle/sld:Rule/sld:PointSymbolizer/sld:Graphic/sld:ExternalGraphic/sld:OnlineResource", d);
        assertEquals(1, list.getLength());
        Element onlineResource = (Element)list.item(0);
        assertEquals("otherimage.png", onlineResource.getAttribute("xlink:href"));
        
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/sf/styles/otherimage.png"));
        assertNotNull(getCatalog().getResourceLoader().find("workspaces/sf/styles/sldpackage.sld"));
    }
    
    @Test
    public void testCreateFailsOnExistingStyle() throws Exception {
        URL zip = getClass().getResource( "test-data/sldpackage.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            postAsServletResponse( "/rest/sld/sf/sldpackage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        response = postAsServletResponse( "/rest/sld/sf/sldpackage", bytes, "application/zip");
        assertEquals( 500, response.getStatusCode() );
    }
    
    @Test
    public void testUpdateFailsOnNotExistingStyle() throws Exception {
        URL zip = getClass().getResource( "test-data/sldpackage.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/sld/sf/sldpackage", bytes, "application/zip");
        assertEquals( 500, response.getStatusCode() );
        
    }
}
