package org.geoserver.smartdataloader.postgis;

import org.geoserver.smartdataloader.JDBCFixtureHelper;

public class PostGisFixtureHelper implements JDBCFixtureHelper {
    @Override
    public String getFixtureId() {
        return "smart-data-loader-postgis";
    }
}
