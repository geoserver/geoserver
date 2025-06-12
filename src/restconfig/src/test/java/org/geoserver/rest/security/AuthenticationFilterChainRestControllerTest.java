/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.BadRequest;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.DuplicateChainName;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.FilterChainNotFound;
import org.geoserver.rest.security.AuthenticationFilterChainRestController.NothingToDelete;
import org.geoserver.rest.security.xml.AuthFilterChain;
import org.geoserver.rest.wrapper.RestWrapper;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HtmlLoginFilterChain;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.util.UriComponentsBuilder;

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

    public void setUser() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "admin", "password", Collections.singletonList(new SimpleGrantedAuthority("ROLE_ADMINISTRATOR")));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    private void clearUser() {
        SecurityContextHolder.clearContext();
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testListFilterChains() {
        setUser();

        try {
            RestWrapper<AuthFilterChain> response = controller.list();
            List<AuthFilterChain> authFilterChainList =
                    (List<AuthFilterChain>) Objects.requireNonNull(response).getObject();
            assertNotNull(authFilterChainList);
            authFilterChainList.stream()
                    .filter(chain -> chain.getName().equals(DEFAULT_CHAIN_NAME))
                    .findFirst()
                    .ifPresentOrElse(
                            authFilterChain -> assertEquals(DEFAULT_CHAIN_NAME, authFilterChain.getName()),
                            () -> fail("No default message"));
        } finally {
            clearUser();
        }
    }

    @Test
    public void testViewFilterChain() {
        setUser();
        try {
            RestWrapper<AuthFilterChain> response = controller.view(DEFAULT_CHAIN_NAME);
            AuthFilterChain authFilterChain = (AuthFilterChain) Objects.requireNonNull(response.getObject());
            assertNotNull(authFilterChain);
            assertEquals(DEFAULT_CHAIN_NAME, authFilterChain.getName());
        } finally {
            clearUser();
        }
    }

    @Test(expected = FilterChainNotFound.class)
    public void testViewFilterChain_Unknown() {
        setUser();
        try {
            controller.view("UnknownName");
        } finally {
            clearUser();
        }
    }

    @Test
    public void testCreateFilterChain() {
        setUser();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            AuthFilterChain authFilterChain = createNewAuthFilterChain();
            controller.create(authFilterChain, builder);

            // Check it is accessible
            RestWrapper<AuthFilterChain> viewResponse = controller.view(authFilterChain.getName());
            AuthFilterChain viewFilterChain = (AuthFilterChain) Objects.requireNonNull(viewResponse.getObject());
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
        } finally {
            clearUser();
        }
    }

    @Test(expected = DuplicateChainName.class)
    public void testCreateFilterChain_duplicateName() {
        setUser();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            AuthFilterChain authFilterChain = createNewAuthFilterChain();
            controller.create(authFilterChain, builder);
            controller.create(authFilterChain, builder);
        } finally {
            clearUser();
        }
    }

    @Test
    public void testUpdateFilterChain() {
        setUser();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
            AuthFilterChain authFilterChain = createNewAuthFilterChain();
            controller.create(authFilterChain, builder);

            AuthFilterChain updatedAuthFilterChain = updateAuthFilterChain(authFilterChain);
            controller.update(updatedAuthFilterChain.getName(), updatedAuthFilterChain);
            RestWrapper<AuthFilterChain> responseFilterChainWrapper = controller.view(updatedAuthFilterChain.getName());
            AuthFilterChain responseFilterChain =
                    (AuthFilterChain) Objects.requireNonNull(responseFilterChainWrapper.getObject());
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
        } finally {

            clearUser();
        }
    }

    @Test(expected = BadRequest.class)
    public void testUpdateFilterChain_MismatchName() {
        setUser();
        try {
            AuthFilterChain authFilterChain = createNewAuthFilterChain();
            controller.update("unKnown", authFilterChain);
        } finally {
            clearUser();
        }
    }

    @Test
    public void testDeleteFilterChain() {
        setUser();
        try {
            UriComponentsBuilder builder = UriComponentsBuilder.newInstance();

            AuthFilterChain authFilterChain = createNewAuthFilterChain();
            controller.create(authFilterChain, builder);

            controller.delete(authFilterChain.getName());
            try {
                controller.view(authFilterChain.getName());
                fail("Expected there to not exist");
            } catch (FilterChainNotFound e) {
                // expected
            }
        } finally {
            clearUser();
        }
    }

    @Test(expected = NothingToDelete.class)
    public void testDeleteFilterChain_Unknown() {
        setUser();
        try {
            controller.delete("UnknownName");
        } finally {
            clearUser();
        }
    }

    @Test(expected = BadRequest.class)
    public void testDeleteFilterChain_cannotBeRemoved() {
        setUser();
        try {
            controller.delete("webLogout");
        } finally {
            clearUser();
        }
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
