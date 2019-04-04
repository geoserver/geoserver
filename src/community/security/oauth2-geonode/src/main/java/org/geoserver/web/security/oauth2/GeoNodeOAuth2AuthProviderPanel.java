/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 */

/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.apache.wicket.model.IModel;
import org.geoserver.security.oauth2.GeoNodeOAuth2FilterConfig;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoNodeOAuth2AuthProviderPanel
        extends GeoServerOAuth2AuthProviderPanel<GeoNodeOAuth2FilterConfig> {

    public GeoNodeOAuth2AuthProviderPanel(String id, IModel<GeoNodeOAuth2FilterConfig> model) {
        super(id, model);
    }
}
