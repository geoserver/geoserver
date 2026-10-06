/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.gen.RSAKeyGenerator;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import javax.servlet.Filter;
import org.geoserver.platform.resource.Resource;
import org.geoserver.security.auth.AbstractAuthenticationProviderTest;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilter;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig;
import org.geoserver.security.jwtheaders.filter.GeoServerJwtHeadersFilterConfig.JWTHeaderRoleSource;
import org.geoserver.security.jwtheaders.token.TokenSignatureValidator;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

/** Token content is only logged when the filter's "log sensitive information" option is on. */
public class JwtHeadersSensitiveLoggingTest extends AbstractAuthenticationProviderTest {

    private static final String HEADER = "X-User";
    private static final String JWKS_URL = "https://idp.example.org/realms/r/certs";
    private static final String ISSUER = "https://idp.example.org/realms/r";
    private static final String SIGNATURE = "SIGNATURE-SEGMENT";

    /** Collects what the filter logs at FINE level instead of writing it. */
    static class CapturingFilter extends GeoServerJwtHeadersFilter {
        final List<String> fine = new ArrayList<>();
        final List<String> sensitive = new ArrayList<>();
        final List<Throwable> causes = new ArrayList<>();
        boolean fineLoggable = true;

        @Override
        protected boolean isFineLoggable() {
            return fineLoggable;
        }

        @Override
        protected void logFine(String message) {
            fine.add(message);
        }

        @Override
        protected void logSensitive(String message, Throwable cause) {
            sensitive.add(message);
            if (cause != null) causes.add(cause);
        }

        String all() {
            return String.join("\n", fine) + "\n" + String.join("\n", sensitive);
        }
    }

    private RSAKey key;

    @Before
    public void publishKeySet() throws Exception {
        key = new RSAKeyGenerator(2048).keyID("k1").generate();
        TokenSignatureValidator.jwks.put(JWKS_URL, new JWKSet(key.toPublicJWK()));
    }

    @After
    public void clearKeySet() {
        TokenSignatureValidator.jwks.invalidate(JWKS_URL);
    }

    /** A JWT shaped header value whose signature nobody can verify. */
    private static String unverifiableToken() {
        Base64.Encoder b64 = Base64.getUrlEncoder().withoutPadding();
        String header = b64.encodeToString("{\"alg\":\"RS256\",\"kid\":\"k1\"}".getBytes(StandardCharsets.UTF_8));
        String payload = b64.encodeToString(("{\"preferred_username\":\"alice\",\"department\":\"geo-team\","
                        + "\"resource_access\":{\"geoserver\":{\"roles\":[\"viewer\"]}}}")
                .getBytes(StandardCharsets.UTF_8));
        return header + "." + payload + "." + SIGNATURE;
    }

    /** A token signed with the published key. */
    private String signedToken() throws Exception {
        SignedJWT jwt = new SignedJWT(
                new JWSHeader.Builder(JWSAlgorithm.RS256).keyID("k1").build(),
                new JWTClaimsSet.Builder()
                        .issuer(ISSUER)
                        .claim("preferred_username", "alice")
                        .claim("resource_access", Map.of("geoserver", Map.of("roles", List.of("viewer"))))
                        .build());
        jwt.sign(new RSASSASigner(key));
        return jwt.serialize();
    }

    private GeoServerJwtHeadersFilterConfig config(String name, boolean logSensitive, boolean validate) {
        GeoServerJwtHeadersFilterConfig config = new GeoServerJwtHeadersFilterConfig();
        config.setName(name);
        config.setClassName(GeoServerJwtHeadersFilter.class.getName());
        JwtConfiguration jwt = config.getJwtConfiguration();
        jwt.setUserNameHeaderAttributeName(HEADER);
        jwt.setUserNameJsonPath("preferred_username");
        jwt.setValidateToken(validate);
        jwt.setValidateTokenExpiry(false);
        jwt.setValidateTokenSignatureURL(JWKS_URL);
        jwt.setValidateTokenIssuer(ISSUER);
        config.setRoleSource(JWTHeaderRoleSource.JWT);
        jwt.setRolesJsonPath("resource_access.geoserver.roles");
        config.setLogSensitiveInformation(logSensitive);
        return config;
    }

    private CapturingFilter filter(GeoServerJwtHeadersFilterConfig config) throws Exception {
        config.setId(config.getName() + "-id");
        CapturingFilter filter = new CapturingFilter();
        filter.setSecurityManager(getSecurityManager());
        filter.initializeFromConfig(config);
        return filter;
    }

    private void run(GeoServerJwtHeadersFilter filter, String token) throws Exception {
        MockHttpServletRequest request = createRequest("web/");
        request.addHeader(HEADER, "Bearer " + token);
        SecurityContextHolder.clearContext();
        filter.doFilter(request, new MockHttpServletResponse(), (rq, rs) -> {});
        SecurityContextHolder.clearContext();
    }

    @Test
    public void testOffKeepsTheUsualLines() throws Exception {
        CapturingFilter filter = filter(config("Off", false, false));
        run(filter, unverifiableToken());

        assertTrue(filter.sensitive.isEmpty());
        assertTrue(filter.fine.contains("Extracted user name from JWT token: alice"));
        assertFalse(filter.all().contains("geo-team"));
        assertFalse(filter.all().contains(SIGNATURE));
    }

    @Test
    public void testOffRejectionLogsOnlyTheReason() throws Exception {
        CapturingFilter filter = filter(config("OffRejected", false, true));
        run(filter, unverifiableToken());

        assertTrue(filter.sensitive.isEmpty());
        assertTrue(filter.causes.isEmpty());
        assertTrue(
                filter.fine.toString(),
                filter.fine.contains("JWT Headers filter 'OffRejected' rejected the request header: "
                        + "Could not verify signature of the JWT with the given RSA Public Key"));
        assertFalse(filter.all().contains("geo-team"));
        assertFalse(filter.all().contains(SIGNATURE));
    }

    @Test
    public void testOnWithoutFineLevelLogsNothing() throws Exception {
        CapturingFilter filter = filter(config("OnNotFine", true, false));
        filter.fineLoggable = false;
        run(filter, unverifiableToken());

        assertTrue(filter.sensitive.isEmpty());
        assertTrue(filter.fine.isEmpty());
    }

    @Test
    public void testOnLogsDecodedClaimsOnce() throws Exception {
        CapturingFilter filter = filter(config("OnClaims", true, false));
        run(filter, unverifiableToken());

        String all = filter.all();
        assertTrue(all, all.contains("geo-team"));
        assertTrue(all, all.contains("took the user name 'alice' from 'preferred_username'"));
        assertTrue(all, all.contains("reads the roles from " + HEADER));
        assertFalse("the signature is never logged", all.contains(SIGNATURE));
        assertEquals(
                1,
                filter.sensitive.stream()
                        .filter(m -> m.startsWith("received in"))
                        .count());
    }

    @Test
    public void testOnLogsRejectionCause() throws Exception {
        CapturingFilter filter = filter(config("OnRejected", true, true));
        run(filter, unverifiableToken());

        assertTrue(filter.sensitive.contains("rejected the request header"));
        assertEquals(
                "Could not verify signature of the JWT with the given RSA Public Key",
                filter.causes.get(0).getMessage());
    }

    @Test
    public void testOnLogsRolesClaimOfValidatedToken() throws Exception {
        CapturingFilter filter = filter(config("OnValidatedRoles", true, true));
        String token = signedToken();
        run(filter, token);

        String all = filter.all();
        assertTrue(
                all,
                all.contains("reads the roles from the validated token, claim 'resource_access.geoserver.roles': "
                        + "[\"viewer\"]"));
        assertFalse(all.contains(token.substring(token.lastIndexOf('.') + 1)));
    }

    /** A JWT in a header described as JSON is still shown without its signature. */
    @Test
    public void testJsonRoleSourceNeverLogsTheSignature() throws Exception {
        GeoServerJwtHeadersFilterConfig config = config("OnJsonRoles", true, false);
        config.setRoleSource(JWTHeaderRoleSource.JSON);
        CapturingFilter filter = filter(config);
        run(filter, unverifiableToken());

        assertTrue(filter.all(), filter.all().contains("reads the roles from " + HEADER));
        assertFalse(filter.all().contains(SIGNATURE));
    }

    @Test
    public void testOptionSurvivesSaveAndLoad() throws Exception {
        GeoServerJwtHeadersFilterConfig config = config("SavedLogSensitive", true, false);
        getSecurityManager().saveFilter(config);

        GeoServerJwtHeadersFilterConfig loaded =
                (GeoServerJwtHeadersFilterConfig) getSecurityManager().loadFilterConfig("SavedLogSensitive", false);
        assertTrue(loaded.getLogSensitiveInformation());
    }

    @Test
    public void testLoggingProfileInstalled() {
        Resource profile = getDataDirectory().getResourceLoader().get("logs/JWT_HEADERS_LOGGING.xml");
        assertEquals(Resource.Type.RESOURCE, profile.getType());
    }

    @Override
    protected List<Filter> getFilters() {
        return new ArrayList<>();
    }
}
