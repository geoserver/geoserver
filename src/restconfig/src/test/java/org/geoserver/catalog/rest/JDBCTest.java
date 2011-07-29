/* Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import java.sql.SQLException;
import java.util.HashMap;

import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.data.test.MockData;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureWriter;
import org.geotools.data.Transaction;
import org.geotools.data.h2.H2DataStoreFactory;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

public class JDBCTest extends CatalogRESTTestSupport {

    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        HashMap params = new HashMap();
        params.put( JDBCDataStoreFactory.NAMESPACE.key, MockData.DEFAULT_URI);
        params.put( JDBCDataStoreFactory.DATABASE.key, "target/acme");
        params.put( JDBCDataStoreFactory.DBTYPE.key, "h2");
        
        H2DataStoreFactory fac =  new H2DataStoreFactory();
        fac.setBaseDirectory( getTestData().getDataDirectoryRoot() );
        
        JDBCDataStore ds = fac.createDataStore(params);
        try {
            if ( ds.getSchema("widgets") != null ) {
                return;
            }
        }
        catch( Exception e ) {
            
        }
        
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
    
    public void testCreateDataStore() throws Exception {
        assertNull( catalog.getDataStoreByName( "gs", "acme") );
        
        String xml = 
            "<dataStore>" +
              "<name>acme</name>" + 
              "<connectionParameters>" +
                "<namespace>" + MockData.DEFAULT_URI + "</namespace>" + 
                "<database>target/acme</database>" + 
                "<dbtype>h2</dbtype>" + 
              "</connectionParameters>" + 
            "</dataStore>";
        
        MockHttpServletResponse resp = 
            postAsServletResponse("/rest/workspaces/gs/datastores", xml );
        assertEquals( 201, resp.getStatusCode() );
        
        assertNotNull( catalog.getDataStoreByName( "gs", "acme") );
    }
    
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
}
