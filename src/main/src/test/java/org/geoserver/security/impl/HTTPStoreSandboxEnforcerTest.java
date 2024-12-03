/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This cod e is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.HTTPStoreInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geotools.util.URLs;

public class HTTPStoreSandboxEnforcerTest extends AbstractSandboxEnforcerTest {

    @Override
    protected void testLocation(StoreInfo store, File location) throws Exception {
        HTTPStoreInfo http = (HTTPStoreInfo) store;
        assertEquals(location, URLs.urlToFile(new URL(http.getCapabilitiesURL())));
    }

    @Override
    protected void addStore(String storeName, File location) throws IOException {
        Catalog catalog = getCatalog();
        CatalogBuilder builder = new CatalogBuilder(catalog);
        WMSStoreInfo store = builder.buildWMSStore(storeName);
        store.setWorkspace(catalog.getDefaultWorkspace());
        store.setCapabilitiesURL(URLs.fileToUrl(location).toExternalForm());
        catalog.add(store);
    }

    @Override
    protected void modifyStore(String storeName, File location) {
        Catalog catalog = getCatalog();
        HTTPStoreInfo store = catalog.getWMSStoreByName(storeName);
        store.setCapabilitiesURL(URLs.fileToUrl(location).toExternalForm());
        catalog.save(store);
    }
}
