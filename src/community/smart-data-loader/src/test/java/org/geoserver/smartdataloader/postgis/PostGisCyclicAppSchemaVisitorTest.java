package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCCyclicAppSchemaVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisCyclicAppSchemaVisitorTest extends JDBCCyclicAppSchemaVisitorTest {

    public PostGisCyclicAppSchemaVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp("meteo_db_cyclic.sql");
    }
}
