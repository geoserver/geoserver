/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.web.role.RoleServiceChoice;

/**
 * Configuration panel for {@link GeoServerJ2eeAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class J2eeAuthFilterPanel extends AuthenticationFilterPanel<J2eeAuthenticationFilterConfig> {

    public J2eeAuthFilterPanel(String id, IModel<J2eeAuthenticationFilterConfig> model) {
        super(id, model);
        add(new RoleServiceChoice("roleServiceName"));
    }
}
