/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.AccessToken;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.IdToken;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.MSGraphAPI;
import static org.geoserver.security.oauth2.OpenIdConnectFilterConfig.OpenIdRoleSource.UserInfo;

import com.jayway.jsonpath.JsonPath;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import net.sf.json.JSONObject;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.oauth2.bearer.TokenValidator;
import org.geoserver.security.oauth2.services.OpenIdConnectTokenServices;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.jwt.Jwt;
import org.springframework.security.jwt.JwtHelper;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.token.DefaultRequestEnhancer;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeAccessTokenProvider;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.authentication.OAuth2AuthenticationDetails;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;

/** Authenticate using OpenID Connect. */
public class OpenIdConnectAuthenticationFilter extends GeoServerOAuthAuthenticationFilter {

    private static final Logger LOGGER = Logging.getLogger(OpenIdConnectAuthenticationFilter.class);

    static final String ID_TOKEN_VALUE = "OpenIdConnect-IdTokenValue";
    TokenValidator bearerTokenValidator;

    public OpenIdConnectAuthenticationFilter(
            SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate,
            TokenValidator bearerTokenValidator) {
        super(config, tokenServices, oauth2SecurityConfiguration, oauth2RestTemplate);
        // reconfigure the token services
        if (tokenServices instanceof OpenIdConnectTokenServices
                && config instanceof OpenIdConnectFilterConfig) {
            ((OpenIdConnectTokenServices) tokenServices)
                    .setConfiguration((OpenIdConnectFilterConfig) config);
            AuthorizationCodeAccessTokenProvider provider =
                    (AuthorizationCodeAccessTokenProvider)
                            GeoServerExtensions.bean("authorizationAccessTokenProvider");
            if (((OpenIdConnectFilterConfig) config).isSendClientSecret())
                provider.setTokenRequestEnhancer(new ClientSecretRequestEnhancer());
            else provider.setTokenRequestEnhancer(new DefaultRequestEnhancer());
        }
        // reconfigure the configuration, allow building a useful rest template
        if (oauth2SecurityConfiguration instanceof OpenIdConnectSecurityConfiguration
                && config instanceof OpenIdConnectFilterConfig) {
            OpenIdConnectSecurityConfiguration sc =
                    (OpenIdConnectSecurityConfiguration) oauth2SecurityConfiguration;
            OpenIdConnectFilterConfig idConfig = (OpenIdConnectFilterConfig) config;
            sc.setConfiguration(idConfig);
        }
        this.bearerTokenValidator = bearerTokenValidator;
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {
        String result = super.getPreAuthenticatedPrincipal(req, resp);

        if (result == null) {
            return null;
        }

        OAuth2AuthenticationType type =
                (OAuth2AuthenticationType) req.getAttribute(OAUTH2_AUTHENTICATION_TYPE_KEY);
        if ((type != null)
                && (type.equals(OAuth2AuthenticationType.BEARER))
                && (bearerTokenValidator != null)) {
            if (!((OpenIdConnectFilterConfig) filterConfig).isAllowBearerTokens()) {
                LOGGER.log(
                        Level.WARNING,
                        "OIDC: received an attached Bearer token, but Bearer tokens aren't allowed!");
                throw new IOException(
                        "OIDC: received an attached Bearer token, but Bearer tokens aren't allowed!");
            }
            // we must validate
            String accessToken =
                    (String) req.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);
            Map userinfoMap = (Map) req.getAttribute(OAUTH2_ACCESS_TOKEN_CHECK_KEY);
            Jwt decodedAccessToken = JwtHelper.decode(accessToken);
            Map accessTokenClaims = JSONObject.fromObject(decodedAccessToken.getClaims());
            try {
                bearerTokenValidator.verifyToken(
                        (OpenIdConnectFilterConfig) filterConfig, accessTokenClaims, userinfoMap);
            } catch (Exception e) {
                throw new IOException("Attached Bearer Token is invalid", e);
            }
        }

        return result;
    }

    @Override
    protected Collection<GeoServerRole> getRoles(HttpServletRequest request, String principal)
            throws IOException {
        RoleSource rs = getRoleSource();

        if (filterConfig.isAllowUnSecureLogging()) {
            String rolesAttributePath =
                    ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
            LOGGER.log(
                    Level.FINE,
                    "OIDC: Getting Roles from " + rs + ", location=" + rolesAttributePath);
        }
        Collection<GeoServerRole> result = null;

        if (AccessToken.equals(rs)) {
            result =
                    getRolesFromToken(
                            (String)
                                    request.getAttribute(
                                            OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE));
        } else if (IdToken.equals(rs)) {
            result = getRolesFromToken((String) request.getAttribute(ID_TOKEN_VALUE));
        } else if (UserInfo.equals(rs)) {
            result = getRolesFromUserInfo(request);
        } else if (MSGraphAPI.equals(rs)) {
            result = getRolesFromMSGraphAPI(request);
        } else {
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

    private Collection<GeoServerRole> getRolesFromMSGraphAPI(HttpServletRequest request)
            throws IOException {
        try {
            String accessToken =
                    (String) request.getAttribute(OAuth2AuthenticationDetails.ACCESS_TOKEN_VALUE);
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
    private Collection<GeoServerRole> getRolesFromUserInfo(HttpServletRequest request)
            throws IOException {

        Map userinfoMap = (Map) request.getAttribute(OAUTH2_ACCESS_TOKEN_CHECK_KEY);
        if (userinfoMap == null) {
            return null;
        }

        String rolesAttributePath =
                ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
        Object o = extractFromJSON(userinfoMap, rolesAttributePath);

        List<GeoServerRole> result = getGeoServerRoles(rolesAttributePath, o);
        return result;
    }

    /**
     * Given a part of the json, jsonObject, (from rolesAttributePath in the main json object),
     * generate GeoServerRole roles.
     *
     * <p>jsonObject can either be a single String or a list-of-Strings.
     *
     * @param rolesAttributePath for logging purposes - where are we looking for the role
     *     information?
     * @param jsonObject should be either a String or list-of-String
     * @return
     * @throws IOException
     */
    @SuppressWarnings("unchecked")
    private List<GeoServerRole> getGeoServerRoles(String rolesAttributePath, Object jsonObject)
            throws IOException {
        List<GeoServerRole> result = new ArrayList<>();
        if (jsonObject instanceof String) {
            result.add(new GeoServerRole((String) jsonObject));
        } else if (jsonObject instanceof List) {
            ((List) jsonObject).stream().forEach(v -> result.add(new GeoServerRole((String) v)));
        } else {
            LOGGER.log(
                    Level.FINE,
                    "Did not find "
                            + rolesAttributePath
                            + " in the token, returning an empty role list");
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
        Jwt decoded = JwtHelper.decode(token);
        String claims = decoded.getClaims();
        String rolesAttributePath =
                ((OpenIdConnectFilterConfig) this.filterConfig).getTokenRolesClaim();
        Object o = extractFromJSON(claims, rolesAttributePath);
        List<GeoServerRole> result = getGeoServerRoles(rolesAttributePath, o);

        return result;
    }

    private void enrichWithRoleCalculator(List<GeoServerRole> roles) throws IOException {
        RoleCalculator calc = new RoleCalculator(getSecurityManager().getActiveRoleService());
        calc.addInheritedRoles(roles);
        calc.addMappedSystemRoles(roles);
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {

        String idToken = null;
        if (request.getAttribute(OpenIdConnectAuthenticationFilter.ID_TOKEN_VALUE) != null) {
            idToken =
                    (String) request.getAttribute(OpenIdConnectAuthenticationFilter.ID_TOKEN_VALUE);
        } else {
            OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();
            if (token != null && token.getAdditionalInformation() != null) {
                Object maybeIdToken = token.getAdditionalInformation().get("id_token");
                if (maybeIdToken instanceof String) {
                    idToken = (String) maybeIdToken;
                }
            }
        }

        final String endSessionUrl =
                ((OpenIdConnectFilterConfig) filterConfig).buildEndSessionUrl(idToken).toString();
        super.logout(request, response, authentication);

        request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, endSessionUrl);
    }
}
