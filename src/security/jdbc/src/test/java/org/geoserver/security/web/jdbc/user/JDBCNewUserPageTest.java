package org.geoserver.security.web.jdbc.user;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.user.NewUserPageTest;

public class JDBCNewUserPageTest extends NewUserPageTest {

    public void testFill() throws Exception{
        initializeForJDBC();
        doTestFill();
    }
    
    public void testFill3() throws Exception{
        initializeForJDBC();
        doTestFill3();
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
