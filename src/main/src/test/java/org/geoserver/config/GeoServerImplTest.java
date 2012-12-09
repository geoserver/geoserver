/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.ows.LocalWorkspace;
import org.junit.Before;
import org.junit.Test;

public class GeoServerImplTest {

    protected GeoServerImpl geoServer;
    
    @Before
    public void setUp() throws Exception {
        geoServer = createGeoServer();
    }
    
    protected GeoServerImpl createGeoServer() {
        GeoServerImpl gs = new GeoServerImpl();
        gs.setCatalog(new CatalogImpl());
        return gs;
    }
    
    @Test 
    public void testGlobal() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );
       
        assertEquals( global, geoServer.getGlobal() );
    }
    
    @Test 
    public void testModifyGlobal() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );

        GeoServerInfo g1 = geoServer.getGlobal();
        g1.setAdminPassword( "newAdminPassword" );
        
        GeoServerInfo g2 = geoServer.getGlobal();
        assertNull( g2.getAdminPassword() );
        
        geoServer.save( g1 );
        g2 = geoServer.getGlobal();
        assertEquals( "newAdminPassword", g2.getAdminPassword() );
    }
    
    @Test
    public void testAddService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        service.setName( "foo" );
        geoServer.add( service );
        
        ServiceInfo s2 = geoServer.getFactory().createService();
        ((ServiceInfoImpl)s2).setId(service.getId());
        
        try {
            geoServer.add( s2 );
            fail( "adding service with duplicate id should throw exception" );
        }
        catch( Exception e ) {}
        
        ServiceInfo s = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertTrue( s != service );
        assertEquals( service, s );
    }
    
    @Test
    public void testModifyService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        ((ServiceInfoImpl)service).setId( "id" );
        service.setName( "foo" );
        service.setTitle( "bar" );
        
        geoServer.add( service );
        
        ServiceInfo s1 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        s1.setTitle( "changed" );
        
        ServiceInfo s2 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertEquals( "bar", s2.getTitle() );
        
        geoServer.save( s1 );
        s2 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertEquals( "changed", s2.getTitle() );
    }
    
    @Test
    public void testGlobalEvents() throws Exception {
        
        TestListener tl = new TestListener();
        geoServer.addListener( tl );
        
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );
       
        global = geoServer.getGlobal();
        global.setAdminPassword( "foo" );
        global.setOnlineResource( "bar" );
        
        assertEquals( 0, tl.gPropertyNames.size() );
        geoServer.save( global );
        
        assertEquals( 2, tl.gPropertyNames.size() );
        assertTrue( tl.gPropertyNames.contains( "adminPassword" ) );
        assertTrue( tl.gPropertyNames.contains( "onlineResource" ) );
    }
    
    static class TestListener extends ConfigurationListenerAdapter {

        List<String> gPropertyNames = new ArrayList();
        List<Object> gOldValues = new ArrayList();
        List<Object> gNewValues = new ArrayList();
        
        List<String> sPropertyNames = new ArrayList();
        List<Object> sOldValues = new ArrayList();
        List<Object> sNewValues = new ArrayList();
        
        public void handleGlobalChange(GeoServerInfo global,
                List<String> propertyNames, List<Object> oldValues,
                List<Object> newValues) {
            gPropertyNames.addAll( propertyNames );
            gOldValues.addAll( oldValues );
            gNewValues.addAll( newValues );
        }

        public void handleServiceChange(ServiceInfo service,
                List<String> propertyNames, List<Object> oldValues,
                List<Object> newValues) {
            
            sPropertyNames.addAll( propertyNames );
            sOldValues.addAll( oldValues );
            sNewValues.addAll( newValues );
        }
    }
    

    @Test
    public void testSetClientPropsHasEffect() throws Exception {
        GeoServerInfoImpl gsii = new GeoServerInfoImpl(geoServer);
        Map<Object, Object> before = gsii.getClientProperties();
        
        Map<Object, Object> newProps = new HashMap<Object, Object>();
        newProps.put("123", "456");
		gsii.setClientProperties(newProps);
		
		assertFalse(before.equals(newProps));
    }

    @Test
    public void testGetSettings() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );

        SettingsInfo s = geoServer.getSettings();
        assertNotNull(s);

        assertEquals(4, s.getNumDecimals());
       
        WorkspaceInfo ws = geoServer.getCatalog().getFactory().createWorkspace();
        ws.setName("acme");
        geoServer.getCatalog().add(ws);
        
        SettingsInfo t = geoServer.getFactory().createSettings();
        t.setNumDecimals(7);
        t.setWorkspace(ws);
        geoServer.add(t);

        assertNotNull(geoServer.getSettings(ws));
        assertEquals(7, geoServer.getSettings(ws).getNumDecimals());

        assertEquals(4, geoServer.getSettings().getNumDecimals());
        LocalWorkspace.set(ws);
        try {
            assertNotNull(geoServer.getSettings());
            assertEquals(7, geoServer.getSettings().getNumDecimals());
        }
        finally {
            LocalWorkspace.remove();
        }
    }
}
