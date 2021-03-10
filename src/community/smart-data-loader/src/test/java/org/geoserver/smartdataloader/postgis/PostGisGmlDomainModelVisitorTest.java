package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCGmlDomainModelVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisGmlDomainModelVisitorTest extends JDBCGmlDomainModelVisitorTest {

    public PostGisGmlDomainModelVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
