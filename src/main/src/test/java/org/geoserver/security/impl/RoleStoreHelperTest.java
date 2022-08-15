/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2022 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */ package org.geoserver.security.impl;

import java.util.Iterator;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import org.junit.Test;

public class RoleStoreHelperTest {

    @Test
    public void ensureConcurrentAccessToRolesDoesNotCauseConcurrencyException() {
        // Given
        RoleStoreHelper roleStoreHelper = new RoleStoreHelper();
        roleStoreHelper.roleMap.put("key1", new GeoServerRole("role1"));
        roleStoreHelper.roleMap.put("key2", new GeoServerRole("role2"));
        roleStoreHelper.roleMap.put("key3", new GeoServerRole("role3"));
        roleStoreHelper.roleMap.put("key4", new GeoServerRole("role4"));
        roleStoreHelper.roleMap.put("key5", new GeoServerRole("role5"));

        Iterator<Map.Entry<String, GeoServerRole>> iterator =
                roleStoreHelper.roleMap.entrySet().iterator();

        // When
        roleStoreHelper.roleMap.put("key6", new GeoServerRole("role6"));
        while (iterator.hasNext()) {
            iterator.next();
        }
    }

    @Test
    public void ensureConcurrentAccessToGroupRolesDoesNotCauseConcurrencyException() {
        // Given
        RoleStoreHelper roleStoreHelper = new RoleStoreHelper();
        roleStoreHelper.group_roleMap.put("key1", new TreeSet<>());
        roleStoreHelper.group_roleMap.put("key2", new TreeSet<>());
        roleStoreHelper.group_roleMap.put("key3", new TreeSet<>());
        roleStoreHelper.group_roleMap.put("key4", new TreeSet<>());
        roleStoreHelper.group_roleMap.put("key5", new TreeSet<>());

        Iterator<Map.Entry<String, SortedSet<GeoServerRole>>> iterator =
                roleStoreHelper.group_roleMap.entrySet().iterator();

        // When
        roleStoreHelper.group_roleMap.put("key6", new TreeSet<>());
        while (iterator.hasNext()) {
            iterator.next();
        }
    }

    @Test
    public void ensureConcurrentAccessToUserRolesDoesNotCauseConcurrencyException() {
        // Given
        RoleStoreHelper roleStoreHelper = new RoleStoreHelper();
        roleStoreHelper.user_roleMap.put("key1", new TreeSet<>());
        roleStoreHelper.user_roleMap.put("key2", new TreeSet<>());
        roleStoreHelper.user_roleMap.put("key3", new TreeSet<>());
        roleStoreHelper.user_roleMap.put("key4", new TreeSet<>());
        roleStoreHelper.user_roleMap.put("key5", new TreeSet<>());

        Iterator<Map.Entry<String, SortedSet<GeoServerRole>>> iterator =
                roleStoreHelper.user_roleMap.entrySet().iterator();

        // When
        roleStoreHelper.user_roleMap.put("key6", new TreeSet<>());
        while (iterator.hasNext()) {
            iterator.next();
        }
    }
}
