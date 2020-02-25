/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.jdbc;

import java.io.IOException;
import java.sql.SQLException;
import org.geoserver.data.test.LiveSystemTestData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.GroupAdminServiceTest;
import org.geoserver.security.jdbc.config.JDBCRoleServiceConfig;
import org.junit.After;

public class JDBCGroupAdminServiceTest extends GroupAdminServiceTest {

    @Override
    protected SystemTestData createTestData() throws Exception {
        return new LiveSystemTestData(unpackTestDataDir());
    }

    //    @Before
    //    public void init() throws Exception {
    //        super.init();
    //        ugStore.store();
    //        roleStore.store();
    //    }

    @After
    public void rollback() throws Exception {
        if (ugStore != null) ugStore.load();
        if (roleStore != null) roleStore.load();
    }

    //    @AfterClass
    //    public void dropTables() throws Exception {
    //
    //        JDBCRoleStore rs = (JDBCRoleStore) roleStore;
    //        JDBCTestSupport.dropExistingTables(rs, rs.getConnection());
    //        roleStore.store();
    //
    //        JDBCUserGroupStore ugs = (JDBCUserGroupStore) ugStore;
    //        JDBCTestSupport.dropExistingTables(ugs, ugs.getConnection());
    //        ugStore.store();
    //    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        JDBCUserGroupService service =
                (JDBCUserGroupService)
                        JDBCTestSupport.createH2UserGroupService(name, getSecurityManager());
        if (!service.tablesAlreadyCreated()) {
            service.createTables();
        }

        return service;
    }

    @Override
    public GeoServerRoleService createRoleService(String name) throws Exception {
        JDBCRoleService service =
                (JDBCRoleService) JDBCTestSupport.createH2RoleService(name, getSecurityManager());
        if (!service.tablesAlreadyCreated()) {
            service.createTables();
        }
        JDBCRoleServiceConfig gaConfig =
                (JDBCRoleServiceConfig) getSecurityManager().loadRoleServiceConfig(name);
        gaConfig.setAdminRoleName("adminRole");
        gaConfig.setGroupAdminRoleName("groupAdminRole");
        getSecurityManager().saveRoleService(gaConfig);
        return getSecurityManager().loadRoleService(name);
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
}
