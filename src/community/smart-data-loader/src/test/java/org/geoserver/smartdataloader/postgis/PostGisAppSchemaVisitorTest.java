package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCOverrideAppSchemaVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisAppSchemaVisitorTest extends JDBCOverrideAppSchemaVisitorTest {

    public PostGisAppSchemaVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
