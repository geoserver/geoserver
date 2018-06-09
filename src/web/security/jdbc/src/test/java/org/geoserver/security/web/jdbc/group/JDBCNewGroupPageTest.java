/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc.group;

import org.geoserver.security.jdbc.H2RoleServiceTest;
import org.geoserver.security.jdbc.H2UserGroupServiceTest;
import org.geoserver.security.web.group.NewGroupPage;
import org.geoserver.security.web.group.NewGroupPageTest;
import org.junit.Test;

public class JDBCNewGroupPageTest extends NewGroupPageTest {

    NewGroupPage page;

    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    @Override
    protected void doInitialize() throws Exception {
        initializeForJDBC();
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
