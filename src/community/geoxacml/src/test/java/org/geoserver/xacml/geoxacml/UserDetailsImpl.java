/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.xacml.geoxacml;

import org.springframework.security.GrantedAuthority;
import org.springframework.security.userdetails.UserDetails;

import com.vividsolutions.jts.geom.Geometry;

public class UserDetailsImpl implements UserDetails {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    Geometry geometryRestriction;

    int persNr;

    String username, password;

    GrantedAuthority[] authorities = null;

    public UserDetailsImpl(String name, String pw, GrantedAuthority[] authorities) {
        username = name;
        password = pw;
        this.authorities = authorities;
    }

    public GrantedAuthority[] getAuthorities() {
        return authorities;
    }

    public String getPassword() {
        return password;
    }

    public String getUsername() {
        return username;
    }

    public boolean isAccountNonExpired() {
        return false;
    }

    public boolean isAccountNonLocked() {
        return false;
    }

    public boolean isCredentialsNonExpired() {
        return false;
    }

    public boolean isEnabled() {
        return false;
    }

    public Geometry getGeometryRestriction() {
        return geometryRestriction;
    }

    public void setGeometryRestriction(Geometry geometryRestriction) {
        this.geometryRestriction = geometryRestriction;
    }

    public int getPersNr() {
        return persNr;
    }

    public void setPersNr(int persNr) {
        this.persNr = persNr;
    }

}
