/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import org.geoserver.config.impl.GeoServerLifecycleHandler;

public class JDBCLifecycleHandler implements GeoServerLifecycleHandler {

    ConfigDatabase configDatabase;

    public JDBCLifecycleHandler(ConfigDatabase configDatabase) {
        this.configDatabase = configDatabase;
    }

    @Override
    public void onReset() {
        configDatabase.clearCache();
    }

    @Override
    public void onDispose() {}

    @Override
    public void beforeReload() {}

    @Override
    public void onReload() {
        configDatabase.clearCache();
    }
}
