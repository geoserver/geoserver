package org.geoserver.security.web.jdbc.group;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.group.GroupListPageTest;

public class JDBCGroupListPageTest extends GroupListPageTest {
    public void testRemoveWithRoles() throws Exception {
        withRoles=true;
        initializeForJDBC();
        insertValues();
        addAdditonalData();
        doRemove(getTabbedPanelPath()+":panel:header:removeSelectedWithRoles");
    }

    public void testRemoveJDBC() throws Exception {
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
