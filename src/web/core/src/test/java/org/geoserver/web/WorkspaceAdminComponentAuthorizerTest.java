/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.function.Supplier;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.workspaceadmin.WorkspaceAdminAuthorizer;
import org.geoserver.security.workspaceadmin.WorkspaceAdminRESTAccessRuleDAO;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

public class WorkspaceAdminComponentAuthorizerTest extends GeoServerWicketTestSupport {

    private WorkspaceAdminComponentAuthorizer authorizer;
    private ResourceAccessManager accessManager;
    private TestWorkspaceAdminAuthorizer adminAuthorizer;

    private static class TestWorkspaceAdminAuthorizer extends WorkspaceAdminAuthorizer {
        private Supplier<ResourceAccessManager> ram;

        public TestWorkspaceAdminAuthorizer(WorkspaceAdminRESTAccessRuleDAO dao, Supplier<ResourceAccessManager> ram) {
            super(dao);
            this.ram = ram;
        }

        @Override
        public ResourceAccessManager getAccessManager() {
            return ram.get();
        }
    }

    @Before
    @SuppressWarnings("serial")
    public void setUp() {
        this.accessManager = GeoServerExtensions.bean(SecureCatalogImpl.class).getResourceAccessManager();
        WorkspaceAdminRESTAccessRuleDAO dao = GeoServerExtensions.bean(WorkspaceAdminRESTAccessRuleDAO.class);
        adminAuthorizer = new TestWorkspaceAdminAuthorizer(dao, () -> this.accessManager);
        authorizer = new WorkspaceAdminComponentAuthorizer() {
            @Override
            WorkspaceAdminAuthorizer getWorkspaceAdminAuthorizer() {
                return adminAuthorizer;
            }
        };
    }

    @After
    public void after() {
        RequestContextHolder.setRequestAttributes(null);
    }

    private boolean isAccessAllowed(Authentication user) {
        Class<?> unused = GeoServerBasePage.class;
        return authorizer.isAccessAllowed(unused, user);
    }

    @Test
    public void testGetAccessManager() {
        authorizer = new WorkspaceAdminComponentAuthorizer();
        assertNotNull(adminAuthorizer.getAccessManager());
    }

    @Test
    public void testAdmin() {
        assertTrue(isAccessAllowed(admin()));
    }

    @Test
    public void testNull() {
        assertFalse(isAccessAllowed(null));
    }

    @Test
    public void testNoAccessManager() {
        this.accessManager = null;
        assertNull(adminAuthorizer.getAccessManager());
        Authentication user = user("test", "ROLE_1");
        assertFalse(isAccessAllowed(user));
    }

    @Test
    public void testNotAuthenticated() {
        Authentication user = user("test");
        user.setAuthenticated(false);
        assertFalse(isAccessAllowed(user));
    }

    @Test
    public void testNotWorkspaceAdmin() {
        Authentication user = user("test", "ROLE_USER");
        assertFalse(isAccessAllowed(user));
    }

    @Test
    public void testIsWorkspaceAdmin() {
        Authentication user = user("test", "ROLE_USER");
        accessManager = mock(ResourceAccessManager.class);
        when(accessManager.isWorkspaceAdmin(user, getCatalog())).thenReturn(true);
        assertTrue(isAccessAllowed(user));
    }

    @Test
    public void testRequestCache() {
        Authentication user = user("test", "ROLE_USER");
        accessManager = mock(ResourceAccessManager.class);
        when(accessManager.isWorkspaceAdmin(user, getCatalog())).thenReturn(true);

        RequestAttributes atts = new ServletRequestAttributes(new MockHttpServletRequest());
        RequestContextHolder.setRequestAttributes(atts);

        assertTrue(isAccessAllowed(user));
        assertTrue(isAccessAllowed(user));
        assertTrue(isAccessAllowed(user));

        verify(accessManager, times(1)).isWorkspaceAdmin(same(user), any());
    }

    private Authentication admin() {
        login();
        return SecurityContextHolder.getContext().getAuthentication();
    }

    private Authentication user(String name, String... roles) {
        login(name, "pwd", roles);
        return SecurityContextHolder.getContext().getAuthentication();
    }
}
