/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import java.util.logging.Logger;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.impl.AbstractRoleServiceTest;
import org.junit.After;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public abstract class JDBCRoleServiceTest extends AbstractRoleServiceTest {

    static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.geoserver.security.jdbc");

    protected abstract String getFixtureId();

    @After
    public void dropExistingTables() throws Exception {
        if (store != null) {
            JDBCRoleStore jdbcStore = (JDBCRoleStore) store;
            JDBCTestSupport.dropExistingTables(jdbcStore, jdbcStore.getConnection());
            store.store();
        }
    }

    @Before
    public void init() throws IOException {
        Assume.assumeTrue(getTestData().isTestDataAvailable());

        service = getSecurityManager().loadRoleService(getFixtureId());
        store = createStore(service);
    }

    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createRoleService(
                getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());
    }

    @Override
    public GeoServerRoleStore createStore(GeoServerRoleService service) throws IOException {
        JDBCRoleStore store = (JDBCRoleStore) super.createStore(service);
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
    public void testRoleDatabaseSetup() {
        try {
            JDBCRoleStore jdbcStore = (JDBCRoleStore) store;
            assertTrue(jdbcStore.tablesAlreadyCreated());
            jdbcStore.checkDDLStatements();
            jdbcStore.checkDMLStatements();
            jdbcStore.clear();
            jdbcStore.dropTables();
            jdbcStore.store();
            assertFalse(jdbcStore.tablesAlreadyCreated());
            jdbcStore.load();
        } catch (IOException ex) {
            Assert.fail(ex.getMessage());
        }
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
