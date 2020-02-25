/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import org.geotools.jdbc.ConnectionLifecycleListener;
import org.geotools.jdbc.JDBCDataStore;

class MockConnectionLifecycleListener implements ConnectionLifecycleListener {

    class ActionCount {
        int borrowCount = 0;

        int releaseCount = 0;

        int commitCount = 0;

        int rollbackCount = 0;
    }

    Map<JDBCDataStore, ActionCount> actionCountByDataStore =
            new HashMap<JDBCDataStore, ActionCount>();

    private void initCountIfNecessary(JDBCDataStore store) {
        if (!actionCountByDataStore.containsKey(store)) {
            actionCountByDataStore.put(store, new ActionCount());
        }
    }

    @Override
    public void onBorrow(JDBCDataStore store, Connection cx) throws SQLException {
        initCountIfNecessary(store);
        actionCountByDataStore.get(store).borrowCount++;
    }

    @Override
    public void onRelease(JDBCDataStore store, Connection cx) throws SQLException {
        initCountIfNecessary(store);
        actionCountByDataStore.get(store).releaseCount++;
    }

    @Override
    public void onCommit(JDBCDataStore store, Connection cx) throws SQLException {
        initCountIfNecessary(store);
        actionCountByDataStore.get(store).commitCount++;
    }

    @Override
    public void onRollback(JDBCDataStore store, Connection cx) throws SQLException {
        initCountIfNecessary(store);
        actionCountByDataStore.get(store).rollbackCount++;
    }
}
