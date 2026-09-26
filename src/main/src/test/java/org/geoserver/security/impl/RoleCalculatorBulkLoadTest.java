/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;
import org.geoserver.security.BulkLoadableRoleService;
import org.geoserver.security.GeoServerRoleService;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.event.RoleLoadedListener;
import org.junit.Test;

/**
 * Verifies that {@link RoleCalculator#addInheritedRoles} uses the bulk-load path when the role service implements
 * {@link BulkLoadableRoleService}, avoiding per-role getParentRole() calls.
 */
public class RoleCalculatorBulkLoadTest {

    @Test
    public void testBulkPathAvoidsPerRoleQueries() throws IOException {
        // 10 child roles each with a parent
        AtomicInteger getParentRoleCalls = new AtomicInteger(0);
        Map<String, GeoServerRole> roles = new HashMap<>();
        Map<String, String> parentMappings = new HashMap<>();

        for (int i = 1; i <= 10; i++) {
            roles.put("CHILD_" + i, new GeoServerRole("CHILD_" + i));
            roles.put("PARENT_" + i, new GeoServerRole("PARENT_" + i));
            parentMappings.put("CHILD_" + i, "PARENT_" + i);
            parentMappings.put("PARENT_" + i, null);
        }

        BulkCountingService service = new BulkCountingService(roles, parentMappings, getParentRoleCalls);
        RoleCalculator calc = new RoleCalculator(service);

        Set<GeoServerRole> assigned = new HashSet<>();
        for (int i = 1; i <= 10; i++) {
            assigned.add(roles.get("CHILD_" + i));
        }

        calc.addInheritedRoles(assigned);

        // Bulk path should not call getParentRole at all
        assertEquals("Bulk path should bypass getParentRole()", 0, getParentRoleCalls.get());
        assertEquals("10 children + 10 parents = 20", 20, assigned.size());
    }

    @Test
    public void testBulkPathResolvesDeepChain() throws IOException {
        AtomicInteger getParentRoleCalls = new AtomicInteger(0);
        Map<String, GeoServerRole> roles = new HashMap<>();
        Map<String, String> parentMappings = new HashMap<>();

        // Chain: A -> B -> C -> D -> E
        String[] chain = {"A", "B", "C", "D", "E"};
        for (String name : chain) {
            roles.put(name, new GeoServerRole(name));
        }
        parentMappings.put("A", "B");
        parentMappings.put("B", "C");
        parentMappings.put("C", "D");
        parentMappings.put("D", "E");
        parentMappings.put("E", null);

        BulkCountingService service = new BulkCountingService(roles, parentMappings, getParentRoleCalls);
        RoleCalculator calc = new RoleCalculator(service);

        Set<GeoServerRole> assigned = new HashSet<>();
        assigned.add(roles.get("A"));

        calc.addInheritedRoles(assigned);

        assertEquals(0, getParentRoleCalls.get());
        assertEquals(5, assigned.size());
        for (String name : chain) {
            assertTrue(assigned.contains(roles.get(name)));
        }
    }

    @Test
    public void testBulkPathHandlesCycle() throws IOException {
        AtomicInteger getParentRoleCalls = new AtomicInteger(0);
        Map<String, GeoServerRole> roles = new HashMap<>();
        Map<String, String> parentMappings = new HashMap<>();

        roles.put("X", new GeoServerRole("X"));
        roles.put("Y", new GeoServerRole("Y"));
        parentMappings.put("X", "Y");
        parentMappings.put("Y", "X");

        BulkCountingService service = new BulkCountingService(roles, parentMappings, getParentRoleCalls);
        RoleCalculator calc = new RoleCalculator(service);

        Set<GeoServerRole> assigned = new HashSet<>();
        assigned.add(roles.get("X"));

        calc.addInheritedRoles(assigned);

        assertEquals(0, getParentRoleCalls.get());
        assertEquals(2, assigned.size());
    }

    @Test
    public void testBulkPathPropagatesProperties() throws IOException {
        AtomicInteger getParentRoleCalls = new AtomicInteger(0);
        Map<String, GeoServerRole> roles = new HashMap<>();
        Map<String, String> parentMappings = new HashMap<>();

        roles.put("CHILD", new GeoServerRole("CHILD"));
        roles.put("PARENT", new GeoServerRole("PARENT"));
        parentMappings.put("CHILD", "PARENT");
        parentMappings.put("PARENT", null);

        Map<String, Properties> roleProperties = new HashMap<>();
        Properties parentProps = new Properties();
        parentProps.put("department", "engineering");
        parentProps.put("level", "2");
        roleProperties.put("PARENT", parentProps);

        BulkCountingService service =
                new BulkCountingService(roles, parentMappings, getParentRoleCalls, roleProperties);
        RoleCalculator calc = new RoleCalculator(service);

        Set<GeoServerRole> assigned = new HashSet<>();
        assigned.add(roles.get("CHILD"));

        calc.addInheritedRoles(assigned);

        assertEquals(0, getParentRoleCalls.get());
        assertEquals(2, assigned.size());

        GeoServerRole resolvedParent = assigned.stream()
                .filter(r -> r.getAuthority().equals("PARENT"))
                .findFirst()
                .orElse(null);
        assertNotNull("Parent role should be in resolved set", resolvedParent);
        assertEquals("engineering", resolvedParent.getProperties().get("department"));
        assertEquals("2", resolvedParent.getProperties().get("level"));
    }

    /** Role service implementing BulkLoadableRoleService that counts getParentRole() calls. */
    private static class BulkCountingService implements GeoServerRoleService, BulkLoadableRoleService {

        private final Map<String, GeoServerRole> roles;
        private final Map<String, String> parentMappings;
        private final AtomicInteger callCount;
        private final Map<String, Properties> roleProperties;

        BulkCountingService(
                Map<String, GeoServerRole> roles, Map<String, String> parentMappings, AtomicInteger callCount) {
            this(roles, parentMappings, callCount, new HashMap<>());
        }

        BulkCountingService(
                Map<String, GeoServerRole> roles,
                Map<String, String> parentMappings,
                AtomicInteger callCount,
                Map<String, Properties> roleProperties) {
            this.roles = roles;
            this.parentMappings = parentMappings;
            this.callCount = callCount;
            this.roleProperties = roleProperties;
        }

        @Override
        public Map<String, Properties> getAllRoleProperties() {
            return roleProperties;
        }

        @Override
        public Map<String, String> getParentMappings() {
            return parentMappings;
        }

        @Override
        public GeoServerRole getParentRole(GeoServerRole role) {
            callCount.incrementAndGet();
            String parent = parentMappings.get(role.getAuthority());
            return parent != null ? roles.get(parent) : null;
        }

        @Override
        public GeoServerRole createRoleObject(String role) {
            return new GeoServerRole(role);
        }

        @Override
        public GeoServerRole getAdminRole() {
            return null;
        }

        @Override
        public GeoServerRole getGroupAdminRole() {
            return null;
        }

        @Override
        public SortedSet<GeoServerRole> getRoles() {
            return new TreeSet<>(roles.values());
        }

        @Override
        public GeoServerRole getRoleByName(String role) {
            return roles.get(role);
        }

        @Override
        public SortedSet<GeoServerRole> getRolesForUser(String username) {
            return new TreeSet<>();
        }

        @Override
        public SortedSet<GeoServerRole> getRolesForGroup(String groupname) {
            return new TreeSet<>();
        }

        @Override
        public SortedSet<String> getGroupNamesForRole(GeoServerRole role) {
            return new TreeSet<>();
        }

        @Override
        public SortedSet<String> getUserNamesForRole(GeoServerRole role) {
            return new TreeSet<>();
        }

        @Override
        public int getRoleCount() {
            return roles.size();
        }

        @Override
        public GeoServerRoleStore createStore() {
            return null;
        }

        @Override
        public boolean canCreateStore() {
            return false;
        }

        @Override
        public void registerRoleLoadedListener(RoleLoadedListener listener) {}

        @Override
        public void unregisterRoleLoadedListener(RoleLoadedListener listener) {}

        @Override
        public void load() {}

        @Override
        public Properties personalizeRoleParams(
                String roleName, Properties roleParams, String userName, Properties userProps) {
            return null;
        }

        @Override
        public String getName() {
            return "bulkCountingService";
        }

        @Override
        public void setName(String name) {}

        @Override
        public void initializeFromConfig(SecurityNamedServiceConfig config) {}

        @Override
        public void setSecurityManager(GeoServerSecurityManager securityManager) {}

        @Override
        public GeoServerSecurityManager getSecurityManager() {
            return null;
        }
    }
}
