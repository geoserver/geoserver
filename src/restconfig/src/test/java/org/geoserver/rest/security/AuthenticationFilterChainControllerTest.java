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
import org.geoserver.rest.wrapper.RestWrapper;
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
        ResponseEntity<RestWrapper<AuthFilterList>> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        AuthFilterList authFilterList = (AuthFilterList) result.getBody().getObject();
        authFilterList.getFilters().forEach(filter -> {
            assertNotNull(filter.getId());
            assertNotNull(filter.getName());
            assertNotNull(filter.getConfig());
        });
    }

    @Test
    public void testGet() throws SecurityConfigException, IOException {
        String name = TEST_FILTER_PREFIX + "create-" + UUID.randomUUID();
        BasicAuthenticationFilterConfig newConfig = securityConfigFilterHelper.createBasciAuthFilterConfig(name, true);

        ResponseEntity<RestWrapper<AuthFilter>> result = controller.get(newConfig.getName());
        int status = result.getStatusCode().value();
        assertEquals("Expected an OK response", HttpStatus.SC_OK, status);
        assertNotNull(Objects.requireNonNull(result.getBody()).getObject());
        SecurityFilterConfig config = ((AuthFilter) result.getBody().getObject()).getConfig();
        assertEquals("The config name should be the same", newConfig.getName(), config.getName());
        assertEquals("The config className should be the same", newConfig.getClassName(), config.getClassName());
        assertEquals("The config id should be the same", newConfig.getId(), config.getId());

        BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig) config;
        assertFalse("Remember me should be false", actualConfig.isUseRememberMe());
    }

    @Test
    public void testListReturnAllTheFilters() throws IOException {
        ResponseEntity<RestWrapper<AuthFilterList>> result = controller.list();
        int status = result.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);

        AuthFilterList list = (AuthFilterList)
                Objects.requireNonNull(Objects.requireNonNull(result.getBody()).getObject());
        List<String> names = list.getFilters().stream().map(AuthFilter::getName).collect(Collectors.toList());
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

        ResponseEntity<RestWrapper<AuthFilter>> postResult = controller.post(authFilter);
        int status = postResult.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_CREATED, status);
        AuthFilter postFilter = (AuthFilter) Objects.requireNonNull(
                Objects.requireNonNull(postResult.getBody()).getObject());
        assertNotNull("An Id should be returned", postFilter.getId());
        actualConfig = (BasicAuthenticationFilterConfig) postFilter.getConfig();
        assertFalse("Remember me should be false", actualConfig.isUseRememberMe());

        ResponseEntity<RestWrapper<AuthFilter>> getResult = controller.get(newConfig.getName());
        int getStatus = getResult.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, getStatus);
        AuthFilter getFilter = (AuthFilter) Objects.requireNonNull(
                Objects.requireNonNull(getResult.getBody()).getObject());
        assertNotNull("An Id should be returned", getFilter.getId());
        actualConfig = (BasicAuthenticationFilterConfig) getFilter.getConfig();
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
        AuthFilter createdFilter =
                (AuthFilter) controller.post(authFilter).getBody().getObject();

        ResponseEntity<RestWrapper<AuthFilter>> result = controller.put(name, createdFilter);
        int status = result.getStatusCode().value();
        assertEquals("Expected a CREATED response", HttpStatus.SC_OK, status);
        AuthFilter putFilter = (AuthFilter)
                Objects.requireNonNull(Objects.requireNonNull(result.getBody()).getObject());
        assertNotNull("An Id should be returned", putFilter.getId());

        ResponseEntity<RestWrapper<AuthFilter>> getResult = controller.get(newConfig.getName());
        int getStatus = getResult.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, getStatus);
        AuthFilter getFilter = (AuthFilter)
                Objects.requireNonNull(Objects.requireNonNull(result.getBody()).getObject());
        assertNotNull("An Id should be returned", getFilter.getId());
        BasicAuthenticationFilterConfig actualConfig = (BasicAuthenticationFilterConfig) getFilter.getConfig();
        assertNotNull(actualConfig);
    }

    @Test
    public void testFind() throws IOException {
        ResponseEntity<RestWrapper<AuthFilter>> getResult = controller.get("anonymous");
        int status = getResult.getStatusCode().value();
        assertEquals("Expected a OK response", HttpStatus.SC_OK, status);
        AuthFilter body = (AuthFilter) Objects.requireNonNull(
                Objects.requireNonNull(getResult.getBody()).getObject());
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
