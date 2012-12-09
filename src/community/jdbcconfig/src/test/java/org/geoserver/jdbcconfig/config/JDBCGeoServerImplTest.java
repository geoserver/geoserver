/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.config;

import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.config.GeoServerFacade;
import org.geoserver.config.GeoServerImplTest;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JdbcConfigTestSupport;

public class JDBCGeoServerImplTest extends GeoServerImplTest {

    private GeoServerFacade facade;

    private JdbcConfigTestSupport testSupport;

    @Override
    protected void setUp() throws Exception {
        testSupport = new JdbcConfigTestSupport();
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCGeoServerFacade(configDb);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
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

}
