package org.geoserver.data.versioning;

import org.geogit.test.RepositoryTestCase;
import org.geoserver.data.versioning.decorator.DataStoreDecorator;
import org.geotools.data.DataStore;
import org.geotools.data.simple.SimpleFeatureStore;

/**
 * Base class for versioning tests.
 * <p>
 * {@link #setUpInternal()} leaves {@link #unversionedStore} with two features,
 * {@link RepositoryTestCase#lines1} and {@link RepositoryTestCase#lines2}
 * 
 * @author groldan
 * 
 */
public abstract class VersioningTestSupport extends RepositoryTestCase {

    protected DataStore unversionedStore;

    protected DataStoreDecorator versioningStore;

    protected SimpleFeatureStore lines, points;

    @SuppressWarnings({ "rawtypes" })
    @Override
    protected void setUpInternal() throws Exception {
        unversionedStore = new SimpleMemoryDataAccess();
        versioningStore = new DataStoreDecorator(unversionedStore, super.repo);

        versioningStore.createSchema(linesType);
        versioningStore.createSchema(pointsType);

        lines = (SimpleFeatureStore) versioningStore.getFeatureSource(linesTypeName);
        points = (SimpleFeatureStore) versioningStore.getFeatureSource(pointsTypeName);

    }
}
