package org.geotools.dggs.clickhouse;

import java.util.Properties;
import org.geootols.dggs.clickhouse.ClickHouseJDBCDataStoreFactory;
import org.geotools.jdbc.JDBCDataStoreFactory;
import org.geotools.jdbc.JDBCTestSetup;

class ClickHouseJDBCTestSetup extends JDBCTestSetup {
    @Override
    protected JDBCDataStoreFactory createDataStoreFactory() {
        return new ClickHouseJDBCDataStoreFactory();
    }

    @Override
    protected Properties createExampleFixture() {
        Properties fixture = new Properties();
        fixture.put("driver", "ru.yandex.clickhouse.ClickHouseDriver");
        fixture.put("url", "jdbc:clickhouse://localhost:8123/test");
        fixture.put("host", "localhost");
        fixture.put("database", "test");
        fixture.put("port", "8123");
        fixture.put("user", "default");
        fixture.put("password", "");
        return fixture;
    }
}
