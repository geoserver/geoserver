/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.catalog;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.jdbcconfig.internal.ConfigDatabase;
import org.geoserver.jdbcconfig.internal.JdbcConfigTestSupport;

public class CatalogImplWithJDBCFacadeTest extends org.geoserver.catalog.impl.CatalogImplTest {

    private JDBCCatalogFacade facade;

    private JdbcConfigTestSupport testSupport;

    @Override
    protected void setUp() throws Exception {
        super.GET_LAYER_BY_ID_WITH_CONCURRENT_ADD_TEST_COUNT = 10;
        testSupport = new JdbcConfigTestSupport();
        testSupport.setUp();

        ConfigDatabase configDb = testSupport.getDatabase();
        facade = new JDBCCatalogFacade(configDb);

        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        facade.dispose();
//        testSupport.tearDown();
    }

    @Override
    protected Catalog createCatalog() {
        CatalogImpl catalogImpl = new CatalogImpl();
        catalogImpl.setFacade(facade);
        return catalogImpl;
    }

//    @Override
//    public void testGetLayerGroupByNameWithWorkspace() {
//        try {
//            super.testGetLayerGroupByNameWithWorkspace();
//        } catch (AssertionFailedError e) {
//            // ignoring failure, we need to fix this as we did for styles by workspace. Check the
//            // comment in the original test case:
//            // "//will randomly return one... we should probably return null with multiple matches"
//            e.printStackTrace();
//        }
//    }
}
