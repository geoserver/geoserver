/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;
import org.junit.Test;

/**
 * Verifies that in-memory role services (non-BulkLoadable) still resolve hierarchies correctly via the legacy
 * addParentRole() path.
 */
public class RoleCalculatorPreservationTest {

    @Test
    public void testLegacyPathResolvesChain() throws IOException {
        InMemoryRoleService service = new InMemoryRoleService();
        GeoServerRole a = service.addRole("A");
        GeoServerRole b = service.addRole("B");
        GeoServerRole c = service.addRole("C");
        service.setParent(a, b);
        service.setParent(b, c);

        RoleCalculator calc = new RoleCalculator(service);
        Set<GeoServerRole> roles = new HashSet<>();
        roles.add(a);
        calc.addInheritedRoles(roles);

        assertEquals(3, roles.size());
        assertTrue(roles.contains(b));
        assertTrue(roles.contains(c));
    }

    @Test
    public void testLegacyPathHandlesCycle() throws IOException {
        InMemoryRoleService service = new InMemoryRoleService();
        GeoServerRole a = service.addRole("A");
        GeoServerRole b = service.addRole("B");
        service.setParent(a, b);
        service.setParent(b, a);

        RoleCalculator calc = new RoleCalculator(service);
        Set<GeoServerRole> roles = new HashSet<>();
        roles.add(a);
        calc.addInheritedRoles(roles);

        assertEquals(2, roles.size());
    }

    @Test
    public void testLegacyPathPreservesProperties() throws IOException {
        InMemoryRoleService service = new InMemoryRoleService();
        GeoServerRole child = service.addRole("CHILD");
        GeoServerRole parent = service.addRole("PARENT");
        parent.getProperties().put("key", "value");
        service.setParent(child, parent);

        RoleCalculator calc = new RoleCalculator(service);
        Set<GeoServerRole> roles = new HashSet<>();
        roles.add(child);
        calc.addInheritedRoles(roles);

        GeoServerRole resolved = roles.stream()
                .filter(r -> r.getAuthority().equals("PARENT"))
                .findFirst()
                .orElse(null);
        assertEquals("value", resolved.getProperties().get("key"));
    }

    /** Minimal in-memory role service using RoleStoreHelper — does NOT implement BulkLoadableRoleService. */
    private static class InMemoryRoleService extends AbstractRoleService {

        private final RoleStoreHelper helper = new RoleStoreHelper();

        GeoServerRole addRole(String name) {
            GeoServerRole role = new GeoServerRole(name);
            helper.roleMap.put(name, role);
            return role;
        }

        void setParent(GeoServerRole child, GeoServerRole parent) {
            helper.role_parentMap.put(child, parent);
        }

        @Override
        protected void deserialize() {}

        @Override
        public GeoServerRole getParentRole(GeoServerRole role) {
            return helper.role_parentMap.get(role);
        }

        @Override
        public GeoServerRole createRoleObject(String role) {
            return new GeoServerRole(role);
        }
    }
}
