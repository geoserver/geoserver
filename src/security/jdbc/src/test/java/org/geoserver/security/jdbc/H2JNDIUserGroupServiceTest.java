/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.jdbc;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.jdbc.config.JDBCUserGroupServiceConfig;

public class H2JNDIUserGroupServiceTest extends JDBCUserGroupServiceTest {

    @Override
    protected String getFixtureId() {
        return "h2";
    }

    @Override

    protected JDBCUserGroupServiceConfig createConfigObject(String serviceName) {
        return JDBCTestSupport.createConfigObjectH2Jndi(serviceName, getSecurityManager());
    }


    
    public GeoServerUserGroupService createUserGroupService(String serviceName) throws Exception {        
        return JDBCTestSupport.createH2UserGroupServiceFromJNDI(getFixtureId(), getSecurityManager());
    }

}
