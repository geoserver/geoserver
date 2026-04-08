/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.jdbc.user;

import java.io.Serial;
import org.geoserver.security.jdbc.HSQLRoleServiceTest;
import org.geoserver.security.jdbc.HSQLUserGroupServiceTest;
import org.geoserver.security.web.user.ConfirmRemovalUserPanelTest;
import org.junit.Test;

public class JDBCConfirmRemovalUserPanelTest extends ConfirmRemovalUserPanelTest {

    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    @Test
    public void testRemoveUser() throws Exception {
        disassociateRoles = false;
        initializeForJDBC();
        removeObject();
    }

    @Override
    @Test
    public void testRemoveUserWithRoles() throws Exception {
        disassociateRoles = true;
        initializeForJDBC();
        removeObject();
    }

    void initializeForJDBC() throws Exception {
        initialize(new HSQLUserGroupServiceTest(), new HSQLRoleServiceTest());
    }
}
