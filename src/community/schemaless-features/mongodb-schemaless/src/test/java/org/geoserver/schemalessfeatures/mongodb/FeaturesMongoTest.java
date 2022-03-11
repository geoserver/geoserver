package org.geoserver.schemalessfeatures.mongodb;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.List;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.schemalessfeatures.data.ComplexFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureIterator;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;

public class FeaturesMongoTest extends AbstractMongoDBOnlineTestSupport {

    private static final String DATA_STORE_NAME = "stationsFeatures";

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

    @Test
    public void simplifiedPropertyNameToNestedFeatureNode() throws IOException {
        FeatureTypeInfo fti =
                getCatalog()
                        .getFeatureTypeByName(
                                getCatalog().getDefaultNamespace().getURI(),
                                StationsTestSetup.COLLECTION_NAME);
        Filter filter = FF.equals(FF.property("name"), FF.literal("station 2"));
        ComplexFeatureCollection fcol =
                (ComplexFeatureCollection) fti.getFeatureSource(null, null).getFeatures(filter);

        try (FeatureIterator<Feature> it = fcol.features(); ) {
            while (it.hasNext()) {
                Feature f = it.next();
                PropertyName propertyName = FF.property("measurements.values");
                Object o = propertyName.evaluate(f);
                List<Object> attributes = (List<Object>) o;
                assertEquals(5, attributes.size());
                for (Object nested : attributes) {
                    assertTrue(nested instanceof Feature);
                }
            }
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
}
