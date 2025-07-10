package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCAppSchemaVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisPrefixAppSchemaVisitorTest extends JDBCAppSchemaVisitorTest {

    public PostGisPrefixAppSchemaVisitorTest() {
        super(new PostGisFixtureHelper(), "Mt1");
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
