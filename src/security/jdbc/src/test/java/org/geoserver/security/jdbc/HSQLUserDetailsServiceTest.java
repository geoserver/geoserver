/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerUserGroupService;
import org.junit.Ignore;

@Ignore
public class HSQLUserDetailsServiceTest extends JDBCUserDetailsServiceTest {

    @Override
    protected String getFixtureId() {
        return "hsql";
    }

    @Override
    public GeoServerRoleService createRoleService(String serviceName) throws Exception {
        return JDBCTestSupport.createHSQLRoleService(getFixtureId(), getSecurityManager());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createHsqlUserGroupService(getFixtureId(), getSecurityManager());
    }
}
