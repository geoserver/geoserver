/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.keycloak;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jose.JWSObject;
import com.nimbusds.jose.util.JSONObjectUtils;
import dasniko.testcontainers.keycloak.KeycloakContainer;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.Filter;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.jwtheaders.JwtConfiguration;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geotools.util.logging.Logging;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.testcontainers.DockerClientFactory;
import org.testcontainers.images.builder.Transferable;

/**
 * Runs the JWT Headers filter against tokens issued by a real Keycloak. Skipped when Docker is not available.
 *
 * <p>Three realms are generated at start-up: {@code tenant-a} and {@code tenant-b} sign their tokens with the same RSA
 * key (as the tenants of a multi-tenant identity provider do), {@code tenant-c} has its own keys. Every realm has a
 * public client {@code geoserver} with the client roles {@code GeoserverAdministrator} and {@code viewer}.
 */
public class JwtHeadersKeycloakIntegrationTest extends AbstractAuthenticationProviderTest {

    private static final Logger LOGGER = Logging.getLogger(JwtHeadersKeycloakIntegrationTest.class);

    private static final String CLIENT = "geoserver";
    private static final String ROLES_PATH = "resource_access." + CLIENT + ".roles";

    private static KeycloakContainer keycloak;

    @BeforeClass
    public static void startKeycloak() throws Exception {
        Assume.assumeTrue("Skipping Keycloak integration tests: Docker not available", dockerAvailable());

        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        KeyPair shared = generator.generateKeyPair();
        String sharedKey =
                Base64.getEncoder().encodeToString(shared.getPrivate().getEncoded());

        keycloak = new KeycloakContainer("quay.io/keycloak/keycloak:26.1")
                .withCopyToContainer(
                        Transferable.of(realm(
                                "tenant-a",
                                sharedKey,
                                user("boss", "GeoserverAdministrator"),
                                user("lowpriv", "viewer"),
                                user("admin", "viewer"),
                                user("root", "viewer"))),
                        "/opt/keycloak/data/import/tenant-a.json")
                .withCopyToContainer(
                        Transferable.of(realm("tenant-b", sharedKey, user("intruder", "GeoserverAdministrator"))),
                        "/opt/keycloak/data/import/tenant-b.json")
                .withCopyToContainer(
                        Transferable.of(realm("tenant-c", null, user("someone", "viewer"))),
                        "/opt/keycloak/data/import/tenant-c.json")
                .withCustomCommand("--log-level=INFO");
        try {
            keycloak.start();
        } catch (Exception e) {
            LOGGER.warning("Keycloak container did not start, skipping: " + e.getMessage());
            Assume.assumeTrue("Skipping Keycloak integration tests: " + e.getMessage(), false);
        }
    }

    @AfterClass
    public static void stopKeycloak() {
        if (keycloak != null) {
            keycloak.stop();
            keycloak = null;
        }
    }

    private static boolean dockerAvailable() {
        try {
            DockerClientFactory.instance().client();
            return true;
        } catch (Throwable t) {
            return false;
        }
    }

    // ------------------------------------------------------------------ realms and tokens

    private static String user(String name, String clientRole) {
        return """
                {"username": "%1$s", "enabled": true, "email": "%1$s@example.org", "emailVerified": true,
                 "firstName": "%1$s", "lastName": "test",
                 "credentials": [{"type": "password", "value": "%1$s", "temporary": false}],
                 "clientRoles": {"%2$s": ["%3$s"]}}"""
                .formatted(name, CLIENT, clientRole);
    }

    private static String realm(String name, String sharedPrivateKey, String... users) {
        String keys = sharedPrivateKey == null
                ? ""
                : """
                "components": {"org.keycloak.keys.KeyProvider": [{
                  "name": "shared-rsa", "providerId": "rsa", "subComponents": {},
                  "config": {"privateKey": ["%s"], "priority": ["1000"], "enabled": ["true"], "active": ["true"],
                             "algorithm": ["RS256"]}}]},
                """
                        .formatted(sharedPrivateKey);
        return """
                {"realm": "%s", "enabled": true, "accessTokenLifespan": 600,
                 %s
                 "clients": [{"clientId": "%s", "enabled": true, "publicClient": true,
                              "directAccessGrantsEnabled": true, "standardFlowEnabled": false}],
                 "roles": {"client": {"%3$s": [{"name": "GeoserverAdministrator"}, {"name": "viewer"}]}},
                 "users": [%s]}"""
                .formatted(name, keys, CLIENT, String.join(",", users));
    }

    private static String realmUrl(String realm) {
        return keycloak.getAuthServerUrl() + "/realms/" + realm;
    }

    private static String jwksUrl(String realm) {
        return realmUrl(realm) + "/protocol/openid-connect/certs";
    }

    private static String userinfoUrl(String realm) {
        return realmUrl(realm) + "/protocol/openid-connect/userinfo";
    }

    /** Access token from the password grant (password = user name). */
    private static String accessToken(String realm, String user) throws Exception {
        String form = "grant_type=password&scope=openid&client_id=" + CLIENT + "&username="
                + URLEncoder.encode(user, StandardCharsets.UTF_8) + "&password="
                + URLEncoder.encode(user, StandardCharsets.UTF_8);
        HttpRequest request = HttpRequest.newBuilder(URI.create(realmUrl(realm) + "/protocol/openid-connect/token"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(form))
                .build();
        HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        assertEquals(response.body(), 200, response.statusCode());
        return (String) JSONObjectUtils.parse(response.body()).get("access_token");
    }

    /** A roles token signed by nobody the filter trusts. */
    private static String forgedRolesToken() {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"RS256\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(
                ("{\"resource_access\":{\"" + CLIENT + "\":{\"roles\":[\"GeoserverAdministrator\"]}}}")
                        .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + ".AAAA";
    }

    // ------------------------------------------------------------------ filters

    /** Bearer token in Authorization, signature checked against the realm keys, issuer pinned to the realm. */
    private static GeoServerJwtHeadersFilterConfig signatureConfig(String name, String keysRealm, String issuerRealm) {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setName(name);
        config.setClassName(GeoServerJwtHeadersFilter.class.getName());
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setUserNameHeaderAttributeName("Authorization");
        jwt.setUserNameJsonPath("preferred_username");
        jwt.setValidateTokenSignatureURL(jwksUrl(keysRealm));
        jwt.setValidateTokenIssuer(realmUrl(issuerRealm));
        config.setRoleSource(JWTHeaderRoleSource.JWT);
        jwt.setRolesJsonPath(ROLES_PATH);
        jwt.setRoleConverterString("GeoserverAdministrator=ROLE_ADMINISTRATOR");
        return config;
    }

    /** Same, but the token is checked against the realm userinfo endpoint instead of its signature. */
    private static GeoServerJwtHeadersFilterConfig endpointConfig(String name, String realm) {
        GeoServerJwtHeadersFilterConfig config = signatureConfig(name, realm, realm);
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setValidateTokenSignature(false);
        jwt.setValidateTokenAgainstURL(true);
        jwt.setValidateTokenAgainstURLEndpoint(userinfoUrl(realm));
        jwt.setValidateSubjectWithEndpoint(true);
        return config;
    }

    private GeoServerJwtHeadersFilter saved(GeoServerJwtHeadersFilterConfig config) throws Exception {
        getSecurityManager().saveFilter(config);
        return (GeoServerJwtHeadersFilter) getSecurityManager().loadFilter(config.getName());
    }

    /** Builds a filter without the save-time validation, as for a configuration stored by an earlier version. */
    private GeoServerJwtHeadersFilter storedEarlier(GeoServerJwtHeadersFilterConfig config) throws IOException {
        config.setId(config.getName() + "-id");
        GeoServerJwtHeadersFilter filter = new GeoServerJwtHeadersFilter();
        filter.setSecurityManager(getSecurityManager());
        filter.initializeFromConfig(config);
        return filter;
    }

    private Authentication run(GeoServerJwtHeadersFilter filter, String token, Map<String, String> extraHeaders)
            throws Exception {
        MockHttpServletRequest request = createRequest("web/");
        request.addHeader("Authorization", "Bearer " + token);
        extraHeaders.forEach(request::addHeader);
        SecurityContextHolder.clearContext();
        AtomicReference<Authentication> seen = new AtomicReference<>();
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (rq, rs) -> seen.set(SecurityContextHolder.getContext().getAuthentication()));
        SecurityContextHolder.clearContext();
        return seen.get();
    }

    private Authentication run(GeoServerJwtHeadersFilter filter, String token) throws Exception {
        return run(filter, token, Map.of());
    }

    private static List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
    }

    // ------------------------------------------------------------------ tests

    @Test
    public void testRolesFromValidatedToken() throws Exception {
        GeoServerJwtHeadersFilter filter = saved(signatureConfig("kc-roles", "tenant-a", "tenant-a"));

        Authentication boss = run(filter, accessToken("tenant-a", "boss"));
        assertEquals("boss", boss.getPrincipal());
        assertTrue(authorities(boss).contains("ROLE_ADMINISTRATOR"));

        Authentication lowpriv = run(filter, accessToken("tenant-a", "lowpriv"));
        assertEquals("lowpriv", lowpriv.getPrincipal());
        assertFalse(authorities(lowpriv).contains("ROLE_ADMINISTRATOR"));
    }

    /** A configuration stored before the save-time check reads no roles from a header that is not the token. */
    @Test
    public void testSeparateRolesHeaderIgnored() throws Exception {
        GeoServerJwtHeadersFilterConfig config = signatureConfig("kc-roles-header", "tenant-a", "tenant-a");
        config.getJwtConfiguration().setRolesHeaderName("X-Roles");
        GeoServerJwtHeadersFilter filter = storedEarlier(config);

        Authentication auth = run(filter, accessToken("tenant-a", "lowpriv"), Map.of("X-Roles", forgedRolesToken()));
        assertNotNull(auth);
        assertEquals("lowpriv", auth.getPrincipal());
        assertEquals(List.of("ROLE_AUTHENTICATED"), authorities(auth));
    }

    /** Two realms signing with the same key: only the issuer tells their tokens apart. */
    @Test
    public void testOtherIssuerWithSameKeysRejected() throws Exception {
        String intruder = accessToken("tenant-b", "intruder");
        String boss = accessToken("tenant-a", "boss");
        assertEquals(
                "the two realms must share their signing key",
                JWSObject.parse(boss).getHeader().getKeyID(),
                JWSObject.parse(intruder).getHeader().getKeyID());

        GeoServerJwtHeadersFilter filter = saved(signatureConfig("kc-issuer", "tenant-a", "tenant-a"));
        assertNotNull(run(filter, boss));
        assertNull(run(filter, intruder));
    }

    @Test
    public void testSignatureAcceptanceIsPerFilter() throws Exception {
        String token = accessToken("tenant-a", "lowpriv");
        GeoServerJwtHeadersFilter trustsA = saved(signatureConfig("kc-sig-a", "tenant-a", "tenant-a"));
        GeoServerJwtHeadersFilter trustsC = saved(signatureConfig("kc-sig-c", "tenant-c", "tenant-a"));

        assertNotNull(run(trustsA, token));
        assertNull(run(trustsC, token));
    }

    @Test
    public void testEndpointAcceptanceIsPerFilter() throws Exception {
        String token = accessToken("tenant-a", "lowpriv");
        GeoServerJwtHeadersFilter endpointA = saved(endpointConfig("kc-userinfo-a", "tenant-a"));
        GeoServerJwtHeadersFilter endpointB = saved(endpointConfig("kc-userinfo-b", "tenant-b"));

        assertEquals("lowpriv", run(endpointA, token).getPrincipal());
        assertNull(run(endpointB, token));
    }

    @Test
    public void testBuiltInAccountsRefused() throws Exception {
        GeoServerJwtHeadersFilterConfig config = signatureConfig("kc-builtin", "tenant-a", "tenant-a");
        config.setRoleSource(JWTHeaderRoleSource.RoleService);
        config.setRoleServiceName("default");
        GeoServerJwtHeadersFilter filter = saved(config);

        assertNull(run(filter, accessToken("tenant-a", "root")));
        assertNull(run(filter, accessToken("tenant-a", "admin")));

        GeoServerJwtHeadersFilterConfig allowing = signatureConfig("kc-builtin-allowed", "tenant-a", "tenant-a");
        allowing.setRoleSource(JWTHeaderRoleSource.RoleService);
        allowing.setRoleServiceName("default");
        allowing.setAllowAdminLogin(true);
        GeoServerJwtHeadersFilter allowingFilter = saved(allowing);

        assertNull(run(allowingFilter, accessToken("tenant-a", "root")));
        Authentication admin = run(allowingFilter, accessToken("tenant-a", "admin"));
        assertEquals("admin", admin.getPrincipal());
        assertTrue(authorities(admin).contains("ROLE_ADMINISTRATOR"));
    }

    @Override
    protected List<Filter> getFilters() {
        return new ArrayList<>();
    }
}
