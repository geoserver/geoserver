/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a service access rule: identifies a service, a method, and the set of roles that are
 * allowed to access it
 */
@SuppressWarnings("serial")
public class ServiceAccessRule implements Comparable<ServiceAccessRule>, Serializable {

    /** Any service or method */
    public static final String ANY = "*";

    public static ServiceAccessRule READ_ALL = new ServiceAccessRule(ANY, ANY);

    public static ServiceAccessRule WRITE_ALL = new ServiceAccessRule(ANY, ANY);

    String service;

    String method;

    Set<String> roles;

    /** Builds a new rule */
    public ServiceAccessRule(String service, String method, Set<String> roles) {
        super();
        this.service = service;
        this.method = method;
        if (roles == null) this.roles = new HashSet<String>();
        else this.roles = new HashSet<String>(roles);
    }

    /** Builds a new rule */
    public ServiceAccessRule(String service, String method, String... roles) {
        this(service, method, roles == null ? null : new HashSet<String>(Arrays.asList(roles)));
    }

    /** Copy constructor */
    public ServiceAccessRule(ServiceAccessRule other) {
        this.service = other.service;
        this.method = other.method;
        this.roles = new HashSet<String>(other.roles);
    }

    /** Builds the default rule: *.*.r=* */
    public ServiceAccessRule() {
        this(ANY, ANY);
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String layer) {
        this.method = layer;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /** Returns the key for the current rule. No other rule should have the same */
    public String getKey() {
        return service + "." + method;
    }

    /** Returns the list of roles as a comma separated string for this rule */
    public String getValue() {
        if (roles.isEmpty()) {
            return ServiceAccessRule.ANY;
        } else {
            StringBuffer sb = new StringBuffer();
            for (String role : roles) {
                sb.append(role);
                sb.append(",");
            }
            sb.setLength(sb.length() - 1);
            return sb.toString();
        }
    }

    /**
     * Comparison implemented so that generic rules get first, specific one are compared by service
     * and method
     */
    public int compareTo(ServiceAccessRule other) {
        int compareService = compareServiceItems(service, other.service);
        if (compareService != 0) return compareService;

        return compareServiceItems(method, other.method);
    }

    /** Equality based on service/method only */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ServiceAccessRule)) return false;

        return 0 == compareTo((ServiceAccessRule) obj);
    }

    /** Hashcode based on service/method only */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(service).append(method).toHashCode();
    }

    /** Generic string comparison that considers the use of {@link #ANY} */
    public int compareServiceItems(String item, String otherItem) {
        if (item.equals(otherItem)) return 0;
        else if (ANY.equals(item)) return -1;
        else if (ANY.equals(otherItem)) return 1;
        else return item.compareTo(otherItem);
    }

    @Override
    public String toString() {
        return getKey() + "=" + getValue();
    }
}
