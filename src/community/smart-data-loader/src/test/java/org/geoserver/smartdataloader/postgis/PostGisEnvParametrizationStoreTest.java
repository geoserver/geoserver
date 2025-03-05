package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.data.store.EnvParametrizationStoreTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisEnvParametrizationStoreTest extends EnvParametrizationStoreTest {

    public PostGisEnvParametrizationStoreTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
