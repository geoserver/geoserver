package org.geoserver.smartdataloader;

import java.util.Properties;

public interface JDBCFixtureHelper {

    String getFixtureId();

    default Properties createExampleFixture() {
        Properties fixture = new Properties();

        fixture.put("url", "jdbc\\:postgresql\\://localhost/mock");
        fixture.put("dbtype", "postgis");
        fixture.put("database", "mock");
        fixture.put("port", "5432");
        fixture.put("host", "localhost");
        fixture.put("user", "geoserver");
        fixture.put("password", "geoserver");
        fixture.put("driver", "org.postgresql.Driver");

        return fixture;
    }
}
