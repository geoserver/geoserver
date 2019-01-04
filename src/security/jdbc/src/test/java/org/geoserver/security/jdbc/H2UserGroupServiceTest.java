/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;

public class H2UserGroupServiceTest extends JDBCUserGroupServiceTest {

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override
    protected JDBCUserGroupServiceConfig createConfigObject(String serviceName) {
        return JDBCTestSupport.createConfigObjectH2(serviceName, getSecurityManager());
    }

    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {
        return JDBCTestSupport.createH2UserGroupService(getFixtureId(), getSecurityManager());
    }
}
