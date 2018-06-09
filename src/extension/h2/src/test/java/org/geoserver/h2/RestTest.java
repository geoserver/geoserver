/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.h2;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipOutputStream;
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
import org.geotools.data.Query;
import org.geotools.feature.NameImpl;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.feature.type.Name;
import org.springframework.mock.web.MockHttpServletResponse;

/** Contains tests that invoke REST resources that will use H2 data store. */
public final class RestTest extends GeoServerSystemTestSupport {

    private static final String WORKSPACE_NAME = "h2-tests";
    private static final String WORKSPACE_URI = "http://h2-tests.org";

    private static File ROOT_DIRECTORY;
    private static File DATABASE_DIR;

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
        // create root tests directory
        ROOT_DIRECTORY = IOUtils.createTempDirectory("h2-tests");
        // create the test database
        DATABASE_DIR = new File(ROOT_DIRECTORY, "testdb");
        DATABASE_DIR.mkdirs();
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
    public void createDataStoreUsingRestSingleFile() throws Exception {
        String dataStoreName = "h2-test-db-single";
        // send only the database file
        byte[] content = readSqLiteDatabaseFile();
        genericCreateDataStoreUsingRestTest(dataStoreName, "application/octet-stream", content);
    }

    @Test
    public void createDataStoreUsingRestZipFile() throws Exception {
        String dataStoreName = "h2-test-db-zip";
        // send the database directory
        byte[] content = readSqLiteDatabaseDir();
        genericCreateDataStoreUsingRestTest(dataStoreName, "application/zip", content);
    }

    public void genericCreateDataStoreUsingRestTest(
            String dataStoreName, String mimeType, byte[] content) throws Exception {
        // perform a PUT request, a new H2 data store should be created
        // we also require that all available feature types should be created
        String path =
                String.format(
                        "/rest/workspaces/%s/datastores/%s/file.h2?configure=all",
                        WORKSPACE_NAME, dataStoreName);
        MockHttpServletResponse response = putAsServletResponse(path, content, mimeType);
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

    /**
     * Helper method that just reads the test H2 database file and stores it in a array of bytes.
     */
    private static byte[] readSqLiteDatabaseFile() throws Exception {
        // open the database file
        InputStream input = RestTest.class.getResourceAsStream("/test-database.data.db");
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

    /** Helper method that zips the H2 data directory and returns it as an array of bytes. */
    private static byte[] readSqLiteDatabaseDir() throws Exception {
        // copy database file to database directory
        File outputFile = new File(DATABASE_DIR, "test-database.data.db");
        InputStream input = RestTest.class.getResourceAsStream("/test-database.data.db");
        IOUtils.copy(input, new FileOutputStream(outputFile));
        // zip the database directory
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ZipOutputStream zip = new ZipOutputStream(output);
        // ignore the lock files
        IOUtils.zipDirectory(
                DATABASE_DIR, zip, (dir, name) -> !name.toLowerCase().contains("lock"));
        zip.close();
        // just return the output stream content
        return output.toByteArray();
    }
}
