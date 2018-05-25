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

import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataStore;
import org.geotools.feature.NameImpl;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

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
    public void testLookupNotExisting() throws IOException {
        CatalogRepository repository = getCatalog().getResourcePool().getRepository();
        DataStore store = repository.dataStore(new NameImpl("foo", "bar"));
        assertNull(store);
    }
}
