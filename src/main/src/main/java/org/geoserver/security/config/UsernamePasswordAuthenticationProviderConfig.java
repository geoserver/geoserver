/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import java.io.Serial;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;

/**
 * Config object for {@link UsernamePasswordAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UsernamePasswordAuthenticationProviderConfig extends BaseSecurityNamedServiceConfig
        implements SecurityAuthProviderConfig {

    @Serial
    private static final long serialVersionUID = 1L;

    String userGroupServiceName;

    public UsernamePasswordAuthenticationProviderConfig() {}

    public UsernamePasswordAuthenticationProviderConfig(UsernamePasswordAuthenticationProviderConfig other) {
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
