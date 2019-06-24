/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Collection;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** An authentication based on a unique key. Allows the unique key to be retrieved later */
public class KeyAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = -6354705060521817602L;

    public static final String DEFAULT_URL_PARAM = "authkey";

    private String key, authKeyParamName;

    public KeyAuthenticationToken(String key, String authKeyParamName, UserDetails user) {
        this(key, authKeyParamName, user, user.getAuthorities());
    }

    public KeyAuthenticationToken(
            String key,
            String authKeyParamName,
            UserDetails user,
            Collection<? extends GrantedAuthority> authorities) {
        super(authorities);
        this.key = key;
        setDetails(user);
        this.authKeyParamName = authKeyParamName;
        setAuthenticated(true);
    }

    public Object getCredentials() {
        return key;
    }

    public Object getPrincipal() {
        return ((UserDetails) getDetails()).getUsername();
    }

    public String getAuthKeyParamName() {
        return authKeyParamName;
    }
}
