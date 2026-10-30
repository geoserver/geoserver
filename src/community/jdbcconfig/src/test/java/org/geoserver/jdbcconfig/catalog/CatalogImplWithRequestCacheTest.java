/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.RequestCachingFacade;
import org.geoserver.jdbcconfig.JDBCConfigTestSupport;
import org.junit.After;

/** Runs the catalog tests on jdbcconfig through the request cache, with a request open for each whole test. */
public class CatalogImplWithRequestCacheTest extends CatalogImplWithJDBCFacadeTest {

    public CatalogImplWithRequestCacheTest(JDBCConfigTestSupport.DBConfig dbConfig) {
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
    protected Catalog createCatalog() {
        CatalogImpl catalog = new CatalogImpl();
        catalog.setFacade(RequestCachingFacade.create(new JDBCCatalogFacade(testSupport.getDatabase())));
        return catalog;
    }
}
