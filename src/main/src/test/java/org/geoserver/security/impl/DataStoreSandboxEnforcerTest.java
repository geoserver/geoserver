/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This cod e is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geotools.data.property.PropertyDataStoreFactory;

public class DataStoreSandboxEnforcerTest extends AbstractSandboxEnforcerTest {

    @Override
    protected void addStore(String storeName, File location) {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        DataStoreInfo store = builder.buildDataStore(storeName);
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.getConnectionParameters().put("directory", location);
        store.setType(new PropertyDataStoreFactory().getDisplayName());
        catalog.add(store);
    }

    @Override
    protected void modifyStore(String storeName, File location) {
        Catalog catalog = getCatalog();
        DataStoreInfo store = catalog.getDataStoreByName(storeName);
        store.getConnectionParameters().put("directory", location);
        catalog.save(store);
    }

    @Override
    protected void testLocation(StoreInfo citeStore, File location) {
        assertEquals(location, citeStore.getConnectionParameters().get("directory"));
    }
}
