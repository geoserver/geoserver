/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;

/**
 * GeoServer implementation of {@link UserDetails}.
 *
 * @author christian
 */
public class GeoServerUser implements UserDetails, CredentialsContainer, Comparable<GeoServerUser> {

    private static final long serialVersionUID = 1L;

    public static final String DEFAULT_ADMIN_PASSWD = "geoserver";
    public static final boolean AdminEnabled = true;
    public static final String ADMIN_USERNAME = "admin";
    public static final String ROOT_USERNAME = "root";
    public static final String ANONYMOUS_USERNAME = "anonymous";

    /** Create the geoserver default administrator */
    public static GeoServerUser createDefaultAdmin() {
        GeoServerUser admin = new GeoServerUser(ADMIN_USERNAME);
        admin.setPassword(DEFAULT_ADMIN_PASSWD);
        admin.setEnabled(AdminEnabled);
        return admin;
    }

    public static GeoServerUser createRoot() {
        GeoServerUser root = new GeoServerUser(GeoServerUser.ROOT_USERNAME);
        root.setPassword(null);
        root.setEnabled(true);
        Set<GrantedAuthority> roles = new HashSet<GrantedAuthority>();
        roles.add(GeoServerRole.ADMIN_ROLE);
        root.setAuthorities(roles);
        return root;
    }

    public static GeoServerUser createAnonymous() {
        GeoServerUser anon = new GeoServerUser(GeoServerUser.ANONYMOUS_USERNAME);
        anon.setPassword(null);
        anon.setEnabled(true);
        Set<GrantedAuthority> roles = new HashSet<GrantedAuthority>();
        roles.add(GeoServerRole.ANONYMOUS_ROLE);
        anon.setAuthorities(roles);
        return anon;
    }

    private String password;
    private String username;
    private boolean accountNonExpired;
    private boolean accountNonLocked;
    private boolean credentialsNonExpired;
    private boolean enabled;

    protected Properties properties;
    protected Collection<GrantedAuthority> authorities;

    public GeoServerUser(String username) {
        this.username = username;
        this.enabled = true;
        this.accountNonExpired = this.accountNonLocked = this.credentialsNonExpired = true;
        this.authorities = null;
    }

    public GeoServerUser(GeoServerUser other) {
        this.username = other.getUsername();
        this.password = other.getPassword();
        this.accountNonExpired = other.isAccountNonExpired();
        this.accountNonLocked = other.isAccountNonLocked();
        this.credentialsNonExpired = other.isCredentialsNonExpired();
        this.authorities =
                other.getAuthorities() != null
                        ? new ArrayList<GrantedAuthority>(other.getAuthorities())
                        : null;
    }

    /** The user name. */
    public String getUsername() {
        return username;
    }

    /** The user password. */
    public String getPassword() {
        return password;
    }

    public void setPassword(String passwd) {
        this.password = passwd;
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#isEnabled()
     */
    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonExpired()
     */
    public boolean isAccountNonExpired() {
        return accountNonExpired;
    }

    public void setAccountNonExpired(boolean accountNonExpired) {
        if (this.accountNonExpired != accountNonExpired) {
            this.accountNonExpired = accountNonExpired;
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#isAccountNonLocked()
     */
    public boolean isAccountNonLocked() {
        return accountNonLocked;
    }

    public void setAccountNonLocked(boolean accountNonLocked) {
        if (this.accountNonLocked != accountNonLocked) {
            this.accountNonLocked = accountNonLocked;
            // calculateGrantedAuthorities();
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#isCredentialsNonExpired()
     */
    public boolean isCredentialsNonExpired() {
        return credentialsNonExpired;
    }

    public void setCredentialsNonExpired(boolean credentialsNonExpired) {
        if (this.credentialsNonExpired != credentialsNonExpired) {
            this.credentialsNonExpired = credentialsNonExpired;
            // calculateGrantedAuthorities();
        }
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.userdetails.UserDetails#getAuthorities()
     */
    public Collection<GrantedAuthority> getAuthorities() {
        if (authorities == null)
            authorities = Collections.unmodifiableSet(new TreeSet<GrantedAuthority>());
        return authorities;
    }

    /** Set the roles of the user. */
    public void setAuthorities(Set<? extends GrantedAuthority> roles) {
        authorities = Collections.unmodifiableSet(roles);
    }

    /* (non-Javadoc)
     * @see org.springframework.security.core.CredentialsContainer#eraseCredentials()
     */
    public void eraseCredentials() {
        password = null;
    }

    /**
     * Additional properties associated with the user.
     *
     * <p>This typically is information filled in by the backend user/group service. For examples:
     * eMail Address, telephone number, etc..
     */
    public Properties getProperties() {
        if (properties == null) properties = new Properties();
        return properties;
    }

    /* (non-Javadoc)
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    public int compareTo(GeoServerUser o) {
        if (o == null) return 1;
        return getUsername().compareTo(o.getUsername());
    }

    public GeoServerUser copy() {
        return new GeoServerUser(this);
    }

    /**
     * Returns {@code true} if the supplied object is a {@code User} instance with the same {@code
     * username} value.
     *
     * <p>In other words, the objects are equal if they have the same username, representing the
     * same principal.
     */
    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof GeoServerUser) {
            return username.equals(((GeoServerUser) rhs).username);
        }
        return false;
    }

    /** Returns the hashcode of the {@code username}. */
    @Override
    public int hashCode() {
        return username.hashCode();
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        // sb.append(super.toString()).append(": ");
        sb.append("Username: ").append(this.username).append("; ");
        sb.append("Password: [PROTECTED]; ");
        sb.append("Enabled: ").append(this.enabled).append("; ");
        sb.append("AccountNonExpired: ").append(this.accountNonExpired).append("; ");
        sb.append("CredentialsNonExpired: ").append(this.credentialsNonExpired).append("; ");
        sb.append("AccountNonLocked: ").append(this.accountNonLocked).append("; ");
        sb.append(" [ ");
        if (authorities != null)
            sb.append(StringUtils.collectionToCommaDelimitedString(authorities));
        sb.append(" ] ");

        return sb.toString();
    }
}
