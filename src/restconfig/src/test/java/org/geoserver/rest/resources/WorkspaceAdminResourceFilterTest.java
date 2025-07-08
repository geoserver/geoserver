/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.geoserver.rest.resources.WorkspaceAdminResourceFilter.ResourceAccess;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;

/** Unit tests for {@link WorkspaceAdminResourceFilter}. */
public class WorkspaceAdminResourceFilterTest {

    private WorkspaceAdminResourceFilter filter;
    private WorkspaceAdminAuthorizer authorizer;
    private Authentication authentication;

    @Before
    public void setUp() {
        // Mock dependencies
        authorizer = mock(WorkspaceAdminAuthorizer.class);
        authentication = mock(Authentication.class);

        // Create the filter
        filter = new WorkspaceAdminResourceFilter(authorizer);
    }

    @Test
    public void testAccessLevelsEnum() {
        // Test the ResourceAccess enum values behave as expected
        assertTrue(ResourceAccess.READ.canRead());
        assertFalse(ResourceAccess.READ.canWrite());

        assertTrue(ResourceAccess.WRITE.canRead());
        assertTrue(ResourceAccess.WRITE.canWrite());

        assertFalse(ResourceAccess.NONE.canRead());
        assertFalse(ResourceAccess.NONE.canWrite());
    }

    @Test
    public void testGetAccessLimitsRootPath() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Root path should be readable but not writable
        ResourceAccess access = filter.getAccessLimits(authentication, "");
        assertTrue(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.READ, access);
    }

    @Test
    public void testGetAccessLimitsWorkspacesPath() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Workspaces path should be readable but not writable
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces");
        assertTrue(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.READ, access);
    }

    @Test
    public void testGetAccessLimitsSpecificWorkspacePath() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Setup workspace access limits for the "topp" workspace
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);
        when(limits.isAdminable()).thenReturn(true);
        when(authorizer.getWorkspaceAccessLimits(eq(authentication), eq("topp")))
                .thenReturn(limits);

        // User should have full access to the workspace path
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces/topp");
        assertTrue(access.canRead());
        assertTrue(access.canWrite());
        assertEquals(ResourceAccess.WRITE, access);
    }

    @Test
    public void testGetAccessLimitsSubPathWithinWorkspace() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Setup workspace access limits for the "topp" workspace
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);
        when(limits.isAdminable()).thenReturn(true);
        when(authorizer.getWorkspaceAccessLimits(eq(authentication), eq("topp")))
                .thenReturn(limits);

        // User should have full access to paths within the workspace
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces/topp/datastores/tiger/featuretypes");
        assertTrue(access.canRead());
        assertTrue(access.canWrite());
        assertEquals(ResourceAccess.WRITE, access);
    }

    @Test
    public void testGetAccessLimitsUnauthorizedWorkspace() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Setup no access to "sf" workspace
        when(authorizer.getWorkspaceAccessLimits(eq(authentication), eq("sf"))).thenReturn(null);

        // User should not have access to unauthorized workspace
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces/sf");
        assertFalse(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.NONE, access);
    }

    @Test
    public void testGetAccessLimitsNonAdminableWorkspace() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Setup workspace access limits for the "topp" workspace, but not adminable
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);
        when(limits.isAdminable()).thenReturn(false);
        when(authorizer.getWorkspaceAccessLimits(eq(authentication), eq("topp")))
                .thenReturn(limits);

        // User should not have access to non-adminable workspace
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces/topp");
        assertFalse(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.NONE, access);
    }

    @Test
    public void testGetAccessLimitsNonWorkspaceAdmin() {
        // Setup user not as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(false);

        // User should not have access to any paths
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces");
        assertFalse(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.NONE, access);

        access = filter.getAccessLimits(authentication, "workspaces/topp");
        assertFalse(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.NONE, access);
    }

    @Test
    public void testGetAccessLimitsWithTrailingSlash() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Setup workspace access limits for the "topp" workspace
        WorkspaceAccessLimits limits = mock(WorkspaceAccessLimits.class);
        when(limits.isAdminable()).thenReturn(true);
        when(authorizer.getWorkspaceAccessLimits(eq(authentication), eq("topp")))
                .thenReturn(limits);

        // Trailing slash should be handled correctly
        ResourceAccess access = filter.getAccessLimits(authentication, "workspaces/topp/");
        assertTrue(access.canRead());
        assertTrue(access.canWrite());
        assertEquals(ResourceAccess.WRITE, access);
    }

    @Test
    public void testGetAccessLimitsNonMatchingPath() {
        // Setup user as workspace admin
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // Non-matching path should not be accessible
        ResourceAccess access = filter.getAccessLimits(authentication, "logging");
        assertFalse(access.canRead());
        assertFalse(access.canWrite());
        assertEquals(ResourceAccess.NONE, access);
    }
}
