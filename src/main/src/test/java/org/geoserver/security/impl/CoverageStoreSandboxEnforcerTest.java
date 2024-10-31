/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This cod e is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.StoreInfo;

public class CoverageStoreSandboxEnforcerTest extends AbstractSandboxEnforcerTest {

    @Override
    protected void testLocation(StoreInfo store, File location) throws Exception {
        CoverageStoreInfo coverageStore = (CoverageStoreInfo) store;
        assertEquals(location.getAbsolutePath(), coverageStore.getURL());
    }

    @Override
    protected void addStore(String storeName, File location) {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        CoverageStoreInfo store = builder.buildCoverageStore(storeName);
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.setURL(location.getPath());
        catalog.add(store);
    }

    @Override
    protected void modifyStore(String storeName, File location) {
        Catalog catalog = getCatalog();
        CoverageStoreInfo store = catalog.getCoverageStoreByName(storeName);
        store.setURL(location.getPath());
        catalog.save(store);
    }
}
