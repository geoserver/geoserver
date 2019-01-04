/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.spatialite;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;
import org.apache.commons.io.output.ByteArrayOutputStream;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.impl.NamespaceInfoImpl;
import org.geoserver.catalog.impl.WorkspaceInfoImpl;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geoserver.util.IOUtils;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.spatialite.SpatiaLiteDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.Name;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains tests that invoke REST resources that will use SpatiaLite data store. */
public final class RestTest extends GeoServerSystemTestSupport {

    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger(RestTest.class);

    private static final String SQLITE_MIME_TYPE = "application/x-sqlite3";

    private static final String WORKSPACE_NAME = "spatialite-tests";
    private static final String WORKSPACE_URI = "http://spatialite-tests.org";

    private static File ROOT_DIRECTORY;
    private static File DATABASE_FILE;

    private static byte[] DATABASE_FILE_AS_BYTES;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // create a test workspace
        WorkspaceInfoImpl workspace = new WorkspaceInfoImpl();
        workspace.setName(WORKSPACE_NAME);
        getCatalog().add(workspace);
        // create test workspace namespace
        NamespaceInfoImpl nameSpace = new NamespaceInfoImpl();
        nameSpace.setPrefix(WORKSPACE_NAME);
        nameSpace.setURI(WORKSPACE_URI);
        getCatalog().add(nameSpace);
    }

    @BeforeClass
    public static void setUp() throws Throwable {
        // make sure that SpatiaLite database are supported
        boolean available = new SpatiaLiteDataStoreFactory().isAvailable();
        if (!available) {
            // SpatiaLite requires some native libraries to be installed
            LOGGER.warning(
                    "SpatialLite data store is not available REST tests will not run, "
                            + "this is probably due to missing native libraries.");
        }
        Assume.assumeTrue(available);
        // create root tests directory
        ROOT_DIRECTORY = IOUtils.createTempDirectory("spatialite-tests");
        // create the test database
        DATABASE_FILE = new File(ROOT_DIRECTORY, UUID.randomUUID().toString() + ".db");
        createTestDatabase();
        // read the test SpatiaLite database file
        DATABASE_FILE_AS_BYTES = readSqLiteDatabaseFile();
    }

    @AfterClass
    public static void tearDown() throws Throwable {
        // check if the root directory was initiated (test may have been skipped)
        if (ROOT_DIRECTORY != null) {
            // remove root tests directory
            IOUtils.delete(ROOT_DIRECTORY);
        }
    }

    @Before
    public void login() {
        // make sure we perform all requests logged as admin
        login("admin", "geoserver", "ROLE_ADMINISTRATOR");
    }

    @Test
    public void createDataStoreUsingRest() throws Exception {
        String dataStoreName = UUID.randomUUID().toString();
        // perform a PUT request, a new SpatiaLite data store should be created
        // we require that all available feature types should be created
        String path =
                String.format(
                        "/rest/workspaces/%s/datastores/%s/file.spatialite?configure=all",
                        WORKSPACE_NAME, dataStoreName);
        MockHttpServletResponse response =
                putAsServletResponse(path, DATABASE_FILE_AS_BYTES, SQLITE_MIME_TYPE);
        // we should get a HTTP 201 status code meaning that the data store was created
        assertThat(response.getStatus(), is(201));
        // let's see if the data store was correctly created
        DataStoreInfo storeInfo = getCatalog().getDataStoreByName(dataStoreName);
        assertThat(storeInfo, notNullValue());
        DataAccess store = storeInfo.getDataStore(null);
        assertThat(store, notNullValue());
        List<Name> names = store.getNames();
        assertThat(store, notNullValue());
        // check that at least the table points is available
        Name found =
                names.stream()
                        .filter(name -> name != null && name.getLocalPart().equals("points"))
                        .findFirst()
                        .orElse(null);
        assertThat(found, notNullValue());
        // check that the points layer was correctly created
        LayerInfo layerInfo = getCatalog().getLayerByName(new NameImpl(WORKSPACE_URI, "points"));
        assertThat(layerInfo, notNullValue());
        assertThat(layerInfo.getResource(), notNullValue());
        assertThat(layerInfo.getResource(), instanceOf(FeatureTypeInfo.class));
        // check that we have the expected features
        FeatureTypeInfo featureTypeInfo = (FeatureTypeInfo) layerInfo.getResource();
        int count = featureTypeInfo.getFeatureSource(null, null).getCount(Query.ALL);
        assertThat(count, is(4));
    }

    /** Helper method that just creates the test data store using GeoTools APIs. */
    private static void createTestDatabase() throws Exception {
        // connect to the test data store
        Map<String, String> params = new HashMap<>();
        params.put("dbtype", "spatialite");
        params.put("database", DATABASE_FILE.getAbsolutePath());
        DataStore datastore = DataStoreFinder.getDataStore(params);
        // create the points table (feature type)
        SimpleFeatureType featureType =
                DataUtilities.createType(
                        "points", "id:Integer,name:String,geometry:Point:srid=4326");
        datastore.createSchema(featureType);
        // get write access to the data store
        SimpleFeatureSource featureSource = datastore.getFeatureSource("points");
        if (!(featureSource instanceof SimpleFeatureStore)) {
            throw new RuntimeException("SpatiaLite data store doesn't support write access.");
        }
        SimpleFeatureStore featureStore = (SimpleFeatureStore) featureSource;
        Transaction transaction = new DefaultTransaction("create");
        featureStore.setTransaction(transaction);
        // create some features
        SimpleFeatureCollection features =
                new ListFeatureCollection(
                        featureType,
                        new SimpleFeature[] {
                            DataUtilities.createFeature(featureType, "1|point_a|POINT(-1,1)"),
                            DataUtilities.createFeature(featureType, "2|point_b|POINT(-1,-1)"),
                            DataUtilities.createFeature(featureType, "3|point_c|POINT(1,-1)"),
                            DataUtilities.createFeature(featureType, "4|point_d|POINT(1,1)"),
                        });
        try {
            // insert the features
            featureStore.addFeatures(features);
            transaction.commit();
        } finally {
            transaction.close();
        }
    }

    /**
     * Helper method that just reads the test SpatiaLite database file and stores it in a array of
     * bytes.
     */
    private static byte[] readSqLiteDatabaseFile() throws Exception {
        // open the database file
        InputStream input = new FileInputStream(DATABASE_FILE);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            // copy the input stream to the output stream
            IOUtils.copy(input, output);
        } catch (Exception exception) {
            throw new RuntimeException(
                    "Error reading SQLite database file to byte array.", exception);
        }
        return output.toByteArray();
    }
}
