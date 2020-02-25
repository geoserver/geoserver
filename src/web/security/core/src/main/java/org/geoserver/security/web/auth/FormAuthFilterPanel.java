/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerUserNamePasswordAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class FormAuthFilterPanel
        extends AuthenticationFilterPanel<UsernamePasswordAuthenticationFilterConfig> {

    public FormAuthFilterPanel(
            String id, IModel<UsernamePasswordAuthenticationFilterConfig> model) {
        super(id, model);

        add(new TextField("usernameParameterName"));
        add(new TextField("passwordParameterName"));
    }
}
