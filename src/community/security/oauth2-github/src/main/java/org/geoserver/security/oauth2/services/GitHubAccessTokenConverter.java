/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

/**
 * Access Token Converter for GitHub token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubAccessTokenConverter extends GeoServerAccessTokenConverter {

    public GitHubAccessTokenConverter() {
        super(new GitHubUserAuthenticationConverter());
    }
}
