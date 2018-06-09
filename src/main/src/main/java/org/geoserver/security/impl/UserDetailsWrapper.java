/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * Simple wrapper class for a {@link UserDetails} object. Subclasses should override individual
 * methods.
 *
 * @author christian
 */
public class UserDetailsWrapper implements UserDetails {

    private static final long serialVersionUID = 1L;

    private UserDetails details;

    public UserDetails getWrappedObject() {
        return details;
    }

    public UserDetailsWrapper(UserDetails details) {
        this.details = details;
    }

    public Collection<? extends GrantedAuthority> getAuthorities() {
        return details.getAuthorities();
    }

    public String getPassword() {
        return details.getPassword();
    }

    public String getUsername() {
        return details.getUsername();
    }

    public boolean isAccountNonExpired() {
        return details.isAccountNonExpired();
    }

    public boolean isAccountNonLocked() {
        return details.isAccountNonLocked();
    }

    public boolean isCredentialsNonExpired() {
        return details.isCredentialsNonExpired();
    }

    public boolean isEnabled() {
        return details.isEnabled();
    }
}
