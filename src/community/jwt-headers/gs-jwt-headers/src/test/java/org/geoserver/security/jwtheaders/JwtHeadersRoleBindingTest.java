/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import jakarta.servlet.Filter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.jwtheaders.filter.JwtHeadersFilterConfigException;
import org.geoserver.security.jwtheaders.token.TokenSignatureValidator;
import org.geoserver.security.validation.FilterConfigException;
import org.geoserver.security.validation.SecurityConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/** With token validation on, roles may only come from the validated identity token. */
public class JwtHeadersRoleBindingTest extends AbstractAuthenticationProviderTest {

    private static final String JWKS_URL = "https://idp.example.org/realms/r/certs";
    private static final String ISSUER = "https://idp.example.org/realms/r";
    private static final String IDENTITY_HEADER = "X-User-JWT";
    private static final String ROLES_PATH = "resource_access.geoserver.roles";

    private static RSAKey key;

    @BeforeClass
    public static void createKey() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("k1").generate();
    }

    @Before
    public void publishKeySet() {
        TokenSignatureValidator.jwks.put(JWKS_URL, new JWKSet(key.toPublicJWK()));
    }

    @After
    public void clearKeySet() {
        TokenSignatureValidator.jwks.invalidate(JWKS_URL);
    }

    private static String signedToken(String username, String... roles) throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(),
                new JWTClaimsSet.Builder()
                        .issuer(ISSUER)
                        .claim("preferred_username", username)
                        .expirationTime(new Date(System.currentTimeMillis() + 600_000))
                        .claim("resource_access", Map.of("geoserver", Map.of("roles", List.of(roles))))
                        .build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    /** A roles token signed by a key nobody trusts. */
    private static String foreignRolesToken() throws Exception {
        RSAKey other = new RSAKeyGenerator(2048).keyID("k1").generate();
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(),
                new JWTClaimsSet.Builder()
                        .claim(
                                "resource_access",
                                Map.of("geoserver", Map.of("roles", List.of("GeoserverAdministrator"))))
                        .build());
        jwt.sign(new RSASSASigner(other));
        return jwt.serialize();
    }

    private static GeoServerJwtHeadersFilterConfig validatingConfig(String name) {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setName(name);
        config.setClassName(GeoServerJwtHeadersFilter.class.getName());
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setUserNameHeaderAttributeName(IDENTITY_HEADER);
        jwt.setUserNameJsonPath("preferred_username");
        jwt.setValidateTokenSignatureURL(JWKS_URL);
        jwt.setValidateTokenIssuer(ISSUER);
        jwt.setRolesJsonPath(ROLES_PATH);
        jwt.setRoleConverterString("GeoserverAdministrator=ROLE_ADMINISTRATOR");
        config.setRoleSource(JWTHeaderRoleSource.JWT);
        return config;
    }

    /** Builds a filter without going through the save-time validation, as for a configuration stored earlier. */
    private GeoServerJwtHeadersFilter storedEarlier(GeoServerJwtHeadersFilterConfig config) throws IOException {
        config.setId(config.getName() + "-id");
        GeoServerJwtHeadersFilter filter = new GeoServerJwtHeadersFilter();
        filter.setSecurityManager(getSecurityManager());
        filter.initializeFromConfig(config);
        return filter;
    }

    private GeoServerJwtHeadersFilter saved(GeoServerJwtHeadersFilterConfig config) throws Exception {
        getSecurityManager().saveFilter(config);
        return (GeoServerJwtHeadersFilter) getSecurityManager().loadFilter(config.getName());
    }

    private static Authentication run(GeoServerJwtHeadersFilter filter, MockHttpServletRequest request)
            throws Exception {
        SecurityContextHolder.clearContext();
        AtomicReference<Authentication> seen = new AtomicReference<>();
        filter.doFilter(
                request,
                new MockHttpServletResponse(),
                (rq, rs) -> seen.set(SecurityContextHolder.getContext().getAuthentication()));
        SecurityContextHolder.clearContext();
        return seen.get();
    }

    private static List<String> authorities(Authentication auth) {
        return auth.getAuthorities().stream().map(a -> a.getAuthority()).collect(Collectors.toList());
    }

    @Test
    public void testRolesComeFromTheValidatedToken() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("RolesFromToken");
        config.getJwtConfiguration().setRolesHeaderName(IDENTITY_HEADER);
        GeoServerJwtHeadersFilter filter = saved(config);

        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("boss", "GeoserverAdministrator"));
        Authentication auth = run(filter, request);
        assertEquals("boss", auth.getPrincipal());
        assertTrue(authorities(auth).contains("ROLE_ADMINISTRATOR"));

        // guard: a second value of the same header is never read
        request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("lowpriv", "viewer"));
        request.addHeader(IDENTITY_HEADER, foreignRolesToken());
        auth = run(filter, request);
        assertEquals("lowpriv", auth.getPrincipal());
        assertFalse(authorities(auth).contains("ROLE_ADMINISTRATOR"));
    }

    /** A configuration stored before the save-time check: the separate roles header is ignored. */
    @Test
    public void testSeparateRolesHeaderIgnoredWhenTokenValidated() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("SeparateRolesHeader");
        config.getJwtConfiguration().setRolesHeaderName("X-Roles-JWT");
        GeoServerJwtHeadersFilter filter = storedEarlier(config);

        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("lowpriv", "viewer"));
        request.addHeader("X-Roles-JWT", foreignRolesToken());
        Authentication auth = run(filter, request);

        assertNotNull(auth);
        assertEquals("lowpriv", auth.getPrincipal());
        assertEquals(List.of("ROLE_AUTHENTICATED"), authorities(auth));
    }

    /** Same for the plain request header role source. */
    @Test
    public void testHeaderRoleSourceIgnoredWhenTokenValidated() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("HeaderRoleSource");
        config.setRoleSource(JWTHeaderRoleSource.Header);
        config.setRolesHeaderAttribute("X-Roles");
        GeoServerJwtHeadersFilter filter = storedEarlier(config);

        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("lowpriv", "viewer"));
        request.addHeader("X-Roles", "ROLE_ADMINISTRATOR");
        Authentication auth = run(filter, request);

        assertEquals("lowpriv", auth.getPrincipal());
        assertFalse(authorities(auth).contains("ROLE_ADMINISTRATOR"));

        // control: the same request with the header explicitly trusted does give the role
        GeoServerJwtHeadersFilterConfig trusting = validatingConfig("HeaderRoleSourceTrusted");
        trusting.setRoleSource(JWTHeaderRoleSource.Header);
        trusting.setRolesHeaderAttribute("X-Roles");
        trusting.setTrustUnvalidatedRolesHeader(true);
        GeoServerJwtHeadersFilter trustingFilter = storedEarlier(trusting);
        request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("lowpriv", "viewer"));
        request.addHeader("X-Roles", "ROLE_ADMINISTRATOR");
        auth = run(trustingFilter, request);
        assertTrue(authorities(auth).contains("ROLE_ADMINISTRATOR"));
    }

    @Test
    public void testSaveRequiresRoleSource() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("NoRoleSource");
        config.setRoleSource(null);

        SecurityConfigException e = assertThrows(
                SecurityConfigException.class, () -> getSecurityManager().saveFilter(config));
        assertEquals(FilterConfigException.ROLE_SOURCE_NEEDED, e.getId());
    }

    @Test
    public void testSaveRejectsRolesNotFromValidatedToken() {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("RejectedRolesHeader");
        config.getJwtConfiguration().setRolesHeaderName("X-Roles-JWT");

        SecurityConfigException e = assertThrows(
                SecurityConfigException.class, () -> getSecurityManager().saveFilter(config));
        assertEquals(JwtHeadersFilterConfigException.ROLES_NOT_FROM_VALIDATED_TOKEN, e.getId());
    }

    /** The explicit opt-in restores reading the separate header, for proxies that always overwrite it. */
    @Test
    public void testTrustedRolesHeaderIsRead() throws Exception {
        GeoServerJwtHeadersFilterConfig config = validatingConfig("TrustedRolesHeader");
        config.getJwtConfiguration().setRolesHeaderName("X-Roles-JWT");
        config.setTrustUnvalidatedRolesHeader(true);
        GeoServerJwtHeadersFilter filter = saved(config);

        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(IDENTITY_HEADER, "Bearer " + signedToken("proxied"));
        request.addHeader("X-Roles-JWT", foreignRolesToken());
        Authentication auth = run(filter, request);

        assertEquals("proxied", auth.getPrincipal());
        assertTrue(authorities(auth).contains("ROLE_ADMINISTRATOR"));
    }

    /**
     * Without validation (header set by an authenticating proxy) a missing roles header means no roles, not an error.
     */
    @Test
    public void testMissingRolesHeaderGivesNoRoles() throws Exception {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setName("MissingRolesHeader");
        config.setClassName(GeoServerJwtHeadersFilter.class.getName());
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setValidateToken(false);
        jwt.setUserNameFormatChoice(JwtConfiguration.UserNameHeaderFormat.JSON);
        jwt.setUserNameHeaderAttributeName("X-User");
        jwt.setUserNameJsonPath("preferred_username");
        config.setRoleSource(JWTHeaderRoleSource.JSON);
        jwt.setRolesHeaderName("X-Roles");
        jwt.setRolesJsonPath(ROLES_PATH);
        GeoServerJwtHeadersFilter filter = saved(config);

        MockHttpServletRequest request = createRequest("web/");
        request.addHeader("X-User", "{\"preferred_username\":\"alice\"}");
        Authentication auth = run(filter, request);

        assertEquals("alice", auth.getPrincipal());
        assertEquals(List.of("ROLE_AUTHENTICATED"), authorities(auth));
    }

    @Override
    protected List<Filter> getFilters() {
        return new ArrayList<>();
    }
}
