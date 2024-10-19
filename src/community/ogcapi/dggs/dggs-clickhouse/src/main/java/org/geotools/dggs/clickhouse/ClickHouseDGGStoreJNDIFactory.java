/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.dggs.clickhouse;

/**
 * Factory for {@link org.geotools.dggs.gstore.DGGSStore} based on ClickHouse storage. Compared to
 * {@link ClickHouseDGGStoreFactory} this version grabs the connection pool from JNDI
 */
public class ClickHouseDGGStoreJNDIFactory extends ClickHouseDGGStoreFactory {

    public ClickHouseDGGStoreJNDIFactory() {
        this.delegate = new ClickHouseJDBCJNDIDataStoreFactory();
    }

    @Override
    public String getDisplayName() {
        return "ClickHouse DGGS integration (JNDI)";
    }

    @Override
    public String getDescription() {
        return "ClickHouse DGGS integration (JNDI)";
    }
}
