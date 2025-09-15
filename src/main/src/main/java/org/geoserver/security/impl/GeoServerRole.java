/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.Serial;
import java.util.Properties;
import org.geotools.util.SuppressFBWarnings;
import org.springframework.security.core.GrantedAuthority;

/**
 * Extends {@link GrantedAuthority} and represents an anonymous role
 *
 * <p>If a user name is set, the role is personalized
 *
 * <p>Example: the role ROLE_EMPLOYEE could have a role parameter EPLOYEE_NUMBER
 *
 * @author christian
 */
public class GeoServerRole implements GrantedAuthority, Comparable<GeoServerRole> {

    /** Pre-defined role assigned to adminstrator. */
    public static final GeoServerRole ADMIN_ROLE = new GeoServerRole("ROLE_ADMINISTRATOR");

    /** Pre-defined role assigned to group adminstrators. */
    public static final GeoServerRole GROUP_ADMIN_ROLE = new GeoServerRole("ROLE_GROUP_ADMIN");

    /** Pre-defined role assigned to any authenticated user. */
    public static final GeoServerRole AUTHENTICATED_ROLE = new GeoServerRole("ROLE_AUTHENTICATED");

    /** Pre-defined wildcard role. */
    public static final GeoServerRole ANY_ROLE = new GeoServerRole("*");

    /** Predefined anonymous role */
    public static final GeoServerRole ANONYMOUS_ROLE = new GeoServerRole("ROLE_ANONYMOUS");

    /** Geoserver system roles */
    public static final GeoServerRole[] SystemRoles = {ADMIN_ROLE, GROUP_ADMIN_ROLE, AUTHENTICATED_ROLE, ANONYMOUS_ROLE
    };

    /** Mappable system roles */
    public static final GeoServerRole[] MappedRoles = {ADMIN_ROLE, GROUP_ADMIN_ROLE};

    /** Roles which cannot be assigned to a user or a group */
    public static final GeoServerRole[] UnAssignableRoles = {AUTHENTICATED_ROLE, ANONYMOUS_ROLE};

    @Serial
    private static final long serialVersionUID = 1L;

    protected String userName;
    protected Properties properties;
    protected String role;

    public GeoServerRole(String role) {
        this.role = role;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }

    public boolean isAnonymous() {
        return getUserName() == null;
    }

    /**
     * Generic mechanism to store additional information (role paramaters)
     *
     * <p>examples: a user with the role ROLE_EMPLOYEE could have a role parameter EMPLOYEE_NUMBER To be filled by the
     * backend store
     */
    public Properties getProperties() {
        if (properties == null) properties = new Properties();
        return properties;
    }

    @Override
    public int compareTo(GeoServerRole o) {
        if (o == null) return 1;
        if (getAuthority().equals(o.getAuthority())) {
            if (getUserName() == null && o.getUserName() == null) return 0;
            if (getUserName() == null) return -1;
            if (o.getUserName() == null) return 1;
            return getUserName().compareTo(o.getUserName());
        }
        return getAuthority().compareTo(o.getAuthority());
    }

    // not sure why the equals would compare against types that are not a GeoServerRole
    // suppressing for the moment...
    @Override
    @SuppressFBWarnings("EQ_CHECK_FOR_OPERAND_NOT_COMPATIBLE_WITH_THIS")
    public boolean equals(Object obj) {
        if (obj == null) return false;

        if (obj instanceof String && getUserName() == null) {
            return equalsWithoutUserName(obj);
        }

        if (obj instanceof GrantedAuthority && getUserName() == null) {
            if (obj instanceof GeoServerRole == false) return equalsWithoutUserName(obj);
        }

        if (obj instanceof GeoServerRole serverRole) {
            return compareTo(serverRole) == 0;
        }
        return false;
    }

    public boolean equalsWithoutUserName(Object obj) {
        if (obj instanceof String) {
            return obj.equals(this.role);
        }
        return this.role.equals(((GrantedAuthority) obj).getAuthority());
    }

    @Override
    public int hashCode() {
        int hash = getAuthority().hashCode();
        if (getUserName() != null) hash += getUserName().hashCode();
        return hash;
    }

    @Override
    public String toString() {
        if (getUserName() != null) {
            StringBuffer buff = new StringBuffer(role);
            buff.append(" for user ").append(getUserName());
            return buff.toString();
        } else return role;
    }

    @Override
    public String getAuthority() {
        return role;
    }
}
