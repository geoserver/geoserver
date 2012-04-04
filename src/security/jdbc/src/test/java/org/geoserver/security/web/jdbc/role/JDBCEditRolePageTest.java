package org.geoserver.security.web.jdbc.role;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.role.EditRolePageTest;

public class JDBCEditRolePageTest extends EditRolePageTest {

    public void testFill() throws Exception{
        initializeForJDBC();
        doTestFill();
    }

    public void testFill2() throws Exception{
        initializeForJDBC();
        doTestFill2();
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
