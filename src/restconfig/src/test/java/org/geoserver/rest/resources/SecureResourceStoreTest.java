/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.resources;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import org.geoserver.platform.GeoServerExtensionsHelper;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;

/** Unit tests for {@link SecureResourceStore}. */
public class SecureResourceStoreTest {

    private SecureResourceStore secureStore;
    private ResourceStore delegate;
    private WorkspaceAdminAuthorizer authorizer;
    private Authentication authentication;
    private GeoServerSecurityManager securityManager;
    private Resource workspacesDir;
    private Resource toppWorkspace;
    private Resource sfWorkspace;
    private Resource styleDir;

    @Before
    public void setUp() {
        // Mock dependencies
        delegate = mock(ResourceStore.class);
        authorizer = mock(WorkspaceAdminAuthorizer.class);
        authentication = mock(Authentication.class);
        securityManager = mock(GeoServerSecurityManager.class);

        // Set up security context with our mocked authentication
        SecurityContextImpl securityContext = new SecurityContextImpl();
        securityContext.setAuthentication(authentication);
        SecurityContextHolder.setContext(securityContext);

        // Set up mock resources
        workspacesDir = mock(Resource.class);
        when(workspacesDir.path()).thenReturn("workspaces");
        when(workspacesDir.name()).thenReturn("workspaces");

        toppWorkspace = mock(Resource.class);
        when(toppWorkspace.path()).thenReturn("workspaces/topp");
        when(toppWorkspace.name()).thenReturn("topp");

        sfWorkspace = mock(Resource.class);
        when(sfWorkspace.path()).thenReturn("workspaces/sf");
        when(sfWorkspace.name()).thenReturn("sf");

        styleDir = mock(Resource.class);
        when(styleDir.path()).thenReturn("styles");
        when(styleDir.name()).thenReturn("styles");

        // Configure delegate to return our mock resources
        when(delegate.get("workspaces")).thenReturn(workspacesDir);
        when(delegate.get("workspaces/topp")).thenReturn(toppWorkspace);
        when(delegate.get("workspaces/sf")).thenReturn(sfWorkspace);
        when(delegate.get("styles")).thenReturn(styleDir);

        // Configure workspaces directory to return workspace resources
        when(workspacesDir.list()).thenReturn(Arrays.asList(toppWorkspace, sfWorkspace));

        GeoServerExtensionsHelper.singleton("mockSecurityManager", securityManager, GeoServerSecurityManager.class);
        GeoServerExtensionsHelper.singleton("mockWorkspaceAdminAuthorizer", authorizer, WorkspaceAdminAuthorizer.class);

        // Create the secure store
        secureStore = new SecureResourceStore(delegate);
    }

    @After
    public void tearDown() {
        // Reset static mocks
        GeoServerExtensionsHelper.init(null);

        // Clear security context
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testGetResourceAsAdmin() {
        // Setup user as admin
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(true);

        // When user is admin, should get direct access to resources without security wrapping
        Resource resource = secureStore.get("workspaces/topp");
        assertEquals(toppWorkspace, resource);
        assertFalse(resource instanceof SecuredResource);
    }

    @Test
    public void testGetResourceAsWorkspaceAdmin() {
        // Setup user as workspace admin, but not global admin
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);
        when(authorizer.isWorkspaceAdmin(authentication)).thenReturn(true);

        // When user is workspace admin, resources should be security wrapped
        Resource resource = secureStore.get("workspaces/topp");
        assertNotNull(resource);
        assertTrue(resource instanceof SecuredResource);
    }

    @Test
    public void testRemoveAllowed() {
        // Setup security so user can write to the resource
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        // Create a spied secureStore so we can mock the canWrite method
        secureStore = spy(secureStore);
        doReturn(true).when(secureStore).canWrite(anyString());

        // Should delegate to underlying store when allowed
        when(delegate.remove("workspaces/topp")).thenReturn(true);

        assertTrue(secureStore.remove("workspaces/topp"));
        verify(delegate).remove("workspaces/topp");
    }

    @Test
    public void testRemoveDenied() {
        // Setup security so user cannot write to the resource
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        // Create a spied secureStore so we can mock the canWrite method
        secureStore = spy(secureStore);
        doReturn(false).when(secureStore).canWrite(anyString());

        // Should not delegate to underlying store when not allowed
        assertFalse(secureStore.remove("workspaces/sf"));

        // Note: verify that remove was not called on delegate
    }

    @Test
    public void testMoveAllowed() {
        // Setup security so user can write to both source and target
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        // Create a spied secureStore so we can mock the canWrite method
        secureStore = spy(secureStore);
        doReturn(true).when(secureStore).canWrite(anyString());

        // Should delegate to underlying store when allowed
        when(delegate.move("workspaces/topp/old", "workspaces/topp/new")).thenReturn(true);

        assertTrue(secureStore.move("workspaces/topp/old", "workspaces/topp/new"));
        verify(delegate).move("workspaces/topp/old", "workspaces/topp/new");
    }

    @Test
    public void testMoveDeniedSourceNotWritable() {
        // Setup security so user cannot write to source
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        // Create a spied secureStore so we can mock the canWrite method
        secureStore = spy(secureStore);
        doReturn(false).when(secureStore).canWrite("workspaces/sf/source");
        doReturn(true).when(secureStore).canWrite("workspaces/topp/dest");

        // Should not delegate to underlying store when source not writable
        assertFalse(secureStore.move("workspaces/sf/source", "workspaces/topp/dest"));
    }

    @Test
    public void testMoveDeniedTargetNotWritable() {
        // Setup security so user cannot write to target
        when(securityManager.checkAuthenticationForAdminRole()).thenReturn(false);

        // Create a spied secureStore so we can mock the canWrite method
        secureStore = spy(secureStore);
        doReturn(true).when(secureStore).canWrite("workspaces/topp/source");
        doReturn(false).when(secureStore).canWrite("workspaces/sf/dest");

        // Should not delegate to underlying store when target not writable
        assertFalse(secureStore.move("workspaces/topp/source", "workspaces/sf/dest"));
    }
}
