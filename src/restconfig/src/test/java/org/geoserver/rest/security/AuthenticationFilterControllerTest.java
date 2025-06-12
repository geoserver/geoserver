/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;
import static org.springframework.http.MediaType.TEXT_PLAIN;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.UUID;
import java.util.logging.Level;
import java.util.stream.Collectors;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.security.AuthenticationFilterController.NotAuthorised;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

public class AuthenticationFilterControllerTest extends GeoServerTestSupport {

    private static final String TEST_FILTER_PREFIX = "TEST-";

    private AuthenticationFilterController controller;
    private SecurityConfigFilterHelper securityConfigFilterHelper;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(AuthenticationFilterController.class);
        securityConfigFilterHelper = new SecurityConfigFilterHelper(getSecurityManager());
    }

    @Before
    public void revertFilters() throws Exception {
        GeoServerSecurityManager secMgr = getSecurityManager();
        secMgr.listFilters().stream()
                .filter(name -> name.startsWith(TEST_FILTER_PREFIX))
                .forEach(name -> {
                    try {
                        SecurityFilterConfig config = secMgr.loadFilterConfig(name, true);
                        secMgr.removeFilter(config);
                    } catch (Exception e) {
                        fail("Cannot remove security filters" + e.getMessage());
                    }
                });
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testList() {
        setUser();
        try {
            RestWrapper<AuthFilter> result = controller.list();
            assertNotNull(result.getObject());
            List<AuthFilter> authFilterList = (List<AuthFilter>) result.getObject();
            authFilterList.forEach(filter -> {
                assertNotNull(filter.getId());
                assertNotNull(filter.getName());
                assertNotNull(filter.getConfig());
            });
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = NotAuthorised.class)
    public void testList_NotAuthorized() {
        SecurityContextHolder.clearContext();
        controller.list();
    }

    @Test
    public void testView() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);

            RestWrapper<AuthFilter> result = controller.view(newConfig.getName());
            assertNotNull(result.getObject());
            SecurityFilterConfig config = ((AuthFilter) result.getObject()).getConfig();
            assertEquals("The config name should be the same", newConfig.getName(), config.getName());
            assertEquals("The config className should be the same", newConfig.getClassName(), config.getClassName());
            assertEquals("The config id should be the same", newConfig.getId(), config.getId());

            BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig) config;
            assertFalse("Remember me should be false", actualConfig.isUseRememberMe());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = NotAuthorised.class)
    public void testView_NotAuthorized() {
        SecurityContextHolder.clearContext();
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);
        controller.view(newConfig.getName());
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListReturnAllTheFilters() throws IOException {
        setUser();
        try {
            RestWrapper<AuthFilter> result = controller.list();

            List<AuthFilter> list = (List<AuthFilter>) Objects.requireNonNull(result.getObject());
            List<String> names = list.stream().map(AuthFilter::getName).collect(Collectors.toList());
            SortedSet<String> filters = getSecurityManager().listFilters();
            assertEquals("Expected all the filters to be returned", filters.size(), names.size());
            filters.stream()
                    .filter(name -> !names.contains(name))
                    .map(name -> "Expected filter " + name + " to be returned")
                    .forEach(Assert::fail);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testView_ConfigDoesNotExist() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
            controller.view(name);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testCreate_BasicAuthenticationFilter() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
            AuthFilter authFilter = new AuthFilter(newConfig);
            BasicAuthenticationFilterConfig actualConfig;

            ResponseEntity<?> postResult = controller.post(authFilter, UriComponentsBuilder.newInstance());
            assertEquals(TEXT_PLAIN, postResult.getHeaders().getContentType());
            String location = Objects.requireNonNull(postResult.getHeaders().getLocation())
                    .toString();
            assertTrue(location.endsWith(RestBaseController.ROOT_PATH + "/security/authFilters/" + name));
            RestWrapper<AuthFilter> getResult = controller.view(newConfig.getName());
            AuthFilter getFilter = (AuthFilter) Objects.requireNonNull(getResult.getObject());
            assertNotNull("An Id should be returned", getFilter.getId());
            actualConfig = (BasicAuthenticationFilterConfig) getFilter.getConfig();
            assertFalse("Remember me should be false", actualConfig.isUseRememberMe());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = NotAuthorised.class)
    public void testCreate_NotAuthorized() {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter(newConfig);

        controller.post(authFilter, UriComponentsBuilder.newInstance());
        SecurityContextHolder.clearContext();
    }

    @Test(expected = AuthenticationFilterController.MissingNameException.class)
    public void testCreate_BasicAuthenticationFilter_MissingName() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
            newConfig.setName(null);
            AuthFilter authFilter = new AuthFilter(newConfig);

            controller.post(authFilter, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = AuthenticationFilterController.DuplicateNameException.class)
    public void testCreate_BasicAuthenticationFilter_ErrorsWithTheSameName() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
            AuthFilter authFilter = new AuthFilter(newConfig);

            controller.post(authFilter, UriComponentsBuilder.newInstance());
            if (newConfig.getId() != null) {
                newConfig.setId(null);
                authFilter.setId(null);
            }
            controller.post(authFilter, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = AuthenticationFilterController.IdSetByServerException.class)
    public void testCreate_BasicAuthenticationFilter_IdSetByClient() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
            newConfig.setId("123456789");
            AuthFilter authFilter = new AuthFilter(newConfig);

            controller.post(authFilter, UriComponentsBuilder.newInstance());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testUpdate_BasicAuthenticationFilter() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);
            AuthFilter authFilter = new AuthFilter(newConfig);
            controller.put(name, authFilter);

            RestWrapper<AuthFilter> getResult = controller.view(newConfig.getName());
            AuthFilter getFilter = (AuthFilter) Objects.requireNonNull(getResult.getObject());
            assertNotNull("An Id should be returned", getFilter.getId());
            BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig) getFilter.getConfig();
            assertNotNull(actualConfig);
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test
    public void testDelete() {
        setUser();
        try {
            String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
            BasicAuthenticationFilterConfig newConfig =
                    securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);
            controller.delete(newConfig.getName());

            try {
                controller.view(newConfig.getName());
                Assert.fail("Should have thrown an exception");
            } catch (IllegalArgumentException e) {
                // Expected
            }
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    @Test(expected = NotAuthorised.class)
    public void testDelete_NotAuthorised() {
        SecurityContextHolder.clearContext();
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);
        controller.delete(newConfig.getName());
    }

    @Test
    public void testFind() {
        setUser();
        try {
            RestWrapper<AuthFilter> getResult = controller.view("anonymous");
            AuthFilter body = (AuthFilter) Objects.requireNonNull(getResult.getObject());
            assertNotNull("A body should be returned", body);
            assertEquals("Expected the anonymous filter", "anonymous", body.getName());
            assertNotNull("Expected the id to be set", body.getId());
            assertEquals(
                    "Expected the name to be set", "anonymous", body.getConfig().getName());
            assertEquals(
                    "Expected the className to be set",
                    "org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter",
                    body.getConfig().getClassName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "", e);
            Assert.fail(e.getLocalizedMessage());
        } finally {
            SecurityContextHolder.clearContext();
        }
    }

    private void setUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }
}
