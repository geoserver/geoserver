package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.domain.DomainModelBuilderCyclicTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisDomainModelBuilderCyclicTest extends DomainModelBuilderCyclicTest {

    public PostGisDomainModelBuilderCyclicTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp("meteo_db_cyclic.sql");
    }
}
