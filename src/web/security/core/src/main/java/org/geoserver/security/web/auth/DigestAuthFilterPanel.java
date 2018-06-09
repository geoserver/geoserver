/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerDigestAuthenticationFilter;
import org.geoserver.security.web.usergroup.UserGroupServiceChoice;

/**
 * Configuration panel for {@link GeoServerDigestAuthenticationFilter}.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class DigestAuthFilterPanel
        extends AuthenticationFilterPanel<DigestAuthenticationFilterConfig> {

    /** serialVersionUID */
    private static final long serialVersionUID = 1L;

    public DigestAuthFilterPanel(String id, IModel<DigestAuthenticationFilterConfig> model) {
        super(id, model);

        add(new UserGroupServiceChoice("userGroupServiceName"));
        add(new TextField("nonceValiditySeconds").setType(Integer.class));
    }
}
