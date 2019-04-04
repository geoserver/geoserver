/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.apache.wicket.model.IModel;
import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.GitHubOAuth2FilterConfig;

/**
 * Configuration panel for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubOAuth2AuthProviderPanel
        extends GeoServerOAuth2AuthProviderPanel<GitHubOAuth2FilterConfig> {

    public GitHubOAuth2AuthProviderPanel(String id, IModel<GitHubOAuth2FilterConfig> model) {
        super(id, model);
    }
}
