/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.Properties;

import org.springframework.security.core.GrantedAuthority;

/**
 * Extends {@link GrantedAuthority} and represents an 
 * anonymous role 
 * 
 *  If a user name is set, the role is personalized
 * 
 * Example: the role ROLE_EMPLOYEE could have a role 
 * parameter EPLOYEE_NUMBER

 * 
 * 
 * @author christian
 *
 */
public class GeoServerRole  implements GrantedAuthority,  Comparable<GeoServerRole>{

    /**
     * Pre-defined role assigned to adminstrator.
     */
    public final static GeoServerRole ADMIN_ROLE = new GeoServerRole("ROLE_ADMINISTRATOR");

    /**
     * Pre-defined role assigned to group adminstrators.
     */
    public final static GeoServerRole GROUP_ADMIN_ROLE = new GeoServerRole("ROLE_GROUP_ADMIN");

    /**
     * Pre-defined role assigned to any authenticated user. 
     */
    public final static GeoServerRole AUTHENTICATED_ROLE = new GeoServerRole("ROLE_AUTHENTICATED");

    /**
     * Pre-defined wildcard role. 
     */
    public final static GeoServerRole ANY_ROLE = new GeoServerRole("*");

        
    /**
     * Predefined anonymous role
     */
    public final static GeoServerRole ANONYMOUS_ROLE = new GeoServerRole("ROLE_ANONYMOUS");
    
    
    /**
     * Geoserver system roles
     */
    public final static GeoServerRole[] SystemRoles = new GeoServerRole[] 
            {ADMIN_ROLE,GROUP_ADMIN_ROLE,AUTHENTICATED_ROLE,ANONYMOUS_ROLE };
    
    /**
     * Mappable system roles
     */
    public final static GeoServerRole[] MappedRoles = new GeoServerRole[] {ADMIN_ROLE,GROUP_ADMIN_ROLE };
    
    
    /**
     * Roles which cannot be assigned to a user or a group
     */
    public final static GeoServerRole[] UnAssignableRoles = new GeoServerRole[] { AUTHENTICATED_ROLE,ANONYMOUS_ROLE };
    

    private static final long serialVersionUID = 1L;

    protected String userName;
    protected Properties properties;
    protected String role;


    public GeoServerRole(String role) {
        this.role=role;        
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
    
    public boolean isAnonymous() {
        return getUserName()==null;
    }

    /**
     * Generic mechanism to store 
     * additional information (role paramaters)
     * 
     * examples: a user with the role ROLE_EMPLOYEE
     * could have a role parameter EMPLOYEE_NUMBER
     * To be filled by the backend store
     * 
     * @return 
     */
    public Properties getProperties() {
        if (properties==null)
            properties = new Properties();
        return properties;    
    }

    public int compareTo(GeoServerRole o) {
        if (o==null) return 1;
        if (getAuthority().equals(o.getAuthority())) {
            if (getUserName()==null && o.getUserName()==null)
                return 0;
            if (getUserName()==null) 
                return -1;
            if (o.getUserName()==null) 
                return 1;
            return getUserName().compareTo(o.getUserName());
        }
        return getAuthority().compareTo(o.getAuthority());        
    }

    public boolean equals(Object obj) {
        if (obj == null) return false;
        
        if (obj instanceof String && getUserName()==null) {
            return equalsWithoutUserName(obj);
        }

        if (obj instanceof GrantedAuthority && getUserName()==null) {
            if (obj instanceof GeoServerRole ==false)
                return equalsWithoutUserName(obj);
        }

        if (obj instanceof GeoServerRole) {
            return compareTo((GeoServerRole) obj)==0;
        }
        return false;
    }
    
    
    public boolean equalsWithoutUserName(Object obj) {
        if (obj instanceof String) {
            return obj.equals(this.role);
        }
        return this.role.equals(((GrantedAuthority) obj).getAuthority());
    }

    public int hashCode() {
        int hash = getAuthority().hashCode();
        if (getUserName()!=null)
            hash+=getUserName().hashCode();
        return hash;
            
    }

    public String toString() {
        if (getUserName()!=null) {
            StringBuffer buff = new StringBuffer(role);
            buff.append(" for user ").append(getUserName());
            return buff.toString();
        } else
          return role;
    }

    @Override
    public String getAuthority() {
        return role;
    }

}
