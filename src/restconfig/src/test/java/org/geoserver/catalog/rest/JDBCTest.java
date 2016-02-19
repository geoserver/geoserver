/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.HashMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class JDBCTest extends CatalogRESTTestSupport {

    protected String databasePath() {
        File path = new File(getTestData().getDataDirectoryRoot(), "target/acme");
        return path.getAbsolutePath();
    }

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        HashMap params = new HashMap();
        params.put( JDBCDataStoreFactory.NAMESPACE.key, MockData.DEFAULT_URI);
        params.put( JDBCDataStoreFactory.DATABASE.key, databasePath());
        params.put( JDBCDataStoreFactory.DBTYPE.key, "h2");
        
        H2DataStoreFactory fac =  new H2DataStoreFactory();
        
        JDBCDataStore ds = fac.createDataStore(params);

        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName( "widgets" );
        tb.setSRS( "EPSG:4326");
        tb.add( "g", Point.class );
        tb.add( "name", String.class );
        
        ds.createSchema( tb.buildFeatureType() );
        
        FeatureWriter fw = ds.getFeatureWriterAppend( "widgets", Transaction.AUTO_COMMIT );
        fw.hasNext();
        SimpleFeature sf = (SimpleFeature) fw.next();
        sf.setAttribute("g", new GeometryFactory().createPoint( new Coordinate(1,1)));
        sf.setAttribute( "name", "one");
        fw.write();
        
        fw.hasNext();
        sf = (SimpleFeature) fw.next();
        sf.setAttribute("g", new GeometryFactory().createPoint( new Coordinate(2,2)));
        sf.setAttribute( "name", "two");
        fw.write();
        
        fw.close();
        ds.dispose();
    }

    @Before
    public void removeAcmeDataStore() {
        removeStore("gs", "acme");
    }

    @Test
    public void testCreateDataStore() throws Exception {
        assertNull( catalog.getDataStoreByName( "gs", "acme") );
        
        String xml = 
            "<dataStore>" +
              "<name>acme</name>" + 
              "<connectionParameters>" +
                "<namespace>" + MockData.DEFAULT_URI + "</namespace>" + 
                "<database>" + databasePath() + "</database>" + 
                "<dbtype>h2</dbtype>" + 
              "</connectionParameters>" + 
            "</dataStore>";
        MockHttpServletResponse resp = 
            postAsServletResponse("/rest/workspaces/gs/datastores", xml );
        assertEquals(resp.getOutputStreamContent(), 201, resp.getStatusCode() );
        
        assertNotNull( catalog.getDataStoreByName( "gs", "acme") );
    }

    @Test
    public void testCreateFeatureType() throws Exception {
        testCreateDataStore();
        DataStoreInfo ds = catalog.getDataStoreByName( "gs", "acme");
        assertNull( catalog.getFeatureTypeByDataStore(ds, "widgets"));
        
        String xml = 
            "<featureType>" +
              "<name>widgets</name>" + 
            "</featureType>";
        
        MockHttpServletResponse resp = 
            postAsServletResponse("/rest/workspaces/gs/datastores/acme/featuretypes", xml );
        assertEquals( 201, resp.getStatusCode() );
        
        assertNotNull( catalog.getFeatureTypeByDataStore(ds, "widgets"));
        
        //do a get feature for a sanity check
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:widgets");
        assertEquals( 2, dom.getElementsByTagName( "gs:widgets" ).getLength() );
    }

    @Test
    public void testCreateGeometrylessFeatureType() throws Exception {
        testCreateDataStore();
        
        DataStoreInfo dsinfo = catalog.getDataStoreByName( "gs", "acme");
        assertNull( catalog.getFeatureTypeByDataStore(dsinfo, "widgetsNG"));
        
        DataStore ds = (DataStore) dsinfo.getDataStore(null);
        try {
            if ( ds.getSchema("widgetsNG") != null ) {
                return;
            }
        }
        catch( Exception e ) {}
        
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName( "widgetsNG" );
        tb.add( "name", String.class );
        
        ds.createSchema( tb.buildFeatureType() );
        
        FeatureWriter fw = ds.getFeatureWriterAppend( "widgetsNG", Transaction.AUTO_COMMIT );
        fw.hasNext();
        SimpleFeature sf = (SimpleFeature) fw.next();
        sf.setAttribute( "name", "one");
        fw.write();
        
        fw.hasNext();
        sf = (SimpleFeature) fw.next();
        sf.setAttribute( "name", "two");
        fw.write();
        
        fw.close();
        
        String xml = 
            "<featureType>" +
              "<name>widgetsNG</name>" + 
            "</featureType>";
        
        MockHttpServletResponse resp = 
            postAsServletResponse("/rest/workspaces/gs/datastores/acme/featuretypes", xml );
        assertEquals( 201, resp.getStatusCode() );
        
        assertNotNull( catalog.getFeatureTypeByDataStore(dsinfo, "widgetsNG"));
        
        //do a get feature for a sanity check
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:widgetsNG");
        assertEquals( 2, dom.getElementsByTagName( "gs:widgetsNG" ).getLength() );
    }

    // FIXME This test fails due to an outdated version of H2.
    @Test
    @Ignore
    public void testCreateSQLView() throws Exception {
        
        // first create the store
        testCreateDataStore();
        DataStoreInfo ds = catalog.getDataStoreByName( "gs", "acme");
        assertNull( catalog.getFeatureTypeByDataStore(ds, "widgets"));
        
        // create the sql view
        String xml = "<featureType>\n" + 
        "  <name>sqlview</name>\n" + 
        "  <nativeName>sqlview</nativeName>\n" + 
        "  <namespace>\n" + 
        "    <name>gs</name>\n" + 
        "  </namespace>\n" + 
        "  <srs>EPSG:4326</srs>\n" +
        "  <metadata>\n" + 
        "  <entry key=\"JDBC_VIRTUAL_TABLE\">\n" + 
        "     <virtualTable>" +
        "       <name>sqlview</name>" +
        "       <sql>select \"g\" from \"widgets\"</sql>\n" + 
        "       <geometry>" +
        "         <name>g</name>" +
        "         <type>Point</type>" +
        "         <srid>4326</srid>" +
        "       </geometry>\n" + 
        "     </virtualTable>" +
        "  </entry>" +
        "  </metadata>" +
        "</featureType>";        
        
        MockHttpServletResponse resp = 
            postAsServletResponse("/rest/workspaces/gs/datastores/acme/featuretypes", xml );
        assertEquals( 201, resp.getStatusCode() );
        
        assertNotNull( catalog.getFeatureTypeByDataStore(ds, "sqlview"));
        assertNotNull( catalog.getFeatureTypeByName("gs:sqlview"));
        
        //do a get feature for a sanity check
        Document dom = getAsDOM( "wfs?request=getfeature&typename=gs:sqlview");
        // print(dom);
        assertEquals( 2, dom.getElementsByTagName( "gs:sqlview" ).getLength() );
    }

    @Test
    public void testUploadUsesNativeNameForConflictDetection() throws Exception {
        testCreateDataStore(); // creates "acme" datastore

        // We expect no featuretypes as nothing has been configured yet.
        assertXpathEvaluatesTo("0", "count(/featureTypes/featureType)",
                getAsDOM("/rest/workspaces/gs/datastores/acme/featuretypes.xml"));

        byte[] dataToUpload = zippedPropertyFile("pds.properties");
        MockHttpServletResponse resp = 
            putAsServletResponse("/rest/workspaces/gs/datastores/acme/file.properties",
                    dataToUpload, "application/zip");

        // Upload data, will be imported into DB table named "pds"
        assertEquals("Upload into database datastore failed: " + resp.getOutputStreamContent(),
                201, resp.getStatusCode());
        // Now we expect one featuretype since we just imported it
        assertXpathEvaluatesTo("1", "count(/featureTypes/featureType)",
                getAsDOM("/rest/workspaces/gs/datastores/acme/featuretypes.xml"));

        // Rename the published resource - note the underlying DB table is still named "pds"
        resp = putAsServletResponse("/rest/workspaces/gs/datastores/acme/featuretypes/pds.xml",
                "<featureType><name>pds_alt</name></featureType>", "application/xml");
        assertEquals("Couldn't update featuretype settings: " + resp.getOutputStreamContent(),
                200, resp.getStatusCode());

        // Upload data to be imported into "pds" again. Should simply append to
        // the table and not change the resource configuration
        resp = putAsServletResponse("/rest/workspaces/gs/datastores/acme/file.properties",
                dataToUpload, "application/zip");
        assertEquals("Second upload to database datastore failed: " + resp.getOutputStreamContent(),
                201, resp.getStatusCode());

        // We expect one featuretype again since we should have appended data to the existing
        // featuretype this time
        assertXpathEvaluatesTo("1", "count(/featureTypes/featureType)",
                getAsDOM("/rest/workspaces/gs/datastores/acme/featuretypes.xml"));
    }

    byte[] zippedPropertyFile(String filename) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ZipOutputStream zout = new ZipOutputStream(out);
        zout.putNextEntry(new ZipEntry(filename));
        zout.write(propertyFile());
        zout.flush();
        zout.close();
        return out.toByteArray();
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

    @After
    public void cleanupDataStore() {
        removeStore("gs", "acme");
    }
}
