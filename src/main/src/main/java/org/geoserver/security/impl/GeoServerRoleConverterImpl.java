/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import org.geoserver.security.GeoServerRoleConverter;
import org.springframework.security.core.GrantedAuthority;

/**
 * Converts {@link GeoServerRole} and collections of roles into a string representation and vice
 * versa.
 *
 * <p>Default format by example:
 *
 * <p>role1;role2;role3
 *
 * <p>Default format with role parameters by example
 *
 * <p>role1(param1=value1,param2=value2);role2(param3=value3);role3
 *
 * @author mcr
 */
public class GeoServerRoleConverterImpl implements GeoServerRoleConverter {
    private String roleDelimiterString = ";";
    private String roleParameterDelimiterString = ",";
    private String roleParameterStartString = "(";
    private String roleParameterEndString = ")";
    private String roleParameterAssignmentString = "=";
    private boolean checked = false;

    public String getRoleDelimiterString() {
        return roleDelimiterString;
    }

    public void setRoleDelimiterString(String roleDelimiterString) {
        this.roleDelimiterString = roleDelimiterString;
        checked = false;
    }

    public String getRoleParameterDelimiterString() {
        return roleParameterDelimiterString;
    }

    public void setRoleParameterDelimiterString(String roleParameterDelimiterString) {
        this.roleParameterDelimiterString = roleParameterDelimiterString;
        checked = false;
    }

    public String getRoleParameterStartString() {
        return roleParameterStartString;
    }

    public void setRoleParameterStartString(String roleParameterStartString) {
        this.roleParameterStartString = roleParameterStartString;
        checked = false;
    }

    public String getRoleParameterEndString() {
        return roleParameterEndString;
    }

    public void setRoleParameterEndString(String roleParameterEndString) {
        this.roleParameterEndString = roleParameterEndString;
        checked = false;
    }

    public String getRoleParameterAssignmentString() {
        return roleParameterAssignmentString;
    }

    public void setRoleParameterAssignmentString(String roleParameterAssignmentString) {
        this.roleParameterAssignmentString = roleParameterAssignmentString;
        checked = false;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.GeoServerRoleConverter#convertRoleToString(org.geoserver.security.impl.GeoServerRole)
     */
    @Override
    public String convertRoleToString(GeoServerRole role) {

        checkDelimiters();

        StringBuffer buff = new StringBuffer();
        writeRole(buff, role);
        return buff.toString();
    }

    /** internal helper method */
    protected void writeRole(StringBuffer buff, GeoServerRole role) {
        buff.append(role.getAuthority());
        Properties props = role.getProperties();

        if (props == null || props.isEmpty()) return;

        buff.append(getRoleParameterStartString());
        boolean firstTime = true;
        for (Entry<Object, Object> entry : props.entrySet()) {
            if (firstTime == true) firstTime = false;
            else buff.append(getRoleParameterDelimiterString());
            buff.append(entry.getKey()).append(getRoleParameterAssignmentString());
            buff.append(entry.getValue() == null ? "" : entry.getValue());
        }
        buff.append(getRoleParameterEndString());
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.GeoServerRoleConverter#convertRolesToString(java.util.Collection)
     */
    @Override
    public String convertRolesToString(Collection<? extends GrantedAuthority> roles) {

        checkDelimiters();

        StringBuffer buff = new StringBuffer();
        boolean firstTime = true;
        for (GrantedAuthority role : roles) {
            if (firstTime == true) firstTime = false;
            else buff.append(getRoleDelimiterString());

            writeRole(buff, (GeoServerRole) role);
        }
        return buff.toString();
    }

    /** internal helper method to split strings based on delimiter strings */
    protected List<String> splitString(String theString, String delim) {

        List<String> result = new ArrayList<String>();
        int startIndex = 0;
        while (true) {
            int index = theString.indexOf(delim, startIndex);
            if (index == -1) {
                result.add(theString.substring(startIndex));
                break;
            } else {
                result.add(theString.substring(startIndex, index));
                startIndex = index + delim.length();
            }
        }
        return result;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.GeoServerRoleConverter#convertRolesFromString(java.lang.String, java.lang.String)
     */
    @Override
    public Collection<GeoServerRole> convertRolesFromString(String rolesString, String userName) {

        checkDelimiters();
        List<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        List<String> working = splitString(rolesString, getRoleDelimiterString());
        for (String roleString : working) {
            GeoServerRole role = convertRoleFromString(roleString, userName);
            if (role != null) roles.add(role);
        }
        return roles;
    }

    /* (non-Javadoc)
     * @see org.geoserver.security.impl.GeoServerRoleConverter#convertRoleFromString(java.lang.String, java.lang.String)
     */
    @Override
    public GeoServerRole convertRoleFromString(String roleString, String userName) {

        if (roleString == null) return null;
        roleString = roleString.trim();
        if (roleString.isEmpty()) return null;

        checkDelimiters();

        List<String> working = splitString(roleString.trim(), getRoleParameterStartString());
        GeoServerRole result = new GeoServerRole(working.get(0));

        if (working.size() == 1) {
            return result;
        }

        // we have role parameters
        result.setUserName(userName);

        if (working.get(1).endsWith(getRoleParameterEndString()) == false)
            throw createExcpetion(roleString + " does not end with " + getRoleParameterEndString());

        int index = working.get(1).lastIndexOf(getRoleParameterEndString());
        String roleParamString = working.get(1).substring(0, index).trim();
        working = splitString(roleParamString, getRoleParameterDelimiterString());
        for (String kvp : working) {
            List<String> tmp = splitString(kvp.trim(), getRoleParameterAssignmentString());
            if (tmp.size() != 2)
                throw createExcpetion(roleString + " Invalid role string:  " + roleString);
            result.getProperties().put(tmp.get(0).trim(), tmp.get(1).trim());
        }
        return result;
    }

    /** internal method the check for proper delimter strings */
    protected void checkDelimiters() {
        if (checked) return;

        if (roleDelimiterString == null || roleDelimiterString.isEmpty())
            throw createExcpetion("Missing roleDelimiterString");
        if (roleParameterDelimiterString == null || roleParameterDelimiterString.isEmpty())
            throw createExcpetion("Missing roleParameterDelimiterString");
        if (roleParameterStartString == null || roleParameterStartString.isEmpty())
            throw createExcpetion("Missing roleParameterStartString");
        if (roleParameterEndString == null || roleParameterEndString.isEmpty())
            throw createExcpetion("Missing roleParameterEndString");
        if (roleParameterAssignmentString == null || roleParameterAssignmentString.isEmpty())
            throw createExcpetion("Missing roleParameterAssignmentString");

        Set<String> set = new HashSet<String>();
        set.add(roleDelimiterString);
        set.add(roleParameterDelimiterString);
        set.add(roleParameterStartString);
        set.add(roleParameterEndString);
        set.add(roleParameterAssignmentString);

        if (set.size() < 5) throw createExcpetion("Delimiters must be unique");
        checked = true;
    }

    protected RuntimeException createExcpetion(String msg) {
        return new RuntimeException(msg);
    }
}
