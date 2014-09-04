/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.hib;

import static org.junit.Assert.*;

import java.lang.reflect.Method;
import java.util.List;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFacade;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImplTest;
import org.geoserver.hibernate.HibUtil;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibCatalogImplTest extends CatalogImplTest {

    XmlWebApplicationContext ctx;
    SessionFactory sessionFactory;
    
    @Override
    protected Catalog createCatalog() {
        ctx = new XmlWebApplicationContext() {
            public String[] getConfigLocations() {
                return new String[]{
                    "file:src/main/resources/applicationContext.xml", 
                    "file:src/test/resources/applicationContext-test.xml"
                };
            }
        };
        ctx.refresh();
        return (Catalog) ctx.getBean("catalog");
    }
    
    @Before
    public void setUp() throws Exception {
        super.setUp();
        
        sessionFactory = (SessionFactory) ctx.getBean("hibSessionFactory");
        HibUtil.setUpSession(sessionFactory, true);
        
        CatalogFacade dao = catalog.getFacade();
        
        for (LayerGroupInfo lg : dao.getLayerGroups())  { dao.remove(lg); }
        for (LayerInfo l : dao.getLayers())  { dao.remove(l); }
        for (ResourceInfo r : dao.getResources(ResourceInfo.class))  { dao.remove(r); }
        for (StoreInfo s : dao.getStores(StoreInfo.class)) { dao.remove(s); }
        for (WorkspaceInfo ws : dao.getWorkspaces()){ dao.remove(ws); }
        for (NamespaceInfo ns : dao.getNamespaces()){ dao.remove(ns); }
        for (StyleInfo s : dao.getStyles()){ dao.remove(s); }
    }
    
    @After
    public void tearDown() throws Exception {
        HibUtil.tearDownSession(sessionFactory, null);
    }
    
    @Override
    @Test
    public void testProxyBehaviour() throws Exception {
        // do nothing, does not apply
    }
    
    @Override
    @Test
    public void testProxyListBehaviour() throws Exception {
        // do nothing, does not apply
    }
    
    @Override
    @Test
    public void testModifyMetadata() {
        // TODO: currently this does not work becuae hibernate does not intercept the change to the 
        // metadata map... figure out how to do this. 
    }
    
    @Override
    @Test
    public void testGetNamespaceById() {
        catalog.add( ns );
        NamespaceInfo ns2 = catalog.getNamespace(ns.getId());
        
        assertNotNull(ns2);
        assertEquals( ns, ns2 );
    }
    
    @Override
    @Test
    public void testGetNamespaceByPrefix() {
        catalog.add( ns );

        NamespaceInfo ns2 = catalog.getNamespaceByPrefix(ns.getPrefix());
        assertNotNull(ns2);
        assertEquals( ns, ns2 );
        
        NamespaceInfo ns3 = catalog.getNamespaceByPrefix(null);
        assertNotNull(ns3);
        assertEquals( ns, ns3 );
        
        NamespaceInfo ns4 = catalog.getNamespaceByPrefix(Catalog.DEFAULT);
        assertNotNull(ns4);
        assertEquals( ns, ns4 );
    }
    
    @Override
    @Test
    public void testGetNamespaceByURI() {
        catalog.add( ns );
        NamespaceInfo ns2 = catalog.getNamespaceByURI(ns.getURI());
        
        assertNotNull(ns2);
        assertEquals( ns, ns2 );
    }
    
    @Override
    @Test
    public void testModifyNamespace() {
  catalog.add( ns );
        
        NamespaceInfo ns2 = catalog.getNamespaceByPrefix(ns.getPrefix());
        ns2.setPrefix( null );
        ns2.setURI( null );
        
        try {
            catalog.save(ns2);
            fail( "setting prefix to null should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setPrefix( "ns2Prefix" );
        try {
            catalog.save(ns2);
            fail( "setting uri to null should throw exception");
        }
        catch( Exception e ) {
        }
        
        ns2.setURI( "ns2URI");
        
        catalog.save( ns2 );
        //ns3 = catalog.getNamespaceByPrefix(ns.getPrefix());
        NamespaceInfo ns3 = catalog.getNamespaceByPrefix("ns2Prefix");
        assertEquals(ns2, ns3);
        assertEquals( "ns2Prefix", ns3.getPrefix() );
        assertEquals( "ns2URI", ns3.getURI() );
    }
    
    @Override
    @Test
    public void testGetWorkspaceById() {
        catalog.add( ws );
        WorkspaceInfo ws2 = catalog.getWorkspace(ws.getId());
        
        assertNotNull(ws2);
        assertEquals( ws, ws2 );
    }
    
    @Override
    @Test
    public void testGetWorkspaceByName() {
        catalog.add( ws );
        WorkspaceInfo ws2 = catalog.getWorkspaceByName(ws.getName());
        
        assertNotNull(ws2);
        assertEquals( ws, ws2 );
        
        WorkspaceInfo ws3 = catalog.getWorkspaceByName(null);
        assertNotNull(ws3);
        assertEquals( ws, ws3 );
        
        WorkspaceInfo ws4 = catalog.getWorkspaceByName(Catalog.DEFAULT);
        assertNotNull(ws4);
        assertEquals( ws, ws4 );
    }
    
    @Override
    @Test
    public void testModifyWorkspace() {
        catalog.add( ws );
        
        WorkspaceInfo ws2 = catalog.getWorkspaceByName(ws.getName());
        ws2.setName( null );
        try {
            catalog.save( ws2 );
            fail( "setting name to null should throw exception");
        }
        catch( Exception e) {
        }
        
        ws2.setName( "ws2");
        
        catalog.save( ws2 );
        WorkspaceInfo ws3 = catalog.getWorkspaceByName(ws2.getName());
        assertEquals(ws2, ws3);
        assertEquals( "ws2", ws3.getName() );
    }
    
    @Override
    @Test
    public void testGetDataStoreById() {
        addDataStore();
        
        DataStoreInfo ds2 = catalog.getDataStore(ds.getId());
        assertNotNull(ds2);
        assertEquals( ds, ds2 );
    }
    
    @Override
    @Test
    public void testGetDataStoreByName() {
        addDataStore();
        
        DataStoreInfo ds2 = catalog.getDataStoreByName(ds.getName());
        assertNotNull(ds2);
        assertEquals( ds, ds2 );
        
        DataStoreInfo ds3 = catalog.getDataStoreByName(ws, null);
        assertNotNull(ds3);
        assertEquals( ds, ds3 );
        
        DataStoreInfo ds4 = catalog.getDataStoreByName(ws, Catalog.DEFAULT);
        assertNotNull(ds4);
        assertEquals( ds, ds4 );
        
        DataStoreInfo ds5 = catalog.getDataStoreByName(Catalog.DEFAULT, Catalog.DEFAULT);
        assertNotNull(ds5);
        assertEquals( ds, ds5 );
    }
    
    @Override
    @Test
    public void testModifyDataStore() {
        addDataStore();
        
        DataStoreInfo ds2 = catalog.getDataStoreByName(ds.getName());
        ds2.setName( "dsName2" );
        ds2.setDescription( "dsDescription2" );
        
        catalog.save( ds2 );
        DataStoreInfo ds3 = catalog.getDataStoreByName("dsName2");
        assertEquals(ds2, ds3);
        assertEquals( "dsName2", ds3.getName() );
        assertEquals( "dsDescription2", ds3.getDescription() );
    }
    
    @Override
    @Test
    public void testGetFeatureTypeById() {
        addFeatureType();
        FeatureTypeInfo  ft2 = catalog.getFeatureType(ft.getId());
        
        assertNotNull(ft2);
        assertEquals( ft, ft2 );
    }
    
    @Override
    @Test
    public void testGetFeatureTypeByName() {
        addFeatureType();
        FeatureTypeInfo  ft2 = catalog.getFeatureTypeByName(ft.getName());
        
        assertNotNull(ft2);
        assertEquals( ft, ft2 );
        
        NamespaceInfo ns2 = catalog.getFactory().createNamespace();
        ns2.setPrefix( "ns2Prefix" );
        ns2.setURI( "ns2URI" );
        catalog.add( ns2 );
        
        FeatureTypeInfo ft3 = catalog.getFactory().createFeatureType();
        ft3.setName( "ft3Name" );
        ft3.setStore( ds );
        ft3.setNamespace( ns2 );
        catalog.add( ft3 );
        
        FeatureTypeInfo ft4 = catalog.getFeatureTypeByName(ns2.getPrefix(), ft3.getName() );
        assertNotNull(ft4);
        assertEquals( ft3, ft4 );
        
        ft4 = catalog.getFeatureTypeByName(ns2.getURI(), ft3.getName() );
        assertNotNull(ft4);
        assertEquals( ft3, ft4 );
    }
    
    @Override
    @Test
    public void testModifyFeatureType() {
        addFeatureType();
        
        FeatureTypeInfo ft2 = catalog.getFeatureTypeByName(ft.getName());
        ft2.setDescription( "ft2Description" );
        ft2.getKeywords().add(new Keyword("ft2"));
        catalog.save( ft2 );
        
        FeatureTypeInfo  ft3 = catalog.getFeatureTypeByName(ft.getName());
        assertEquals(ft2, ft3);
        assertEquals( "ft2Description", ft3.getDescription() );
        assertEquals( 1, ft3.getKeywords().size() );
    }
    
    @Override
    @Test
    public void testGetLayerById() {
        addLayer();
        
        LayerInfo l2 = catalog.getLayer( l.getId() );
        assertNotNull(l2);
        assertEquals( l, l2 );
    }
    
    @Override
    @Test
    public void testGetLayerByName() {
        addLayer();
        
        LayerInfo l2 = catalog.getLayerByName( l.getName() );
        assertNotNull(l2);
        assertEquals( l, l2 );
    }
    
    @Override
    @Test
    public void testGetLayerByResource() {
        addLayer();
        
        List<LayerInfo> layers = catalog.getLayers(ft);
        assertEquals( 1, layers.size() );
        LayerInfo l2 = layers.get(0);
        
        assertEquals( l, l2 );
    }
    
    @Override
    @Test
    public void testModifyLayer() {
        addLayer();
        
        LayerInfo l2 = catalog.getLayerByName( l.getName() );
        l2.setResource( null );
        
        try {
            catalog.save(l2);
            fail( "setting resource to null should throw exception");
        }
        catch( Exception e ) {}
        
        l2.setResource(ft);
        catalog.save(l2);
        
        // TODO: reinstate with resource/publishing split done
        // l3 = catalog.getLayerByName( "changed" );
        LayerInfo l3 = catalog.getLayerByName( ft.getName() );
        assertNotNull(l3);
    }
    
    @Override
    @Test
    public void testGetStyleById() {
        addStyle();
        
        StyleInfo s2 = catalog.getStyle( s.getId() );
        assertNotNull( s2 );
        assertEquals(s,s2);
    }
    
    @Override
    @Test
    public void testGetStyleByName() {
        addStyle();
        
        StyleInfo s2 = catalog.getStyleByName( s.getName() );
        assertNotNull( s2 );
        assertEquals(s,s2);
    }

    @Override
    @Test
    public void testModifyStyle() {
        addStyle();
        
        StyleInfo s2 = catalog.getStyleByName( s.getName() );
        s2.setName( null );
        s2.setFilename( null );
        
        try {
            catalog.save(s2);
            fail("setting name to null should fail");
        }
        catch( Exception e ) {}
        
        s2.setName( "s2Name");
        try {
            catalog.save(s2);
            fail("setting filename to null should fail");
        }
        catch( Exception e ) {}
        
        s2.setFilename( "s2Filename");
        catalog.save( s2 );
        
        StyleInfo s3 = catalog.getStyleByName( "styleName" );
        assertNull( s3 );
        
        s3 = catalog.getStyleByName( s2.getName() );
        assertEquals( s2, s3 );
    }
    
    @Override
    @Test
    public void testGetLayerByIdWithConcurrentAdd() throws Exception {
        //disabling for now, requires lazy access to hibernate object in separate thread so need
        // to setu p session there, doable just need to refactor a bit the base test
    }
}
