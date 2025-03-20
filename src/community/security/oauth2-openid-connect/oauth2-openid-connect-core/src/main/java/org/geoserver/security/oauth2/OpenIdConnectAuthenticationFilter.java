/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import com.jayway.jsonpath.JsonPath;
import com.nimbusds.jose.JOSEObject;
import com.nimbusds.jose.JWEObject;
import com.nimbusds.jwt.JWT;
import com.nimbusds.jwt.JWTParser;
import java.io.IOException;
import java.text.ParseException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.auth.AuthenticationCache;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource;
import org.geoserver.security.oauth2.bearer.TokenValidator;
import org.geoserver.security.oauth2.pkce.PKCERequestEnhancer;
import org.geoserver.security.oauth2.services.OpenIdConnectTokenServices;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.keygen.Base64StringKeyGenerator;
import org.springframework.security.crypto.keygen.StringKeyGenerator;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.DefaultRequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/** Authenticate using OpenID Connect. */
public class OpenIdConnectAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {

    private static final Logger LOGGER = Logging.getLogger(OpenIdConnectAuthenticationFilter.class);

    static final String ID_TOKEN_VALUE = "OpenIdConnect-IdTokenValue";

    /**
     * Contains the value of the "exp" claim of the access token (for JWE tokens, the equivalent attribute obtained from
     * the instrospection call). This is used to cache the access token. According to spec: "its value is a JSON
     * [RFC8259] number representing the number of seconds from 1970-01-01T00:00:00Z as measured in UTC until the
     * date/time."
     */
    static final String ACCESS_TOKEN_EXPIRATION = "OpenIdConnect-AccessTokenExpiration";

    TokenValidator bearerTokenValidator;

    /** Generator used for Public Key Code Exchange code_verifier */
    private final StringKeyGenerator secureKeyGenerator =
            new Base64StringKeyGenerator(Base64.getUrlEncoder().withoutPadding(), 96);

    public OpenIdConnectAuthenticationFilter(
            SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate,
            TokenValidator bearerTokenValidator) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
        // reconfigure the token services
        if (tokenServices instanceof OpenIdConnectTokenServices && config instanceof OpenIdConnectFilterConfig) {

            OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) config;

            ((OpenIdConnectTokenServices) tokenServices).setConfiguration(idConfig);
            AuthorizationCodeAccessTokenProvider provider =
                    (AuthorizationCodeAccessTokenProvider) GeoServerExtensions.bean("authorizationAccessTokenProvider");
            if (idConfig.isUsePKCE()) provider.setTokenRequestEnhancer(new PKCERequestEnhancer(idConfig));
            else if (idConfig.isSendClientSecret()) provider.setTokenRequestEnhancer(new ClientSecretRequestEnhancer());
            else provider.setTokenRequestEnhancer(new DefaultRequestEnhancer());
        }
        // reconfigure the configuration, allow building a useful rest template
        if (oauth2SecurityConfiguration instanceof OpenIdConnectSecurityConfiguration
                && config instanceof OpenIdConnectFilterConfig) {
            OpenIdConnectSecurityConfiguration sc = (OpenIdConnectSecurityConfiguration) oauth2SecurityConfiguration;
            OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) config;
            sc.setConfiguration(idConfig);
        }
        this.bearerTokenValidator = bearerTokenValidator;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        if (config instanceof OpenIdConnectFilterConfig) {
            // in case cache authentication got disabled, clear the cache
            OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) config;
            if (!idConfig.isCacheAuthentication()) {
                AuthenticationCache cache = getSecurityManager().getAuthenticationCache();
                if (cache != null) cache.removeAll(getName());
            }
        }
    }

    @Override
    protected void tryCacheAuthentication(HttpServletRequest request, String cacheKey, Authentication auth) {
        OpenIdConnectFilterConfig config = (OpenIdConnectFilterConfig) filterConfig;
        if (auth != null
                && request.getAttribute(ACCESS_TOKEN_EXPIRATION) instanceof Long
                && config.isCacheAuthentication()) {
            long exp = (Long) request.getAttribute(ACCESS_TOKEN_EXPIRATION);
            long now = Instant.now().getEpochSecond(); // epoch is guaranteed to be in UTC like exp
            int ttlSeconds = (int) (exp - now);
            if (ttlSeconds > 0)
                getSecurityManager().getAuthenticationCache().put(getName(), cacheKey, auth, ttlSeconds, ttlSeconds);
        }
    }

    @Override
    protected void enhanceAccessTokenRequest(HttpServletRequest httpRequest, AccessTokenRequest accessTokenRequest) {
        super.enhanceAccessTokenRequest(httpRequest, accessTokenRequest);

        OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) filterConfig;
        if (idConfig.isUsePKCE()) {
            var session = httpRequest.getSession();
            var validator = (String) session.getAttribute("OIDC_CODE_VERIFIER");
            if (validator != null) {
                accessTokenRequest.put("code_verifier", Collections.singletonList(validator));
            }
        }
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String result = super.getPreAuthenticatedPrincipal(req, resp);

        if (result == null) {
            return null;
        }

        OAuth2AuthenticationType type = (OAuth2AuthenticationType) req.getAttribute(OAUTH2_AUTHENTICATION_TYPE_KEY);
        if ((type != null) && (type.equals(OAuth2AuthenticationType.BEARER)) && (bearerTokenValidator != null)) {
            if (!((OpenIdConnectFilterConfig) filterConfig).isAllowBearerTokens()) {
                LOGGER.log(Level.WARNING, "OIDC: received an attached Bearer token, but Bearer tokens aren't allowed!");
                throw new IOException("OIDC: received an attached Bearer token, but Bearer tokens aren't allowed!");
            }
            // we must validate

            try {
                validateBearerToken(req);
            } catch (Exception e) {
                throw new IOException("Attached Bearer Token is invalid", e);
            }
        }

        return result;
    }

    private void validateBearerToken(HttpServletRequest req) throws Exception {
        String accessToken = (String) req.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);
        Map userinfoMap = (Map) req.getAttribute(OAUTH2_ACCESS_TOKEN_CHECK_KEY);

        JWT jwt = JWTParser.parse(accessToken);

        if (jwt.getJWTClaimsSet() != null) {
            Map<String, Object> accessTokenClaims = Optional.ofNullable(jwt.getJWTClaimsSet())
                    .map(cs -> cs.getClaims())
                    .orElse(Collections.emptyMap());
            bearerTokenValidator.verifyToken((OpenIdConnectFilterConfig) filterConfig, accessTokenClaims, userinfoMap);
            collectExpiration(req, accessTokenClaims);
        } else if (jwt instanceof JWEObject) {
            if (tokenServices instanceof OpenIdConnectTokenServices) {
                OpenIdConnectTokenServices ots = (OpenIdConnectTokenServices) tokenServices;
                Map<String, Object> result = ots.introspectToken(accessToken);
                if (!Boolean.TRUE.equals(result.get("active"))) {
                    throw new Exception("Bearer token is not active");
                }
                collectExpiration(req, result);
            } else {
                throw new Exception("Cannot verify bearer token, please setup an introspection endpoint");
            }
        } else {
            throw new Exception("Bearer token validation not supported for this configuration");
        }
    }

    /** If an expiration attribute is set, we can use it to cache the access token */
    private static void collectExpiration(HttpServletRequest req, Map<String, Object> properties) {
        if (properties.get("exp") instanceof Number) {
            long exp = ((Number) properties.get("exp")).longValue();
            if (exp > Instant.now().getEpochSecond()) { // epoch is guaranteed to be in UTC like exp
                req.setAttribute(ACCESS_TOKEN_EXPIRATION, exp);
            }
        }
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal) throws IOException {
        RoleSource rs = getRoleSource();
        if (rs == null) {
            LOGGER.log(
                    Level.WARNING,
                    "OIDC: None of the supported token claims [{}] have been set for delivering roles.",
                    Arrays.stream(OpenIdRoleSource.values())
                            .map(v -> v.toString())
                            .collect(Collectors.joining(", ")));

            return null;
        }
        if (!(rs instanceof OpenIdRoleSource)) {
            return super.getRoles(request, principal);
        }
        OpenIdRoleSource oirs = (OpenIdRoleSource) rs;

        if (filterConfig.isAllowUnSecureLogging()) {
            String rolesAttributePath = ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
            LOGGER.log(
                    Level.FINE, "OIDC: Getting Roles from {0}, location={1}", new Object[] {oirs, rolesAttributePath});
        }
        Collection<GeoServerRole> result = null;
        switch (oirs) {
            case AccessToken:
                result = getRolesFromToken(
                        (String) request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE));
                break;
            case IdToken:
                result = getRolesFromToken((String) request.getAttribute(ID_TOKEN_VALUE));
                break;
            case UserInfo:
                result = getRolesFromUserInfo(request);
                break;
            case MSGraphAPI:
                result = getRolesFromMSGraphAPI(request);
                break;
            default:
                LOGGER.log(Level.FINE, "OIDC: Unknown OpenIdRoleSource = {0}", oirs);
                result = super.getRoles(request, principal);
        }

        if (filterConfig.isAllowUnSecureLogging()) {
            if (result == null) {
                LOGGER.log(Level.FINE, "OIDC: roles returned null (unexpected)");
            } else if (result.isEmpty()) {
                LOGGER.log(Level.FINE, "OIDC: roles returned NO ROLES");
            } else {
                for (GeoServerRole role : result) {
                    LOGGER.log(Level.FINE, "OIDC: Geoserver Roles: " + role.getAuthority());
                }
            }
        }

        return result;
    }

    private Collection<GeoServerRole> getRolesFromMSGraphAPI(HttpServletRequest request) throws IOException {
        try {
            String accessToken = (String) request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);
            MSGraphRolesResolver resolver = new MSGraphRolesResolver();

            List<String> groupIds = resolver.resolveRoles(accessToken);
            List<GeoServerRole> result = new ArrayList<>();
            groupIds.stream().forEach(v -> result.add(new GeoServerRole(v)));
            if (!result.isEmpty()) {
                enrichWithRoleCalculator(result);
            }
            return result;
        } catch (Exception e) {
            throw new IOException("error getting roles from MS Graph API", e);
        }
    }

    static Object extractFromJSON(String json, String path) {
        try {
            return JsonPath.read(json, path);
        } catch (Exception e) {
            // do nothing - the ID Token doesn't have that attribute - handled later with o=null
            return null;
        }
    }

    static Object extractFromJSON(Map json, String path) {
        try {
            return JsonPath.read(json, path);
        } catch (Exception e) {
            // do nothing - the ID Token doesn't have that attribute - handled later with o=null
            return null;
        }
    }

    // since we've already requested the userinfo for an oidc bearer (or via code) to validate it.
    // We saved this information in the OAuth2Request extension so we don't have to request it
    // again.
    // HttpServletRequest to OAuth2Request via an attribute (key OAUTH2_AUTHENTICATION_KEY)
    // OAuth2Request -> Extensions
    // Extensions -> Map<String, Serializable> (the actual userinfo response) (via key
    // ACCESS_TOKEN_CHECK_KEY)
    //
    // NOTE: in oauth2 this is the "check access token" endpoint.  In oidc this is the userinfo
    // endpoint.
    private Collection<GeoServerRole> getRolesFromUserInfo(HttpServletRequest request) throws IOException {

        Map userinfoMap = (Map) request.getAttribute(OAUTH2_ACCESS_TOKEN_CHECK_KEY);
        if (userinfoMap == null) {
            return null;
        }

        String rolesAttributePath = ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
        Object o = extractFromJSON(userinfoMap, rolesAttributePath);

        List<GeoServerRole> result = getGeoServerRoles(rolesAttributePath, o);
        return result;
    }

    /**
     * Given a part of the json, jsonObject, (from rolesAttributePath in the main json object), generate GeoServerRole
     * roles.
     *
     * <p>jsonObject can either be a single String or a list-of-Strings.
     *
     * @param rolesAttributePath for logging purposes - where are we looking for the role information?
     * @param jsonObject should be either a String or list-of-String
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private List<GeoServerRole> getGeoServerRoles(String rolesAttributePath, Object jsonObject) throws IOException {
        List<GeoServerRole> result = new ArrayList<>();
        if (jsonObject instanceof String) {
            result.add(new GeoServerRole((String) jsonObject));
        } else if (jsonObject instanceof List) {
            ((List) jsonObject).stream().forEach(v -> result.add(new GeoServerRole((String) v)));
        } else {
            LOGGER.log(
                    Level.FINE, "Did not find " + rolesAttributePath + " in the token, returning an empty role list");
        }
        if (!result.isEmpty()) {
            enrichWithRoleCalculator(result);
        }
        return result;
    }

    private Collection<GeoServerRole> getRolesFromToken(String token) throws IOException {
        if (token == null) {
            LOGGER.warning("Token not found, cannot perform role extraction");
            return new ArrayList<>();
        }
        try {
            JOSEObject jo = JOSEObject.parse(token);
            String claims = jo.getPayload().toString();
            String rolesAttributePath = ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
            Object o = extractFromJSON(claims, rolesAttributePath);
            List<GeoServerRole> result = getGeoServerRoles(rolesAttributePath, o);

            return result;
        } catch (ParseException e) {
            throw new IOException("Error parsing token", e);
        }
    }

    private void enrichWithRoleCalculator(List<GeoServerRole> roles) throws IOException {
        RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
        calc.addInheritedRoles(roles);
        calc.addMappedSystemRoles(roles);
    }

    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {

        String idToken = null;
        if (request.getAttribute(OpenIdConnectAuthenticationFilter.ID_TOKEN_VALUE) != null) {
            idToken = (String) request.getAttribute(OpenIdConnectAuthenticationFilter.ID_TOKEN_VALUE);
        } else {
            OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();
            if (token != null && token.getAdditionalInformation() != null) {
                Object maybeIdToken = token.getAdditionalInformation().get("id_token");
                if (maybeIdToken instanceof String) {
                    idToken = (String) maybeIdToken;
                }
            }
        }

        final String endSessionUrl = ((OpenIdConnectFilterConfig) filterConfig)
                .buildEndSessionUrl(idToken)
                .toString();
        super.logout(request, response, authentication);

        request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, endSessionUrl);
    }
}
