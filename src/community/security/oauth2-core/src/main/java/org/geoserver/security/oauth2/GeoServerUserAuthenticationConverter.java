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
package org.geoserver.security.oauth2;

import java.util.Map;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.provider.token.DefaultUserAuthenticationConverter;

/**
 * User Authentication Converter
 *
 * @author Alessio Fabiani, GeoSolutions S.A.S.
 */
public class GeoServerUserAuthenticationConverter extends DefaultUserAuthenticationConverter {

    private Object usernameKey = USERNAME;

    /** Default Constructor. */
    public GeoServerUserAuthenticationConverter() {
        super();
    }

    /** Default Constructor. */
    public GeoServerUserAuthenticationConverter(final String username_key) {
        super();

        usernameKey = username_key;
    }

    @Override
    public Authentication extractAuthentication(Map<String, ?> map) {
        if (map.containsKey(usernameKey)) {
            return new UsernamePasswordAuthenticationToken(map.get(usernameKey), "N/A", null);
        }
        return null;
    }
}
