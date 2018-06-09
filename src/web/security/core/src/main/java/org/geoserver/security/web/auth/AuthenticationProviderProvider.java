/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.web.SecurityNamedServiceProvider;

/**
 * Data provider for authentication provider configurations.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AuthenticationProviderProvider
        extends SecurityNamedServiceProvider<SecurityAuthProviderConfig> {

    @Override
    protected List<SecurityAuthProviderConfig> getItems() {
        List<SecurityAuthProviderConfig> result = new ArrayList<SecurityAuthProviderConfig>();
        try {
            for (String name : getSecurityManager().listAuthenticationProviders()) {
                result.add(getSecurityManager().loadAuthenticationProviderConfig(name));
            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        return result;
    }
}
