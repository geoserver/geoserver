/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;
import org.springframework.util.StringUtils;

/**
 * This class is common helper for {@link AbstractUserGroupService} and {@link
 * AbstractUserGroupStore} to avoid code duplication
 *
 * @author christian
 */
public class UserGroupStoreHelper {

    public TreeMap<String, GeoServerUser> userMap = new TreeMap<String, GeoServerUser>();
    public TreeMap<String, GeoServerUserGroup> groupMap = new TreeMap<String, GeoServerUserGroup>();
    public TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>> group_userMap =
            new TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>>();
    public TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>> user_groupMap =
            new TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>>();
    public TreeMap<String, SortedSet<GeoServerUser>> propertyMap =
            new TreeMap<String, SortedSet<GeoServerUser>>();

    protected SortedSet<GeoServerUser> emptyUsers;
    protected SortedSet<GeoServerUserGroup> emptyGroups;

    public UserGroupStoreHelper() {
        emptyUsers = Collections.unmodifiableSortedSet(new TreeSet<GeoServerUser>());
        emptyGroups = Collections.unmodifiableSortedSet(new TreeSet<GeoServerUserGroup>());
    }

    public GeoServerUser getUserByUsername(String username) throws IOException {
        return userMap.get(username);
    }

    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return groupMap.get(groupname);
    }

    public SortedSet<GeoServerUser> getUsers() throws IOException {

        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(userMap.values());
        return Collections.unmodifiableSortedSet(users);
    }

    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException {

        SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
        groups.addAll(groupMap.values());
        return Collections.unmodifiableSortedSet(groups);
    }

    public SortedSet<GeoServerUserGroup> getGroupsForUser(GeoServerUser user) throws IOException {
        SortedSet<GeoServerUserGroup> groups = user_groupMap.get(user);
        if (groups == null) return emptyGroups;
        return Collections.unmodifiableSortedSet(groups);
    }

    public SortedSet<GeoServerUser> getUsersForGroup(GeoServerUserGroup group) throws IOException {
        SortedSet<GeoServerUser> users = group_userMap.get(group);
        if (users == null) return emptyUsers;
        return Collections.unmodifiableSortedSet(users);
    }

    public void clearMaps() {
        userMap.clear();
        groupMap.clear();
        user_groupMap.clear();
        group_userMap.clear();
        propertyMap.clear();
    }

    public int getUserCount() throws IOException {
        return userMap.size();
    }

    public int getGroupCount() throws IOException {
        return groupMap.size();
    }

    SortedSet<GeoServerUser> getUsersHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        SortedSet<GeoServerUser> users = propertyMap.get(propname);
        if (users == null) return emptyUsers;

        return Collections.unmodifiableSortedSet(users);
    }

    int getUserCountHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return 0;

        SortedSet<GeoServerUser> users = propertyMap.get(propname);
        if (users == null) return 0;
        else return users.size();
    }

    SortedSet<GeoServerUser> getUsersNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        SortedSet<GeoServerUser> users = getUsersHavingProperty(propname);
        SortedSet<GeoServerUser> result = new TreeSet<GeoServerUser>();
        result.addAll(userMap.values());
        result.removeAll(users);
        return Collections.unmodifiableSortedSet(result);
    }

    int getUserCountNotHavingProperty(String propname) throws IOException {
        if (StringUtils.hasLength(propname) == false) return userMap.size();

        return userMap.size() - getUserCountHavingProperty(propname);
    }

    SortedSet<GeoServerUser> getUsersHavingPropertyValue(String propname, String propvalue)
            throws IOException {
        if (StringUtils.hasLength(propname) == false) return emptyUsers;

        if (StringUtils.hasLength(propvalue) == false) return emptyUsers;

        SortedSet<GeoServerUser> result = new TreeSet<GeoServerUser>();
        for (GeoServerUser user : getUsersHavingProperty(propname)) {
            if (propvalue.equals(user.getProperties().getProperty(propname))) result.add(user);
        }
        return Collections.unmodifiableSortedSet(result);
    }

    int getUserCountHavingPropertyValue(String propname, String propvalue) throws IOException {
        int count = 0;
        if (StringUtils.hasLength(propname) == false) return count;

        if (StringUtils.hasLength(propvalue) == false) return count;

        for (GeoServerUser user : getUsersHavingProperty(propname)) {
            if (propvalue.equals(user.getProperties().getProperty(propname))) count++;
        }
        return count;
    }
}
