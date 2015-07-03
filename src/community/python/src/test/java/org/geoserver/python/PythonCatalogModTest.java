/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.python;

import java.sql.SQLException;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.DataStore;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.h2.tools.DeleteDbFiles;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.springframework.context.ApplicationContext;

import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.replay;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

public class PythonCatalogModTest extends PythonTestSupport {

    Catalog cat;
    
    @Before
    public void setUp() throws Exception {
        DeleteDbFiles.execute("target", "bar", false);
        
        cat = new CatalogImpl();
        
        ApplicationContext context = createNiceMock(ApplicationContext.class);
        expect(context.getBean("catalog")).andReturn(cat).anyTimes();
        replay(context);
        
        new GeoServerExtensions().setApplicationContext(context);
    }
    
    @After
    public void tearDown() throws Exception {
        new GeoServerExtensions().setApplicationContext(null);
        DeleteDbFiles.execute("target", "bar", false);
    }
    
    @Test
    public void testAddWorkspace() {
        assertEquals(0, cat.getWorkspaces().size());
        
        pi.exec("from geoserver.catalog import Workspace");
        pi.exec("ws = Workspace('foo')");
        pi.exec("ws.save()");
        
        assertEquals(1, cat.getWorkspaces().size());
        assertNotNull(cat.getWorkspaceByName("foo"));
    }
    
    @Test
    public void testSaveWorkspace() {
        testAddWorkspace();
        
        assertNotNull(cat.getWorkspaceByName("foo"));
        
        pi.exec("from geoserver.catalog import Workspace");
        pi.exec("ws = Workspace('foo')");
        pi.exec("ws.name = 'bar'");
        pi.exec("ws.save()");
  
        assertNull(cat.getWorkspaceByName("foo"));
        assertNotNull(cat.getWorkspaceByName("bar"));
    }
   
    @Test
    public void testAddStore() throws Exception {
        testAddWorkspace();
        
        assertNull(cat.getDataStoreByName("foo", "bar"));
      
        pi.exec("from geoserver.catalog import Store");
        pi.exec("st = Store('bar')");
        pi.exec("st.connectionParameters.putAll({'database':'target/bar', 'dbtype': 'h2'})");
        pi.exec("st.save()");
        
        DataStoreInfo ds = cat.getDataStoreByName("foo", "bar");
        assertNotNull(ds);
        assertNotNull(ds.getDataStore(null));
    }
    
    @Test
    public void testAddStoreNonDefaultWorkspace() throws Exception {
        testAddWorkspace();
        
        assertNull(cat.getDataStoreByName("acme", "bar"));
        
        pi.exec("from geoserver.catalog import Workspace, Store");
        pi.exec("ws = Workspace('acme')");
        pi.exec("ws.save()");
        
        pi.exec("st = Store('bar', ws)");
        pi.exec("st.connectionParameters.putAll({'database':'target/bar', 'dbtype': 'h2'})");
        pi.exec("st.save()");
        
        assertNotNull(cat.getDataStoreByName("acme", "bar"));
    }
    
    @Test
    public void testSaveStore() throws Exception {
        testAddStore();
        
        assertNull(cat.getDataStoreByName("foo", "bar").getDescription());
        
        pi.exec("from geoserver.catalog import Store");
        pi.exec("st = Store('bar', 'foo')");
        pi.exec("st.description = 'foobar'");
        pi.exec("st.save()");
        
        assertEquals("foobar", cat.getDataStoreByName("foo", "bar").getDescription());
    }
    
    @Test
    public void testAddFeatureType() throws Exception {
        testAddStore();
        
        NamespaceInfo ns = cat.getFactory().createNamespace();
        ns.setPrefix("foo");
        ns.setURI("http://foo.org");
        cat.add(ns);
        
        DataStoreInfo ds = cat.getDataStoreByName("foo", "bar");
        
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        tb.setName("baz");
        tb.add("x", Integer.class);
        tb.add("y", String.class);
        ((DataStore)ds.getDataStore(null)).createSchema(tb.buildFeatureType());
        
        assertNull(cat.getFeatureTypeByDataStore(ds, "baz"));
        
        pi.exec("from geoserver.catalog import Catalog");
        pi.exec("from geoserver.catalog import Layer");
        pi.exec("cat = Catalog()");
        pi.exec("st = cat['foo']['bar']");
        pi.exec("l = Layer('baz', st)");
        pi.exec("l.save()");
        
        FeatureTypeInfo ft = cat.getFeatureTypeByDataStore(ds, "baz"); 
        
        assertNotNull(ft);
        assertNotNull(ft.getFeatureType());
        assertNotNull(ft.getFeatureSource(null, null));
    }
    
    @Test
    public void testSaveFeatureType() throws Exception {
        testAddStore();
        
        
    }
}
