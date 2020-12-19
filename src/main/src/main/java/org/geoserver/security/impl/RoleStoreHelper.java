/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.impl;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * This class is common helper for {@link AbstractRoleService} and {@link AbstractRoleStore} to
 * avoid code duplication
 *
 * @author christian
 */
public class RoleStoreHelper {
    public TreeMap<String, GeoServerRole> roleMap = new TreeMap<>();
    public TreeMap<String, SortedSet<GeoServerRole>> group_roleMap = new TreeMap<>();
    public TreeMap<String, SortedSet<GeoServerRole>> user_roleMap = new TreeMap<>();
    public HashMap<GeoServerRole, GeoServerRole> role_parentMap = new HashMap<>();

    public void clearMaps() {
        roleMap.clear();
        role_parentMap.clear();
        group_roleMap.clear();
        user_roleMap.clear();
    }

    public Map<String, String> getParentMappings() throws IOException {
        Map<String, String> parentMap = new HashMap<>();
        for (GeoServerRole role : roleMap.values()) {
            GeoServerRole parentRole = role_parentMap.get(role);
            parentMap.put(
                    role.getAuthority(), parentRole == null ? null : parentRole.getAuthority());
        }
        return Collections.unmodifiableMap(parentMap);
    }

    public SortedSet<GeoServerRole> getRoles() throws IOException {
        SortedSet<GeoServerRole> result = new TreeSet<>();
        result.addAll(roleMap.values());
        return Collections.unmodifiableSortedSet(result);
    }

    public SortedSet<GeoServerRole> getRolesForUser(String username) throws IOException {
        SortedSet<GeoServerRole> roles = user_roleMap.get(username);
        if (roles == null) roles = new TreeSet<>();
        return Collections.unmodifiableSortedSet(roles);
    }

    public SortedSet<GeoServerRole> getRolesForGroup(String groupname) throws IOException {
        SortedSet<GeoServerRole> roles = group_roleMap.get(groupname);
        if (roles == null) roles = new TreeSet<>();
        return Collections.unmodifiableSortedSet(roles);
    }

    public GeoServerRole getParentRole(GeoServerRole role) throws IOException {
        return role_parentMap.get(role);
    }

    public GeoServerRole getRoleByName(String role) throws IOException {
        return roleMap.get(role);
    }

    public SortedSet<String> getGroupNamesForRole(GeoServerRole role) throws IOException {
        SortedSet<String> result = new TreeSet<>();
        for (Entry<String, SortedSet<GeoServerRole>> entry : group_roleMap.entrySet()) {
            if (entry.getValue().contains(role)) result.add(entry.getKey());
        }
        return Collections.unmodifiableSortedSet(result);
    }

    public SortedSet<String> getUserNamesForRole(GeoServerRole role) throws IOException {
        SortedSet<String> result = new TreeSet<>();
        for (Entry<String, SortedSet<GeoServerRole>> entry : user_roleMap.entrySet()) {
            if (entry.getValue().contains(role)) result.add(entry.getKey());
        }
        return Collections.unmodifiableSortedSet(result);
    }

    public int getRoleCount() throws IOException {
        return roleMap.size();
    }
}
