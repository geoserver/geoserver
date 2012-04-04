package org.geoserver.security.web.jdbc.group;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.group.NewGroupPage;
import org.geoserver.security.web.group.NewGroupPageTest;

public class JDBCNewGroupPageTest extends NewGroupPageTest {

    NewGroupPage page;

    public void testFill() throws Exception{
        initializeForJDBC();
        doTestFill();
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
