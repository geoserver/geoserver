/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

import static junit.framework.TestCase.assertNotNull;
import static org.geoserver.data.test.MockData.PRIMITIVEGEOFEATURE;
import static org.geoserver.data.test.MockData.SF_PREFIX;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataStore;
import org.geotools.feature.NameImpl;
import org.geotools.util.URLs;
import org.junit.Test;

public class CatalogRepositoryTest extends GeoServerSystemTestSupport {

    @Test
    public void testLookupExistingStore() throws IOException {
        CatalogRepository repository = getCatalog().getResourcePool().getRepository();
        DataStore store = repository.dataStore(new NameImpl(SF_PREFIX, SF_PREFIX));
        assertNotNull(store);
        List<String> typeNames = Arrays.asList(store.getTypeNames());
        assertTrue(typeNames.contains(PRIMITIVEGEOFEATURE.getLocalPart()));
    }

    @Test
    public void testLookupNotQualified() throws IOException {
        CatalogRepository repository = getCatalog().getResourcePool().getRepository();
        DataStore store = repository.dataStore(new NameImpl(null, SF_PREFIX));
        assertNotNull(store);
        List<String> typeNames = Arrays.asList(store.getTypeNames());
        assertTrue(typeNames.contains(PRIMITIVEGEOFEATURE.getLocalPart()));
    }

    @Test
    public void testLookupNotExisting() {
        CatalogRepository repository = getCatalog().getResourcePool().getRepository();
        DataStore store = repository.dataStore(new NameImpl("foo", "bar"));
        assertNull(store);
    }

    @Test
    public void testLookupFreshlyAdded() {
        Catalog catalog = getCatalog();
        CatalogBuilder cb = new CatalogBuilder(getCatalog());
        String nsURI = catalog.getDefaultNamespace().getURI();
        URL buildings = MockData.class.getResource("Buildings.properties");
        File testData = URLs.urlToFile(buildings).getParentFile();
        DataStoreInfo storeInfo = cb.buildDataStore("freshOffTheBoat");
        storeInfo.getConnectionParameters().put("directory", testData);
        storeInfo.getConnectionParameters().put("namespace", nsURI);
        catalog.save(storeInfo);

        CatalogRepository repository = getCatalog().getResourcePool().getRepository();
        DataStore store = repository.dataStore(new NameImpl("freshOffTheBoat"));
        assertNotNull(store);
    }
}
