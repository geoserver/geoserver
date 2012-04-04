package org.geoserver.security.web.jdbc.role;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.role.RoleListPageTest;

public class JDBCRoleListPageTest extends RoleListPageTest {

    public void testRemove() throws Exception {
        initializeForJDBC();
        insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath()+":panel:header:removeSelected");
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
