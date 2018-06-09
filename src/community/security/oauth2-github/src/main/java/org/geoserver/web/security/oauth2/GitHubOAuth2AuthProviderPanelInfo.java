/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2;

import org.geoserver.security.oauth2.GeoServerOAuthAuthenticationFilter;
import org.geoserver.security.oauth2.GitHubOAuth2FilterConfig;
import org.geoserver.security.oauth2.GitHubOAuthAuthenticationFilter;
import org.geoserver.security.web.auth.AuthenticationFilterPanelInfo;

/**
 * Configuration panel extension for {@link GeoServerOAuthAuthenticationFilter}.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubOAuth2AuthProviderPanelInfo
        extends AuthenticationFilterPanelInfo<
                GitHubOAuth2FilterConfig, GitHubOAuth2AuthProviderPanel> {

    /** serialVersionUID */
    private static final long serialVersionUID = -3891569684560944819L;

    public GitHubOAuth2AuthProviderPanelInfo() {
        setComponentClass(GitHubOAuth2AuthProviderPanel.class);
        setServiceClass(GitHubOAuthAuthenticationFilter.class);
        setServiceConfigClass(GitHubOAuth2FilterConfig.class);
    }
}
