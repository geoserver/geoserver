package org.geoserver.security.jdbc;

import org.geoserver.data.test.LiveData;
import org.geoserver.data.test.TestData;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.GroupAdminServiceTest;

public class JDBCGroupAdminServiceTest extends GroupAdminServiceTest {

    @Override
    protected TestData buildTestData() throws Exception {
        return new LiveData(unpackTestDataDir());
    }

    @Override
    protected void tearDownInternal() throws Exception {
        super.tearDownInternal();

        JDBCRoleStore rs = (JDBCRoleStore) roleStore;
        JDBCTestSupport.dropExistingTables(rs, rs.getConnection());
        roleStore.store();

        JDBCUserGroupStore ugs = (JDBCUserGroupStore) ugStore;
        JDBCTestSupport.dropExistingTables(ugs, ugs.getConnection());
        ugStore.store();
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String name) throws Exception {
        JDBCUserGroupService service = (JDBCUserGroupService) 
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

        return service;
    }
}
