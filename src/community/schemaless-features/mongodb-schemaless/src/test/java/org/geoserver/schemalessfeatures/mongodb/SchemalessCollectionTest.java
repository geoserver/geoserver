/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.schemalessfeatures.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.util.Arrays;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.FeatureSource;
import org.geotools.data.Query;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

public class SchemalessCollectionTest extends AbstractMongoDBOnlineTestSupport {

    private static final String DATA_STORE_NAME = "stationsMongoWfs";

    private static MongoTestSetup testSetup;

    private static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    @Before
    @Override
    public void setUp() throws Exception {
        super.setUp();
        Catalog cat = getCatalog();
        DataStoreInfo storeInfo = cat.getDataStoreByName(DATA_STORE_NAME);
        if (storeInfo == null) {
            WorkspaceInfo wi = cat.getDefaultWorkspace();
            storeInfo = addMongoSchemalessStore(wi, DATA_STORE_NAME);
            addMongoSchemalessLayer(wi, storeInfo, StationsTestSetup.COLLECTION_NAME);
        }
    }

    @Override
    protected MongoTestSetup createTestSetups() {
        StationsTestSetup setup = new StationsTestSetup(databaseName);
        testSetup = setup;
        return setup;
    }

    @AfterClass
    public static void tearDown() {
        if (testSetup != null) testSetup.tearDown();
    }

    @Test
    public void testPropertyNameWithDotSeparator() throws Exception {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName("gs:" + StationsTestSetup.COLLECTION_NAME);
        @SuppressWarnings("unchecked")
        FeatureSource<FeatureType, Feature> source =
                (FeatureSource<FeatureType, Feature>) fti.getFeatureSource(null, null);
        Filter eq1 = FF.equals(FF.property("name"), FF.literal("station 2"));
        FeatureIterator<Feature> it = source.getFeatures(eq1).features();
        // use dot separators in the property name
        PropertyName pn = FF.property("measurements.values.value");
        while (it.hasNext()) {
            Feature f = it.next();
            Object result = pn.evaluate(f);
            if (result instanceof List) {
                List listRes = (List) result;
                assertEquals(5, listRes.size());
                assertTrue(listRes.containsAll(Arrays.asList(35, 25, 80, 1019, 1015)));
            }
        }
    }

    @Test
    public void testPropertyNameReturningNestedFeaturesList() throws Exception {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName("gs:" + StationsTestSetup.COLLECTION_NAME);
        @SuppressWarnings("unchecked")
        FeatureSource<FeatureType, Feature> source =
                (FeatureSource<FeatureType, Feature>) fti.getFeatureSource(null, null);
        Filter eq1 = FF.equals(FF.property("name"), FF.literal("station 2"));
        FeatureIterator<Feature> it = source.getFeatures(eq1).features();
        // will return the values nested features.
        PropertyName pn = FF.property("measurements.values");
        Feature f = it.next();
        Object result = pn.evaluate(f);
        if (result instanceof List) {
            List listRes = (List) result;
            assertEquals(5, listRes.size());
            ((List) result).forEach(e -> assertTrue(e instanceof Feature));
        }
    }

    @Test
    public void testPostFilterEvaluation() throws Exception {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName("gs:" + StationsTestSetup.COLLECTION_NAME);
        @SuppressWarnings("unchecked")
        FeatureSource<FeatureType, Feature> source =
                (FeatureSource<FeatureType, Feature>) fti.getFeatureSource(null, null);
        Expression filter = FF.function("filter", FF.literal("value < 40"));
        Expression stream =
                FF.function(
                        "stream", FF.property("measurements.values"), filter, FF.property("value"));
        Expression aggregate = FF.function("aggregate", stream, FF.literal("AVG"));
        Filter eq = FF.equals(aggregate, FF.literal(30));
        FeatureCollection<FeatureType, Feature> collection = source.getFeatures(eq);
        collection.size();

        FeatureIterator<Feature> it = collection.features();
        PropertyName pn = FF.property("measurements.values.value");
        while (it.hasNext()) {
            Feature f = it.next();
            Object result = pn.evaluate(f);
            List<Number> values = (List<Number>) result;
            assertAvg(values);
        }
    }

    @Test
    public void testWrongPropertyName() throws Exception {
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName("gs:" + StationsTestSetup.COLLECTION_NAME);
        @SuppressWarnings("unchecked")
        FeatureSource<FeatureType, Feature> source =
                (FeatureSource<FeatureType, Feature>) fti.getFeatureSource(null, null);
        FeatureCollection<FeatureType, Feature> collection = source.getFeatures(Query.ALL);

        FeatureIterator<Feature> it = collection.features();
        PropertyName pn = FF.property("some_wrong_heading.measurements.values.value");
        while (it.hasNext()) {
            Feature f = it.next();
            Object result = pn.evaluate(f);
            assertNull(result);
        }
    }

    private void assertAvg(List<Number> result) {
        int sum = 0;
        int denom = 0;
        for (Number n : result) {
            int integer = n.intValue();
            if (integer < 40) {
                sum += integer;
                denom++;
            }
        }
        assertEquals(30, sum / denom);
    }

    @Test
    public void testNotExistingPropUnderUnboundedFeatures() throws Exception {
        // test that when evaluating a property name traversing nested features
        // with cardinality > 1 and not existing or null property for all the nested features
        // the return value is null and not empty list.
        FeatureTypeInfo fti =
                getCatalog().getFeatureTypeByName("gs:" + StationsTestSetup.COLLECTION_NAME);
        @SuppressWarnings("unchecked")
        FeatureSource<FeatureType, Feature> source =
                (FeatureSource<FeatureType, Feature>) fti.getFeatureSource(null, null);
        FeatureCollection<FeatureType, Feature> collection =
                source.getFeatures(FF.equals(FF.property("name"), FF.literal("station 1")));

        FeatureIterator<Feature> it = collection.features();
        PropertyName pn = FF.property("measurements.notExisting");
        Feature f = it.next();
        assertNull(pn.evaluate(f));
    }
}
