package org.geoserver.rest.security;

import org.apache.http.HttpStatus;
import org.geoserver.rest.security.xml.AuthFilter;
import org.geoserver.rest.security.xml.AuthFilterList;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.filter.GeoServerBasicAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.validation.SecurityConfigException;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.SortedSet;
import java.util.UUID;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public class AuthenticationFilterChainControllerTest extends GeoServerTestSupport {

    private static final String TEST_FILTER_PREFIX = "TEST-";

    public AuthenticationFilterChainController controller;
    public GeoServerSecurityManager securityManager;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        controller = applicationContext.getBean(AuthenticationFilterChainController.class);
        securityManager = applicationContext.getBean(GeoServerSecurityManager.class);
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
    // Test endpoints as both json and xml
    // Need one test per type

    @Test
    public void testList() throws SecurityConfigException, IOException {
        ResponseEntity<AuthFilterList> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
    }

    @Test
    public void testGet() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, true);

        ResponseEntity<AuthFilter> result = controller.get(newConfig.getName());
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertEquals("The config name should be the same", newConfig.getName(), result.getBody().getConfig().getName());
        assertEquals("The config className should be the same", newConfig.getClassName(), result.getBody().getConfig().getClassName());
        assertEquals("The config id should be the same", newConfig.getId(), result.getBody().getConfig().getId());

        BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig)result.getBody().getConfig();
        assertEquals("Remember me should be false", actualConfig.isUseRememberMe(), false);
    }
    @Test
    public void testListReturnAllTheFilters() throws SecurityConfigException, IOException {
        ResponseEntity<AuthFilterList> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);

        List<String> names = Objects.requireNonNull(result.getBody())
                .getFilters()
                .stream()
                .map(AuthFilter::getName)
                .collect(Collectors.toList());
        SortedSet<String> filters = securityManager.listFilters();
        assertEquals("Expected all the filters to be returned", filters.size(), names.size());
        filters.stream().filter(name -> !names.contains(name)).map(name -> "Expected filter " + name + " to be returned").forEach(Assert::fail);
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGet_ConfigDoesNotExist() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        controller.get(name);
    }

    @Test
    public void testCreate_BasicAuthenticationFilter() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter();
        authFilter.setName(newConfig.getName());
        authFilter.setClassName(newConfig.getClassName());
        authFilter.setConfig(newConfig);

        ResponseEntity<AuthFilter> result = controller.post(authFilter);
        int status = result.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_CREATED, status);
        assertNotNull("An Id should be returned", result.getBody().getId());
        BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig)result.getBody().getConfig();
        assertEquals("Remember me should be false", actualConfig.isUseRememberMe(), false);
    }

    @Test(expected = AuthenticationFilterChainController.MissingNameException.class)
    public void testCreate_BasicAuthenticationFilter_MissingName() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, false);
        newConfig.setName(null);

        AuthFilter authFilter = new AuthFilter();
        authFilter.setClassName(newConfig.getClassName());
        authFilter.setConfig(newConfig);

        controller.post(authFilter);
    }

    @Test(expected = AuthenticationFilterChainController.DuplicateNameException.class)
    public void testCreate_BasicAuthenticationFilter_ErrorsWithTheSameName() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter();
        authFilter.setName(newConfig.getName());
        authFilter.setClassName(newConfig.getClassName());
        authFilter.setConfig(newConfig);

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
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, false);
        newConfig.setId("123456789");
        AuthFilter authFilter = new AuthFilter();
        authFilter.setName(newConfig.getName());
        authFilter.setClassName(newConfig.getClassName());
        authFilter.setConfig(newConfig);

        controller.post(authFilter);
    }

    @Test
    public void testUpdate_BasicAuthenticationFilter() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = createBasciAuthFilterConfig(name, false);
        AuthFilter authFilter = new AuthFilter();
        authFilter.setName(newConfig.getName());
        authFilter.setClassName(newConfig.getClassName());
        authFilter.setConfig(newConfig);
        controller.post(authFilter);

        newConfig.setUseRememberMe(true);
        ResponseEntity<AuthFilter> result = controller.put(name, authFilter);
        int status = result.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_OK, status);
        assertNotNull("An Id should be returned", result.getBody().getId());
        BasicAuthenticationFilterConfig updatedConfig = (BasicAuthenticationFilterConfig)result.getBody().getConfig();
        assertTrue("Remember me should be true", updatedConfig.isUseRememberMe());
    }

    @Test
    public void testFind() throws SecurityConfigException, IOException {
        ResponseEntity<AuthFilter> result = controller.get("anonymous");
        int status = result.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);
        AuthFilter body = result.getBody();
        assertNotNull("A body should be returned", body);
        assertEquals("Expected the anonymous filter", "anonymous", body.getName());
        assertNotNull("Expected the id to be set", body.getId());
        assertEquals("Expected the name to be set",
                "anonymous",
                body.getConfig().getName());
        assertEquals("Expected the className to be set",
                "org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter",
                body.getConfig().getClassName());
    }


    private BasicAuthenticationFilterConfig createBasciAuthFilterConfig(String name, boolean save) throws IOException, SecurityConfigException {
        BasicAuthenticationFilterConfig basicAuthConfig = new BasicAuthenticationFilterConfig();
        basicAuthConfig.setClassName(GeoServerBasicAuthenticationFilter.class.getName());
        basicAuthConfig.setUseRememberMe(false);
        basicAuthConfig.setName(name);

        if (save) {
            getSecurityManager().saveFilter(basicAuthConfig);
        }
        return basicAuthConfig;
    }

    private LogoutFilterConfig createLogoutFilterConfig(String name, boolean save) throws IOException, SecurityConfigException {
        LogoutFilterConfig logoutConfig = new LogoutFilterConfig();
        logoutConfig.setClassName(GeoServerLogoutFilter.class.getName());
        logoutConfig.setName(name);
        logoutConfig.setRedirectURL(GeoServerLogoutFilter.URL_AFTER_LOGOUT);

        if (save) {
            getSecurityManager().saveFilter(logoutConfig);
        }
        return logoutConfig;
    }

}