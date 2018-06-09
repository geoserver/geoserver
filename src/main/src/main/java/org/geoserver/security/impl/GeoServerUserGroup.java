/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.Serializable;

/**
 * Implementation of {@link UserGroup}
 *
 * @author christian
 */
public class GeoServerUserGroup implements Comparable<GeoServerUserGroup>, Serializable {

    private static final long serialVersionUID = 1L;

    private final String groupname;
    private boolean enabled;

    public GeoServerUserGroup(String name) {
        this.groupname = name;
        this.enabled = true;
    }

    public GeoServerUserGroup(GeoServerUserGroup other) {
        this.groupname = other.getGroupname();
        this.enabled = other.isEnabled();
    }

    public String getGroupname() {
        return groupname;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public GeoServerUserGroup copy() {
        return new GeoServerUserGroup(this);
    }

    @Override
    public boolean equals(Object rhs) {
        if (rhs instanceof GeoServerUserGroup) {
            return getGroupname().equals(((GeoServerUserGroup) rhs).getGroupname());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return getGroupname().hashCode();
    }

    public int compareTo(GeoServerUserGroup o) {
        if (o == null) return 1;
        return getGroupname().compareTo(o.getGroupname());
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("Groupname: ").append(getGroupname());
        sb.append(" Enabled: ").append(this.enabled);
        return sb.toString();
    }
}
