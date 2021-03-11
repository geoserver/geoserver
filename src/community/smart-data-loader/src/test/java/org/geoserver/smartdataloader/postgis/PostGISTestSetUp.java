package org.geoserver.smartdataloader.postgis;

import static org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport.ONLINE_DB_SCHEMA;

import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.postgis.PostgisNGDataStoreFactory;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGISTestSetUp extends JDBCTestSetup {

    @Override
    protected JDBCDataStoreFactory createDataStoreFactory() {
        return new PostgisNGDataStoreFactory();
    }

    @Override
    protected void setUpDataStore(JDBCDataStore dataStore) {
        super.setUpDataStore(dataStore);

        // the unit tests assume a non loose behaviour
        ((PostGISDialect) dataStore.getSQLDialect()).setLooseBBOXEnabled(false);

        // the tests assume non estimated extents
        ((PostGISDialect) dataStore.getSQLDialect()).setEstimatedExtentsEnabled(false);

        // let's work with the most common schema please
        dataStore.setDatabaseSchema("public");
    }

    protected void dropSchema() throws Exception {
        String sql = "DROP SCHEMA IF EXISTS " + ONLINE_DB_SCHEMA + " CASCADE;";
        runSafe(sql);
    }
}
