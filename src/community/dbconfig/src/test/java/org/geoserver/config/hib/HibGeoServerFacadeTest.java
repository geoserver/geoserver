/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.hib;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.ServiceInfoImpl;
import org.geoserver.hibernate.HibTestSupport;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

public class HibGeoServerFacadeTest extends HibTestSupport {

    static GeoServerFacade dao;
    
    @BeforeClass
    public static void init() throws Exception {
        dao = (GeoServerFacade) ctx.getBean("hibGeoServerFacade");
    }
    
    @Before
    public void setUpData() throws Exception {
        for (ServiceInfo s : dao.getServices()) { dao.remove(s); }
    }
    @Test
    public void testGlobal() throws Exception {
        assertNull(dao.getGlobal());
        
        GeoServerInfo global = dao.getGeoServer().getFactory().createGlobal();
        dao.setGlobal(global);
        
        assertEquals(global, dao.getGlobal());
        
        global = dao.getGlobal();
        global.setAdminPassword("somePassword");
        dao.setGlobal(global);
        
        assertEquals(global, dao.getGlobal());
        
        global = dao.getGlobal();
        global.setAdminUsername("someUsername");
        dao.save(global);
        
        assertEquals(global, dao.getGlobal());
    }
    
    @Test
    public void testLogging() throws Exception {
        assertNull(dao.getLogging());
        
        LoggingInfo logging = dao.getGeoServer().getFactory().createLogging();
        dao.setLogging(logging);
        
        assertEquals(logging, dao.getLogging());
        
        logging = dao.getLogging();
        logging.setLevel("someLevel");
        dao.setLogging(logging);
        
        assertEquals(logging, dao.getLogging());
        
        logging = dao.getLogging();
        logging.setLocation("someLocation");
        dao.save(logging);
        
        assertEquals(logging, dao.getLogging());
    }
    
    @Test
    public void testAddService() throws Exception {
        assertEquals(0, dao.getServices().size());
        
        ServiceInfo service = dao.getGeoServer().getFactory().createService();
        ((ServiceInfoImpl)service).setId("someService");
        service.setName("someName");
        
        dao.add(service);
        assertEquals(1, dao.getServices().size());
        
        assertEquals(service, dao.getServices().iterator().next());
    }
    
    @Test
    public void testModifyService() throws Exception {
        testAddService();
        
        ServiceInfo service = dao.getServiceByName("someName", ServiceInfo.class);
        service.setName("someOtherName");
        dao.save(service);
        
        assertNull(dao.getServiceByName("someName", ServiceInfo.class));
        assertNotNull(dao.getServiceByName("someOtherName", ServiceInfo.class));
    }
    
    @Test
    public void testRemoveService() throws Exception {
        testAddService();
        assertEquals(1, dao.getServices().size());
        
        dao.remove(dao.getServices().iterator().next());
        assertEquals(0, dao.getServices().size());
        
    }
}
