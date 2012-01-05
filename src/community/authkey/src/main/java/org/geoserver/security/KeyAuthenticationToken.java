/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.springframework.security.providers.AbstractAuthenticationToken;
import org.springframework.security.userdetails.UserDetails;

/**
 * An authentication based on a unique key. Allows the unique key to be retrieved later
 */
public class KeyAuthenticationToken extends AbstractAuthenticationToken {
    private static final long serialVersionUID = -6354705060521817602L;

    public static final String KEY = "authkey";

    private String key;

    private UserDetails user;

    public KeyAuthenticationToken(String key, UserDetails user) {
        super(user.getAuthorities());
        this.key = key;
        this.user = user;
    }

    public Object getCredentials() {
        return key;
    }

    public Object getPrincipal() {
        return user.getUsername();
    }

}
