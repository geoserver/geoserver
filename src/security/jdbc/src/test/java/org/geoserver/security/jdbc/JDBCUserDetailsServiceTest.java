/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.impl.AbstractUserDetailsServiceTest;
import org.junit.After;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public abstract class JDBCUserDetailsServiceTest extends AbstractUserDetailsServiceTest {

    protected abstract String getFixtureId();

    @Before
    public void init() {
        Assume.assumeTrue(getTestData().isTestDataAvailable());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {

        return JDBCTestSupport.createUserGroupService(
                getFixtureId(), (LiveDbmsDataSecurity) getTestData(), getSecurityManager());
    }

    @Override
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

    @After
    public void dropTables() throws Exception {
        if (roleStore != null) {
            JDBCRoleStore jdbcStore1 = (JDBCRoleStore) roleStore;
            JDBCTestSupport.dropExistingTables(jdbcStore1, jdbcStore1.getConnection());
            roleStore.store();
        }

        if (usergroupStore != null) {
            JDBCUserGroupStore jdbcStore2 = (JDBCUserGroupStore) usergroupStore;
            JDBCTestSupport.dropExistingTables(jdbcStore2, jdbcStore2.getConnection());
            usergroupStore.store();
        }
    }

    @Override
    protected void setServices(String serviceName) throws Exception {
        if (getSecurityManager().loadRoleService(getFixtureId()) == null)
            super.setServices(getFixtureId());
        else {
            roleService = getSecurityManager().loadRoleService(getFixtureId());
            roleStore = createStore(roleService);
            usergroupService = getSecurityManager().loadUserGroupService(getFixtureId());
            usergroupStore = createStore(usergroupService);
            getSecurityManager().setActiveRoleService(roleService);
        }
    }

    @Override
    protected boolean isJDBCTest() {
        return true;
    }

    @Override
    protected SystemTestData createTestData() throws Exception {
        if ("h2".equalsIgnoreCase(getFixtureId())) return super.createTestData();
        return new LiveDbmsDataSecurity(getFixtureId());
    }

    @Test
    public void testConfiguration() throws Exception {
        setServices("config");
        assertEquals(roleService, getSecurityManager().getActiveRoleService());
        // assertEquals(usergroupService,getSecurityManager().getActiveUserGroupService());
        assertEquals(
                usergroupService.getName(),
                getSecurityManager().loadUserGroupService(getFixtureId()).getName());
        assertTrue(roleService.canCreateStore());
        assertTrue(usergroupService.canCreateStore());
    }
}
