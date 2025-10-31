/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import static java.util.Map.entry;
import static org.geoserver.test.AbstractAppSchemaMockData.GSML_PREFIX;
import static org.geoserver.test.AbstractAppSchemaMockData.GSML_URI;
import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.util.Map;
import java.util.Properties;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogRepository;
import org.geoserver.catalog.DataStoreInfo;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.data.complex.AppSchemaDataAccessFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.junit.Test;
import org.vfny.geoserver.util.DataStoreUtils;

public class PostgisDelegateStoreTest extends GeoServerSystemTestSupport {

    private static final String DELEGATE_STORE_NAME = "delegateStore";
    private DelegateStoreMockData mockData;

    @Override
    protected FeatureChainingMockData createTestData() {
        mockData = new DelegateStoreMockData();
        return mockData;
    }

    @Test
    public void testDelegateStoreQuery() throws IOException {
        assumeTrue("PostGIS fixture not available", mockData.isPostgisOnlineTest());
        Catalog catalog = getCatalog();

        DataStoreInfo dataStore = catalog.getFactory().createDataStore();
        configureDatastore(dataStore);
        catalog.add(dataStore);

        AppSchemaDataAccessFactory appSchemaDataAccessFactory = new AppSchemaDataAccessFactory();
        CatalogRepository repository = catalog.getResourcePool().getRepository();
        Map<String, Serializable> params = createAppSchemaProperties(repository);
        DataAccess dataAccess = DataStoreUtils.getDataAccess(appSchemaDataAccessFactory, params);

        FeatureSource mappedFeature = dataAccess.getFeatureSource(new NameImpl(GSML_URI, "MappedFeature"));

        FeatureCollection features = mappedFeature.getFeatures(Query.ALL);
        // Check that store can run queries against DB
        assertEquals(5, features.size());
    }

    private Map<String, Serializable> createAppSchemaProperties(CatalogRepository repository)
            throws MalformedURLException {
        Map<String, Serializable> params = Map.ofEntries(
                entry("dbtype", "app-schema"),
                entry("delegateStoreName", DELEGATE_STORE_NAME),
                entry("repository", repository),
                entry(
                        "url",
                        new File(
                                        new File(
                                                mockData.featureTypesBaseDir,
                                                mockData.getDataStoreName(GSML_PREFIX, "MappedFeature")),
                                        "MappedFeaturePropertyfile.xml")
                                .toURI()
                                .toURL()));
        return params;
    }

    private void configureDatastore(DataStoreInfo dataStore) {
        dataStore.setName(DELEGATE_STORE_NAME);
        Properties fixture = mockData.fixture;
        dataStore.getConnectionParameters().put("dbtype", fixture.getProperty("dbtype"));
        dataStore.getConnectionParameters().put("host", fixture.getProperty("host"));
        dataStore.getConnectionParameters().put("port", Integer.valueOf(fixture.getProperty("port")));
        dataStore.getConnectionParameters().put("database", fixture.getProperty("database"));
        dataStore.getConnectionParameters().put("user", fixture.getProperty("user"));
        dataStore.getConnectionParameters().put("passwd", fixture.getProperty("passwd"));
        dataStore.getConnectionParameters().put("schema", "appschematest");
    }
}
