/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.junit.Assert.*;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import javax.servlet.Filter;

import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.filters.LoggingFilter;
import org.geotools.data.DataUtilities;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DataStoreFileUploadTest extends CatalogRESTTestSupport {

    @Override
    protected List<Filter> getFilters() {
        LoggingFilter filter = new LoggingFilter();
        filter.setEnabled(true);
        filter.setLogBodies(true);
        return Collections.singletonList((Filter) filter);
    }

    @Before
    public void removePdsDataStore() {
        removeStore("gs", "pds");
        removeStore("gs", "store with spaces");
    }

    @After
    public void cleanUpDbFiles() throws Exception {
        DeleteDbFiles.execute("target", "foo", true);
        DeleteDbFiles.execute("target", "pds", true);
        DeleteDbFiles.execute("target", "chinese_poly", true);
    }

    @Test
    public void testPropertyFileUpload() throws Exception {
        /*
        Properties p = new Properties();
        p.put( "_", "name:String,pointProperty:Point");
        p.put( "pds.0", "'zero'|POINT(0 0)");
        p.put( "pds.1", "'one'|POINT(1 1)");
        */
        byte[] bytes = propertyFile();
        //p.store( output, null );
        
        put( "/rest/workspaces/gs/datastores/pds/file.properties", bytes, "text/plain");
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures( dom );
    }

    @Test
    public void testPropertyFileUploadWithWorkspace() throws Exception {
        byte[] bytes = propertyFile();
        
        put( "/rest/workspaces/sf/datastores/pds/file.properties", bytes, "text/plain");
        Document dom = getAsDOM( "wfs?request=getfeature&typename=sf:pds");
        assertFeatures( dom, "sf" );
    }

    @Test
    public void testPropertyFileUploadZipped() throws Exception {
        byte[] bytes = propertyFile();
        
        //compress
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream( out );
        zout.putNextEntry( new ZipEntry ( "pds.properties" ) );
        zout.write( bytes );
        zout.flush();
        zout.close();
        
        put( "/rest/workspaces/gs/datastores/pds/file.properties", out.toByteArray(), "application/zip");
        
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures( dom );
        
    }
 
    byte[] propertyFile() throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        BufferedWriter writer = new BufferedWriter( new OutputStreamWriter( output ) );
        writer.write( "_=name:String,pointProperty:Point\n" );
        writer.write( "ds.0='zero'|POINT(0 0)\n");
        writer.write( "ds.1='one'|POINT(1 1)\n");
        writer.flush();
        return output.toByteArray();
    }
    
    void assertFeatures( Document dom ) throws Exception {
        assertFeatures( dom, "gs" );
    }
    
    void assertFeatures( Document dom, String ns ) throws Exception {
        assertEquals( "wfs:FeatureCollection", dom.getDocumentElement().getNodeName() );
        assertEquals( 2, dom.getElementsByTagName( ns + ":pds").getLength() );
    }

    @Test
    public void testShapeFileUpload() throws Exception {
       byte[] bytes = shpZipAsBytes();
        put( "/rest/workspaces/gs/datastores/pds/file.shp", bytes, "application/zip");
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures( dom );
    }

    @Test
    public void testShapeFileUploadWithCharset() throws Exception {
        /* Requires that a zipped shapefile (chinese_poly.zip) be in test-data directory */
    	byte[] bytes = shpChineseZipAsBytes();
        MockHttpServletResponse response = 
             putAsServletResponse("/rest/workspaces/gs/datastores/chinese_poly/file.shp?charset=UTF-8", bytes, "application/zip");
        assertEquals( 201, response.getStatusCode() );
         
        MockHttpServletResponse response2 = 
            getAsServletResponse("wfs?request=getfeature&typename=gs:chinese_poly", "GB18030");
        assertTrue(response2.getOutputStreamContent().contains("\u951f\u65a4\u62f7"));
     }    
        
    byte[] shpZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream( "test-data/pds.zip" ));
    }

    byte[] shpChineseZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream( "test-data/chinese_poly.zip" ));
    }
    
    byte[] shpMultiZipAsBytes() throws IOException {
        return toBytes(getClass().getResourceAsStream( "test-data/pdst.zip" ));
    }

    byte[] toBytes(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        
        int c = -1;
        while ( ( c = in.read() ) != -1 ) {
            out.write( c );
        }
        return out.toByteArray();
    }

    @Test
    public void testShapeFileUploadExternal() throws Exception {
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertEquals("ows:ExceptionReport", dom.getDocumentElement().getNodeName());
        
        File target = new File("target");
        File f = File.createTempFile("rest", "dir", target);
        f.delete();
        f.mkdir();
        
        File zip = new File(f, "pds.zip");
        IOUtils.copy(getClass().getResourceAsStream( "test-data/pds.zip" ), new FileOutputStream(zip));
        org.geoserver.rest.util.IOUtils.inflate(new ZipFile(zip), f, null);
        
        MockHttpServletResponse resp = putAsServletResponse("/rest/workspaces/gs/datastores/pds/external.shp", 
            new File(f, "pds.shp").toURL().toString(), "text/plain");
        assertEquals(201, resp.getStatusCode());
        
        dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures(dom);
    }
    
    @Test
    public void testShapeFileUploadNotExisting() throws Exception {
        File file = new File("./target/notThere.tiff");
        if(file.exists()) {
            assertTrue(file.delete());
        }
        
        URL url = DataUtilities.fileToURL(file.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response = putAsServletResponse("/rest/workspaces/gs/datastores/pds/external.shp", 
                body, "text/plain");
        assertEquals(400, response.getStatusCode());
    }
    
    @Test
    public void testShapeFileUploadIntoExisting() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "foo_h2"));
        
        String xml = 
        "<dataStore>" + 
        " <name>foo_h2</name>" + 
        " <type>H2</type>" + 
        " <connectionParameters>" + 
            "<namespace>" + MockData.DEFAULT_URI + "</namespace>" + 
            "<database>target/foo</database>" + 
            "<dbtype>h2</dbtype>" + 
        " </connectionParameters>" +
        "</dataStore>";
        
        post("/rest/workspaces/gs/datastores", xml);
        
        DataStoreInfo ds = cat.getDataStoreByName("gs", "foo_h2"); 
        assertNotNull(ds);
        
        assertTrue(cat.getFeatureTypesByDataStore(ds).isEmpty());
        
        byte[] bytes = shpZipAsBytes();
        put( "/rest/workspaces/gs/datastores/foo_h2/file.shp", bytes, "application/zip");
        
        assertFalse(cat.getFeatureTypesByDataStore(ds).isEmpty());
        
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures( dom );
    }

    @Test
    public void testShapeFileUploadWithTarget() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "pds"));
        
        byte[] bytes = shpZipAsBytes();
        put( "/rest/workspaces/gs/datastores/pds/file.shp?target=h2", bytes, "application/zip");
        
        DataStoreInfo ds = cat.getDataStoreByName("gs", "pds"); 
        assertNotNull(ds);
        assertFalse(cat.getFeatureTypesByDataStore(ds).isEmpty());
        
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:pds" );
        assertFeatures( dom );
    }
    
    @Test
    @Ignore
    // fixing https://jira.codehaus.org/browse/GEOS-6845, re-enable when a proper fix for spaces in
    // name has been made
    public void testShapeFileUploadWithSpaces() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "store with spaces"));
        
        byte[] bytes = shpZipAsBytes();
        put( "/rest/workspaces/gs/datastores/store%20with%20spaces/file.shp", bytes, "application/zip");
        
        DataStoreInfo ds = cat.getDataStoreByName("gs", "store with spaces"); 
        assertNull(ds);
    }
 
    @Test
    public void testShapefileUploadMultiple() throws Exception {
        Catalog cat = getCatalog();
        assertNull(cat.getDataStoreByName("gs", "pdst"));
        
        put("/rest/workspaces/gs/datastores/pdst/file.shp?configure=all", shpMultiZipAsBytes(), "application/zip");

        DataStoreInfo ds = cat.getDataStoreByName("gs", "pdst");
        assertNotNull(ds);

        assertEquals(2, cat.getFeatureTypesByDataStore(ds).size());
    }

    @Test
    public void testGet() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("/rest/workspaces/gs/datastores/pds/file.properties");
        assertEquals( 404, resp.getStatusCode() );
        
        byte[] bytes = propertyFile();
        put( "/rest/workspaces/gs/datastores/pds/file.properties", bytes, "text/plain");
        
        resp = getAsServletResponse("/rest/workspaces/gs/datastores/pds/file.properties");
        assertEquals( 200, resp.getStatusCode() );
        assertEquals( "application/zip", resp.getContentType() );
        
        ByteArrayInputStream bin = getBinaryInputStream(resp);
        ZipInputStream zin = new ZipInputStream( bin );
        
        ZipEntry entry = zin.getNextEntry();
        assertNotNull( entry );
        assertEquals( "pds.properties", entry.getName() );
    }
}
