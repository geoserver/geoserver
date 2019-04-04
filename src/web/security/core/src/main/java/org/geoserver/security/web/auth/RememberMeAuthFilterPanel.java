/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.RememberMeAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerRememberMeAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerRememberMeAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class RememberMeAuthFilterPanel
        extends AuthenticationFilterPanel<RememberMeAuthenticationFilterConfig> {

    public RememberMeAuthFilterPanel(
            String id, IModel<RememberMeAuthenticationFilterConfig> model) {
        super(id, model);
    }
}
