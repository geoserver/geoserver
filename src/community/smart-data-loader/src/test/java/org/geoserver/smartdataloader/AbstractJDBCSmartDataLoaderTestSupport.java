package org.geoserver.smartdataloader;

import static org.junit.Assume.assumeNotNull;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.DatabaseMetaData;
import java.util.Properties;
import javax.sql.DataSource;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataFactory;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadataConfig;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.jdbc.JDBCTestSetup;
import org.geotools.test.FixtureUtilities;
import org.junit.Before;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;

public abstract class AbstractJDBCSmartDataLoaderTestSupport extends GeoServerSystemTestSupport {

    public static final String ONLINE_DB_SCHEMA = "smartappschematest";
    public String NAMESPACE_PREFIX = "mt";
    public String TARGET_NAMESPACE = "http://www.geo-solutions.it/smartappschema/1.0";

    protected JDBCFixtureHelper fixtureHelper;

    protected Properties fixture;
    protected DataSource dataSource;

    public AbstractJDBCSmartDataLoaderTestSupport(JDBCFixtureHelper fixtureHelper) {
        this.fixtureHelper = fixtureHelper;
    }

    @Before
    public void setUp() throws Exception {
        this.fixture = getFixture();
        assumeNotNull(fixture);
        JDBCTestSetup testSetup = createTestSetup();
        testSetup.setFixture(fixture);
        testSetup.setUp();
        this.dataSource = testSetup.getDataSource();
    }

    protected Properties getFixture() {
        File fixtureFile =
                FixtureUtilities.getFixtureFile(
                        getFixtureDirectory(), fixtureHelper.getFixtureId());
        if (fixtureFile.exists()) {
            return FixtureUtilities.loadProperties(fixtureFile);
        } else {
            Properties exampleFixture = fixtureHelper.createExampleFixture();
            if (exampleFixture != null) {
                File exFixtureFile = new File(fixtureFile.getAbsolutePath() + ".example");
                if (!exFixtureFile.exists()) {
                    createExampleFixture(exFixtureFile, exampleFixture);
                }
            }
            FixtureUtilities.printSkipNotice(fixtureHelper.getFixtureId(), fixtureFile);
            return null;
        }
    }

    private void createExampleFixture(File exFixtureFile, Properties exampleFixture) {
        FileOutputStream fout = null;
        try {
            exFixtureFile.getParentFile().mkdirs();
            exFixtureFile.createNewFile();

            fout = new FileOutputStream(exFixtureFile);

            exampleFixture.store(
                    fout,
                    "This is an example fixture. Update the "
                            + "values and remove the .example suffix to enable the test");
            fout.flush();
            fout.close();
        } catch (IOException ioe) {
            throw new RuntimeException(ioe);
        } finally {
            if (fout != null) {
                try {
                    fout.close();
                } catch (IOException e) {
                }
            }
        }
    }

    File getFixtureDirectory() {
        return new File(System.getProperty("user.home") + File.separator + ".geoserver");
    }

    protected abstract JDBCTestSetup createTestSetup();

    protected DataStoreMetadata getDataStoreMetadata(DatabaseMetaData metaData) throws Exception {
        DataStoreMetadataConfig config =
                new JdbcDataStoreMetadataConfig(
                        ONLINE_DB_SCHEMA, metaData.getConnection(), null, ONLINE_DB_SCHEMA);
        DataStoreMetadata dsm = (new DataStoreMetadataFactory()).getDataStoreMetadata(config);
        return dsm;
    }

    /**
     * Helper method that allows to remove sourceDataStore node from AppSchema xml doc. Useful to
     * clean xml docs when required to compare assertXML (since those sections of documents contains
     * specific information from dataStores based on JDBC Connection, it's required to avoid the
     * comparision.
     *
     * @param appSchemaDoc
     */
    protected void removeSourceDataStoresNode(Document appSchemaDoc) {
        NodeList sds = appSchemaDoc.getElementsByTagName("sourceDataStores");
        if (sds != null && sds.getLength() > 0) {
            sds.item(0).getParentNode().removeChild(sds.item(0));
        }
    }
}
