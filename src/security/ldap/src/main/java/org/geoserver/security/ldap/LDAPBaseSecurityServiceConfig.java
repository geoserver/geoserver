/* (c) 2014-2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.ldap;

import org.geoserver.security.config.BaseSecurityNamedServiceConfig;

/**
 * Basic class for LDAP service related configurations.
 *
 * @author "Mauro Bartolomeoli - mauro.bartolomeoli@geo-solutions.it"
 * @author Niels Charlier
 */
public abstract class LDAPBaseSecurityServiceConfig extends BaseSecurityNamedServiceConfig {
    private static final long serialVersionUID = -6478665500954608763L;

    String serverURL;
    String groupSearchBase;
    String groupFilter; // more appropriate name would be groupNameFilter - consistency with
    // userFilter
    String groupNameAttribute;
    String allGroupsSearchFilter;
    String groupSearchFilter; // more appropriate name would be groupMembershipFilter - XStream
    // backwards compatibility
    String groupMembershipAttribute;
    String userSearchBase;
    String userFilter; // more appropriate name would be userNameFilter - XStream backwards
    // compatibility
    String userNameAttribute;
    String allUsersSearchFilter;

    Boolean useTLS;

    /** Activates hierarchical nested parent groups search */
    private boolean useNestedParentGroups = false;

    /** The max recursion level for search Hierarchical groups */
    private int maxGroupSearchLevel = 10;

    /** Pattern used for nested group filtering */
    private String nestedGroupSearchFilter = "(member={0})";

    /**
     * bind to the server before extracting groups some LDAP server require this (e.g.
     * ActiveDirectory)
     */
    Boolean bindBeforeGroupSearch;

    String adminGroup;
    String groupAdminGroup;

    /** user complete name for authenticated search of roles */
    String user;

    /** user complete password for authenticated search of roles */
    String password;

    public LDAPBaseSecurityServiceConfig() {}

    public LDAPBaseSecurityServiceConfig(LDAPBaseSecurityServiceConfig other) {
        super(other);
        serverURL = other.getServerURL();
        groupSearchBase = other.getGroupSearchBase();
        groupFilter = other.getGroupFilter();
        adminGroup = other.getAdminGroup();
        groupAdminGroup = other.getGroupAdminGroup();
        bindBeforeGroupSearch = other.isBindBeforeGroupSearch();
        userFilter = other.getUserFilter();
        useTLS = other.isUseTLS();
        user = other.getUser();
        password = other.getPassword();
    }

    public String getServerURL() {
        return serverURL;
    }

    public void setServerURL(String serverURL) {
        this.serverURL = serverURL;
    }

    public String getGroupSearchBase() {
        return groupSearchBase;
    }

    public void setGroupSearchBase(String groupSearchBase) {
        this.groupSearchBase = groupSearchBase;
    }

    public String getGroupFilter() {
        return groupFilter;
    }

    public void setGroupFilter(String groupSearchFilter) {
        this.groupFilter = groupSearchFilter;
    }

    public void setUseTLS(Boolean useTLS) {
        this.useTLS = useTLS;
    }

    public Boolean isUseTLS() {
        return useTLS;
    }

    public Boolean isBindBeforeGroupSearch() {
        return bindBeforeGroupSearch == null ? false : bindBeforeGroupSearch;
    }

    public void setBindBeforeGroupSearch(Boolean bindBeforeGroupSearch) {
        this.bindBeforeGroupSearch = bindBeforeGroupSearch;
    }

    public String getAdminGroup() {
        return adminGroup;
    }

    public void setAdminGroup(String adminGroup) {
        this.adminGroup = adminGroup;
    }

    public String getGroupAdminGroup() {
        return groupAdminGroup;
    }

    public void setGroupAdminGroup(String groupAdminGroup) {
        this.groupAdminGroup = groupAdminGroup;
    }

    public String getUserFilter() {
        return userFilter;
    }

    public void setUserFilter(String userFilter) {
        this.userFilter = userFilter;
    }

    public String getGroupNameAttribute() {
        return groupNameAttribute;
    }

    public void setGroupNameAttribute(String groupNameAttribute) {
        this.groupNameAttribute = groupNameAttribute;
    }

    public String getAllGroupsSearchFilter() {
        return allGroupsSearchFilter;
    }

    public void setAllGroupsSearchFilter(String allGroupsSearchFilter) {
        this.allGroupsSearchFilter = allGroupsSearchFilter;
    }

    public String getGroupSearchFilter() {
        return groupSearchFilter;
    }

    public void setGroupSearchFilter(String groupMembershipFilter) {
        this.groupSearchFilter = groupMembershipFilter;
    }

    public String getGroupMembershipAttribute() {
        return groupMembershipAttribute;
    }

    public void setGroupMembershipAttribute(String groupMembershipAttribute) {
        this.groupMembershipAttribute = groupMembershipAttribute;
    }

    public String getUserSearchBase() {
        return userSearchBase;
    }

    public void setUserSearchBase(String userSearchBase) {
        this.userSearchBase = userSearchBase;
    }

    public String getUserNameAttribute() {
        return userNameAttribute;
    }

    public void setUserNameAttribute(String userNameAttribute) {
        this.userNameAttribute = userNameAttribute;
    }

    public String getAllUsersSearchFilter() {
        return allUsersSearchFilter;
    }

    public void setAllUsersSearchFilter(String allUsersSearchFilter) {
        this.allUsersSearchFilter = allUsersSearchFilter;
    }

    public Boolean getUseTLS() {
        return useTLS;
    }

    public Boolean getBindBeforeGroupSearch() {
        return bindBeforeGroupSearch;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String userDn) {
        this.user = userDn;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isUseNestedParentGroups() {
        return useNestedParentGroups;
    }

    public void setUseNestedParentGroups(boolean useNestedParentGroups) {
        this.useNestedParentGroups = useNestedParentGroups;
    }

    public int getMaxGroupSearchLevel() {
        return maxGroupSearchLevel;
    }

    public void setMaxGroupSearchLevel(int maxGroupSearchLevel) {
        this.maxGroupSearchLevel = maxGroupSearchLevel;
    }

    public String getNestedGroupSearchFilter() {
        return nestedGroupSearchFilter;
    }

    public void setNestedGroupSearchFilter(String nestedGroupSearchFilter) {
        this.nestedGroupSearchFilter = nestedGroupSearchFilter;
    }
}
