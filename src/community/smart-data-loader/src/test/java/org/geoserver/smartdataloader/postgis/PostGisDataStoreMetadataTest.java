package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.metadata.jdbc.JDBCDataStoreMetadataTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisDataStoreMetadataTest extends JDBCDataStoreMetadataTest {

    public PostGisDataStoreMetadataTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
