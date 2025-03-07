package org.geoserver.smartdataloader.data.store;

import static org.geotools.data.postgis.PostgisNGDataStoreFactory.SSL_MODE;
import static org.geotools.jdbc.JDBCDataStoreFactory.DATABASE;
import static org.geotools.jdbc.JDBCDataStoreFactory.DBTYPE;
import static org.geotools.jdbc.JDBCDataStoreFactory.HOST;
import static org.geotools.jdbc.JDBCDataStoreFactory.PASSWD;
import static org.geotools.jdbc.JDBCDataStoreFactory.PORT;
import static org.geotools.jdbc.JDBCDataStoreFactory.USER;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;
import java.util.Map;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.data.SmartDataLoaderDataAccessFactory;
import org.geoserver.smartdataloader.metadata.jdbc.utils.JdbcUrlSplitter;
import org.geotools.api.feature.type.Name;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.junit.Assert;
import org.junit.Test;
import org.postgresql.jdbc.SslMode;

/**
 * Test class for testing the environment parametrization of the SmartDataLoader store. This test class set up a JDBC
 * store with an env parametrized host, and use it from a SmartDataLoader store.
 */
public abstract class EnvParametrizationStoreTest extends AbstractJDBCSmartDataLoaderTestSupport {

    private static final String SIMPLE_DATA_STORE_NAME = "simple-data-store";
    private static final String SMART_DATA_LOADER_STORE_NAME = "smart-data-loader-store";
    public static final String ALLOW_ENV_PARAMETRIZATION = "ALLOW_ENV_PARAMETRIZATION";
    public static final String HOST_ENV_NAME = "TEST_DB_HOST";

    private DataStoreInfo jdbcDataStore;

    public EnvParametrizationStoreTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
        NAMESPACE_PREFIX = "mt";
        TARGET_NAMESPACE = "http://www.geo-solutions.it/smartappschema/1.0";
    }

    protected static void enableEnvParametrization() {
        System.setProperty(ALLOW_ENV_PARAMETRIZATION, "true");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    protected static void disableEnvParametrization() {
        System.setProperty(ALLOW_ENV_PARAMETRIZATION, "false");
        GeoServerEnvironment.reloadAllowEnvParametrization();
    }

    @Override
    protected void afterSetup() {
        setupGeoServerSimpleTestData();
    }

    /**
     * Test that the env parametrization is enabled and the store creation is successful by resolving the host env
     * variable.
     *
     * @throws IOException
     */
    @Test
    public void testEnvParametrization() throws IOException {
        enableEnvParametrization();
        try {
            setupSmartDataLoaderStore();
            Catalog catalog = getCatalog();
            DataStoreInfo storeInfo =
                    catalog.getDataStoreByName(getCatalog().getDefaultWorkspace(), SMART_DATA_LOADER_STORE_NAME);
            Assert.assertNotNull(storeInfo);
            // create a layer from ths storeInfo
            // check that the layer has the correct connection parameters
            List<Name> nameList = storeInfo.getDataStore(null).getNames();
            Assert.assertEquals(5, nameList.size());
        } finally {
            disableEnvParametrization();
        }
    }

    /**
     * Test that the env parametrization is disabled and the store creation fails with an IOException.
     *
     * @throws IOException
     */
    @Test(expected = IOException.class)
    public void testEnvParametrizationDisabled() throws IOException {
        disableEnvParametrization();
        setupSmartDataLoaderStore();
        Catalog catalog = getCatalog();
        DataStoreInfo storeInfo =
                catalog.getDataStoreByName(getCatalog().getDefaultWorkspace(), SMART_DATA_LOADER_STORE_NAME);
        Assert.assertNotNull(storeInfo);
        List<Name> nameList = storeInfo.getDataStore(null).getNames();
    }

    protected void setupSmartDataLoaderStore() {
        Catalog catalog = getCatalog();
        // use default workspace
        WorkspaceInfo workspace = catalog.getDefaultWorkspace();
        DataStoreInfo storeInfo = catalog.getDataStoreByName(workspace, SMART_DATA_LOADER_STORE_NAME);
        if (storeInfo == null) {
            storeInfo = catalog.getFactory().createDataStore();
            storeInfo.setWorkspace(workspace);
            SmartDataLoaderDataAccessFactory dataStoreFactory = new SmartDataLoaderDataAccessFactory();
            storeInfo.setName(SMART_DATA_LOADER_STORE_NAME);
            storeInfo.setType(dataStoreFactory.getDisplayName());
            storeInfo.setDescription(dataStoreFactory.getDescription());
            Map<String, Serializable> params = storeInfo.getConnectionParameters();
            setupSmartStoreParams(params);
            storeInfo.setEnabled(true);
            catalog.save(storeInfo);
        }
    }

    protected Map<String, Serializable> setupSmartStoreParams(Map<String, Serializable> params) {
        params.put(SmartDataLoaderDataAccessFactory.ROOT_ENTITY.key, "meteo_stations");
        params.put(SmartDataLoaderDataAccessFactory.DATASTORE_METADATA.key, this.jdbcDataStore.getId());
        params.put(SmartDataLoaderDataAccessFactory.DATASTORE_NAME.key, SIMPLE_DATA_STORE_NAME);
        params.put(SmartDataLoaderDataAccessFactory.DBTYPE.key, SmartDataLoaderDataAccessFactory.DBTYPE_STRING);
        return params;
    }

    protected void setupGeoServerSimpleTestData() {
        // insert workspace with defined prefix into geoserver
        Catalog catalog = getCatalog();
        // use default workspace
        WorkspaceInfo workspace = catalog.getDefaultWorkspace();
        DataStoreInfo storeInfo = catalog.getDataStoreByName(workspace, SIMPLE_DATA_STORE_NAME);
        if (storeInfo == null) {
            storeInfo = catalog.getFactory().createDataStore();
            storeInfo.setWorkspace(workspace);
            PostgisNGDataStoreFactory storeFactory = new PostgisNGDataStoreFactory();
            storeInfo.setName(SIMPLE_DATA_STORE_NAME);
            storeInfo.setType(storeFactory.getDisplayName());
            storeInfo.setDescription(storeFactory.getDescription());
            storeInfo.setEnabled(true);
            Map<String, Serializable> params = storeInfo.getConnectionParameters();
            setUpSimpleStoreParameters(params);
            catalog.save(storeInfo);
            this.jdbcDataStore = storeInfo;
        }
    }

    protected Map<String, Serializable> setUpSimpleStoreParameters(Map<String, Serializable> params) {
        JdbcUrlSplitter jdbcUrl = new JdbcUrlSplitter(fixture.getProperty("url"));
        System.setProperty(HOST_ENV_NAME, jdbcUrl.host);
        params.put(HOST.key, "${" + HOST_ENV_NAME + "}");
        params.put(DATABASE.key, jdbcUrl.database);
        String port = jdbcUrl.port != null ? jdbcUrl.port : fixture.getProperty("port");
        params.put(PORT.key, port);
        params.put(USER.key, fixture.getProperty(USER.key));
        params.put(PASSWD.key, fixture.getProperty("passwd"));
        params.put(PostgisNGDataStoreFactory.SCHEMA.key, ONLINE_DB_SCHEMA);
        params.put(SSL_MODE.key, SslMode.DISABLE);
        params.put(DBTYPE.key, fixture.getProperty("dbtype"));
        return params;
    }
}
