/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.TestData;
import org.geotools.data.DataUtilities;
import org.geotools.factory.GeoTools;
import org.junit.Test;
import org.opengis.coverage.grid.GridCoverage;
import org.opengis.coverage.grid.GridCoverageReader;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletRequest;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class CoverageStoreFileUploadTest extends CatalogRESTTestSupport {

    @Test
    public void testWorldImageUploadZipped() throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/sf/coveragestores/usa/file.worldimage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        assertEquals( "coverageStore", d.getDocumentElement().getNodeName());
        
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("sf", "usa");
        assertNotNull(cs);
        CoverageInfo ci = getCatalog().getCoverageByName("sf", "usa");
        assertNotNull(ci);
    }
    
    @Test
    public void testUploadWithSpaces() throws Exception {
        URL zip = getClass().getResource( "test-data/usa.zip" );
        byte[] bytes = FileUtils.readFileToByteArray( DataUtilities.urlToFile(zip) );
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/store%20with%20spaces/file.worldimage", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        assertEquals( "coverageStore", d.getDocumentElement().getNodeName());
        
        CoverageStoreInfo cs = getCatalog().getCoverageStoreByName("gs", "store with spaces");
        assertNotNull(cs);
        CoverageInfo ci = getCatalog().getCoverageByName("gs", "usa");
        assertNotNull(ci);
    }
    
    @Test
    public void testUploadImageMosaic() throws Exception {
        URL zip = MockData.class.getResource( "watertemp.zip" );
        InputStream is = null;
        byte[] bytes;
        try  {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/watertemp/file.imagemosaic", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        // check the response contents
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        
        XMLAssert.assertXpathEvaluatesTo("watertemp", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);
        
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
    }
    
    @Test
    public void testHarvestImageMosaic() throws Exception {
        // Upload of the Mosaic via REST
        URL zip = MockData.class.getResource( "watertemp.zip" );
        InputStream is = null;
        byte[] bytes;
        try  {
            is = zip.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        
        MockHttpServletResponse response = 
            putAsServletResponse( "/rest/workspaces/gs/coveragestores/watertemp2/file.imagemosaic", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
        
        // check the response contents
        String content = response.getOutputStreamContent();
        Document d = dom( new ByteArrayInputStream( content.getBytes() ));
        
        XMLAssert.assertXpathEvaluatesTo("watertemp2", "//coverageStore/name", d);
        XMLAssert.assertXpathEvaluatesTo("ImageMosaic", "//coverageStore/type", d);
        
        // check the coverage is actually there
        CoverageStoreInfo storeInfo = getCatalog().getCoverageStoreByName("watertemp2");
        assertNotNull(storeInfo);
        CoverageInfo ci = getCatalog().getCoverageByName("watertemp2");
        assertNotNull(ci);
        assertEquals(storeInfo, ci.getStore());
        
        // Harvesting of the Mosaic
        URL zipHarvest = getClass().getResource( "test-data/harvesting.zip" );
        // Extract a Byte array from the zip file
        is = null;
        try  {
            is = zipHarvest.openStream();
            bytes = IOUtils.toByteArray(is);
        } finally {
            IOUtils.closeQuietly(is);
        }
        // Create the POST request
        MockHttpServletRequest request = createRequest( "/rest/workspaces/gs/coveragestores/watertemp2/file.imagemosaic" ); 
        request.setMethod( "POST" );
        request.setContentType("application/zip" );
        request.setBodyContent(bytes);
        request.setHeader( "Content-type", "application/zip" );
        // Get The response
        response = dispatch( request );
        // Get the Mosaic Reader
        GridCoverageReader reader = storeInfo.getGridCoverageReader(null, GeoTools.getDefaultHints());
        // Test if all the TIME DOMAINS are present
        String[] metadataNames = reader.getMetadataNames();
        assertNotNull(metadataNames);
        assertEquals("true", reader.getMetadataValue("HAS_TIME_DOMAIN"));
        assertEquals("2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z,2008-11-02T00:00:00.000Z", 
                reader.getMetadataValue(metadataNames[0]));
    }
}
