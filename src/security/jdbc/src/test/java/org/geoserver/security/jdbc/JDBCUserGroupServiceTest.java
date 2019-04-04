/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.AbstractUserGroupServiceTest;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public abstract class JDBCUserGroupServiceTest extends AbstractUserGroupServiceTest {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");

    protected abstract String getFixtureId();

    @Before
    public void init() {
        Assume.assumeTrue(getTestData().isTestDataAvailable());
    }

    @After
    public void dropExistingTables() throws Exception {
        if (store != null) {
            JDBCUserGroupStore jdbcStore = (JDBCUserGroupStore) store;
            JDBCTestSupport.dropExistingTables(jdbcStore, jdbcStore.getConnection());
            store.store();
        }
    }

    @Override
    public void setServiceAndStore() throws Exception {
        if (getTestData().isTestDataAvailable()) {
            service = getSecurityManager().loadUserGroupService(getFixtureId());
            store = createStore(service);
        }
    }

    @Override
    protected SecurityUserGroupServiceConfig createConfigObject(String name) {

        try {
            return JDBCTestSupport.createConfigObject(
                    getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {

        return JDBCTestSupport.createUserGroupService(
                getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerUserGroupStore createStore(GeoServerUserGroupService service)
            throws IOException {
        JDBCUserGroupStore store = (JDBCUserGroupStore) super.createStore(service);
        try {
            JDBCTestSupport.dropExistingTables(store, store.getConnection());
        } catch (SQLException e) {
            throw new IOException(e);
        }
        store.createTables();
        store.store();
        return store;
    }

    @Test
    public void testUserGroupDatabaseSetup() throws IOException {

        JDBCUserGroupStore jdbcStore = (JDBCUserGroupStore) store;
        assertTrue(jdbcStore.tablesAlreadyCreated());
        jdbcStore.checkDDLStatements();
        jdbcStore.checkDMLStatements();
        jdbcStore.clear();
        jdbcStore.dropTables();
        jdbcStore.store();
        assertFalse(jdbcStore.tablesAlreadyCreated());
        jdbcStore.load();
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        if ("h2".equalsIgnoreCase(getFixtureId())) return super.createTestData();
        return new LiveDbmsDataSecurity(getFixtureId());
    }

    @Override
    protected boolean isJDBCTest() {
        return true;
    }
}
