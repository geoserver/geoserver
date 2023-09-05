/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.security.decorators.DecoratingFeatureSource;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.PropertyIsLike;
import org.geotools.appschema.filter.FilterFactoryImplNamespaceAware;
import org.geotools.data.complex.AttributeMapping;
import org.geotools.data.complex.DataAccessMappingFeatureIterator;
import org.geotools.data.complex.FeatureTypeMapping;
import org.geotools.data.complex.MappingFeatureSource;
import org.geotools.data.complex.config.AppSchemaDataAccessConfigurator;
import org.geotools.data.joining.JoiningNestedAttributeMapping;
import org.geotools.data.util.NullProgressListener;
import org.geotools.feature.FeatureIterator;
import org.geotools.jdbc.JDBCDataStore;
import org.junit.Before;
import org.junit.Test;

public class FeatureChainingSharedConnectionTest extends AbstractAppSchemaTestSupport {

    private int nestedFeaturesCount;

    private Transaction mfTransaction;

    private Transaction guTransaction;

    private JDBCDataStore mfSourceDataStore;

    private JDBCDataStore guSourceDataStore;

    @Override
    protected FeatureChainingMockData createTestData() {
        return new FeatureChainingMockData();
    }

    @Before
    public void setUp() {

        nestedFeaturesCount = 0;
        mfTransaction = null;
        guTransaction = null;
        mfSourceDataStore = null;
        guSourceDataStore = null;
    }

    /**
     * Tests that connection is automatically shared among top feature iterators and nested feature
     * iterators, but only in the context of a single AppSchemaDataAccess instance.
     *
     * <p>What this means in practice is:
     *
     * <ul>
     *   <li><em>MappedFeature</em> and <em>GeologicUnit</em> belong to different
     *       AppSchemaDataAccess instances, so an iterator on MappedFeature will open a new database
     *       connection to retrieve the nested GeologicUnit features
     *   <li><em>GeologicUnit, CompositionPart, ControlledConcept, CGI_TermValue</em> belong to the
     *       same AppSchemaDataAccess instances, so an iterator on GeologicUnit will NOT open a new
     *       database connection to retrieve the nested <em>CompositionPart, ControlledConcept,
     *       CGI_TermValue</em> "features"
     * </ul>
     */
    @Test
    public void testSharedConnection() throws Exception {
        MockConnectionLifecycleListener connListener = new MockConnectionLifecycleListener();

        FeatureTypeInfo mfTypeInfo = getCatalog().getFeatureTypeByName("gsml", "MappedFeature");
        assertNotNull(mfTypeInfo);
        FeatureTypeInfo guTypeInfo = getCatalog().getFeatureTypeByName("gsml", "GeologicUnit");
        assertNotNull(guTypeInfo);

        FeatureSource fs = mfTypeInfo.getFeatureSource(new NullProgressListener(), null);
        MappingFeatureSource mfFs = unwrap(fs);

        FeatureSource mfSourceFs = mfFs.getMapping().getSource();

        fs = guTypeInfo.getFeatureSource(new NullProgressListener(), null);
        MappingFeatureSource guFs = unwrap(fs);
        FeatureSource guSourceFs = guFs.getMapping().getSource();

        // The test only makes sense if we have a databae backend and joining is enabled
        assumeTrue(
                mfSourceFs.getDataStore() instanceof JDBCDataStore
                        && guSourceFs.getDataStore() instanceof JDBCDataStore
                        && AppSchemaDataAccessConfigurator.isJoining());

        mfSourceDataStore = (JDBCDataStore) mfSourceFs.getDataStore();
        guSourceDataStore = (JDBCDataStore) guSourceFs.getDataStore();

        // retrieve one feature to trigger all necessary initialization steps so they don't
        // interfere
        // with the test's outcome
        try (FeatureIterator fIt = mfFs.getFeatures().features()) {
            assertTrue(fIt.hasNext());
            assertNotNull(fIt.next());
        }

        // register connection listeners
        mfSourceDataStore.getConnectionLifecycleListeners().add(connListener);
        guSourceDataStore.getConnectionLifecycleListeners().add(connListener);

        FilterFactoryImplNamespaceAware ff = new FilterFactoryImplNamespaceAware();
        ff.setNamepaceContext(mfFs.getMapping().getNamespaces());

        PropertyIsLike like =
                ff.like(
                        ff.property("gsml:specification/gsml:GeologicUnit/gml:description"),
                        "*sedimentary*");

        try (DataAccessMappingFeatureIterator mappingIt =
                (DataAccessMappingFeatureIterator) mfFs.getFeatures(like).features()) {
            assertTrue(mappingIt.hasNext());

            // fetch one feature to trigger opening of nested iterators
            Feature f = mappingIt.next();
            assertNotNull(f);

            FeatureSource mappedSource = mappingIt.getMappedSource();
            assertSame(mappedSource.getDataStore(), mfSourceDataStore);
            assertNotNull(mappingIt.getTransaction());
            mfTransaction = mappingIt.getTransaction();

            testSharedConnectionRecursively(
                    mfFs.getMapping(), mappingIt, mfSourceDataStore, mfTransaction);
        }

        assertEquals(2, connListener.actionCountByDataStore.size());
        assertTrue(connListener.actionCountByDataStore.containsKey(mfSourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(mfSourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(mfSourceDataStore).releaseCount);
        assertTrue(connListener.actionCountByDataStore.containsKey(guSourceDataStore));
        assertEquals(1, connListener.actionCountByDataStore.get(guSourceDataStore).borrowCount);
        assertEquals(1, connListener.actionCountByDataStore.get(guSourceDataStore).releaseCount);
        assertEquals(8, nestedFeaturesCount);
    }

    @SuppressWarnings("unchecked")
    private MappingFeatureSource unwrap(FeatureSource fs) {
        MappingFeatureSource mfFs;
        if (fs instanceof DecoratingFeatureSource) {
            mfFs =
                    ((DecoratingFeatureSource<FeatureType, Feature>) fs)
                            .unwrap(MappingFeatureSource.class);
        } else {
            assertTrue(fs instanceof MappingFeatureSource);
            mfFs = (MappingFeatureSource) fs;
        }
        return mfFs;
    }

    @SuppressWarnings("PMD.CloseResource") // weird dance with transaction fields, leaving it alone
    private void testSharedConnectionRecursively(
            FeatureTypeMapping mapping,
            DataAccessMappingFeatureIterator mappingIt,
            DataAccess parentDataStore,
            Transaction parentTx)
            throws IOException {
        List<AttributeMapping> attrs = mapping.getAttributeMappings();
        assertNotNull(attrs);
        assertFalse(attrs.isEmpty());

        for (AttributeMapping attr : attrs) {
            if (attr instanceof JoiningNestedAttributeMapping) {
                nestedFeaturesCount++;

                JoiningNestedAttributeMapping joiningNestedAttr =
                        (JoiningNestedAttributeMapping) attr;
                Map<Name, DataAccessMappingFeatureIterator> nestedFeatureIterators =
                        joiningNestedAttr.getNestedFeatureIterators(mappingIt);
                assertNotNull(nestedFeatureIterators);

                if (!nestedFeatureIterators.isEmpty()) {
                    assertEquals(1, nestedFeatureIterators.size());

                    FeatureTypeMapping nestedMapping =
                            joiningNestedAttr.getFeatureTypeMapping(null);

                    DataAccessMappingFeatureIterator nestedIt =
                            nestedFeatureIterators.values().iterator().next();

                    Transaction expectedTx = parentTx;
                    DataAccess expectedDataStore = parentDataStore;
                    String nestedFeatureName = nestedMapping.getTargetFeature().getLocalName();
                    if (nestedFeatureName.equals("GeologicUnit")) {
                        expectedDataStore = guSourceDataStore;
                        if (guTransaction == null) {
                            guTransaction = nestedIt.getTransaction();
                        }
                        expectedTx = guTransaction;
                    } else if (nestedFeatureName.equals("MappedFeature")) {
                        expectedDataStore = mfSourceDataStore;
                        if (mfTransaction == null) {
                            mfTransaction = nestedIt.getTransaction();
                        }
                        expectedTx = mfTransaction;
                    }

                    FeatureSource nestedMappedSource = nestedIt.getMappedSource();
                    assertEquals(expectedDataStore, nestedMappedSource.getDataStore());
                    assertEquals(expectedTx, nestedIt.getTransaction());

                    testSharedConnectionRecursively(
                            nestedMapping, nestedIt, expectedDataStore, expectedTx);
                }
            }
        }
    }
}
