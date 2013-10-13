/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;

import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.EnumChoiceRenderer;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig.RolesTakenFrom;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.web.role.RoleServiceChoice;

/**
 * Configuration panel for {@link GeoServerJ2eeAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class J2eeAuthFilterPanel extends AuthenticationFilterPanel<J2eeAuthenticationFilterConfig> {

    
    
    DropDownChoice<RolesTakenFrom> rolesTakenFromChoice;

    public J2eeAuthFilterPanel(String id, IModel<J2eeAuthenticationFilterConfig> model) {
        super(id, model);
        add(new RoleServiceChoice("roleServiceName"));
        add(rolesTakenFromChoice = new DropDownChoice<RolesTakenFrom>("rolesTakenFrom", Arrays.asList(RolesTakenFrom.values()),
                new EnumChoiceRenderer<RolesTakenFrom>()));
        rolesTakenFromChoice.setNullValid(false);
    }
}
