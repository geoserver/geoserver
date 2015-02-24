/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.hibernate.HibTestSupport;
import org.h2.tools.DeleteDbFiles;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HibCatalogFacadeTest extends HibTestSupport {

    static CatalogFacade dao;
    
    @BeforeClass
    public static void init() throws Exception {
        dao = (CatalogFacade) ctx.getBean("hibCatalogFacade");
    }
    
    @AfterClass
    public static void destroy() throws Exception {
        ctx.close();
        DeleteDbFiles.execute(".", "geoserver", false);
    }
    
    @Before
    public void setUpData() throws Exception {
        //clear the catalog
        
        for (LayerGroupInfo lg : dao.getLayerGroups())  { dao.remove(lg); }
        for (LayerInfo l : dao.getLayers())  { dao.remove(l); }
        for (ResourceInfo r : dao.getResources(ResourceInfo.class))  { dao.remove(r); }
        for (StoreInfo s : dao.getStores(StoreInfo.class)) { dao.remove(s); }
        for (WorkspaceInfo ws : dao.getWorkspaces()){ dao.remove(ws); }
        for (NamespaceInfo ns : dao.getNamespaces()){ dao.remove(ns); }
        for (StyleInfo s : dao.getStyles()){ dao.remove(s); }
    
        assertEquals(0, dao.getWorkspaces().size());
    }
    
    @Test
    public void testAddWorkspace() throws Exception {
        assertEquals(0, dao.getWorkspaces().size());
        
        WorkspaceInfo ws = dao.getCatalog().getFactory().createWorkspace();
        ws.setName("acme");
        assertNull(ws.getId());
        
        dao.add(ws);
        assertNotNull(ws.getId());
        
        assertEquals(1, dao.getWorkspaces().size());
        assertEquals(ws, dao.getWorkspace(ws.getId()));
    }
    
    @Test
    public void testGetWorkspaceByName() throws Exception {
        assertNull(dao.getWorkspaceByName("acme"));
        testAddWorkspace();
        assertNotNull(dao.getWorkspaceByName("acme"));
    }
    
    @Test
    public void testModifyWorkspace() throws Exception {
        WorkspaceInfo ws = dao.getCatalog().getFactory().createWorkspace();
        ws.setName("foo");
        dao.add(ws);
        
        ws = dao.getWorkspaceByName("foo");
        ws.setName("bar");
        dao.save(ws);
        
        assertNull(dao.getWorkspaceByName("foo"));
        assertNotNull(dao.getWorkspaceByName("bar"));
    }
    
    @Test
    public void testRemoveWorkspace() throws Exception {
        WorkspaceInfo ws = dao.getCatalog().getFactory().createWorkspace();
        ws.setName("baz");
        dao.add(ws);
        
        assertNotNull(dao.getWorkspaceByName("baz"));
        int n = dao.getWorkspaces().size();
        
        dao.remove(ws);
        assertNull(dao.getWorkspaceByName("baz"));
        assertEquals(n-1, dao.getWorkspaces().size());
    }
    
    @Test
    public void testDefaultWorkspace() throws Exception {
        testAddWorkspace();
        
        assertNull(dao.getDefaultWorkspace());
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        dao.setDefaultWorkspace(ws);
     
        assertNotNull(dao.getDefaultWorkspace());
        assertEquals("acme", dao.getDefaultWorkspace().getName());
        
        ws = dao.getCatalog().getFactory().createWorkspace();
        ws.setName("bam");
        dao.add(ws);
        dao.setDefaultWorkspace(ws);
        
        assertEquals("bam", dao.getDefaultWorkspace().getName());
    
        dao.setDefaultWorkspace(null);
        assertNull(dao.getDefaultWorkspace());
    }
    
    @Test
    public void testAddNamespace() throws Exception {
        assertEquals(0, dao.getNamespaces().size());
        
        NamespaceInfo ws = dao.getCatalog().getFactory().createNamespace();
        ws.setPrefix("acme");
        ws.setURI("http://acme.com");
        assertNull(ws.getId());
        
        dao.add(ws);
        assertNotNull(ws.getId());
        
        assertEquals(1, dao.getNamespaces().size());
        assertEquals(ws, dao.getNamespace(ws.getId()));
    }
    
    @Test
    public void testGetNamespace() throws Exception {
        assertNull(dao.getNamespaceByPrefix("acme"));
        testAddNamespace();
        assertNotNull(dao.getNamespaceByPrefix("acme"));
        assertNotNull(dao.getNamespaceByURI("http://acme.com"));
    }
    
    @Test
    public void testModifyNamespace() throws Exception {
        NamespaceInfo ws = dao.getCatalog().getFactory().createNamespace();
        ws.setPrefix("foo");
        ws.setURI("http://foo.org");
        dao.add(ws);
        
        ws = dao.getNamespaceByPrefix("foo");
        ws.setPrefix("bar");
        dao.save(ws);
        
        assertNull(dao.getNamespaceByPrefix("foo"));
        assertNotNull(dao.getNamespaceByPrefix("bar"));
    }
    
    @Test
    public void testRemoveNamespace() throws Exception {
        NamespaceInfo ws = dao.getCatalog().getFactory().createNamespace();
        ws.setPrefix("baz");
        ws.setURI("http://baz.org");
        dao.add(ws);
        
        assertNotNull(dao.getNamespaceByPrefix("baz"));
        int n = dao.getNamespaces().size();
        
        dao.remove(ws);
        assertNull(dao.getNamespaceByPrefix("baz"));
        assertEquals(n-1, dao.getNamespaces().size());
    }
    
    @Test
    public void testDefaultNamespace() throws Exception {
        testAddNamespace();
        
        assertNull(dao.getDefaultNamespace());
        NamespaceInfo ws = dao.getNamespaceByPrefix("acme");
        dao.setDefaultNamespace(ws);
     
        assertNotNull(dao.getDefaultNamespace());
        assertEquals("acme", dao.getDefaultNamespace().getName());
        
        ws = dao.getCatalog().getFactory().createNamespace();
        ws.setPrefix("bam");
        dao.add(ws);
        dao.setDefaultNamespace(ws);
        
        assertEquals("bam", dao.getDefaultNamespace().getName());
    
        dao.setDefaultNamespace(null);
        assertNull(dao.getDefaultNamespace());
    }
    
    @Test
    public void testAddDataStore() throws Exception {
        testAddWorkspace();
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        
        assertEquals(0, dao.getStoresByWorkspace(ws, DataStoreInfo.class).size());
        
        DataStoreInfo ds = dao.getCatalog().getFactory().createDataStore();
        ds.setWorkspace(ws);
        ds.setName("widgets");
        dao.add(ds);
     
        assertEquals(1, dao.getStoresByWorkspace(ws, DataStoreInfo.class).size());
    }
    
    @Test
    public void testModifyDataStore() throws Exception {
        testAddDataStore();
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme"); 
        
        DataStoreInfo ds = dao.getStoreByName( ws, "widgets", DataStoreInfo.class );
        ds.setName("foo");
        dao.save(ds);
        
        assertNull(dao.getStoreByName( ws, "widgets", DataStoreInfo.class));
        assertNotNull(dao.getStoreByName( ws, "foo", DataStoreInfo.class));
    }
    
    @Test
    public void testRemoveDataStore() throws Exception {
        testAddDataStore();
        assertEquals(1, dao.getStores(DataStoreInfo.class).size());
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        
        DataStoreInfo ds = dao.getStoreByName( ws, "widgets", DataStoreInfo.class );
        dao.remove(ds);
        
        assertNull(dao.getStoreByName( ws, "widgets", DataStoreInfo.class ));
        assertEquals(0, dao.getStores(DataStoreInfo.class).size());
    }
    
    @Test
    public void testDefaultDataStore() throws Exception {
        testAddDataStore();
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        DataStoreInfo ds = dao.getStoreByName(ws, "widgets", DataStoreInfo.class);
        dao.setDefaultDataStore(ws, ds);
        
        DataStoreInfo ds1 = dao.getDefaultDataStore(ws);
        
        assertEquals(ds, ds1);
        
        DataStoreInfo ds2 = dao.getCatalog().getFactory().createDataStore();
        ds2.setWorkspace(ws);
        ds2.setName("things");
        dao.add(ds2);
        dao.setDefaultDataStore(ws, ds2);
        
        assertEquals(dao.getStoreByName(ws, "things", DataStoreInfo.class), ds2);
        
    }
    
    @Test
    public void testAddCoverageStore() throws Exception {
        testAddWorkspace();
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        
        assertEquals(0, dao.getStoresByWorkspace(ws, CoverageStoreInfo.class).size());
        
        CoverageStoreInfo cs = dao.getCatalog().getFactory().createCoverageStore();
        cs.setWorkspace(ws);
        cs.setName("widgets");
        dao.add(cs);
     
        assertEquals(1, dao.getStoresByWorkspace(ws, CoverageStoreInfo.class).size());
    }
    
    @Test
    public void testModifyCoverageStore() throws Exception {
        testAddCoverageStore();
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme"); 
        
        CoverageStoreInfo cs = dao.getStoreByName( ws, "widgets", CoverageStoreInfo.class );
        cs.setName("foo");
        dao.save(cs);
        
        assertNull(dao.getStoreByName( ws, "widgets", CoverageStoreInfo.class));
        assertNotNull(dao.getStoreByName( ws, "foo", CoverageStoreInfo.class));
    }
    
    @Test
    public void testRemoveCoverageStore() throws Exception {
        testAddCoverageStore();
        assertEquals(1, dao.getStores(CoverageStoreInfo.class).size());
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        
        CoverageStoreInfo cs = dao.getStoreByName( ws, "widgets", CoverageStoreInfo.class );
        dao.remove(cs);
        
        assertNull(dao.getStoreByName( ws, "widgets", CoverageStoreInfo.class ));
        assertEquals(0, dao.getStores(CoverageStoreInfo.class).size());
    }
    
    @Test
    public void testAddFeatureType() throws Exception {
        testAddDataStore();
        testAddNamespace();
        
        DataStoreInfo ds = 
            dao.getStoreByName(dao.getWorkspaceByName("acme"), "widgets", DataStoreInfo.class );
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme"); 
        
        assertNull(dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class));
        
        FeatureTypeInfo ft = dao.getCatalog().getFactory().createFeatureType();
        ft.setName("anvil");
        ft.setNativeName("anvil");
        ft.setStore(ds);
        ft.setNamespace(ns);
        
        dao.add(ft);
        
        assertNotNull(dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class));
    }
    
    @Test
    public void testModifyFeatureType() throws Exception {
        testAddFeatureType();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme"); 
        FeatureTypeInfo ft = dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class);
        
        ft.setName("dynamite");
        dao.save(ft);
        
        assertNull(dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class));
        assertNotNull(dao.getResourceByName(ns, "dynamite", FeatureTypeInfo.class));
    }
    
    @Test
    public void testRemoveFeatureType() throws Exception {
        testAddFeatureType();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        FeatureTypeInfo ft = dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class);
        
        dao.remove(ft);
        assertNull(dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class));
    }
    
    @Test
    public void testGetFeatureType() throws Exception {
        testAddFeatureType();
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        DataStoreInfo ds = dao.getStoreByName(ws, "widgets", DataStoreInfo.class);
        
        assertNotNull(dao.getResourceByStore(ds, "anvil", FeatureTypeInfo.class));
        assertEquals(1, dao.getResourcesByStore(ds, FeatureTypeInfo.class).size());
        
        assertNotNull(dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class));
        assertEquals(1, dao.getResourcesByNamespace(ns, FeatureTypeInfo.class).size());
    }
    
    @Test
    public void testAddCoverage() throws Exception {
        testAddCoverageStore();
        testAddNamespace();
        
        CoverageStoreInfo ds = 
            dao.getStoreByName(dao.getWorkspaceByName("acme"), "widgets", CoverageStoreInfo.class );
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme"); 
        
        assertNull(dao.getResourceByName(ns, "anvil", CoverageInfo.class));
        
        CoverageInfo ft = dao.getCatalog().getFactory().createCoverage();
        ft.setName("anvil");
        ft.setNativeName("anvil");
        ft.setStore(ds);
        ft.setNamespace(ns);
        
        dao.add(ft);
        
        assertNotNull(dao.getResourceByName(ns, "anvil", CoverageInfo.class));
    }
    
    @Test
    public void testModifyCoverage() throws Exception {
        testAddCoverage();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme"); 
        CoverageInfo ft = dao.getResourceByName(ns, "anvil", CoverageInfo.class);
        
        ft.setName("dynamite");
        dao.save(ft);
        
        assertNull(dao.getResourceByName(ns, "anvil", CoverageInfo.class));
        assertNotNull(dao.getResourceByName(ns, "dynamite", CoverageInfo.class));
    }
    
    @Test
    public void testRemoveCoverage() throws Exception {
        testAddCoverage();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        CoverageInfo ft = dao.getResourceByName(ns, "anvil", CoverageInfo.class);
        
        dao.remove(ft);
        assertNull(dao.getResourceByName(ns, "anvil", CoverageInfo.class));
    }
    
    @Test
    public void testGetCoverage() throws Exception {
        testAddCoverage();
        
        WorkspaceInfo ws = dao.getWorkspaceByName("acme");
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        CoverageStoreInfo ds = dao.getStoreByName(ws, "widgets", CoverageStoreInfo.class);
        
        assertNotNull(dao.getResourceByStore(ds, "anvil", CoverageInfo.class));
        assertEquals(1, dao.getResourcesByStore(ds, CoverageInfo.class).size());
        
        assertNotNull(dao.getResourceByName(ns, "anvil", CoverageInfo.class));
        assertEquals(1, dao.getResourcesByNamespace(ns, CoverageInfo.class).size());
    }
    
    @Test
    public void testAddStyle() throws Exception {
        StyleInfo s = dao.getCatalog().getFactory().createStyle();
        s.setName("blue");
        
        dao.add(s);
        
        assertEquals(s, dao.getStyleByName("blue"));
    }
    
    @Test
    public void testModifyStyle() throws Exception {
        testAddStyle();
        
        StyleInfo st = dao.getStyleByName("blue");
        st.setName("red");
        dao.save(st);
        
        assertNull(dao.getStyleByName("blue"));
        assertNotNull(dao.getStyleByName("red"));
    }
    
    @Test
    public void testRemoveStyle() throws Exception {
        testAddStyle();
        
        StyleInfo st = dao.getStyleByName("blue");
        dao.remove(st);
        
        assertNull(dao.getStyleByName("blue"));
    }
    
    @Test
    public void testAddLayer() throws Exception {
        testAddFeatureType();
        testAddStyle();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        FeatureTypeInfo ft = dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class);
        StyleInfo s = dao.getStyleByName("blue");
        
        LayerInfo l = dao.getCatalog().getFactory().createLayer();
        l.setPath("/anvil");
        l.setResource(ft);
        l.setDefaultStyle(s);
        
        dao.add(l);
        
        assertEquals(l, dao.getLayerByName("anvil"));
    }
    
    @Test
    public void testModifyLayer() throws Exception {
        testAddLayer();
        
        LayerInfo l = dao.getLayerByName("anvil");
        l.setPath("changed");
        dao.save(l);
        
        l = dao.getLayerByName("anvil");
        assertEquals("changed", l.getPath());
    }
    
    @Test
    public void testRemoveLayer() throws Exception {
        testAddLayer();
        
        LayerInfo l = dao.getLayerByName("anvil");
        dao.remove(l);
       
        assertNull(dao.getLayerByName("anvil"));
    }
    
    @Test
    public void testGetLayer() throws Exception {
        testAddLayer();
        
        NamespaceInfo ns = dao.getNamespaceByPrefix("acme");
        FeatureTypeInfo ft = dao.getResourceByName(ns, "anvil", FeatureTypeInfo.class);
        assertEquals(1, dao.getLayers(ft).size());
        
        StyleInfo s1 = dao.getStyleByName("blue");
        assertEquals(1, dao.getLayers(s1).size());
        
        //TODO: we need to ge tthe following test to pass
//        StyleInfo s2 = dao.getCatalog().getFactory().createStyle();
//        s2.setName("red");
//        dao.add(s2);
//        
//        LayerInfo l = dao.getLayerByName("anvil");
//        l.getStyles().add(s2);
//        dao.save(l);
//        
//        assertEquals(1, dao.getLayers(s2).size());
    }
    
    @Test
    public void testAddLayerGroup() throws Exception {
        testAddLayer();
        
        LayerInfo l = dao.getLayerByName("anvil");
       
        LayerGroupInfo lg = dao.getCatalog().getFactory().createLayerGroup();
        lg.setName("anvils");
        lg.getLayers().add(l);
        
        dao.add(lg);
        
        assertEquals(lg, dao.getLayerGroupByName("anvils"));
        
    }
}
