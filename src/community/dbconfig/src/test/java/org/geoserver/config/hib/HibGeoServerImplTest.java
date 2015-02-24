/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.hib;

import static org.junit.Assert.*;

import org.geoserver.catalog.Keyword;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerImplTest;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.hibernate.HibUtil;
import org.h2.tools.DeleteDbFiles;
import org.hibernate.SessionFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.web.context.support.XmlWebApplicationContext;

public class HibGeoServerImplTest extends GeoServerImplTest {

    XmlWebApplicationContext ctx;
    SessionFactory sessionFactory;
    
    @Override
    protected GeoServerImpl createGeoServer() {
        ctx = new XmlWebApplicationContext() {
            public String[] getConfigLocations() {
                return new String[]{
                    "file:src/main/resources/applicationContext.xml", 
                    "file:src/test/resources/applicationContext-test.xml"
                };
            }
        };
        ctx.refresh();
        return (GeoServerImpl) ctx.getBean("geoServer");
    }
    
    @Before
    public void setUp() throws Exception {
        DeleteDbFiles.execute("target", "geoserver", true);
        super.setUp();
        
        GeoServerFacade dao = geoServer.getFacade();
        for (ServiceInfo s : dao.getServices()) { dao.remove(s); }
        GeoServerInfo global = dao.getGlobal();
        if (global != null) {
            //global.setAdminPassword(null);
            //dao.save(global);
            dao.setGlobal(null);
        }
        
        sessionFactory = (SessionFactory) ctx.getBean("hibSessionFactory");
        HibUtil.setUpSession(sessionFactory);
    }
    
    @After
    public void tearDown() throws Exception {
        HibUtil.tearDownSession(sessionFactory, new Exception());
    }
    
    //
    // the following methods don't pass because of assumptions that the test makes about the 
    // default catalog implementation, we need a proper GeoServer api test
    //
    
    @Override
    @Test
    public void testModifyGlobal() throws Exception {
        GeoServerInfo global = geoServer.getFactory().createGlobal();
        geoServer.setGlobal( global );

        GeoServerInfo g1 = geoServer.getGlobal();
        g1.setAdminPassword( "newAdminPassword" );
        geoServer.save( g1 );
        
        GeoServerInfo g2 = geoServer.getGlobal();
        assertEquals( "newAdminPassword", g2.getAdminPassword() );
    }
    
    @Override
    @Test
    public void testAddService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        service.setName( "foo" );
        
        try {
            geoServer.add( service );
            fail( "adding without id should throw exception" );
        }
        catch( Exception e ) {};
        
        ((ServiceInfoImpl)service).setId( "id" );
        service.getKeywords().add(new Keyword("keyword"));
        geoServer.add( service );

        assertEquals(service.getKeywords(), geoServer.getService("id", ServiceInfo.class).getKeywords());

        ServiceInfo s2 = geoServer.getFactory().createService();
        ((ServiceInfoImpl)s2).setId( "id" );
        try {
            geoServer.add( s2 );
            fail( "adding service with duplicate id should throw exception" );
        }
        catch( Exception e ) {}
        
        ServiceInfo s = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertEquals( service, s );
    }
    
    @Override
    @Test
    public void testModifyService() throws Exception {
        ServiceInfo service = geoServer.getFactory().createService();
        ((ServiceInfoImpl)service).setId( "id" );
        service.setName( "foo" );
        service.setTitle( "bar" );
        
        geoServer.add( service );
        
        ServiceInfo s1 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        s1.setTitle( "changed" );
        
        geoServer.save( s1 );
        ServiceInfo s2 = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertEquals( "changed", s2.getTitle() );
    }
}
