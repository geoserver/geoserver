/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.ClassRule;

public class H2JNDIUserDetailsServiceTest extends JDBCUserDetailsServiceTest {

    @ClassRule
    public static final H2JNDITestConfig jndiConfig = new H2JNDITestConfig();

    @BeforeClass
    public static void checkGithub() {
        // on Github this test randomly fails with a H2 lock issue
        Assume.assumeFalse(System.getProperty("github-build") != null);
    }

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2RoleServiceFromJNDI(getFixtureId(), getSecurityManager());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2UserGroupServiceFromJNDI(getFixtureId(), getSecurityManager());
    }
}
