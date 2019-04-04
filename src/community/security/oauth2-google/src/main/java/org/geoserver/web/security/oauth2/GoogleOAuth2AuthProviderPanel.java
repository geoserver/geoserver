/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.apache.wicket.model.IModel;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.GoogleOAuth2FilterConfig;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GoogleOAuth2AuthProviderPanel
        extends GeoServerOAuth2AuthProviderPanel<GoogleOAuth2FilterConfig> {

    public GoogleOAuth2AuthProviderPanel(String id, IModel<GoogleOAuth2FilterConfig> model) {
        super(id, model);
    }
}
