/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders;

import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JSON;
import static org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource.JWT;

import jakarta.servlet.Filter;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.SortedSet;
import java.util.stream.Collectors;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.*;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerWebAuthenticationDetails;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.details.JwtHeadersWebAuthenticationDetails;
import org.geoserver.security.validation.SecurityConfigException;
import org.geotools.util.Base64;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.springframework.web.context.request.ServletRequestAttributes;

public class JwtHeadersIntegrationTest extends AbstractAuthenticationProviderTest {

    private static final List<String> TEST_FILTER_CONFIGS = Arrays.asList("JwtHeaders1", "JwtHeaders2", "JwtHeaders3");

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
    }

    protected GeoServerJwtHeadersFilterConfig injectConfig1() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        removeTestFilters(manager);

        GeoServerJwtHeadersFilterConfig filterConfig = new GeoServerJwtHeadersFilterConfig();
        filterConfig.setName("JwtHeaders1");

        filterConfig.setClassName(GeoServerJwtHeadersFilter.class.getName());

        // username
        filterConfig.getJwtConfiguration().setUserNameJsonPath("preferred_username");
        filterConfig.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JSON);
        filterConfig.getJwtConfiguration().setUserNameHeaderAttributeName("json-header");

        // roles
        filterConfig.setRoleSource(JSON);
        filterConfig.getJwtConfiguration().setRoleConverterString("GeoserverAdministrator=ROLE_ADMINISTRATOR");
        filterConfig.getJwtConfiguration().setRolesJsonPath("resource_access.live-key2.roles");
        filterConfig.getJwtConfiguration().setRolesHeaderName("json-header");
        filterConfig.getJwtConfiguration().setOnlyExternalListedRoles(true);

        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        //  www.setFilterNames("JwtHeaders1", "anonymous");
        www.setFilterNames("JwtHeaders1");

        manager.saveSecurityConfig(config);
        return filterConfig;
    }

    protected GeoServerJwtHeadersFilterConfig injectConfig2() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        removeTestFilters(manager);

        GeoServerJwtHeadersFilterConfig filterConfig = new GeoServerJwtHeadersFilterConfig();
        filterConfig.setName("JwtHeaders2");

        filterConfig.setClassName(GeoServerJwtHeadersFilter.class.getName());

        // username
        filterConfig.getJwtConfiguration().setUserNameJsonPath("preferred_username");
        filterConfig.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JWT);
        filterConfig.getJwtConfiguration().setUserNameHeaderAttributeName("json-header");

        // roles
        filterConfig.setRoleSource(JWT);
        filterConfig.getJwtConfiguration().setRoleConverterString("GeoserverAdministrator=ROLE_ADMINISTRATOR");
        filterConfig.getJwtConfiguration().setRolesJsonPath("resource_access.live-key2.roles");
        filterConfig.getJwtConfiguration().setRolesHeaderName("json-header");
        filterConfig.getJwtConfiguration().setOnlyExternalListedRoles(true);

        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");

        www.setFilterNames("JwtHeaders2");

        manager.saveSecurityConfig(config);
        return filterConfig;
    }

    protected GeoServerJwtHeadersFilterConfig injectConfig3() throws Exception {
        GeoServerSecurityManager manager = getSecurityManager();
        removeTestFilters(manager);

        GeoServerJwtHeadersFilterConfig filterConfig = new GeoServerJwtHeadersFilterConfig();
        filterConfig.setName("JwtHeaders3");

        filterConfig.setClassName(GeoServerJwtHeadersFilter.class.getName());

        // username
        filterConfig.getJwtConfiguration().setUserNameJsonPath("preferred_username");
        filterConfig.getJwtConfiguration().setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JWT);
        filterConfig.getJwtConfiguration().setUserNameHeaderAttributeName("Authorization");

        // roles
        filterConfig.setRoleSource(JWT);
        filterConfig.getJwtConfiguration().setRoleConverterString("GeoserverAdministrator=ROLE_ADMINISTRATOR");
        filterConfig.getJwtConfiguration().setRolesJsonPath("resource_access.live-key2.roles");
        filterConfig.getJwtConfiguration().setRolesHeaderName("Authorization");
        filterConfig.getJwtConfiguration().setOnlyExternalListedRoles(true);

        manager.saveFilter(filterConfig);

        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");

        www.setFilterNames("basic", "JwtHeaders3");

        manager.saveSecurityConfig(config);
        return filterConfig;
    }

    private void removeTestFilters(GeoServerSecurityManager manager) throws IOException, SecurityConfigException {
        SortedSet<String> filters = manager.listFilters();
        for (String name : TEST_FILTER_CONFIGS) {
            if (filters.contains(name)) {
                SecurityFilterConfig config = manager.loadFilterConfig(name, false);
                manager.removeFilter(config);
            }
        }
    }

    /** Enable the Spring Security authentication filters, we want the test to be complete and realistic */
    @Override
    protected List<Filter> getFilters() {

        SecurityManagerConfig mconfig = getSecurityManager().getSecurityConfig();
        GeoServerSecurityFilterChain filterChain = mconfig.getFilterChain();
        VariableFilterChain chain = (VariableFilterChain) filterChain.getRequestChainByName("web");
        List<Filter> result = new ArrayList<>();
        for (String filterName : chain.getCompiledFilterNames()) {
            try {
                result.add(getSecurityManager().loadFilter(filterName));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return result;
    }

    String json =
            "{\"exp\":1707155912,\"iat\":1707155612,\"jti\":\"888715ae-a79d-4633-83e5-9b97dee02bbc\",\"iss\":\"https://login-live-dev.geocat.live/realms/dave-test2\",\"aud\":\"account\",\"sub\":\"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d\",\"typ\":\"Bearer\",\"azp\":\"live-key2\",\"session_state\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"acr\":\"1\",\"realm_access\":{\"roles\":[\"default-roles-dave-test2\",\"offline_access\",\"uma_authorization\"]},\"resource_access\":{\"live-key2\":{\"roles\":[\"GeoserverAdministrator\"]},\"account\":{\"roles\":[\"manage-account\",\"manage-account-links\",\"view-profile\"]}},\"scope\":\"openidprofileemail\",\"sid\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"email_verified\":false,\"name\":\"davidblasby\",\"preferred_username\":\"david.blasby@geocat.net\",\"given_name\":\"david\",\"family_name\":\"blasby\",\"email\":\"david.blasby@geocat.net\"}";

    @Test
    public void testSimpleJSON() throws Exception {
        GeoServerJwtHeadersFilterConfig filterConfig = injectConfig1();
        // mimick user pressing on login button
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        int responseStatus = webResponse.getStatus();

        Assert.assertTrue(responseStatus > 400); // access denied

        webRequest = createRequest("web/");
        webRequest.addHeader("json-header", json);

        webResponse = executeOnSecurityFilters(webRequest);

        responseStatus = webResponse.getStatus();

        Assert.assertEquals(responseStatus, 200); // good request

        // get the security context
        Authentication auth = getAuthentication(webRequest, webResponse);

        Assert.assertNotNull(auth);
        Assert.assertEquals(PreAuthenticatedAuthenticationToken.class, auth.getClass());
        Assert.assertEquals(
                JwtHeadersWebAuthenticationDetails.class, auth.getDetails().getClass());

        String authFilterId = ((JwtHeadersWebAuthenticationDetails) auth.getDetails()).getJwtHeadersConfigId();
        Assert.assertEquals(filterConfig.getId(), authFilterId);

        List<String> roles =
                auth.getAuthorities().stream().map(x -> x.getAuthority()).collect(Collectors.toList());
        Assert.assertEquals(2, roles.size());
        Assert.assertTrue(roles.contains("ROLE_ADMINISTRATOR"));
        Assert.assertTrue(roles.contains("ROLE_AUTHENTICATED"));
        int tt = 0;
    }

    String accessToken =
            "eyJhbGciOiJSUzI1NiIsInR5cCIgOiAiSldUIiwia2lkIiA6ICItWEdld190TnFwaWRrYTl2QXNJel82WEQtdnJmZDVyMlNWTWkwcWMyR1lNIn0.eyJleHAiOjE3MDcxNTMxNDYsImlhdCI6MTcwNzE1Mjg0NiwiYXV0aF90aW1lIjoxNzA3MTUyNjQ1LCJqdGkiOiJlMzhjY2ZmYy0zMWNjLTQ0NmEtYmU1Yy04MjliNDE0NTkyZmQiLCJpc3MiOiJodHRwczovL2xvZ2luLWxpdmUtZGV2Lmdlb2NhdC5saXZlL3JlYWxtcy9kYXZlLXRlc3QyIiwiYXVkIjoiYWNjb3VudCIsInN1YiI6ImVhMzNlM2NjLWYwZTEtNDIxOC04OWNiLThkNDhjMjdlZWUzZCIsInR5cCI6IkJlYXJlciIsImF6cCI6ImxpdmUta2V5MiIsIm5vbmNlIjoiQldzc2M3cTBKZ0tHZC1OdFc1QlFhVlROMkhSa25LQmVIY0ZMTHZ5OXpYSSIsInNlc3Npb25fc3RhdGUiOiIxY2FiZmU1NC1lOWU0LTRjMmMtODQwNy03NTZiMjczZmFmZmIiLCJhY3IiOiIwIiwicmVhbG1fYWNjZXNzIjp7InJvbGVzIjpbImRlZmF1bHQtcm9sZXMtZGF2ZS10ZXN0MiIsIm9mZmxpbmVfYWNjZXNzIiwidW1hX2F1dGhvcml6YXRpb24iXX0sInJlc291cmNlX2FjY2VzcyI6eyJsaXZlLWtleTIiOnsicm9sZXMiOlsiR2Vvc2VydmVyQWRtaW5pc3RyYXRvciJdfSwiYWNjb3VudCI6eyJyb2xlcyI6WyJtYW5hZ2UtYWNjb3VudCIsIm1hbmFnZS1hY2NvdW50LWxpbmtzIiwidmlldy1wcm9maWxlIl19fSwic2NvcGUiOiJvcGVuaWQgcGhvbmUgb2ZmbGluZV9hY2Nlc3MgbWljcm9wcm9maWxlLWp3dCBwcm9maWxlIGFkZHJlc3MgZW1haWwiLCJzaWQiOiIxY2FiZmU1NC1lOWU0LTRjMmMtODQwNy03NTZiMjczZmFmZmIiLCJ1cG4iOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImVtYWlsX3ZlcmlmaWVkIjpmYWxzZSwiYWRkcmVzcyI6e30sIm5hbWUiOiJkYXZpZCBibGFzYnkiLCJncm91cHMiOlsiZGVmYXVsdC1yb2xlcy1kYXZlLXRlc3QyIiwib2ZmbGluZV9hY2Nlc3MiLCJ1bWFfYXV0aG9yaXphdGlvbiJdLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCIsImdpdmVuX25hbWUiOiJkYXZpZCIsImZhbWlseV9uYW1lIjoiYmxhc2J5IiwiZW1haWwiOiJkYXZpZC5ibGFzYnlAZ2VvY2F0Lm5ldCJ9.fHzXd7oISnqWb09ah9wikfP2UOBeiOA3vd_aDg3Bw-xcfv9aD3CWhAK5FUDPYSPyj4whAcknZbUgUzcm0qkaI8V_aS65F3Fug4jt4nC9YPL4zMSJ5an4Dp6jlQ3OQhrKFn4FwaoW61ndMmScsZZWEQyj6gzHnn5cknqySB26tVydT6q57iTO7KQFcXRdbXd6GWIoFGS-ud9XzxQMUdNfYmsDD7e6hoWhe9PJD9Zq4KT6JN13hUU4Dos-Z5SBHjRa6ieHoOe9gqkjKyA1jT1NU42Nqr-mTV-ql22nAoXuplpvOYc5-09-KDDzSDuVKFwLCNMN3ZyRF1wWuydJeU-gOQ";

    @Test
    public void testSimpleJWT() throws Exception {
        GeoServerJwtHeadersFilterConfig filterConfig = injectConfig2();
        // mimick user pressing on login button
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        int responseStatus = webResponse.getStatus();

        Assert.assertTrue(responseStatus > 400); // access denied

        webRequest = createRequest("web/");
        webRequest.addHeader("json-header", accessToken);

        webResponse = executeOnSecurityFilters(webRequest);

        responseStatus = webResponse.getStatus();

        Assert.assertEquals(responseStatus, 200); // good request

        Authentication auth = getAuthentication(webRequest, webResponse);

        Assert.assertNotNull(auth);
        Assert.assertEquals(PreAuthenticatedAuthenticationToken.class, auth.getClass());
        Assert.assertEquals(
                JwtHeadersWebAuthenticationDetails.class, auth.getDetails().getClass());

        String authFilterId = ((JwtHeadersWebAuthenticationDetails) auth.getDetails()).getJwtHeadersConfigId();
        Assert.assertEquals(filterConfig.getId(), authFilterId);

        List<String> roles =
                auth.getAuthorities().stream().map(x -> x.getAuthority()).collect(Collectors.toList());
        Assert.assertEquals(2, roles.size());
        Assert.assertTrue(roles.contains("ROLE_ADMINISTRATOR"));
        Assert.assertTrue(roles.contains("ROLE_AUTHENTICATED"));
        int tt = 0;
    }

    @Test
    public void testLogout() throws Exception {
        GeoServerJwtHeadersFilterConfig filterConfig = injectConfig2();
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        // no token, no access
        int responseStatus = webResponse.getStatus();
        Assert.assertTrue(responseStatus > 400);
        HttpSession session = webRequest.getSession();

        // add token - should have access
        webRequest = createRequest("web/");
        webRequest.setSession(session);
        webRequest.addHeader("json-header", accessToken);

        webResponse = executeOnSecurityFilters(webRequest);
        responseStatus = webResponse.getStatus();
        Assert.assertEquals(responseStatus, 200);

        session = webRequest.getSession();
        // remove token - should not have access
        webRequest = createRequest("web/");
        webRequest.setSession(session);

        webResponse = executeOnSecurityFilters(webRequest);
        responseStatus = webResponse.getStatus();
        Assert.assertTrue(responseStatus > 400);
    }

    String json2 =
            "{\"exp\":1707155912,\"iat\":1707155612,\"jti\":\"888715ae-a79d-4633-83e5-9b97dee02bbc\",\"iss\":\"https://login-live-dev.geocat.live/realms/dave-test2\",\"aud\":\"account\",\"sub\":\"ea33e3cc-f0e1-4218-89cb-8d48c27eee3d\",\"typ\":\"Bearer\",\"azp\":\"live-key2\",\"session_state\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"acr\":\"1\",\"realm_access\":{\"roles\":[\"default-roles-dave-test2\",\"offline_access\",\"uma_authorization\"]},\"resource_access\":{\"live-key2\":{\"roles\":[\"GeoserverAdministrator\"]},\"account\":{\"roles\":[\"manage-account\",\"manage-account-links\",\"view-profile\"]}},\"scope\":\"openidprofileemail\",\"sid\":\"ae7796fa-b374-4754-a294-e0eb834b23b5\",\"email_verified\":false,\"name\":\"davidblasby\",\"preferred_username\":\"david.blasby22@geocat.net\",\"given_name\":\"david\",\"family_name\":\"blasby\",\"email\":\"david.blasby@geocat.net\"}";

    @Test
    public void testUserNameChange() throws Exception {
        GeoServerJwtHeadersFilterConfig filterConfig = injectConfig1();
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        // no token, no access
        int responseStatus = webResponse.getStatus();
        Assert.assertTrue(responseStatus > 400);
        HttpSession session = webRequest.getSession();

        // add token - should have access
        webRequest = createRequest("web/");
        webRequest.setSession(session);
        webRequest.addHeader("json-header", json);

        webResponse = executeOnSecurityFilters(webRequest);
        responseStatus = webResponse.getStatus();
        Assert.assertEquals(responseStatus, 200);

        Authentication auth = getAuthentication(webRequest, webResponse);

        Assert.assertEquals("david.blasby@geocat.net", auth.getPrincipal());

        // use different json with different username!
        webRequest = createRequest("web/");
        webRequest.setSession(session);
        webRequest.addHeader("json-header", json2);
        webResponse = executeOnSecurityFilters(webRequest);
        responseStatus = webResponse.getStatus();
        Assert.assertEquals(responseStatus, 200);

        auth = getAuthentication(webRequest, webResponse);

        Assert.assertEquals("david.blasby22@geocat.net", auth.getPrincipal());
    }

    // admin:geoserver
    String basicAuthAuthorizationHeader = "Basic " + Base64.encodeBytes((testUserName + ":" + testPassword).getBytes());

    String jwtAuthorizationHeader = "Bearer " + accessToken;

    @Test
    public void testWithAuthorizationHeader() throws Exception {
        // note: super class AbstractAuthenticationProviderTest sets up the test user for basic auth
        GeoServerJwtHeadersFilterConfig filterConfig = injectConfig3();
        // mimick user pressing on login button
        MockHttpServletRequest webRequest = createRequest("web/");
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);

        int responseStatus = webResponse.getStatus();

        Assert.assertTrue(responseStatus > 400); // access denied

        // basic auth filter should process "Authorization: Basic ..." header
        webRequest = createRequest("web/");
        webRequest.addHeader("Authorization", basicAuthAuthorizationHeader);

        webResponse = executeOnSecurityFilters(webRequest);

        responseStatus = webResponse.getStatus();

        Assert.assertEquals(200, responseStatus); // good request

        Authentication auth = getAuthentication(webRequest, webResponse);

        Assert.assertNotNull(auth);
        Assert.assertEquals(UsernamePasswordAuthenticationToken.class, auth.getClass());
        Assert.assertEquals(
                GeoServerWebAuthenticationDetails.class, auth.getDetails().getClass());

        // JWT header filter should process "Authorization: Bearer ..." header
        webRequest = createRequest("web/");
        webRequest.addHeader("Authorization", jwtAuthorizationHeader);

        webResponse = executeOnSecurityFilters(webRequest);

        responseStatus = webResponse.getStatus();

        Assert.assertEquals(200, responseStatus); // good request

        auth = getAuthentication(webRequest, webResponse);

        Assert.assertNotNull(auth);
        Assert.assertEquals(PreAuthenticatedAuthenticationToken.class, auth.getClass());
        Assert.assertEquals(
                JwtHeadersWebAuthenticationDetails.class, auth.getDetails().getClass());

        String authFilterId = ((JwtHeadersWebAuthenticationDetails) auth.getDetails()).getJwtHeadersConfigId();
        Assert.assertEquals(filterConfig.getId(), authFilterId);

        List<String> roles =
                auth.getAuthorities().stream().map(x -> x.getAuthority()).collect(Collectors.toList());
        Assert.assertEquals(2, roles.size());
        Assert.assertTrue(roles.contains("ROLE_ADMINISTRATOR"));
        Assert.assertTrue(roles.contains("ROLE_AUTHENTICATED"));
        int tt = 0;
    }

    public Authentication getAuthentication(MockHttpServletRequest request, MockHttpServletResponse response) {
        SecurityContext context = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(request, response));
        Authentication auth = context.getAuthentication();
        return auth;
    }

    public HttpSession getSession() {
        ServletRequestAttributes attr = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
        HttpSession session = attr.getRequest().getSession();
        return session;
    }

    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {
        // for session local support in Spring
        new RequestContextListener().requestInitialized(new ServletRequestEvent(request.getServletContext(), request));

        // run on the
        MockFilterChain chain = new MockFilterChain();
        MockHttpServletResponse response = new MockHttpServletResponse();
        GeoServerSecurityFilterChainProxy filterChainProxy =
                GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        filterChainProxy.doFilter(request, response, chain);

        return response;
    }
}
