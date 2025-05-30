package org.geoserver.rest.security;

import org.geoserver.rest.security.AuthenticationFilterChainRestController.BadRequest;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.DuplicateChainName;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.FilterChainNotFound;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.NothingToDelete;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.rest.security.xml.AuthFilterChainList;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

public class AuthenticationFilterChainRestControllerTest extends GeoServerTestSupport {
    private static final String DEFAULT_CHAIN_NAME = "default";
    private static final String TEST_CHAIN_NAME_PREFIX = "TEST-";
    public static final String ROLE_FILTER_NAME = null; // TODO find an actual role
    private static final List<String> TEST_FILTERS = List.of("basic", "anonymous"); // TODO find an actual filter name
    public static final boolean ALLOW_SESSION_CREATION_FLAG = true;
    public static final boolean DISABLED_FLAG = true;
    public static final boolean REQUIRE_SSL_FLAG = true;
    public static final String CLASS_NAME = HtmlLoginFilterChain.class.getName();
    public static final Set<String> HTTP_METHODS = Set.of("GET", "POST");
    public static final Set<String> NEW_HTTP_METHODS = Set.of("GET");
    public static final List<String> PATTERNS = List.of("/test/path1/*", "/test/path2/*");
    public static final int POSITION = 1;
    public static final int NEW_POSITION = 2;
    public static final boolean MATCH_HTTP_METHOD_FLAG = true;
    public static final String NEW_ROLE_FILTER_NAME = null; // TODO find an alternative
    private static final List<String> NEW_TEST_FILTERS = List.of("basic"); // TODO find an actual filter name
    private static final List<String> NEW_PATTERNS = List.of("/test/path1/*");

    private AuthenticationFilterChainRestController controller;

    @Override
    @Before
    public void oneTimeSetUp() throws Exception {
        setValidating(true);
        super.oneTimeSetUp();
        GeoServerSecurityManager securityManager = applicationContext.getBean(GeoServerSecurityManager.class);
        controller = new AuthenticationFilterChainRestController(securityManager);
    }

    @Test
    public void testListFilterChains() {
        ResponseEntity<RestWrapper<AuthFilterChainList>> response = controller.list();
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthFilterChainList authFilterChainList = (AuthFilterChainList) Objects.requireNonNull(
                Objects.requireNonNull(response.getBody()).getObject());
        assertNotNull(authFilterChainList);
        authFilterChainList.getFilterChains().stream()
                .filter(chain -> chain.getName().equals(DEFAULT_CHAIN_NAME))
                .findFirst()
                .ifPresentOrElse(
                        authFilterChain -> assertEquals(DEFAULT_CHAIN_NAME, authFilterChain.getName()),
                        () -> fail("No default message"));
    }

    @Test
    public void testViewFilterChain() {
        ResponseEntity<RestWrapper<AuthFilterChain>> response = controller.view(DEFAULT_CHAIN_NAME);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        AuthFilterChain authFilterChain =
                (AuthFilterChain) Objects.requireNonNull(response.getBody()).getObject();
        assertNotNull(authFilterChain);
        assertEquals(DEFAULT_CHAIN_NAME, authFilterChain.getName());
    }

    @Test(expected = FilterChainNotFound.class)
    public void testViewFilterChain_Unknown() {
        controller.view("UnknownName");
    }

    @Test
    public void testCreateFilterChain() {
        AuthFilterChain authFilterChain = createNewAuthFilterChain();
        ResponseEntity<RestWrapper<AuthFilterChain>> response = controller.create(authFilterChain);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        // Check it is accessible
        ResponseEntity<RestWrapper<AuthFilterChain>> viewResponse = controller.view(authFilterChain.getName());
        assertEquals(HttpStatus.OK, viewResponse.getStatusCode());
        AuthFilterChain viewFilterChain =
                (AuthFilterChain) Objects.requireNonNull(viewResponse.getBody()).getObject();
        assertNotNull(viewFilterChain);
        assertEquals(authFilterChain.getName(), viewFilterChain.getName());
        assertEquals(authFilterChain.getFilters(), viewFilterChain.getFilters());
        assertEquals(authFilterChain.getRoleFilterName(), viewFilterChain.getRoleFilterName());
        assertEquals(authFilterChain.getClassName(), viewFilterChain.getClassName());
        assertEquals(authFilterChain.getHttpMethods(), viewFilterChain.getHttpMethods());
        assertEquals(authFilterChain.getPatterns(), viewFilterChain.getPatterns());
        assertEquals(authFilterChain.getPosition(), viewFilterChain.getPosition());
        assertEquals(authFilterChain.isAllowSessionCreation(), viewFilterChain.isAllowSessionCreation());
        assertEquals(authFilterChain.isDisabled(), viewFilterChain.isDisabled());
        assertEquals(authFilterChain.isRequireSSL(), viewFilterChain.isRequireSSL());
        assertEquals(authFilterChain.isMatchHTTPMethod(), viewFilterChain.isMatchHTTPMethod());
    }

    @Test(expected = DuplicateChainName.class)
    public void testCreateFilterChain_duplicateName() {
        AuthFilterChain authFilterChain = createNewAuthFilterChain();
        ResponseEntity<RestWrapper<AuthFilterChain>> response = controller.create(authFilterChain);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        controller.create(authFilterChain);
    }

    @Test
    public void testUpdateFilterChain() {
        AuthFilterChain authFilterChain = createNewAuthFilterChain();
        ResponseEntity<RestWrapper<AuthFilterChain>> response = controller.create(authFilterChain);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        AuthFilterChain updatedAuthFilterChain = updateAuthFilterChain(authFilterChain);
        ResponseEntity<RestWrapper<AuthFilterChain>> updatedResponse =
                controller.update(updatedAuthFilterChain.getName(), updatedAuthFilterChain);
        AuthFilterChain responseFilterChain = (AuthFilterChain)
                Objects.requireNonNull(updatedResponse.getBody()).getObject();
        assertEquals(HttpStatus.OK, updatedResponse.getStatusCode());
        assertEquals(updatedAuthFilterChain.getName(), responseFilterChain.getName());
        assertEquals(updatedAuthFilterChain.getFilters(), responseFilterChain.getFilters());
        assertEquals(updatedAuthFilterChain.getRoleFilterName(), responseFilterChain.getRoleFilterName());
        assertEquals(updatedAuthFilterChain.getClassName(), responseFilterChain.getClassName());
        assertEquals(updatedAuthFilterChain.getHttpMethods(), responseFilterChain.getHttpMethods());
        assertEquals(updatedAuthFilterChain.getPatterns(), responseFilterChain.getPatterns());
        assertEquals(updatedAuthFilterChain.getPosition(), responseFilterChain.getPosition());
        assertEquals(updatedAuthFilterChain.isAllowSessionCreation(), responseFilterChain.isAllowSessionCreation());
        assertEquals(updatedAuthFilterChain.isDisabled(), responseFilterChain.isDisabled());
        assertEquals(updatedAuthFilterChain.isRequireSSL(), responseFilterChain.isRequireSSL());
        assertEquals(updatedAuthFilterChain.isMatchHTTPMethod(), responseFilterChain.isMatchHTTPMethod());
    }

    @Test(expected = BadRequest.class)
    public void testUpdateFilterChain_MismatchName() {
        AuthFilterChain authFilterChain = createNewAuthFilterChain();
        controller.update("unKnown", authFilterChain);
    }

    @Test
    public void testDeleteFilterChain() {
        AuthFilterChain authFilterChain = createNewAuthFilterChain();
        ResponseEntity<RestWrapper<AuthFilterChain>> response = controller.create(authFilterChain);
        assertEquals(HttpStatus.CREATED, response.getStatusCode());

        ResponseEntity<RestWrapper<AuthFilterChain>> deletedResponse = controller.delete(authFilterChain.getName());
        assertEquals(HttpStatus.OK, deletedResponse.getStatusCode());
    }

    @Test(expected = NothingToDelete.class)
    public void testDeleteFilterChain_Unknown() {
        controller.delete("UnknownName");
    }

    @Test(expected = BadRequest.class)
    public void testDeleteFilterChain_cannotBeRemoved() {
        controller.delete("webLogout");
    }

    public static AuthFilterChain createNewAuthFilterChain() {
        AuthFilterChain authFilterChain = new AuthFilterChain();
        authFilterChain.setName(TEST_CHAIN_NAME_PREFIX + UUID.randomUUID());

        authFilterChain.setRoleFilterName(ROLE_FILTER_NAME);
        authFilterChain.setFilters(TEST_FILTERS);
        authFilterChain.setAllowSessionCreation(ALLOW_SESSION_CREATION_FLAG);
        authFilterChain.setDisabled(DISABLED_FLAG);
        authFilterChain.setRequireSSL(REQUIRE_SSL_FLAG);
        authFilterChain.setClassName(CLASS_NAME);
        authFilterChain.setHttpMethods(HTTP_METHODS);
        authFilterChain.setPatterns(PATTERNS);
        authFilterChain.setPosition(POSITION);
        authFilterChain.setMatchHTTPMethod(MATCH_HTTP_METHOD_FLAG);

        return authFilterChain;
    }

    public static AuthFilterChain updateAuthFilterChain(AuthFilterChain authFilterChain) {

        authFilterChain.setRoleFilterName(NEW_ROLE_FILTER_NAME);
        authFilterChain.setFilters(NEW_TEST_FILTERS);
        authFilterChain.setAllowSessionCreation(!ALLOW_SESSION_CREATION_FLAG);
        authFilterChain.setDisabled(!DISABLED_FLAG);
        authFilterChain.setRequireSSL(!REQUIRE_SSL_FLAG);
        authFilterChain.setHttpMethods(NEW_HTTP_METHODS);
        authFilterChain.setPatterns(NEW_PATTERNS);
        authFilterChain.setPosition(NEW_POSITION);
        authFilterChain.setMatchHTTPMethod(!MATCH_HTTP_METHOD_FLAG);

        return authFilterChain;
    }
}
