/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerRoleService;
import org.junit.ClassRule;

public class H2JNDIRoleServiceTest extends JDBCRoleServiceTest {

    @ClassRule
    public static final H2JNDITestConfig jndiConfig = new H2JNDITestConfig();

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2RoleServiceFromJNDI(getFixtureId(), getSecurityManager());
    }
}
