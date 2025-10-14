/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.changeset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.startsWith;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Properties;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.geoserver.data.test.SystemTestData;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.DataStoreFinder;
import org.geotools.data.DataUtilities;
import org.junit.AfterClass;

/** Tests the changeset store with a custom database */
public class ChangesetCustomDBTest extends ChangesetTest {

    public static final String CHANGESET_STORE_PROPERTIES = "changeset-store.properties";
    static File DATA_DIRECTORY;

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        super.setUpTestData(testData);

        // cleanup and setup database
        File file = new File("./target/changeset-custom-db");
        if (file.exists()) {
            FileUtils.deleteQuietly(file);
        }
        file.mkdir();

        // copy over the config file
        DATA_DIRECTORY = testData.getDataDirectoryRoot();
        File configFile = new File(DATA_DIRECTORY, CHANGESET_STORE_PROPERTIES);
        try (InputStream is = ChangesetCustomDBTest.class.getResourceAsStream(CHANGESET_STORE_PROPERTIES);
                OutputStream os = new FileOutputStream(configFile)) {
            IOUtils.copy(is, os);
        }
    }

    @AfterClass
    public static void checkDatabaseUsage() throws Exception {
        assertThat(DATA_DIRECTORY.listFiles().length, greaterThan(1));

        Properties props = new Properties();
        try (InputStream is = ChangesetCustomDBTest.class.getResourceAsStream(CHANGESET_STORE_PROPERTIES)) {
            props.load(is);
        }
        DataStore datastore = DataStoreFinder.getDataStore(DataUtilities.toConnectionParameters(props));
        try {
            assertThat(datastore, notNullValue());
            // if the cleanup has worked correctly, we expect only one residual table
            assertThat(datastore.getTypeNames().length, equalTo(1));
            assertThat(datastore.getTypeNames()[0], startsWith("CoverageInfoImpl-"));
        } finally {
            datastore.dispose();
        }
    }
}
