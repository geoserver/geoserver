/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.workspaceadmin;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpMethod;
import org.springframework.security.access.ConfigAttribute;

/** Unit tests for {@link WorkspaceAdminRestfulDefinitionSource}. */
public class WorkspaceAdminRestfulDefinitionSourceTest {

    private WorkspaceAdminAuthorizer authorizer;
    private HttpServletRequest request;

    private WorkspaceAdminRestfulDefinitionSource definitionSource;

    @Before
    public void setUp() {
        authorizer = mock(WorkspaceAdminAuthorizer.class);
        // Setup WorkspaceAdminAuthorizer.get() to return our mock
        GeoServerExtensionsHelper.singleton("workspaceAdminAuthorization", authorizer, WorkspaceAdminAuthorizer.class);

        definitionSource = new WorkspaceAdminRestfulDefinitionSource(authorizer);
        request = mock(HttpServletRequest.class);

        // Setup the request
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/geoserver/rest/workspaces/topp");
        when(request.getContextPath()).thenReturn("/geoserver");
    }

    @After
    public void tearDown() {
        // Reset the mock instance for WorkspaceAdminAuthorizer
        GeoServerExtensionsHelper.init(null);
    }

    @Test
    public void testSupports() {
        // Should support HttpServletRequest
        assertTrue(definitionSource.supports(HttpServletRequest.class));

        // Should not support other classes
        assertFalse(definitionSource.supports(String.class));
        assertFalse(definitionSource.supports(Object.class));
    }

    @Test
    public void testGetAttributesNoMatch() {
        // No matching rule
        when(authorizer.findMatchingRule(anyString(), any(HttpMethod.class))).thenReturn(Optional.empty());

        // Should return empty list when no rule matches
        Collection<ConfigAttribute> attributes = definitionSource.getAttributes(request);
        assertTrue(attributes.isEmpty());

        // Verify the correct URL and method were used
        verify(authorizer).findMatchingRule(eq("/rest/workspaces/topp"), eq(GET));
    }

    @Test
    public void testGetAttributesWithMatch() {
        // Create a matching rule
        WorkspaceAdminRestAccessRule rule =
                new WorkspaceAdminRestAccessRule(1, "/geoserver/rest/workspaces/{workspace}", Set.of(GET));

        // Rule matches
        when(authorizer.findMatchingRule(anyString(), any(HttpMethod.class))).thenReturn(Optional.of(rule));

        // Should return the rule when it matches
        Collection<ConfigAttribute> attributes = definitionSource.getAttributes(request);
        assertFalse(attributes.isEmpty());
        assertEquals(1, attributes.size());

        // The attribute should be our rule
        ConfigAttribute attr = attributes.iterator().next();
        assertTrue(attr instanceof WorkspaceAdminRestAccessRule);
        assertEquals(rule, attr);
    }

    @Test
    public void testGetAllConfigAttributes() {
        // Create some rules
        WorkspaceAdminRestAccessRule rule1 =
                new WorkspaceAdminRestAccessRule(1, "/rest/workspaces/{workspace}", Set.of(GET));
        WorkspaceAdminRestAccessRule rule2 =
                new WorkspaceAdminRestAccessRule(2, "/rest/namespaces/{workspace}", Set.of(POST));

        // Authorizer returns these rules
        when(authorizer.getAccessRules()).thenReturn(List.of(rule1, rule2));

        // Should return all rules
        Collection<ConfigAttribute> attributes = definitionSource.getAllConfigAttributes();
        assertEquals(List.of(rule1, rule2), attributes);
    }
}
