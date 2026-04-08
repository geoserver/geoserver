/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;
import org.junit.ClassRule;

public class HSQLJNDIUserGroupServiceTest extends JDBCUserGroupServiceTest {

    @ClassRule
    public static final HSQLJNDITestConfig jndiConfig = new HSQLJNDITestConfig();

    @Override
    protected String getFixtureId() {
        return "hsql";
    }

    @Override
    protected JDBCUserGroupServiceConfig createConfigObject(String serviceName) {
        return JDBCTestSupport.createConfigObjectHSQLJNDI(serviceName, getSecurityManager());
    }

    @Override
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createHSQLUserGroupServiceFromJNDI(getFixtureId(), getSecurityManager());
    }
}
