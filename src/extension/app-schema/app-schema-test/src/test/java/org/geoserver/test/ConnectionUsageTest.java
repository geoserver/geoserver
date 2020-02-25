/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.Transaction;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.MappingFeatureCollection;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.joining.JoiningNestedAttributeMapping;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.PropertyIsEqualTo;

public class ConnectionUsageTest extends AbstractAppSchemaTestSupport {

    private FilterFactoryImplNamespaceAware ff;

    private MockConnectionLifecycleListener connListener;

    private MappingFeatureSource mappingFs;

    private Transaction transaction;

    private JDBCDataStore sourceDataStore;

    private int nestedFeaturesCount;

    @Override
    protected ConnectionUsageMockData createTestData() {
        return new ConnectionUsageMockData();
    }

    @Before
    public void setUp() throws Exception {
        nestedFeaturesCount = 0;

        init();
    }

    @Test
    public void testConnectionSharedAmongNestedIterators() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        try (FeatureIterator fIt = mappingFs.getFeatures(equals).features()) {
            testNestedIterators(fIt);
        }

        assertEquals(1, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
        assertEquals(2, nestedFeaturesCount);
    }

    @Test
    public void testConnectionSharedIfTransactionIs() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        FeatureCollection fc = mappingFs.getFeatures(equals);
        assertTrue(fc instanceof MappingFeatureCollection);
        MappingFeatureCollection mfc = (MappingFeatureCollection) fc;

        try (Transaction tx = new DefaultTransaction()) {

            // explicitly specifying the transaction to use
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(2, nestedFeaturesCount);

            // open another iterator using the same transaction
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            // no new connection has been opened
            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(4, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    @Test
    public void testConnectionNotSharedIfTransactionIsNot() throws Exception {
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("C_nested_second"));

        FeatureCollection fc = mappingFs.getFeatures(equals);
        assertTrue(fc instanceof MappingFeatureCollection);
        MappingFeatureCollection mfc = (MappingFeatureCollection) fc;

        try (Transaction tx = new DefaultTransaction()) {

            // explicitly specifying the transaction to use
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(0, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(2, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);

        try (Transaction tx = new DefaultTransaction()) {
            // open another iterator using a different transaction
            try (FeatureIterator fIt = mfc.features(tx)) {
                testNestedIterators(fIt);
            }

            // a new connection has been opened
            assertEquals(1, connListener.actionCountByDataStore.size());
            assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
            assertEquals(2, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
            assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
            assertEquals(4, nestedFeaturesCount);
        }

        // at this point transaction has been closed and so the connection has been released
        assertEquals(2, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    /**
     * This test uses a conditionally joined feature with a broken mapping configuration to trigger
     * a RuntimeException when iterator.next() is called and verifies that no connection leak
     * occurs, even if the caller forgets to catch unchecked exceptions.
     */
    @Test
    @SuppressWarnings("TryFailThrowable")
    public void testNoConnectionLeakIfExceptionThrown() throws Exception {
        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mappingFs.getMapping().getNamespaces());

        // this filter selects the feature with GML ID "scp.1", the only one which joins the broken
        // feature type ex:ConnectionUsageThirdNested
        PropertyIsEqualTo equals =
                ff.equals(
                        ff.property(
                                "ex:nestedFeature/ex:ConnectionUsageFirstNested/ex:nestedFeature/ex:ConnectionUsageSecondNested/gml:name"),
                        ff.literal("A_nested_second"));

        FeatureIterator fIt = mappingFs.getFeatures(equals).features();
        try {
            testNestedIterators(fIt);
            fail("Expected exception was not thrown!");
        } catch (Throwable e) {
            // This was expected
        }

        // iterator should have been automatically closed, so no connection should be leaked
        assertEquals(1, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(sourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(sourceDataStore).releaseCount);
    }

    private void init() throws Exception {
        connListener = new MockConnectionLifecycleListener();

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName("ex", "ConnectionUsageParent");
        assertNotNull(typeInfo);

        FeatureSource fs = typeInfo.getFeatureSource(new NullProgressListener(), null);
        if (fs instanceof DecoratingFeatureSource) {
            mappingFs =
                    ((DecoratingFeatureSource<FeatureType, Feature>) fs)
                            .unwrap(MappingFeatureSource.class);
        } else {
            assertTrue(fs instanceof MappingFeatureSource);
            mappingFs = (MappingFeatureSource) fs;
        }

        FeatureSource sourceFs = mappingFs.getMapping().getSource();

        // The test only makes sense if we have a databae backend and joining is enabled
        assumeTrue(
                sourceFs.getDataStore() instanceof JDBCDataStore
                        && AppSchemaDataAccessConfigurator.isJoining());

        sourceDataStore = (JDBCDataStore) sourceFs.getDataStore();

        ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mappingFs.getMapping().getNamespaces());

        // retrieve one feature to trigger all necessary initialization steps so they don't
        // interfere
        // with the test's outcome
        PropertyIsEqualTo equals = ff.equals(ff.property("gml:name"), ff.literal("C_parent"));

        try (FeatureIterator fIt = mappingFs.getFeatures(equals).features()) {
            assertTrue(fIt.hasNext());
            assertNotNull(fIt.next());
        }

        // register connection listener
        sourceDataStore.getConnectionLifecycleListeners().add(connListener);
    }

    private void testNestedIterators(FeatureIterator iterator) throws IOException {
        assertTrue(iterator instanceof DataAccessMappingFeatureIterator);
        DataAccessMappingFeatureIterator mappingIt = (DataAccessMappingFeatureIterator) iterator;
        assertTrue(iterator.hasNext());

        // fetch one feature to trigger opening of nested iterators
        Feature f = iterator.next();
        assertNotNull(f);

        FeatureSource mappedSource = mappingIt.getMappedSource();
        assertTrue(mappedSource.getDataStore() == sourceDataStore);
        assertNotNull(mappingIt.getTransaction());
        transaction = mappingIt.getTransaction();

        testNestedIteratorsRecursively(mappingFs.getMapping(), mappingIt);
    }

    private void testNestedIteratorsRecursively(
            FeatureTypeMapping mapping, DataAccessMappingFeatureIterator mappingIt)
            throws IOException {
        List<AttributeMapping> attrs = mapping.getAttributeMappings();
        assertTrue(attrs != null);
        assertTrue(attrs.size() > 0);

        for (AttributeMapping attr : attrs) {
            if (attr instanceof JoiningNestedAttributeMapping) {
                nestedFeaturesCount++;

                JoiningNestedAttributeMapping joiningNestedAttr =
                        (JoiningNestedAttributeMapping) attr;
                Map<Name, DataAccessMappingFeatureIterator> nestedFeatureIterators =
                        joiningNestedAttr.getNestedFeatureIterators(mappingIt);
                assertNotNull(nestedFeatureIterators);

                if (nestedFeatureIterators.size() > 0) {
                    assertEquals(1, nestedFeatureIterators.size());

                    FeatureTypeMapping nestedMapping =
                            joiningNestedAttr.getFeatureTypeMapping(null);

                    DataAccessMappingFeatureIterator nestedIt =
                            nestedFeatureIterators.values().iterator().next();

                    FeatureSource nestedMappedSource = nestedIt.getMappedSource();
                    assertEquals(sourceDataStore, nestedMappedSource.getDataStore());
                    assertEquals(transaction, nestedIt.getTransaction());

                    testNestedIteratorsRecursively(nestedMapping, nestedIt);
                }
            }
        }
    }
}
