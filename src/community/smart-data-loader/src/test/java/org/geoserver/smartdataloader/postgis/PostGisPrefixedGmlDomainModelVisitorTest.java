package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.visitors.JDBCGmlDomainModelVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisPrefixedGmlDomainModelVisitorTest extends JDBCGmlDomainModelVisitorTest {

    public PostGisPrefixedGmlDomainModelVisitorTest() {
        super(new PostGisFixtureHelper(), "Mt1");
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
