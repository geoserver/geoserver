package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.domain.DomainModelVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisDomainModelVisitorTest extends DomainModelVisitorTest {

    public PostGisDomainModelVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
