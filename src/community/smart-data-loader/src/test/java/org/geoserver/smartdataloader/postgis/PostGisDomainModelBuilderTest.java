package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.domain.DomainModelBuilderTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisDomainModelBuilderTest extends DomainModelBuilderTest {

    public PostGisDomainModelBuilderTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp("meteo_int8_db.sql");
    }
}
