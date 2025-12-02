/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.junit.ClassRule;

public class HSQLJNDIUserDetailsServiceTest extends JDBCUserDetailsServiceTest {

    @ClassRule
    public static final HSQLJNDITestConfig jndiConfig = new HSQLJNDITestConfig();

    @Override
    protected String getFixtureId() {
        return "hsql";
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createHSQLRoleServiceFromJNDI(getFixtureId(), getSecurityManager());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createHSQLUserGroupServiceFromJNDI(getFixtureId(), getSecurityManager());
    }
}
