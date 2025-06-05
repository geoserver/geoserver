/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertTrue;
import static org.springframework.http.HttpMethod.DELETE;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.HEAD;
import static org.springframework.http.HttpMethod.OPTIONS;
import static org.springframework.http.HttpMethod.PATCH;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.http.HttpMethod.PUT;
import static org.springframework.http.HttpMethod.TRACE;

import java.util.Set;
import org.junit.Test;
import org.springframework.security.access.ConfigAttribute;

/** Unit tests for {@link WorkspaceAdminRestAccessRule}. */
public class WorkspaceAdminRestAccessRuleTest {

    @Test
    public void testConstructor() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(5, "/test/path", Set.of(GET, POST));
        assertEquals("/test/path", rule.getAntPattern());
        assertEquals(Set.of(GET, POST), rule.getMethods());
        assertEquals("GET,POST", rule.methods());
        assertEquals("/test/path=GET,POST", rule.getAttribute());
    }

    @Test
    public void testMatches() {
        WorkspaceAdminRestAccessRule rule =
                new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/{workspace}/**", Set.of(GET, POST, PUT, DELETE));

        // Positive tests
        assertTrue(rule.matches("/rest/workspaces/topp", GET));
        assertTrue(rule.matches("/rest/workspaces/topp/datastores", POST));
        assertTrue(rule.matches("/rest/workspaces/topp/datastores/nyc", PUT));
        assertTrue(rule.matches("/rest/workspaces/topp/datastores/nyc/featuretypes/buildings", DELETE));

        // Negative tests - wrong HTTP method
        assertFalse(rule.matches("/rest/workspaces/topp", PATCH));

        // Negative tests - pattern doesn't match
        assertFalse(rule.matches("/rest/namespaces/topp", GET));
        assertFalse(rule.matches("/rest/layers/topp:roads", GET));
    }

    @Test
    public void testMatchesWithWildcards() {
        WorkspaceAdminRestAccessRule rule =
                new WorkspaceAdminRestAccessRule(1, "/rest/*/workspaces/{workspace}/*", Set.of(GET, PUT));

        // Positive tests
        assertTrue(rule.matches("/rest/abc/workspaces/topp/xyz", GET));

        // Negative tests - too many path segments
        assertFalse(rule.matches("/rest/abc/workspaces/topp/xyz/123", PUT));

        // Negative tests - too few path segments
        assertFalse(rule.matches("/rest/abc/workspaces/topp", GET));
    }

    @SuppressWarnings("serial")
    @Test
    public void testEqualsAndHashCode() {
        WorkspaceAdminRestAccessRule rule1 = new WorkspaceAdminRestAccessRule(1, "/path", Set.of(GET, POST));
        WorkspaceAdminRestAccessRule rule2 = new WorkspaceAdminRestAccessRule(1, "/path", Set.of(GET, POST));
        WorkspaceAdminRestAccessRule rule3 = new WorkspaceAdminRestAccessRule(2, "/path", Set.of(GET, POST));
        WorkspaceAdminRestAccessRule rule4 = new WorkspaceAdminRestAccessRule(1, "/different", Set.of(GET, POST));
        WorkspaceAdminRestAccessRule rule5 = new WorkspaceAdminRestAccessRule(1, "/path", Set.of(GET, DELETE));

        // Same pattern and methods should be equal, even with different priorities
        assertEquals(rule1, rule2);
        assertEquals(rule1.hashCode(), rule2.hashCode());
        assertEquals(rule1, rule3);
        assertEquals(rule1.hashCode(), rule3.hashCode());

        // Different pattern should not be equal
        assertNotEquals(rule1, rule4);
        assertNotEquals(rule1.hashCode(), rule4.hashCode());

        assertNotEquals(rule1, rule5);
        assertNotEquals(rule1.hashCode(), rule5.hashCode());

        assertNotEquals(rule1, new ConfigAttribute() {
            @Override
            public String getAttribute() {
                return rule1.getAttribute();
            }
        });
    }

    @Test
    public void testCompareTo() {
        WorkspaceAdminRestAccessRule rule1 = new WorkspaceAdminRestAccessRule(1, "/path", Set.of(GET));
        WorkspaceAdminRestAccessRule rule2 = new WorkspaceAdminRestAccessRule(2, "/path", Set.of(GET));
        WorkspaceAdminRestAccessRule rule3 = new WorkspaceAdminRestAccessRule(3, "/path", Set.of(GET));

        // Lower priority value should come before higher priority value
        assertTrue(rule1.compareTo(rule2) < 0);
        assertTrue(rule2.compareTo(rule3) < 0);
        assertTrue(rule1.compareTo(rule3) < 0);

        // Same priority should be equal
        WorkspaceAdminRestAccessRule ruleSame = new WorkspaceAdminRestAccessRule(1, "/different", Set.of(POST));
        assertEquals(0, rule1.compareTo(ruleSame));
    }

    @Test
    public void testToString() {
        WorkspaceAdminRestAccessRule rule = new WorkspaceAdminRestAccessRule(
                5, "/test/path", Set.of(GET, HEAD, OPTIONS, TRACE, POST, PUT, PATCH, DELETE));

        // toString should return the attribute representation, with methods sorted alphabetically
        assertEquals("/test/path=DELETE,GET,HEAD,OPTIONS,PATCH,POST,PUT,TRACE", rule.toString());
    }
}
