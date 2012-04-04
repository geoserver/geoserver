package org.geoserver.security.web.jdbc.user;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.user.EditUserPageTest;

public class JDBCEditUserPageTest extends EditUserPageTest {

    public void testFill() throws Exception{
        initializeForJDBC();
        doTestFill();
    }

    public void testReadOnlyUserGroupService() throws Exception {
        initializeForJDBC();
        doTestReadOnlyUserGroupService();
    }

    public void testReadOnlyRoleService() throws Exception {
        initializeForJDBC();
        doTestReadOnlyRoleService();
    }

    void initializeForJDBC() throws Exception {
        initialize(new H2UserGroupServiceTest(), new H2RoleServiceTest());
    }

    @Override
    public String getRoleServiceName() {
        return "h2";
    }

    @Override
    public String getUserGroupServiceName() {
        return "h2";
    }

}
