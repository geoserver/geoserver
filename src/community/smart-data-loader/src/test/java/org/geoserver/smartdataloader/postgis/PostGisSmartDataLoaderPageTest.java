package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.data.store.AbstractSmartDataLoaderPageTest;
import org.geotools.jdbc.JDBCTestSetup;

public class PostGisSmartDataLoaderPageTest extends AbstractSmartDataLoaderPageTest {

    public PostGisSmartDataLoaderPageTest() {
        super(new PostGisFixtureHelper());
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsPostGISTestSetUp();
    }
}
