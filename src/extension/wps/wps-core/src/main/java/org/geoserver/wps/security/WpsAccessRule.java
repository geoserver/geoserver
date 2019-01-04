/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.security;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.apache.commons.lang3.builder.HashCodeBuilder;

/**
 * Represents a data access rule: identifies a workspace, a layer, an access mode, and the set of
 * roles that are allowed to access it
 *
 * <p>Mind, two rules are considered equal if the address the same data, if you need full
 * comparison, use {@link #equalsExact(WpsAccessRule)}
 */
@SuppressWarnings("serial")
public class WpsAccessRule implements Comparable<WpsAccessRule>, Serializable {

    /** Any layer, or any workspace, or any role */
    public static final String ANY = "*";

    public static WpsAccessRule EXECUTE_ALL = new WpsAccessRule(ANY, ANY, ANY);

    private String groupName;

    private String wpsName;

    private Set<String> roles;

    /** Builds a new rule */
    public WpsAccessRule(String groupName, String wpsName, Set<String> roles) {
        super();
        this.groupName = groupName;
        this.wpsName = wpsName;
        if (roles == null) this.roles = new HashSet<String>();
        else this.roles = new HashSet<String>(roles);
    }

    /** Builds a new rule */
    public WpsAccessRule(String groupName, String wpsName, String... roles) {
        this(groupName, wpsName, roles == null ? null : new HashSet<String>(Arrays.asList(roles)));
    }

    /** Copy constructor */
    public WpsAccessRule(WpsAccessRule other) {
        this.groupName = other.groupName;
        this.wpsName = other.wpsName;
        this.roles = new HashSet<String>(other.roles);
    }

    /** Builds the default rule: *.*.r=* */
    public WpsAccessRule() {
        this(ANY, ANY);
    }

    public String getGroupName() {
        return groupName;
    }

    public void setGroupName(String groupName) {
        this.groupName = groupName;
    }

    public String getWpsName() {
        return wpsName;
    }

    public void setWpsName(String wpsName) {
        this.wpsName = wpsName;
    }

    public void setRoles(Set<String> roles) {
        this.roles = roles;
    }

    public Set<String> getRoles() {
        return roles;
    }

    /** Returns the key for the current rule. No other rule should have the same */
    public String getKey() {
        return groupName + "." + wpsName;
    }

    /** Returns the list of roles as a comma separated string for this rule */
    public String getValue() {
        if (roles.isEmpty()) {
            return WpsAccessRule.ANY;
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
     * Comparison implemented so that generic rules get first, specific one are compared by name,
     * and if anything else is equal, read comes before write
     */
    public int compareTo(WpsAccessRule other) {
        int compareGroup = compareCatalogItems(groupName, other.groupName);
        if (compareGroup != 0) return compareGroup;

        int compareName = compareCatalogItems(wpsName, other.wpsName);
        return compareName;
    }

    /** Equality based on ws/layer/mode only */
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof WpsAccessRule)) return false;

        return 0 == compareTo((WpsAccessRule) obj);
    }

    /** Full equality, roles included */
    public boolean equalsExact(WpsAccessRule obj) {
        if (0 != compareTo(obj)) return false;
        else return roles.equals(obj.roles);
    }

    /** Hashcode based on wfs/layer/mode only */
    @Override
    public int hashCode() {
        return new HashCodeBuilder().append(groupName).append(wpsName).toHashCode();
    }

    /** Generic string comparison that considers the use of {@link #ANY} */
    public int compareCatalogItems(String item, String otherItem) {
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
