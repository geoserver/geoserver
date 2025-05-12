package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;
import org.apache.http.HttpStatus;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.security.xml.AuthFilterList;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

public class AuthenticationFilterChainControllerTest extends GeoServerTestSupport {

    private static final String TEST_FILTER_PREFIX = "TEST-";

    private AuthenticationFilterChainController controller;
    private SecurityConfigFilterHelper securityConfigFilterHelper;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(AuthenticationFilterChainController.class);
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

    @Test
    public void testList() throws IOException {
        ResponseEntity<AuthFilterList> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
    }

    @Test
    public void testGet() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);

        ResponseEntity<AuthFilter> result = controller.get(newConfig.getName());
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertEquals(
                "The config name should be the same",
                newConfig.getName(),
                Objects.requireNonNull(result.getBody()).getConfig().getName());
        assertEquals(
                "The config className should be the same",
                newConfig.getClassName(),
                result.getBody().getConfig().getClassName());
        assertEquals(
                "The config id should be the same",
                newConfig.getId(),
                result.getBody().getConfig().getId());

        BasicAuthenticationFilterConfig actualConfig =
                (BasicAuthenticationFilterConfig) result.getBody().getConfig();
        assertFalse("Remember me should be false", actualConfig.isUseRememberMe());
    }

    @Test
    public void testListReturnAllTheFilters() throws IOException {
        ResponseEntity<AuthFilterList> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);

        List<String> names = Objects.requireNonNull(result.getBody()).getFilters().stream()
                .map(AuthFilter::getName)
                .collect(Collectors.toList());
        SortedSet<String> filters = getSecurityManager().listFilters();
        assertEquals("Expected all the filters to be returned", filters.size(), names.size());
        filters.stream()
                .filter(name -> !names.contains(name))
                .map(name -> "Expected filter " + name + " to be returned")
                .forEach(Assert::fail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGet_ConfigDoesNotExist() throws IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        controller.get(name);
    }

    @Test
    public void testCreate_BasicAuthenticationFilter() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter(newConfig);
        BasicAuthenticationFilterConfig actualConfig;

        ResponseEntity<AuthFilter> postResult = controller.post(authFilter);
        int status = postResult.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_CREATED, status);
        assertNotNull(
                "An Id should be returned",
                Objects.requireNonNull(postResult.getBody()).getId());
        actualConfig = (BasicAuthenticationFilterConfig) postResult.getBody().getConfig();
        assertFalse("Remember me should be false", actualConfig.isUseRememberMe());

        ResponseEntity<AuthFilter> getResult = controller.get(authFilter.getName());
        int getStatus = getResult.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, getStatus);
        assertNotNull(
                "An Id should be returned",
                Objects.requireNonNull(getResult.getBody()).getId());
        actualConfig = (BasicAuthenticationFilterConfig) getResult.getBody().getConfig();
        assertFalse("Remember me should be false", actualConfig.isUseRememberMe());
    }

    @Test(expected = AuthenticationFilterChainController.MissingNameException.class)
    public void testCreate_BasicAuthenticationFilter_MissingName() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        newConfig.setName(null);
        AuthFilter authFilter = new AuthFilter(newConfig);

        controller.post(authFilter);
    }

    @Test(expected = AuthenticationFilterChainController.DuplicateNameException.class)
    public void testCreate_BasicAuthenticationFilter_ErrorsWithTheSameName()
            throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter(newConfig);

        controller.post(authFilter);
        if (newConfig.getId() != null) {
            newConfig.setId(null);
            authFilter.setId(null);
        }
        controller.post(authFilter);
    }

    @Test(expected = AuthenticationFilterChainController.IdSetByServerException.class)
    public void testCreate_BasicAuthenticationFilter_IdSetByClient() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        newConfig.setId("123456789");
        AuthFilter authFilter = new AuthFilter(newConfig);

        controller.post(authFilter);
    }

    @Test
    public void testUpdate_BasicAuthenticationFilter() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter(newConfig);
        AuthFilter createdFilter = controller.post(authFilter).getBody();

        ResponseEntity<AuthFilter> result = controller.put(name, createdFilter);
        int status = result.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_OK, status);
        assertNotNull(
                "An Id should be returned",
                Objects.requireNonNull(result.getBody()).getId());

        ResponseEntity<AuthFilter> getResult = controller.get(createdFilter.getName());
        int getStatus = getResult.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, getStatus);
        assertNotNull(
                "An Id should be returned",
                Objects.requireNonNull(getResult.getBody()).getId());
        BasicAuthenticationFilterConfig actualConfig =
                (BasicAuthenticationFilterConfig) getResult.getBody().getConfig();
        assertNotNull(actualConfig);
    }

    @Test
    public void testFind() throws IOException {
        ResponseEntity<AuthFilter> result = controller.get("anonymous");
        int status = result.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);
        AuthFilter body = result.getBody();
        assertNotNull("A body should be returned", body);
        assertEquals("Expected the anonymous filter", "anonymous", body.getName());
        assertNotNull("Expected the id to be set", body.getId());
        assertEquals(
                "Expected the name to be set", "anonymous", body.getConfig().getName());
        assertEquals(
                "Expected the className to be set",
                "org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter",
                body.getConfig().getClassName());
    }
}
