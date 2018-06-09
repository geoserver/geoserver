/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.web.auth;

import org.apache.wicket.model.IModel;
import org.geoserver.security.config.J2eeAuthenticationFilterConfig;
import org.geoserver.security.filter.GeoServerJ2eeAuthenticationFilter;

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
