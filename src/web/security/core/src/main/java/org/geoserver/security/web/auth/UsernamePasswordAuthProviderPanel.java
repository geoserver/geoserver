/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.geoserver.security.auth.UsernamePasswordAuthenticationProvider;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link UsernamePasswordAuthenticationProvider}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class UsernamePasswordAuthProviderPanel
        extends AuthenticationProviderPanel<UsernamePasswordAuthenticationProviderConfig> {

    public UsernamePasswordAuthProviderPanel(
            String id, IModel<UsernamePasswordAuthenticationProviderConfig> model) {
        super(id, model);

        add(new UserGroupServiceChoice("userGroupServiceName"));
    }
}
