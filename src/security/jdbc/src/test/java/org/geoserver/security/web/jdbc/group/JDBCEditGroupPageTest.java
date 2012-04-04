package org.geoserver.security.web.jdbc.group;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.group.EditGroupPageTest;

public class JDBCEditGroupPageTest extends EditGroupPageTest {

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
