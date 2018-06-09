/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.services;

import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

/**
 * User Authentication Converter for GitHub token details.
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GitHubUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

    private static Object USERNAME_KEY = USERNAME;

    /**
     * Default Constructor.
     *
     * @param username_key
     */
    public GitHubUserAuthenticationConverter() {
        super();
    }

    /**
     * Default Constructor.
     *
     * @param username_key
     */
    public GitHubUserAuthenticationConverter(String username_key) {
        super();

        USERNAME_KEY = username_key;
    }

    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(USERNAME_KEY)) {
            return new UsernamePasswordAuthenticationToken(map.get(USERNAME_KEY), "N/A", null);
        }
        return null;
    }
}
