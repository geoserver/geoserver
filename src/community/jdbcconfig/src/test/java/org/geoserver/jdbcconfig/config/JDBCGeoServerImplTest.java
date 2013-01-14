/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.config;

import static org.junit.Assert.assertNotNull;

import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerImplTest;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.junit.After;

public class JDBCGeoServerImplTest extends GeoServerImplTest {

    private GeoServerFacade facade;

    private JDBCConfigTestSupport testSupport;

    @Override
    public void setUp() throws Exception {
        testSupport = new JDBCConfigTestSupport();
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCGeoServerFacade(configDb);

        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        facade.dispose();
        testSupport.tearDown();
    }

    @Override
    protected GeoServerImpl createGeoServer() {
        GeoServerImpl gs = new GeoServerImpl();
        gs.setFacade(facade);
        CatalogImpl catalog = testSupport.getCatalog();
        catalog.setFacade(new JDBCCatalogFacade(testSupport.getDatabase()));
        gs.setCatalog(catalog);
        return gs;
    }

    @Override
    public void testAddService() throws Exception {
        super.testAddService();

        //ensure s.getGeoServer() != null
        ServiceInfo s = geoServer.getServiceByName( "foo", ServiceInfo.class );
        assertNotNull(s.getGeoServer());
    }
    
}
