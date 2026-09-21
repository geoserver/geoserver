/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import static org.geoserver.security.oauth2.token.OAuth2ClaimsHelpers.asStringList;
import static org.geoserver.security.oauth2.token.OAuth2ClaimsHelpers.getClaim;

import java.util.List;
import java.util.Set;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.util.StringUtils;

/**
 * Endpoints and Bearer-token rules derived from a configured Microsoft Entra directory (tenant) ID.
 *
 * <p>Everything tenant-derived lives here so the value used to build the {@code ClientRegistration} and the values
 * enforced on incoming Bearer tokens cannot drift apart.
 *
 * <p>Two checks together are what confine a single-tenant filter, and neither is sufficient alone:
 *
 * <ul>
 *   <li><b>Issuer</b> — Entra serves the same v2.0 signing keys from every tenant path, so a valid signature proves
 *       only that Microsoft minted the token, not which tenant it came from.
 *   <li><b>Audience</b> — a tenant typically hosts many app registrations. Without this, a token minted for any other
 *       application in the same tenant authenticates against GeoServer.
 * </ul>
 */
public final class MicrosoftEntraTenant {

    private static final String ERROR_CODE = "invalid_token";

    /** Issuer of tokens from the v2.0 endpoints, which is what the login flow always uses. */
    private static final String V2_ISSUER_PREFIX = "https://login.microsoftonline.com/";

    /**
     * Issuer of tokens from the v1.0 endpoints. Entra emits this form whenever the resource application's
     * {@code accessTokenAcceptedVersion} is left unset, so a Bearer token reaching GeoServer may legitimately carry it
     * even though the login flow never does. Still tenant-scoped, so accepting it costs nothing.
     */
    private static final String V1_ISSUER_PREFIX = "https://sts.windows.net/";

    private MicrosoftEntraTenant() {}

    /** @return the tenant ID trimmed, or {@code null} when none is configured (multi-tenant "common" endpoint) */
    public static String normalize(String pTenantId) {
        return StringUtils.hasText(pTenantId) ? pTenantId.trim() : null;
    }

    /** @return the v2.0 issuer, the value recorded on the {@code ClientRegistration} for id_token validation */
    public static String v2Issuer(String pTenantId) {
        return V2_ISSUER_PREFIX + pTenantId + "/v2.0";
    }

    /** @return the v1.0 issuer, accepted on the Bearer path only */
    public static String v1Issuer(String pTenantId) {
        return V1_ISSUER_PREFIX + pTenantId + "/";
    }

    /** Accepts tokens issued by the given tenant through either the v1.0 or the v2.0 endpoints, and no others. */
    public static OAuth2TokenValidator<Jwt> issuedByTenant(String pTenantId) {
        Set<String> accepted = Set.of(v2Issuer(pTenantId), v1Issuer(pTenantId));
        return token -> {
            if (token == null) {
                return failure("JWT token is missing");
            }
            Object issuer = getClaim(token.getClaims(), "iss");
            if (issuer != null && accepted.contains(issuer.toString())) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("Token was not issued by the configured Microsoft Entra tenant");
        };
    }

    /**
     * Accepts tokens minted for the given application, identified either by its client ID or by the default
     * {@code api://<clientId>} application ID URI. An application ID URI the administrator has customised will not
     * match — those deployments configure audience validation explicitly instead.
     */
    public static OAuth2TokenValidator<Jwt> issuedForClient(String pClientId) {
        if (!StringUtils.hasText(pClientId)) {
            // Configuration validation requires a client ID for an enabled provider; fail closed regardless.
            return token -> failure("Audience validation misconfigured: no client ID");
        }
        Set<String> accepted = Set.of(pClientId, "api://" + pClientId);
        return token -> {
            if (token == null) {
                return failure("JWT token is missing");
            }
            List<String> audiences = asStringList(getClaim(token.getClaims(), "aud"));
            if (audiences != null && audiences.stream().anyMatch(accepted::contains)) {
                return OAuth2TokenValidatorResult.success();
            }
            return failure("Token was not issued for this GeoServer application");
        };
    }

    private static OAuth2TokenValidatorResult failure(String pDescription) {
        return OAuth2TokenValidatorResult.failure(new OAuth2Error(ERROR_CODE, pDescription, null));
    }
}
