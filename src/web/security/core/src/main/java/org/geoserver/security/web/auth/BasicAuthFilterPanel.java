/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerBasicAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class BasicAuthFilterPanel
        extends AuthenticationFilterPanel<BasicAuthenticationFilterConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public BasicAuthFilterPanel(String id, IModel<BasicAuthenticationFilterConfig> model) {
        super(id, model);

        add(new CheckBox("useRememberMe"));
    }
}
