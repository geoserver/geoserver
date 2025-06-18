package org.geoserver.rest.security;

import static org.geoserver.rest.security.AuthenticationProviderHelper.checkProvideUsernamePasswordAuthenticationProvider;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.geoserver.rest.security.AuthenticationProviderRestController.CannotSaveProvider;
import org.geoserver.rest.security.AuthenticationProviderRestController.InvalidData;
import org.geoserver.rest.security.AuthenticationProviderRestController.RequiresAdministrator;
import org.geoserver.rest.security.AuthenticationProviderRestController.UnknownProvider;
import org.geoserver.rest.security.xml.AuthProvider;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationProviderConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

public class AuthenticationProviderRestControllerTest extends GeoServerTestSupport {
    private static final String TEST_PROVIDER_PREFIX = "TEST-";

    private AuthenticationProviderRestController controller;
    private AuthenticationProviderHelper authenticationProviderHelper;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(AuthenticationProviderRestController.class);
        authenticationProviderHelper = new AuthenticationProviderHelper(getSecurityManager());
    }

    @Before
    public void resetProviders() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.listFilters().stream()
                .filter(name -> name.startsWith(TEST_PROVIDER_PREFIX))
                .forEach(name -> {
                    try {
                        SecurityAuthProviderConfig config = secMgr.loadAuthenticationProviderConfig(name);
                        secMgr.removeFilter(config);
                    } catch (Exception e) {
                        fail("Cannot remove security provider" + e.getMessage());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testList() throws IOException {
        try {
            setUser();
            RestWrapper<AuthProvider> result = controller.list();
            assertNotNull(result.getObject());
            List<AuthProvider> authProviderList = (List<AuthProvider>) result.getObject();
            authProviderList.forEach(filter -> {
                assertNotNull(filter.getId());
                assertNotNull(filter.getName());
                assertNotNull(filter.getConfig());
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = RequiresAdministrator.class)
    public void testList_NoUser() throws IOException {
        SecurityContextHolder.clearContext();
        controller.list();
    }

    @Test
    public void testView() {
        try {
            setUser();
            String name = generateName("view");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            RestWrapper<AuthProvider> result = controller.view(provider.getName());
            assertNotNull(result.getObject());
            AuthProvider authProvider = (AuthProvider) result.getObject();
            assertNotNull(authProvider.getId());
            assertEquals(provider.getName(), authProvider.getName());
            assertNotNull(authProvider.getConfig());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = RequiresAdministrator.class)
    public void testView_NoUser() {
        SecurityContextHolder.clearContext();

        String name = generateName("view");
        UsernamePasswordAuthenticationProviderConfig provider =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        controller.view(provider.getName());
    }

    @Test
    public void testCreate() {
        try {
            setUser();
            String name = generateName("create");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
            AuthProvider authProvider = new AuthProvider(provider);
            controller.create(authProvider, UriComponentsBuilder.newInstance());

            // File system check as securityManger has been reloaded
            RestWrapper<AuthProvider> viewProviderResult = controller.view(name);
            AuthProvider viewProvider = (AuthProvider) viewProviderResult.getObject();
            assertNotNull(viewProvider.getId());
            assertEquals(provider.getName(), viewProvider.getName());
            assertNotNull(viewProvider.getConfig());
            checkProvideUsernamePasswordAuthenticationProvider(provider, viewProvider.getConfig());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = RequiresAdministrator.class)
    public void testCreate_authorisationException() {
        SecurityContextHolder.clearContext();

        String name = generateName("create");
        UsernamePasswordAuthenticationProviderConfig provider =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        provider.setId("id");
        AuthProvider authProvider = new AuthProvider(provider);
        controller.create(authProvider, UriComponentsBuilder.newInstance());
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withId() {
        try {
            setUser();
            String name = generateName("create");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
            provider.setId("id");
            AuthProvider authProvider = new AuthProvider(provider);
            controller.create(authProvider, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withMissingName() {
        try {
            setUser();
            String name = generateName("create");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setName(null);
            controller.create(authProvider, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withClassName() {
        try {
            setUser();
            String name = generateName("create");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setClassName(null);
            controller.create(authProvider, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = CannotSaveProvider.class)
    public void testCreate_withInvalidClassName() {
        try {
            setUser();
            String name = generateName("create");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setClassName(String.class.getName());
            controller.create(authProvider, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdate() {
        try {
            setUser();
            String name = generateName("update");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            AuthProvider authProvider = new AuthProvider(provider);
            controller.update(authProvider.getName(), authProvider);

            // File system check as securityManger has been reloaded
            RestWrapper<AuthProvider> viewProviderResult = controller.view(name);
            AuthProvider viewProvider = (AuthProvider) viewProviderResult.getObject();
            assertNotNull(viewProvider.getId());
            assertEquals(provider.getName(), viewProvider.getName());
            assertNotNull(viewProvider.getConfig());
            checkProvideUsernamePasswordAuthenticationProvider(provider, viewProvider.getConfig());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = RequiresAdministrator.class)
    public void testUpdate_NotAuthorised() {
        SecurityContextHolder.clearContext();

        String name = generateName("update");
        UsernamePasswordAuthenticationProviderConfig provider =
                authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        AuthProvider authProvider = new AuthProvider(provider);
        authProvider.setName(null);
        controller.update(authProvider.getName(), authProvider);
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingName() {
        try {
            setUser();
            String name = generateName("update");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setName(null);
            controller.update(authProvider.getName(), authProvider);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingClassName() {
        try {
            setUser();
            String name = generateName("update");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setClassName(null);
            controller.update(authProvider.getName(), authProvider);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingId() {
        try {
            setUser();
            String name = generateName("update");
            UsernamePasswordAuthenticationProviderConfig provider =
                    authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            AuthProvider authProvider = new AuthProvider(provider);
            authProvider.setId(null);
            controller.update(authProvider.getName(), authProvider);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDelete() {
        String name = generateName("delete");
        setUser();
        try {
            authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
            controller.delete(name);
            // File system check as securityManger has been reloaded
            try {
                controller.view(name);
                fail("Expected an UnknownProvider exception");
            } catch (UnknownProvider e) {
                // Expected
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = RequiresAdministrator.class)
    public void testDelete_NotAuthenticated() {
        SecurityContextHolder.clearContext();
        String name = generateName("delete");
        controller.delete(name);
    }

    @Test(expected = UnknownProvider.class)
    public void testDelete_NotFound() {
        try {
            setUser();
            String name = generateName("delete");
            controller.delete(name);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private String generateName(String suffix) {
        return TEST_PROVIDER_PREFIX + System.currentTimeMillis() + "-" + suffix;
    }

    private void setUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
