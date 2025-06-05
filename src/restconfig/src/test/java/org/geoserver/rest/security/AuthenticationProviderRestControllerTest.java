package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.Objects;
import org.apache.http.HttpStatus;
import org.geoserver.rest.security.AuthenticationProviderRestController.CannotSaveProvider;
import org.geoserver.rest.security.AuthenticationProviderRestController.InvalidData;
import org.geoserver.rest.security.AuthenticationProviderRestController.UnknownProvider;
import org.geoserver.rest.security.xml.AuthProvider;
import org.geoserver.rest.security.xml.AuthProviderList;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

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

    @Test
    public void testList() throws IOException {
        ResponseEntity<RestWrapper<AuthProviderList>> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthProviderList authProviderList = (AuthProviderList) result.getBody().getObject();
        authProviderList.getProviders().forEach(filter -> {
            assertNotNull(filter.getId());
            assertNotNull(filter.getName());
            assertNotNull(filter.getConfig());
        });
    }

    @Test
    public void testView() {
        String name = generateName("view");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        ResponseEntity<RestWrapper<AuthProvider>> result = controller.view(provider.getName());
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthProvider authProvider = (AuthProvider) result.getBody().getObject();
        assertNotNull(authProvider.getId());
        assertEquals(provider.getName(), authProvider.getName());
        assertNotNull(authProvider.getConfig());
    }

    @Test
    public void testCreate() {
        String name = generateName("create");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        var authProvider = new AuthProvider(provider);
        ResponseEntity<RestWrapper<AuthProvider>> result = controller.create(authProvider);
        int status = result.getStatusCode().value();
        assertEquals("Expected an Created response", HttpStatus.SC_CREATED, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthProvider createdAuthProvider = (AuthProvider) result.getBody().getObject();
        assertNotNull(createdAuthProvider.getId());
        assertEquals(provider.getName(), createdAuthProvider.getName());
        assertEquals(provider, authProvider.getConfig());

        // File system check as securitymanger has been reloaded
        ResponseEntity<RestWrapper<AuthProvider>> viewProviderResult = controller.view(name);
        AuthProvider viewProvider = (AuthProvider)
                Objects.requireNonNull(viewProviderResult.getBody()).getObject();
        assertNotNull(viewProvider.getId());
        assertEquals(provider.getName(), viewProvider.getName());
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withId() {
        String name = generateName("create");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        provider.setId("id");
        var authProvider = new AuthProvider(provider);
        controller.create(authProvider);
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withMissingName() {
        String name = generateName("create");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        var authProvider = new AuthProvider(provider);
        authProvider.setName(null);
        controller.create(authProvider);
    }

    @Test(expected = InvalidData.class)
    public void testCreate_withClassName() {
        String name = generateName("create");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        var authProvider = new AuthProvider(provider);
        authProvider.setClassName(null);
        controller.create(authProvider);
    }

    @Test(expected = CannotSaveProvider.class)
    public void testCreate_withInvalidClassName() {
        String name = generateName("create");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, false);
        var authProvider = new AuthProvider(provider);
        authProvider.setClassName(String.class.getName());
        controller.create(authProvider);
    }

    @Test
    public void testUpdate() {
        String name = generateName("update");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        var authProvider = new AuthProvider(provider);
        ResponseEntity<RestWrapper<AuthProvider>> result = controller.update(authProvider.getName(), authProvider);
        int status = result.getStatusCode().value();
        assertEquals("Expected an Created response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthProvider createdAuthProvider = (AuthProvider) result.getBody().getObject();
        assertNotNull(createdAuthProvider.getId());
        assertEquals(provider.getName(), createdAuthProvider.getName());
        assertEquals(provider, authProvider.getConfig());

        // File system check as securitymanger has been reloaded
        ResponseEntity<RestWrapper<AuthProvider>> viewProviderResult = controller.view(name);
        AuthProvider viewProvider = (AuthProvider)
                Objects.requireNonNull(viewProviderResult.getBody()).getObject();
        assertNotNull(viewProvider.getId());
        assertEquals(provider.getName(), viewProvider.getName());
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingName() {
        String name = generateName("update");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        var authProvider = new AuthProvider(provider);
        authProvider.setName(null);
        controller.update(authProvider.getName(), authProvider);
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingClassName() {
        String name = generateName("update");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        var authProvider = new AuthProvider(provider);
        authProvider.setClassName(null);
        controller.update(authProvider.getName(), authProvider);
    }

    @Test(expected = InvalidData.class)
    public void testUpdate_MissingId() {
        String name = generateName("update");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        var authProvider = new AuthProvider(provider);
        authProvider.setId(null);
        controller.update(authProvider.getName(), authProvider);
    }

    @Test
    public void testDelete() {
        String name = generateName("delete");
        var provider = authenticationProviderHelper.createUsernamePasswordAuthenticationProviderConfig(name, true);
        var authProvider = new AuthProvider(provider);
        ResponseEntity<RestWrapper<AuthProvider>> result = controller.delete(name);
        int status = result.getStatusCode().value();
        assertEquals("Expected an Created response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthProvider createdAuthProvider = (AuthProvider) result.getBody().getObject();
        assertNotNull(createdAuthProvider.getId());
        assertEquals(provider.getName(), createdAuthProvider.getName());
        assertEquals(provider, authProvider.getConfig());

        // File system check as securitymanger has been reloaded
        try {
            controller.view(name);
            fail("Expected an UnknownProvider exception");
        } catch (UnknownProvider e) {
            // Expected
        }
    }

    @Test(expected = UnknownProvider.class)
    public void testDelete_NotFound() {
        String name = generateName("delete");
        controller.delete(name);
    }

    private String generateName(String suffix) {
        return TEST_PROVIDER_PREFIX + System.currentTimeMillis() + "-" + suffix;
    }
}
