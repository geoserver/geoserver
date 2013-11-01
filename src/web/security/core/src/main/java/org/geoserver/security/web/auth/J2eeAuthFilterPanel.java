/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import java.util.Arrays;
import java.util.List;

import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.form.ChoiceRenderer;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig.J2EERoleSource;
import org.geoserver.security.config.J2eeAuthenticationBaseFilterConfig;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel.HeaderPanel;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel.RoleServicePanel;
import org.geoserver.security.web.auth.PreAuthenticatedUserNameFilterPanel.UserGroupServicePanel;
import org.geoserver.security.web.role.RoleServiceChoice;

/**
 * Configuration panel for {@link GeoServerJ2eeAuthenticationFilter}.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class J2eeAuthFilterPanel extends J2eeBaseAuthFilterPanel<J2eeAuthenticationFilterConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public J2eeAuthFilterPanel(String id, IModel<J2eeAuthenticationFilterConfig> model) {
        super(id, model);
    }

   
}
