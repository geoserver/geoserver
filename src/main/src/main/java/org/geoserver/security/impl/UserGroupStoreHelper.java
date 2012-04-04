/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class is common helper for
 * {@link AbstractUserGroupService} and {@link AbstractUserGroupStore} 
 * to avoid code duplication
 * 
 * @author christian
 *
 */
public class UserGroupStoreHelper{
    public TreeMap<String, GeoServerUser> userMap = new TreeMap<String,GeoServerUser>();
    public TreeMap<String, GeoServerUserGroup>groupMap = new TreeMap<String,GeoServerUserGroup>();
    public TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>>group_userMap =
        new TreeMap<GeoServerUserGroup, SortedSet<GeoServerUser>>();
    public TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>> user_groupMap =
        new TreeMap<GeoServerUser, SortedSet<GeoServerUserGroup>>();     

    
    public GeoServerUser getUserByUsername(String username) throws IOException {
        return  userMap.get(username);

    }

    public GeoServerUserGroup getGroupByGroupname(String groupname) throws IOException {
        return  groupMap.get(groupname);
    }


    public SortedSet<GeoServerUser> getUsers() throws IOException{
        
        SortedSet<GeoServerUser> users = new TreeSet<GeoServerUser>();
        users.addAll(userMap.values());
        return Collections.unmodifiableSortedSet(users);
    }
    
    public SortedSet<GeoServerUserGroup> getUserGroups() throws IOException{
        
        SortedSet<GeoServerUserGroup> groups = new TreeSet<GeoServerUserGroup>();
        groups.addAll(groupMap.values());
        return Collections.unmodifiableSortedSet(groups);
    }
    
    public  SortedSet<GeoServerUserGroup> getGroupsForUser (GeoServerUser user) throws IOException{        
        SortedSet<GeoServerUserGroup> groups = user_groupMap.get(user);
        if  (groups==null) 
            groups =  new TreeSet<GeoServerUserGroup>();
        return Collections.unmodifiableSortedSet(groups);
    }
    
    
    public  SortedSet<GeoServerUser> getUsersForGroup (GeoServerUserGroup group) throws IOException{
        SortedSet<GeoServerUser> users = group_userMap.get(group);
        if  (users==null) 
            users= new TreeSet<GeoServerUser>();
        return Collections.unmodifiableSortedSet(users);
    }
    
    public void clearMaps() {
        userMap.clear();
        groupMap.clear();
        user_groupMap.clear();
        group_userMap.clear();
    }

    public int getUserCount() throws IOException{
        return userMap.size();
    }

    public int getGroupCount() throws IOException{
        return groupMap.size();
    }


}
