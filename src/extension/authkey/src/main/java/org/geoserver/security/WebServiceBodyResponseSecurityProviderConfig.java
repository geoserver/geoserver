/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;

/**
 * Configuration for {@linkplain WebServiceBodyResponseSecurityProvider} class.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class WebServiceBodyResponseSecurityProviderConfig extends BaseSecurityNamedServiceConfig
        implements SecurityAuthProviderConfig {

    /** serialVersionUID */
    private static final long serialVersionUID = -784422971183238680L;

    String userGroupServiceName;

    public WebServiceBodyResponseSecurityProviderConfig() {}

    public WebServiceBodyResponseSecurityProviderConfig(
            WebServiceBodyResponseSecurityProviderConfig other) {
        super(other);
        userGroupServiceName = other.getUserGroupServiceName();
    }

    @Override
    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    @Override
    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }
}
