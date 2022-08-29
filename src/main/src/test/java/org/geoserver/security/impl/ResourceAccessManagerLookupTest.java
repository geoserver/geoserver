/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertTrue;

import org.geoserver.catalog.Catalog;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.ResourceAccessManagerWrapper;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.beans.factory.support.DefaultListableBeanFactory;

public class ResourceAccessManagerLookupTest extends GeoServerSystemTestSupport {

    @Test
    public void testLookupOfDefaultAccessManagerSubclass() throws Exception {
        String name = "customManager";
        try {
            Catalog catalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
            DataAccessRuleDAO dao = (DataAccessRuleDAO) GeoServerExtensions.bean("accessRulesDao");
            TestLookupAccessManager testMan = new TestLookupAccessManager(dao, catalog);
            registerManagerBean(testMan, name);
            SecureCatalogImpl secureCatalog = new SecureCatalogImpl(catalog);
            ResourceAccessManager accessManager = secureCatalog.getResourceAccessManager();
            while (accessManager instanceof ResourceAccessManagerWrapper) {
                accessManager = ((ResourceAccessManagerWrapper) accessManager).unwrap();
            }
            assertTrue(accessManager instanceof TestLookupAccessManager);
        } finally {
            destroyManagerBean(name);
        }
    }

    @Test
    public void testDuplicateDefaultAccessManagerLookup() throws Exception {
        String name = "defaultAccessManager2";
        try {
            Catalog catalog = (Catalog) GeoServerExtensions.bean("rawCatalog");
            DataAccessRuleDAO dao = (DataAccessRuleDAO) GeoServerExtensions.bean("accessRulesDao");
            DefaultResourceAccessManager testMan = new DefaultResourceAccessManager(dao, catalog);
            registerManagerBean(testMan, name);

            SecureCatalogImpl secureCatalog = new SecureCatalogImpl(catalog);
            ResourceAccessManager accessManager = secureCatalog.getResourceAccessManager();
            while (accessManager instanceof ResourceAccessManagerWrapper)
                accessManager = ((ResourceAccessManagerWrapper) accessManager).unwrap();

            assertTrue(accessManager instanceof DefaultResourceAccessManager);
        } finally {
            destroyManagerBean(name);
        }
    }

    private void registerManagerBean(ResourceAccessManager manager, String name) {
        ConfigurableBeanFactory factory = applicationContext.getBeanFactory();
        factory.registerSingleton(name, manager);
    }

    private void destroyManagerBean(String name) {
        DefaultListableBeanFactory factory =
                (DefaultListableBeanFactory) applicationContext.getBeanFactory();
        factory.destroySingleton(name);
    }

    // extends the DefaultAccessManager to test lookup
    static class TestLookupAccessManager extends DefaultResourceAccessManager {

        public TestLookupAccessManager(DataAccessRuleDAO dao, Catalog rawCatalog) {
            super(dao, rawCatalog);
        }
    }
}
