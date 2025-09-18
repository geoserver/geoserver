package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.*;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.servlet.ServletRequestEvent;
import javax.servlet.http.HttpSession;
import org.apache.http.NameValuePair;
import org.apache.http.client.utils.URLEncodedUtils;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginAuthenticationFilter;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.web.GeoServerHomePage;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.context.request.RequestContextListener;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.ImmutablePair;
import org.testcontainers.shaded.org.apache.commons.lang3.tuple.Pair;

public class KeyCloakIntegrationTest extends KeyCloakIntegrationTestSupport {

    String oidcLogin_responseType = "code";
    String oidcLogin_client_id = "gs-client";
    String oidcLogin_scope = "openid profile email phone address";
    String oidcLogin_redirect_uri = "http://localhost:8080/geoserver/web/login/oauth2/code/oidc";

    @Override
    protected String getLogConfiguration() {
        return "VERBOSE_LOGGING";
    }

    @BeforeClass
    public static void beforeClassLocal() {
        System.setProperty("OPENID_TEST_GS_PROXY_BASE", "http://localhost/geoserver");
    }

    @Test
    public void test_login_as_admin() throws Exception {
        var auth = login("admin", "admin");

        assertTrue(auth.isAuthenticated());
        assertEquals("oidc", auth.getAuthorizedClientRegistrationId());

        assertEquals("admin@example.com", auth.getPrincipal().getName());
        assertEquals("admin@example.com", auth.getName());

        assertEquals(
                "ROLE_AUTHENTICATED",
                auth.getAuthorities().stream().toList().get(0).getAuthority());

        assertEquals("admin", auth.getPrincipal().getAttributes().get("preferred_username"));
        assertEquals("gs-client", auth.getPrincipal().getAttributes().get("azp"));
        assertEquals("admin", auth.getPrincipal().getAttributes().get("family_name"));
        assertEquals("admin admin", auth.getPrincipal().getAttributes().get("name"));

        // get IDTOKEN.resource_access.gs-client.roles
        // this should be "geoserverAdmin" (from keycloak)
        var principal = (DefaultOidcUser) auth.getPrincipal();
        var resourceAccess = (Map) principal.getIdToken().getClaim("resource_access");
        assertNotNull(resourceAccess);
        var gsClient = (Map) resourceAccess.get("gs-client");
        assertNotNull(gsClient);
        var gsClientRoles = ((List) gsClient.get("roles"));
        assertEquals("geoserverAdmin", gsClientRoles.get(0));
    }

    @Test
    public void test_login_as_user_sample1() throws Exception {
        var auth = login("user_sample1", "user_sample1");

        assertTrue(auth.isAuthenticated());
        assertEquals("oidc", auth.getAuthorizedClientRegistrationId());

        assertEquals("user_sample1@example.com", auth.getPrincipal().getName());
        assertEquals("user_sample1@example.com", auth.getName());

        assertEquals(
                "ROLE_AUTHENTICATED",
                auth.getAuthorities().stream().toList().get(0).getAuthority());

        assertEquals("user_sample1", auth.getPrincipal().getAttributes().get("preferred_username"));
        assertEquals("gs-client", auth.getPrincipal().getAttributes().get("azp"));
        assertEquals("sample1", auth.getPrincipal().getAttributes().get("family_name"));
    }

    public OAuth2AuthenticationToken login(String keycloakUserName, String keycloakPassword) throws Exception {
        tester.startPage(new GeoServerHomePage());
        var html = tester.getLastResponseAsString();

        // 1. verify that there's a login button for oidc
        assertTrue(html.contains(
                "<a class=\"d-inline-block\" href=\"http://localhost/context/web/oauth2/authorization/oidc\">"));

        // 2. lets "press" the oidc login link:
        MockHttpServletRequest webRequest = createRequest("web/oauth2/authorization/oidc", true);
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);
        var session = webRequest.getSession();

        // should be a 302 redirect to keycloak to start the login process
        var state_nonce = validateRedirectToKeyCloak(webResponse);
        var oidcLogin_state = state_nonce.getLeft();
        var oidcLogin_nonce = state_nonce.getRight();

        var auth = keycloakLogin(session, oidcLogin_state, oidcLogin_nonce, keycloakUserName, keycloakPassword);

        return auth;
    }

    private Pair<String, String> validateRedirectToKeyCloak(MockHttpServletResponse webResponse)
            throws URISyntaxException {
        // should be a 302 redirect to keycloak to start the login process
        assertEquals(302, webResponse.getStatus());
        assertNotNull(webResponse.getHeader("Location"));
        var redirectURL = webResponse.getHeader("Location");

        assertTrue(redirectURL.startsWith(authServerUrl));

        List<NameValuePair> params = URLEncodedUtils.parse(new URI(redirectURL), Charset.forName("UTF-8"));

        assertEquals(
                oidcLogin_responseType,
                params.stream()
                        .filter(x -> x.getName().equals("response_type"))
                        .findFirst()
                        .get()
                        .getValue());
        assertEquals(
                oidcLogin_client_id,
                params.stream()
                        .filter(x -> x.getName().equals("client_id"))
                        .findFirst()
                        .get()
                        .getValue());
        assertEquals(
                oidcLogin_redirect_uri,
                params.stream()
                        .filter(x -> x.getName().equals("redirect_uri"))
                        .findFirst()
                        .get()
                        .getValue());
        assertEquals(
                oidcLogin_scope,
                params.stream()
                        .filter(x -> x.getName().equals("scope"))
                        .findFirst()
                        .get()
                        .getValue());

        var oidcLogin_state = params.stream()
                .filter(x -> x.getName().equals("state"))
                .findFirst()
                .get()
                .getValue();
        String oidcLogin_nonce = params.stream()
                .filter(x -> x.getName().equals("nonce"))
                .findFirst()
                .get()
                .getValue();

        return new ImmutablePair<String, String>(oidcLogin_state, oidcLogin_nonce);
    }

    private OAuth2AuthenticationToken keycloakLogin(
            HttpSession session, String oidcLoginState, String oidcLoginNonce, String username, String password)
            throws Exception {
        // send request to keycloak to start the login process (will return with username/password form)
        var startKeyCloakResponse = executeKeycloakStartUrl(oidcLoginState, oidcLoginNonce);
        assertEquals(200, startKeyCloakResponse.statusCode);

        // send keycloak the completed username/password form
        var keycloakResponseSubmitUserPassword =
                executeKeycloakResponseSubmitUserPassword(startKeyCloakResponse, username, password);
        assertEquals(302, keycloakResponseSubmitUserPassword.statusCode);
        var redirectCodeToGS =
                keycloakResponseSubmitUserPassword.headers.get("Location").get(0);
        // should be redirecting to GS's code endpoint
        assertTrue(redirectCodeToGS.startsWith("http://localhost:8080/geoserver/web/login/oauth2/code/oidc"));

        var shortenedRedirectCodeToGS = redirectCodeToGS.substring("http://localhost:8080/geoserver/".length());
        MockHttpServletRequest webRequest = createRequest(shortenedRedirectCodeToGS);
        webRequest.setSession(session);
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);
        var securityContext = new HttpSessionSecurityContextRepository()
                .loadContext(new HttpRequestResponseHolder(webRequest, webResponse));
        assertNotNull(securityContext);
        assertNotNull(securityContext.getAuthentication());
        assertTrue(securityContext.getAuthentication() instanceof OAuth2AuthenticationToken);
        var authentication = (OAuth2AuthenticationToken) securityContext.getAuthentication();

        return authentication;
    }

    /**
     * keycloak's username/password form has an action with "extra" params in it - submit
     *
     * @param startKeyCloakResponse
     * @param username
     * @param password
     * @return
     * @throws Exception
     */
    public WebRequests.WebResponse executeKeycloakResponseSubmitUserPassword(
            WebRequests.WebResponse startKeyCloakResponse, String username, String password) throws Exception {
        Pattern pattern =
                Pattern.compile(".* action=\"([^\"]+)\".*", Pattern.DOTALL); // Compile the regex into a Pattern object
        Matcher matcher = pattern.matcher(startKeyCloakResponse.body);
        if (!matcher.matches()) {
            throw new Exception("keycloak - couldnt find the login form's action url");
        }
        String postUrl = matcher.group(1).replaceAll("&amp;", "&");
        String postBody = "username=" + username + "&password=" + password + "&credentialId=";
        var response = WebRequests.webRequestPOSTForm(postUrl, postBody, startKeyCloakResponse.cookieManager);
        return response;
    }

    public WebRequests.WebResponse executeKeycloakStartUrl(String oidcLoginState, String oidcLoginNonce)
            throws Exception {
        String startUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/auth?";
        startUrl += "client_id=" + oidcLogin_client_id;
        startUrl += "&response_type=" + oidcLogin_responseType;
        startUrl += "&scope=" + URLEncoder.encode(oidcLogin_scope, StandardCharsets.UTF_8);
        startUrl += "&redirect_uri=" + oidcLogin_redirect_uri;
        startUrl += "&state=" + oidcLoginState;
        startUrl += "&nonce=" + oidcLoginNonce;
        return WebRequests.webRequestGET(startUrl);
    }

    private MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, javax.servlet.ServletException {
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

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        var baseKeycloakUrl = keycloakContainer.getAuthServerUrl() + "/realms/gs-realm/protocol/openid-connect";
        // setup openid
        GeoServerSecurityManager manager = getSecurityManager();
        GeoServerOAuth2LoginFilterConfig filterConfig = new GeoServerOAuth2LoginFilterConfig();
        filterConfig.setName("openidconnect");
        filterConfig.setClassName(GeoServerOAuth2LoginAuthenticationFilter.class.getName());
        filterConfig.setOidcEnabled(true);
        filterConfig.setOidcClientId(oidcClient);
        filterConfig.setOidcClientSecret(oidcClientSecret);
        filterConfig.setBaseRedirectUri("http://localhost:8080/geoserver/");
        filterConfig.calculateRedirectUris();
        filterConfig.setOidcTokenUri(baseKeycloakUrl + "/token");
        filterConfig.setOidcAuthorizationUri(baseKeycloakUrl + "/authorize");
        filterConfig.setOidcUserInfoUri(baseKeycloakUrl + "/userinfo");
        filterConfig.setOidcLogoutUri(baseKeycloakUrl + "/endSession");
        filterConfig.setOidcJwkSetUri(baseKeycloakUrl + "/certs");
        filterConfig.setOidcEnforceTokenValidation(false);
        filterConfig.setOidcScopes("openid profile email phone address");
        filterConfig.setEnableRedirectAuthenticationEntryPoint(false);
        filterConfig.setOidcUserNameAttribute("email");
        filterConfig.setRoleSource(GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource.IdToken);
        filterConfig.setTokenRolesClaim("roles");

        filterConfig.setOidcForceAuthorizationUriHttps(false);
        filterConfig.setOidcForceTokenUriHttps(false);
        //        filterConfig.setOidcJwsAlgorithmName(JwsAlgorithms.HS256);
        manager.saveFilter(filterConfig);

        // add our oidc to the WEB chain
        SecurityManagerConfig config = manager.getSecurityConfig();
        GeoServerSecurityFilterChain chain = config.getFilterChain();
        RequestFilterChain www = chain.getRequestChainByName("web");
        www.setFilterNames("openidconnect", "anonymous");

        manager.saveSecurityConfig(config);
    }
}
