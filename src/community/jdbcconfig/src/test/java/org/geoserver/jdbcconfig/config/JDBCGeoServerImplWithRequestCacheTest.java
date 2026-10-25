/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.config;

import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.RequestCachingFacade;
import org.geoserver.config.impl.GeoServerImpl;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.geoserver.jdbcconfig.catalog.JDBCCatalogFacade;
import org.junit.After;

/** Runs the configuration tests on jdbcconfig through the request cache, with a request open for each whole test. */
public class JDBCGeoServerImplWithRequestCacheTest extends JDBCGeoServerImplTest {

    public JDBCGeoServerImplWithRequestCacheTest(JDBCConfigTestSupport.DBConfig dbConfig) {
        super(dbConfig);
    }

    @Override
    public void setUp() throws Exception {
        RequestCachingFacade.open();
        super.setUp();
    }

    @After
    public void closeCache() {
        RequestCachingFacade.close();
    }

    @Override
    protected GeoServerImpl createGeoServer() {
        GeoServerImpl gs = super.createGeoServer();
        gs.setFacade(RequestCachingFacade.create(gs.getFacade()));
        ((CatalogImpl) gs.getCatalog())
                .setFacade(RequestCachingFacade.create(new JDBCCatalogFacade(testSupport.getDatabase())));
        return gs;
    }
}
