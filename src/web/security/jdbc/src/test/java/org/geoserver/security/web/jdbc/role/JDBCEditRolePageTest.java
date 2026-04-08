/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc.role;

import org.geoserver.security.jdbc.HSQLRoleServiceTest;
import org.geoserver.security.jdbc.HSQLUserGroupServiceTest;
import org.geoserver.security.web.role.EditRolePageTest;
import org.junit.Test;

public class JDBCEditRolePageTest extends EditRolePageTest {

    @Override
    protected void doInitialize() throws Exception {
        initializeForJDBC();
    }

    @Override
    @Test
    public void testFill() throws Exception {
        doTestFill();
    }

    @Override
    @Test
    public void testFill2() throws Exception {
        doTestFill2();
    }

    void initializeForJDBC() throws Exception {
        initialize(new HSQLUserGroupServiceTest(), new HSQLRoleServiceTest());
    }

    @Override
    public String getRoleServiceName() {
        return "hsql";
    }

    @Override
    public String getUserGroupServiceName() {
        return "hsql";
    }
}
