package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.data.store.JDBCExclusionsDomainModelVisitorTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisExclusionDomainModelVisitorTest extends JDBCExclusionsDomainModelVisitorTest {

    public PostGisExclusionDomainModelVisitorTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
