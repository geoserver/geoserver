/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.security.oauth2.intgration.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import dasniko.testcontainers.keycloak.KeycloakContainer;
import jakarta.servlet.ServletRequestEvent;
import jakarta.servlet.http.HttpSession;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URIBuilder;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.web.GeoServerHomePage;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geotools.util.logging.Logging;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.kordamp.json.JSONObject;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.RequestContextListener;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.utility.MountableFile;

/**
 * Spins up a pre-configured Keycloak docker container for integration tests, and carries the plumbing that drives a
 * browser-style authorization-code login against it.
 *
 * <p>If Docker is not available, the entire test class will be <b>skipped</b>.
 *
 * <p>Realms are provided via classpath resources:
 *
 * <ul>
 *   <li>master-realm.json (admin user geoserver/geoserver)
 *   <li>gs-realm-realm.json (test users &amp; client)
 * </ul>
 *
 * <p>See README in resources/org/geoserver/web/security/oauth2/intgration/keycloak for details.
 */
public class KeyCloakIntegrationTestSupport extends GeoServerWicketTestSupport {

    private static final Logger LOGGER = Logging.getLogger(KeyCloakIntegrationTestSupport.class);

    /** Base URL for the Keycloak container (e.g., http://localhost:RANDOM_PORT). */
    static String authServerUrl;

    /** The Keycloak container (created only if Docker is available). */
    protected static KeycloakContainer keycloakContainer;

    // defined in master-realm.json
    String masterRealmUser = "geoserver";
    String masterRealmPassword = "geoserver";

    // defined in gs-realm-realm.json
    String gsAdminUser = "admin";
    String gsAdminPassword = "admin";

    // defined in gs-realm-realm.json
    String normalUserName = "user_sample1";
    String normalUserPassword = "user_sample1";

    // defined in gs-realm-realm.json
    String oidcClient = "gs-client";
    String oidcClientSecret = "CNwDTAKypmFhkzdfx25r7syg56VfdHuH";

    // these are what's expected in the GS->OIDC IDP redirect URL
    String oidcLogin_responseType = "code";
    String oidcLogin_client_id = "gs-client";
    String oidcLogin_scope = "openid profile email phone address";
    // Scoped to the filter name "openidconnect" — each filter's callback URL carries its own scoped registration ID
    // so Spring can map the callback back to the right ClientRegistration. See OAuth2ClientRegistrationId.
    String oidcLogin_redirect_uri = "http://localhost:8080/geoserver/web/login/oauth2/code/openidconnect__oidc";

    @BeforeClass
    public static void beforeAll() throws IOException, InterruptedException {
        LOGGER.info("KeyCloakIntegrationTestSupport.beforeAll() STARTING");

        // Skip entire class when Docker/Testcontainers is not usable
        if (!dockerAvailable()) {
            LOGGER.info("Docker NOT available - skipping tests");
            Assume.assumeTrue("Skipping Keycloak integration tests: Docker not available", false);
        }

        LOGGER.info("Docker available - starting Keycloak container");

        try {
            // Construct the container only after the assumption passes
            // Note: Do NOT use .useTls() as the self-signed certificate will cause
            // SSL validation failures when GeoServer tries to exchange tokens or fetch JWKS
            keycloakContainer = new KeycloakContainer("quay.io/keycloak/keycloak:26.1")
                    // Import realms into the default Keycloak import directory
                    .withCopyToContainer(
                            MountableFile.forClasspathResource(
                                    "org/geoserver/web/security/oauth2/login/keycloak/master-realm.json"),
                            "/opt/keycloak/data/import/master-realm.json")
                    .withCopyToContainer(
                            MountableFile.forClasspathResource(
                                    "org/geoserver/web/security/oauth2/login/keycloak/gs-realm-realm.json"),
                            "/opt/keycloak/data/import/gs-realm-realm.json")
                    // Use INFO logging level to avoid memory issues from verbose output
                    .withCustomCommand("--log-level=INFO");

            LOGGER.info("Calling keycloakContainer.start()");
            keycloakContainer.start();
            authServerUrl = keycloakContainer.getAuthServerUrl();
            LOGGER.info("Keycloak started at: " + authServerUrl);
        } catch (Exception e) {
            // Handle Docker API version mismatch or other known container infrastructure failures
            String message = e.getMessage();
            if (message != null
                    && (message.contains("API version")
                            || message.contains("too old")
                            || message.contains("client version"))) {
                LOGGER.warning("Docker API version mismatch - skipping tests: " + message);
                Assume.assumeTrue("Skipping Keycloak tests: Docker API version incompatible", false);
            }
            // Skip on container startup failures related to Docker availability
            if (message != null
                    && (message.contains("Could not find a valid Docker environment")
                            || message.contains("docker")
                            || message.contains("Container startup failed")
                            || message.contains("Timed out waiting for container"))) {
                LOGGER.warning("Failed to start Keycloak container - skipping tests: " + e.getMessage());
                Assume.assumeTrue("Skipping Keycloak tests: Container failed to start - " + e.getMessage(), false);
            }
            // Rethrow unexpected exceptions so genuine regressions (e.g., broken realm JSON,
            // wrong image tag) are visible in CI rather than silently skipped
            throw e;
        }
    }

    @AfterClass
    public static void afterAll() {
        if (keycloakContainer != null) {
            try {
                keycloakContainer.stop();
            } catch (Throwable ignore) {
                // best-effort shutdown
            } finally {
                keycloakContainer = null;
            }
        }
    }

    @After
    public void clear() {
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    private static boolean dockerAvailable() {
        try {
            // Causes Testcontainers to probe for a working Docker client; throws if not available
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    /**
     * Get an access token from Keycloak using the Resource Owner Password Credentials Grant. This is useful for testing
     * bearer token authentication without going through the browser flow.
     *
     * @param username the username
     * @param password the password
     * @return JSON response containing access_token, refresh_token, id_token, etc.
     */
    protected JSONObject getTokenFromKeycloak(String username, String password) throws Exception {
        String tokenUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/token";

        URL url = new URL(tokenUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setRequestMethod("POST");
        connection.setDoOutput(true);
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(10_000);
        connection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");

        String body = "grant_type=password"
                + "&client_id=" + oidcClient
                + "&client_secret=" + oidcClientSecret
                + "&username=" + username
                + "&password=" + password
                + "&scope=openid profile email";

        try (OutputStream os = connection.getOutputStream()) {
            os.write(body.getBytes(StandardCharsets.UTF_8));
        }

        String response;
        try (InputStream is = connection.getInputStream()) {
            response = IOUtils.toString(is, StandardCharsets.UTF_8);
        } finally {
            connection.disconnect();
        }

        return JSONObject.fromObject(response);
    }

    /**
     * does the actual login (see top-of-class comments for details).
     *
     * @param keycloakUserName username in keycloak (i.e. admin)
     * @param keycloakPassword password in keycloak (i.e. admin)
     * @return authentication (from GS security context)
     * @throws Exception error occurred
     */
    public OAuth2AuthenticationToken login(String keycloakUserName, String keycloakPassword) throws Exception {
        // First verify Keycloak is accessible
        String discoveryUrl = authServerUrl + "/realms/gs-realm/.well-known/openid-configuration";
        LOGGER.fine("Verifying Keycloak accessible at: " + discoveryUrl);
        WebRequests.WebResponse discoveryResponse = WebRequests.webRequestGET(discoveryUrl);
        LOGGER.fine("Discovery endpoint status: " + discoveryResponse.statusCode);
        if (discoveryResponse.statusCode != 200) {
            LOGGER.warning("Discovery response body: " + discoveryResponse.body);
            LOGGER.warning("authServerUrl = " + authServerUrl);
            LOGGER.warning(
                    "keycloakContainer running = " + (keycloakContainer != null && keycloakContainer.isRunning()));
        }
        assertEquals("Keycloak discovery endpoint should be accessible", 200, discoveryResponse.statusCode);

        tester.startPage(new GeoServerHomePage());
        String html = tester.getLastResponseAsString();

        // 1. verify that there's a login button for oidc
        assertTrue(html.contains("href=\"http://localhost/context/web/oauth2/authorization/openidconnect__oidc\""));

        // 2. lets "press" the oidc login link:
        MockHttpServletRequest webRequest = createRequest("web/oauth2/authorization/openidconnect__oidc", true);
        MockHttpServletResponse webResponse = executeOnSecurityFilters(webRequest);
        HttpSession session = webRequest.getSession();

        // Debug: Check if OAuth2AuthorizationRequest was stored in session
        Object storedAuthRequest = session.getAttribute(
                "org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST");
        LOGGER.fine("After initial redirect - stored OAuth2AuthorizationRequest: " + storedAuthRequest);
        LOGGER.fine("Session ID: " + session.getId());
        LOGGER.fine("Redirect Location: " + webResponse.getHeader("Location"));

        // should be a 302 redirect to keycloak to start the login process
        // Extract state, nonce, and PKCE code_challenge from the redirect URL
        Pair<String, String> state_nonce = validateRedirectToKeyCloak(webResponse);
        String oidcLogin_state = state_nonce.getLeft();
        String oidcLogin_nonce = state_nonce.getRight();

        // Extract code_challenge from Spring's URL for PKCE support
        String redirectUrl = webResponse.getHeader("Location");
        String codeChallenge = null;
        String codeChallengeMethod = null;
        if (redirectUrl.contains("code_challenge=")) {
            List<NameValuePair> params = new URIBuilder(new URI(redirectUrl), StandardCharsets.UTF_8).getQueryParams();
            codeChallenge = params.stream()
                    .filter(x -> x.getName().equals("code_challenge"))
                    .findFirst()
                    .map(NameValuePair::getValue)
                    .orElse(null);
            codeChallengeMethod = params.stream()
                    .filter(x -> x.getName().equals("code_challenge_method"))
                    .findFirst()
                    .map(NameValuePair::getValue)
                    .orElse(null);
        }

        return keycloakLogin(
                session,
                oidcLogin_state,
                oidcLogin_nonce,
                codeChallenge,
                codeChallengeMethod,
                keycloakUserName,
                keycloakPassword);
    }

    /**
     * validates the GS -> OIDC IDP URL and returns the state and nonce.
     *
     * @param webResponse response from GS
     * @return .left is state, .right is nonce
     * @throws URISyntaxException error occurred (most likely misconfigured)
     */
    protected Pair<String, String> validateRedirectToKeyCloak(MockHttpServletResponse webResponse)
            throws URISyntaxException {
        // should be a 302 redirect to keycloak to start the login process
        assertEquals(302, webResponse.getStatus());
        assertNotNull(webResponse.getHeader("Location"));
        String redirectURL = webResponse.getHeader("Location");

        assertTrue(redirectURL.startsWith(authServerUrl));

        List<NameValuePair> params = new URIBuilder(new URI(redirectURL), StandardCharsets.UTF_8).getQueryParams();

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
                System.getProperty("OPENID_TEST_GS_PROXY_BASE", "http://localhost:8080/geoserver")
                        + "/web/login/oauth2/code/openidconnect__oidc",
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

        String oidcLogin_state = params.stream()
                .filter(x -> x.getName().equals("state"))
                .findFirst()
                .get()
                .getValue();
        String oidcLogin_nonce = params.stream()
                .filter(x -> x.getName().equals("nonce"))
                .findFirst()
                .get()
                .getValue();

        return new ImmutablePair<>(oidcLogin_state, oidcLogin_nonce);
    }

    /**
     * Given the info from the GS -> OIDC IDP redirect, do the actual keycloak login and GS login.
     *
     * @param session GS session must be the same for all requests
     * @param oidcLoginState state (generated by GS for the login)
     * @param oidcLoginNonce nonce (generated by GS for the login)
     * @param codeChallenge PKCE code challenge (may be null if PKCE is disabled)
     * @param codeChallengeMethod PKCE code challenge method (may be null if PKCE is disabled)
     * @param username who to login to keycloak as
     * @param password keycloak's user's password
     * @return GS security context auth
     * @throws Exception error occurred
     */
    protected OAuth2AuthenticationToken keycloakLogin(
            HttpSession session,
            String oidcLoginState,
            String oidcLoginNonce,
            String codeChallenge,
            String codeChallengeMethod,
            String username,
            String password)
            throws Exception {
        // send request to keycloak using the manually-built URL with PKCE support
        WebRequests.WebResponse startKeyCloakResponse =
                executeKeycloakStartUrl(oidcLoginState, oidcLoginNonce, codeChallenge, codeChallengeMethod);
        LOGGER.fine("Keycloak response status: " + startKeyCloakResponse.statusCode);
        if (startKeyCloakResponse.statusCode != 200) {
            LOGGER.warning("Keycloak response body: " + startKeyCloakResponse.body);
        }
        assertEquals(200, startKeyCloakResponse.statusCode);

        // send keycloak the completed username/password form
        WebRequests.WebResponse keycloakResponseSubmitUserPassword =
                executeKeycloakResponseSubmitUserPassword(startKeyCloakResponse, username, password);
        assertEquals(302, keycloakResponseSubmitUserPassword.statusCode);
        String redirectCodeToGS =
                keycloakResponseSubmitUserPassword.headers.get("Location").get(0);
        // should be redirecting to GS's code endpoint
        assertTrue(redirectCodeToGS.startsWith(
                "http://localhost:8080/geoserver/web/login/oauth2/code/openidconnect__oidc"));

        String shortenedRedirectCodeToGS = redirectCodeToGS.substring("http://localhost:8080/geoserver/".length());

        // Parse the URL to extract path and query parameters separately
        String path;
        String queryString = null;
        int queryIndex = shortenedRedirectCodeToGS.indexOf('?');
        if (queryIndex > 0) {
            path = shortenedRedirectCodeToGS.substring(0, queryIndex);
            queryString = shortenedRedirectCodeToGS.substring(queryIndex + 1);
        } else {
            path = shortenedRedirectCodeToGS;
        }

        MockHttpServletRequest webRequest = createRequest(path);
        webRequest.setSession(session);

        // Debug: Check session state before code exchange
        LOGGER.fine("Before code exchange - Session ID: " + session.getId());
        Object storedAuthRequestBefore = session.getAttribute(
                "org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST");
        LOGGER.fine("Before code exchange - stored OAuth2AuthorizationRequest: " + storedAuthRequestBefore);

        // Set the query string and parse parameters
        if (queryString != null) {
            webRequest.setQueryString(queryString);
            // Parse query parameters and add them to the request
            for (String param : queryString.split("&")) {
                String[] keyValue = param.split("=", 2);
                if (keyValue.length == 2) {
                    webRequest.setParameter(
                            keyValue[0], java.net.URLDecoder.decode(keyValue[1], StandardCharsets.UTF_8));
                } else if (keyValue.length == 1) {
                    webRequest.setParameter(keyValue[0], "");
                }
            }
        }

        // Execute the OAuth2 code callback and capture authentication during filter chain execution
        // This is necessary because Spring Security 6 only has auth in SecurityContextHolder during request processing
        java.util.concurrent.atomic.AtomicReference<Authentication> authRef =
                new java.util.concurrent.atomic.AtomicReference<>();
        MockHttpServletResponse codeResponse = executeOnSecurityFiltersCapturingAuth(webRequest, authRef);

        // After successful OAuth2 login, Spring Security redirects to the success URL
        assertEquals("OAuth2 code exchange should redirect to success URL", 302, codeResponse.getStatus());

        Authentication auth = authRef.get();

        // If auth is null or not OAuth2AuthenticationToken, try loading from session as fallback
        if (auth == null || !(auth instanceof OAuth2AuthenticationToken)) {
            // Try loading from session - Spring Security 6 may have saved it there
            org.springframework.security.web.context.HttpSessionSecurityContextRepository repo =
                    new org.springframework.security.web.context.HttpSessionSecurityContextRepository();
            org.springframework.security.core.context.SecurityContext ctx =
                    repo.loadDeferredContext(webRequest).get();
            if (ctx != null && ctx.getAuthentication() != null) {
                auth = ctx.getAuthentication();
            }
        }

        // Debug output if still failing
        if (auth == null) {
            LOGGER.warning("Authentication is NULL after code exchange");
            LOGGER.warning("Response status: " + codeResponse.getStatus());
            LOGGER.warning("Response location: " + codeResponse.getHeader("Location"));

            // Check for OAuth2 authentication exception stored in session
            Object authException = session.getAttribute("SPRING_SECURITY_LAST_EXCEPTION");
            if (authException != null) {
                LOGGER.warning("Authentication Exception: " + authException);
                if (authException instanceof Throwable) {
                    LOGGER.log(Level.WARNING, "Authentication exception details", (Throwable) authException);
                }
            }

            // Also check for OAuth2AuthorizationRequest to verify state was stored
            Object authRequest = session.getAttribute(
                    "org.springframework.security.oauth2.client.web.HttpSessionOAuth2AuthorizationRequestRepository.AUTHORIZATION_REQUEST");
            LOGGER.warning("Stored OAuth2AuthorizationRequest: " + authRequest);
        } else if (!(auth instanceof OAuth2AuthenticationToken)) {
            LOGGER.fine("Authentication type: " + auth.getClass().getName());
            LOGGER.fine("Authentication principal: " + auth.getPrincipal());
            LOGGER.fine("Is authenticated: " + auth.isAuthenticated());
        }

        assertNotNull("Authentication should not be null after OAuth2 code exchange", auth);
        assertTrue(
                "Should be OAuth2AuthenticationToken but was: "
                        + (auth != null ? auth.getClass().getName() : "null"),
                auth instanceof OAuth2AuthenticationToken);

        return (OAuth2AuthenticationToken) auth;
    }

    /**
     * keycloak's username/password form has an action with "extra" params in it - submit
     *
     * @param startKeyCloakResponse GS's redirect to keycloak to start login
     * @param username keycloak's username
     * @param password keycloak's user's password
     * @return response from keycloak's form submit
     * @throws Exception error occurred
     */
    public WebRequests.WebResponse executeKeycloakResponseSubmitUserPassword(
            WebRequests.WebResponse startKeyCloakResponse, String username, String password) throws Exception {
        Pattern pattern = Pattern.compile(".* action=\"([^\"]+)\".*", Pattern.DOTALL);
        Matcher matcher = pattern.matcher(startKeyCloakResponse.body);
        if (!matcher.matches()) {
            throw new Exception("keycloak - couldnt find the login form's action url");
        }
        String postUrl = matcher.group(1).replaceAll("&amp;", "&");
        String postBody = "username=" + username + "&password=" + password + "&credentialId=";
        return WebRequests.webRequestPOSTForm(postUrl, postBody, startKeyCloakResponse.cookieManager);
    }

    /**
     * Start the keycloak login process with PKCE support
     *
     * @param oidcLoginState state (generated by GS for the login)
     * @param oidcLoginNonce nonce (generated by GS for the login)
     * @param codeChallenge PKCE code challenge (may be null if PKCE is disabled)
     * @param codeChallengeMethod PKCE code challenge method (may be null if PKCE is disabled)
     * @return response from keycloak (i.e. the login form)
     * @throws Exception error occurred
     */
    public WebRequests.WebResponse executeKeycloakStartUrl(
            String oidcLoginState, String oidcLoginNonce, String codeChallenge, String codeChallengeMethod)
            throws Exception {
        String startUrl = authServerUrl + "/realms/gs-realm/protocol/openid-connect/auth?";
        startUrl += "client_id=" + oidcLogin_client_id;
        startUrl += "&response_type=" + oidcLogin_responseType;
        startUrl += "&scope=" + URLEncoder.encode(oidcLogin_scope, StandardCharsets.UTF_8);
        startUrl += "&redirect_uri=" + oidcLogin_redirect_uri;
        startUrl += "&state=" + oidcLoginState;
        startUrl += "&nonce=" + oidcLoginNonce;
        // Add PKCE parameters if present
        if (codeChallenge != null && !codeChallenge.isEmpty()) {
            startUrl += "&code_challenge=" + codeChallenge;
            startUrl += "&code_challenge_method=" + (codeChallengeMethod != null ? codeChallengeMethod : "S256");
        }
        LOGGER.fine("Calling Keycloak URL: " + startUrl);
        return WebRequests.webRequestGET(startUrl);
    }

    /**
     * Start the keycloak login process (legacy method without PKCE)
     *
     * @param oidcLoginState state (generated by GS for the login)
     * @param oidcLoginNonce nonce (generated by GS for the login)
     * @return response from keycloak (i.e. the login form)
     * @throws Exception error occurred
     */
    public WebRequests.WebResponse executeKeycloakStartUrl(String oidcLoginState, String oidcLoginNonce)
            throws Exception {
        return executeKeycloakStartUrl(oidcLoginState, oidcLoginNonce, null, null);
    }

    /**
     * taken from another integration test. Execute a web request on the security filters (i.e. oidc filters) in GS.
     *
     * @param request request to execute
     * @return response from GS
     * @throws IOException error occurred
     * @throws jakarta.servlet.ServletException error occurred
     */
    protected MockHttpServletResponse executeOnSecurityFilters(MockHttpServletRequest request)
            throws IOException, jakarta.servlet.ServletException {
        // for session local support in Spring
        RequestContextListener listener = new RequestContextListener();
        ServletRequestEvent event = new ServletRequestEvent(request.getServletContext(), request);
        listener.requestInitialized(event);
        try {
            MockFilterChain chain = new MockFilterChain();
            MockHttpServletResponse response = new MockHttpServletResponse();
            GeoServerSecurityFilterChainProxy filterChainProxy =
                    GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
            filterChainProxy.doFilter(request, response, chain);

            return response;
        } finally {
            listener.requestDestroyed(event);
        }
    }

    /**
     * Execute a web request on the security filters, capturing the Authentication during filter chain execution. In
     * Spring Security 6, the SecurityContext is only in SecurityContextHolder during request processing and may not be
     * saved to the session automatically. This method captures it during execution using a terminal servlet.
     *
     * @param request request to execute
     * @param authRef AtomicReference to store the captured Authentication
     * @return response from GS
     * @throws IOException error occurred
     * @throws jakarta.servlet.ServletException error occurred
     */
    protected MockHttpServletResponse executeOnSecurityFiltersCapturingAuth(
            MockHttpServletRequest request, java.util.concurrent.atomic.AtomicReference<Authentication> authRef)
            throws IOException, jakarta.servlet.ServletException {
        // for session local support in Spring
        RequestContextListener listener = new RequestContextListener();
        ServletRequestEvent event = new ServletRequestEvent(request.getServletContext(), request);
        listener.requestInitialized(event);
        try {
            // Use a terminal servlet to capture the authentication from SecurityContextHolder
            // during filter chain execution (before it gets cleared)
            jakarta.servlet.http.HttpServlet terminal = new jakarta.servlet.http.HttpServlet() {
                @Override
                protected void service(
                        jakarta.servlet.http.HttpServletRequest req, jakarta.servlet.http.HttpServletResponse resp) {
                    authRef.set(org.springframework.security.core.context.SecurityContextHolder.getContext()
                            .getAuthentication());
                }
            };

            MockFilterChain chain = new MockFilterChain(terminal);
            MockHttpServletResponse response = new MockHttpServletResponse();
            GeoServerSecurityFilterChainProxy filterChainProxy =
                    GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
            filterChainProxy.doFilter(request, response, chain);

            return response;
        } finally {
            listener.requestDestroyed(event);
        }
    }
}
