/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.fail;

import java.io.File;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.decorators.ReadOnlyDataStoreTest;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.test.SystemTest;
import org.geoserver.util.IOUtils;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.feature.FeatureCollection;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.opengis.filter.Filter;

/**
 * Tests that security rules are applied in a real simulation of a GeoServer startup (loading the
 * Spring context, reading the catalog and whatnot)
 *
 * @author Andrea Aime - GeoSolutions
 */
@Category(SystemTest.class)
public class SecureCatalogIntegrationTest extends GeoServerSystemTestSupport {

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        File security = new File(testData.getDataDirectoryRoot(), "security");
        File layers = new File(security, "layers.properties");
        IOUtils.copy(
                SecureCatalogIntegrationTest.class.getResourceAsStream("functional.properties"),
                layers);
    }

    @Test
    public void testFullAccess() throws Exception {
        FeatureSource source = getFeatureSource(MockData.LINES);
        FeatureCollection fc = source.getFeatures();
        FeatureStore store = (FeatureStore) source;
        store.removeFeatures(Filter.INCLUDE);
    }

    @Test
    public void testCannotRead() throws Exception {
        try {
            getFeatureSource(MockData.BUILDINGS);
            fail("This should have failed with a security exception!");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }

    @Test
    public void testCannotWrite() throws Exception {
        FeatureStore fs = (FeatureStore) getFeatureSource(MockData.DELETES);

        try {
            fs.removeFeatures(Filter.INCLUDE);
            fail("This should have failed with a security exception!");
        } catch (Exception e) {
            if (ReadOnlyDataStoreTest.isSpringSecurityException(e) == false)
                fail("Should have failed with a security exception");
        }
    }
}
