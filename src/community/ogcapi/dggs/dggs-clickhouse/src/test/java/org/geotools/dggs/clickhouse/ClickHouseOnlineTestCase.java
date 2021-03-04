/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.dggs.clickhouse;

import java.util.Map;
import java.util.Properties;
import org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore;
import org.geootols.dggs.clickhouse.ClickHouseDGGStoreFactory;
import org.geotools.dggs.DGGSFactoryFinder;
import org.geotools.test.OnlineTestCase;

public abstract class ClickHouseOnlineTestCase extends OnlineTestCase {

    protected ClickHouseDGGSDataStore dataStore;

    @Override
    protected void connect() throws Exception {
        String dggsId = getDGGSId();
        if (!DGGSFactoryFinder.getFactory(dggsId).isPresent()) {
            throw new Exception(dggsId + " is not present, skipping the test");
        }

        ClickHouseDGGStoreFactory factory = new ClickHouseDGGStoreFactory();
        @SuppressWarnings("unchecked")
        Map<String, ?> params = (Map) fixture;
        this.dataStore = (ClickHouseDGGSDataStore) factory.createDataStore(params);
    }

    /**
     * Allows test to create a sample fixture for users.
     *
     * <p>If this method returns a value the first time a fixture is looked up and not found this
     * method will be called to create a fixture file with teh same id, but suffixed with .template.
     */
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
        fixture.put(ClickHouseDGGStoreFactory.DGGS_FACTORY_ID.key, getDGGSId());
        return fixture;
    }

    protected abstract String getDGGSId();

    @Override
    protected String getFixtureId() {
        return "clickhouse-dggs-" + getDGGSId();
    }
}
