/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.common;

import static java.lang.String.format;
import static java.util.logging.Level.SEVERE;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;
import static org.geoserver.security.filter.GeoServerRoleResolvers.PRE_AUTH_ROLE_SOURCE_RESOLVER;
import static org.geoserver.security.impl.GeoServerUser.ADMIN_USERNAME;
import static org.geoserver.security.impl.GeoServerUser.ROOT_USERNAME;
import static org.geoserver.security.oauth2.login.GeoServerOAuth2ClientRegistrationId.REG_ID_MICROSOFT;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.RoleSource;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverContext;
import org.geoserver.security.filter.GeoServerRoleResolvers.ResolverParam;
import org.geoserver.security.filter.GeoServerRoleResolvers.RoleResolver;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.RoleCalculator;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig;
import org.geoserver.security.oauth2.login.GeoServerOAuth2LoginFilterConfig.OpenIdRoleSource;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.user.OAuth2User;

/**
 * Resolves roles for a given user during login with OAuth2 and OpenID Connect.
 *
 * @author awaterme
 */
public class GeoServerOAuth2RoleResolver implements RoleResolver {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2RoleResolver.class);

    public static final class OAuth2ResolverParam extends ResolverParam {

        private OAuth2UserRequest userRequest;

        public OAuth2ResolverParam(
                String pPrincipal,
                HttpServletRequest pRequest,
                ResolverContext pContext,
                OAuth2UserRequest pUserRequest) {
            super(pPrincipal, pRequest, pContext);
            userRequest = pUserRequest;
        }

        /** @return the userRequest */
        public OAuth2UserRequest getUserRequest() {
            return userRequest;
        }
    }

    /** Default {@link Supplier} just creates a new {@link OAuth2UserService}. */
    private Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> userServiceSupplier =
            () -> new DefaultOAuth2UserService();

    /** Default {@link Supplier} just creates a new MSGraphRolesResolver. */
    private Supplier<MSGraphRolesResolver> msGraphRolesResolverSupplier = () -> new MSGraphRolesResolver();

    private GeoServerOAuth2LoginFilterConfig config;

    /** @param pConfig */
    public GeoServerOAuth2RoleResolver(GeoServerOAuth2LoginFilterConfig pConfig) {
        super();
        config = pConfig;
    }

    @Override
    public Collection<GeoServerRole> convert(ResolverParam pParam) {
        if (!(pParam instanceof OAuth2ResolverParam)) {
            throw new IllegalArgumentException(OAuth2ResolverParam.class.getSimpleName() + " required");
        }
        Collection<GeoServerRole> result = new ArrayList<>();
        String lPrincipal = pParam.getPrincipal();
        if (ADMIN_USERNAME.equalsIgnoreCase(lPrincipal) || ROOT_USERNAME.equalsIgnoreCase(lPrincipal)) {
            // avoid unintentional match with pre-existing administrator
            String lMsg = "Potentially harmful OAuth2 user '%s' detected. Granting no roles.";
            LOGGER.log(Level.WARNING, format(lMsg, lPrincipal));
            return result;
        }
        RoleSource rs = pParam.getContext().getRoleSource();
        if (rs == null) {
            LOGGER.log(SEVERE, "Role assignment failed. Role source unspecified.");
        } else if (rs instanceof OpenIdRoleSource) {
            OpenIdRoleSource oirs = (OpenIdRoleSource) rs;
            switch (oirs) {
                case AccessToken:
                    result = getRolesFromAccessToken(pParam);
                    break;
                case IdToken:
                    result = getRolesFromIdToken(pParam);
                    break;
                case UserInfo:
                    result = getRolesFromUserInfo(pParam);
                    break;
                case MSGraphAPI:
                    result = getRolesFromMSGraphAPI(pParam);
                    break;
                default:
                    String lMsg = "Role assigment failed. Unknown roleSource: {0}";
                    LOGGER.log(SEVERE, lMsg, oirs);
            }
        } else {
            result = PRE_AUTH_ROLE_SOURCE_RESOLVER.convert(pParam);
        }

        if (result == null) {
            result = new ArrayList<>();
        }
        GeoServerSecurityManager lSecurityManager = pParam.getSecurityManager();
        RoleCalculator calc = new RoleCalculator(lSecurityManager.getActiveRoleService());
        try {
            calc.addInheritedRoles(result);
        } catch (IOException e) {
            String lMsg = "Role calculation failed on inherited roles for user '%s'.";
            LOGGER.log(SEVERE, format(lMsg, pParam.getPrincipal()), e);
        }
        calc.addMappedSystemRoles(result);
        if (!result.contains(GeoServerRole.AUTHENTICATED_ROLE)) {
            result.add(GeoServerRole.AUTHENTICATED_ROLE);
        }
        if (LOGGER.isLoggable(Level.FINE)) {
            String lUser = lPrincipal;
            String lSrc = rs == null ? null : rs.toString();
            String lRoles = result.stream().map(r -> r.getAuthority()).collect(joining(","));
            LOGGER.fine(format("User '%s' received roles from roleSource=%s: %s", lUser, lSrc, lRoles));
        }
        return result;
    }

    private Collection<GeoServerRole> getRolesFromAccessToken(ResolverParam pParam) {
        OAuth2UserRequest lUsrRequest = ((OAuth2ResolverParam) pParam).getUserRequest();
        OAuth2AccessToken lAccessToken = lUsrRequest.getAccessToken();
        String lClaimName = config.getTokenRolesClaim();
        Collection<String> lRoles = new ArrayList<>();
        Set<String> lScopes = lAccessToken.getScopes();
        if (LOGGER.isLoggable(Level.FINE)) {
            String lMsg = "Analyzing access token for roles. Claim: %s. Scopes: %s, additionals: %s";
            String lScopeTxt = lScopes == null ? null : lScopes.stream().collect(joining(","));
            LOGGER.fine(format(lMsg, lClaimName, lScopeTxt, lUsrRequest.getAdditionalParameters()));
        }
        if ("scope".equals(lClaimName)) {
            lRoles = lAccessToken.getScopes();
        } else {
            Object lObject = lUsrRequest.getAdditionalParameters().get(lClaimName);
            lRoles = toStringList(lObject, pParam);
        }
        return lRoles.stream().map(r -> new GeoServerRole(r)).collect(toList());
    }

    private Collection<GeoServerRole> getRolesFromIdToken(ResolverParam pParam) {
        OAuth2UserRequest lUsrRequest = ((OAuth2ResolverParam) pParam).getUserRequest();
        OidcUserRequest lOidcReq = lUsrRequest instanceof OidcUserRequest ? ((OidcUserRequest) lUsrRequest) : null;

        if (lOidcReq == null) {
            String lMsg = "Role extraction failed. ID token unavailable for clientRegistration %s.";
            LOGGER.log(SEVERE, format(lMsg, lUsrRequest.getClientRegistration().getRegistrationId()));
            return new ArrayList<>();
        }
        String lClaimName = config.getTokenRolesClaim();
        Collection<String> lRoles = new ArrayList<>();

        OidcIdToken lIdToken = lOidcReq.getIdToken();
        if (LOGGER.isLoggable(Level.FINE)) {
            String lMsg = "Analyzing access token for roles. Claim: %s. Claims: %s";
            LOGGER.fine(format(lMsg, lClaimName, lIdToken.getClaims()));
        }
        List<String> lClaimList = lIdToken.getClaimAsStringList(lClaimName);
        if (lClaimList != null) {
            lRoles.addAll(lClaimList);
        }
        return lRoles.stream().map(r -> new GeoServerRole(r)).collect(toList());
    }

    private Collection<GeoServerRole> getRolesFromUserInfo(ResolverParam pParam) {
        OAuth2UserRequest lUsrRequest = ((OAuth2ResolverParam) pParam).getUserRequest();
        OAuth2UserService<OAuth2UserRequest, OAuth2User> lService = userServiceSupplier.get();
        OAuth2User lUser = lService.loadUser(lUsrRequest);

        String lClaimName = config.getTokenRolesClaim();
        Collection<String> lRoles = new ArrayList<>();

        if (LOGGER.isLoggable(Level.FINE)) {
            String lMsg = "Analyzing userInfo for roles. Claim: %s. User: %s";
            LOGGER.fine(format(lMsg, lClaimName, lUser));
        }
        if ("authorities".equals(lClaimName)) {
            Collection<? extends GrantedAuthority> authorities = lUser.getAuthorities();
            lRoles = authorities.stream().map(a -> a.getAuthority()).collect(toList());
        } else {
            Object lObject = lUser.getAttribute(lClaimName);
            lRoles = toStringList(lObject, pParam);
        }
        return lRoles.stream().map(r -> new GeoServerRole(r)).collect(toList());
    }

    private Collection<GeoServerRole> getRolesFromMSGraphAPI(ResolverParam pParam) {
        OAuth2UserRequest lUsrRequest = ((OAuth2ResolverParam) pParam).getUserRequest();
        ClientRegistration lClientReg = lUsrRequest.getClientRegistration();
        String lUsr = pParam.getPrincipal();
        if (!REG_ID_MICROSOFT.equals(lClientReg.getRegistrationId())) {
            // actually prevented by UI validation, but make sure here to not send foreign access
            // token around
            String lMsg = "Resolving roles failed. RoleSource Microsoft Graph API supported with "
                    + "provider %s only. Currently processing login with %s instead.";
            LOGGER.log(SEVERE, format(lMsg, REG_ID_MICROSOFT, lClientReg.getClientName()));
            return new ArrayList<>();
        }
        Collection<String> lRoles = new ArrayList<>();
        try {
            String accessToken = lUsrRequest.getAccessToken().getTokenValue();
            MSGraphRolesResolver resolver = msGraphRolesResolverSupplier.get();
            lRoles = resolver.resolveRoles(accessToken);
            if (LOGGER.isLoggable(Level.FINE)) {
                String lMsg = "Role assignments for '%s' from MS Graph: %s";
                String lRolesTxt = lRoles.stream().collect(joining(","));
                LOGGER.fine(format(lMsg, lUsr, lRolesTxt));
            }
        } catch (IOException e) {
            String lMsg = "Resolving roles from Microsoft Graph API failed for user '%s'.";
            LOGGER.log(SEVERE, format(lMsg, lUsr), e);
        }
        return lRoles.stream().map(r -> new GeoServerRole(r)).collect(toList());
    }

    private Collection<String> toStringList(Object pObject, ResolverParam pParam) {
        if (pObject == null) {
            String lMsg = "Role extraction failed. User '%s', roleSource=%s: Claim '%s' is missing.";
            String lClaim = config.getTokenRolesClaim();
            LOGGER.log(SEVERE, format(lMsg, pParam.getPrincipal(), pParam.getRoleSource(), lClaim));
            return new ArrayList<>();
        } else if (pObject instanceof String) {
            return Collections.singleton(pObject.toString());
        } else if (pObject instanceof String[]) {
            return Arrays.asList((String[]) pObject);
        } else if (pObject instanceof List) {
            List<?> lList = (List<?>) pObject;
            List<String> lRoles = lList.stream()
                    .filter(o -> o instanceof String)
                    .map(o -> (String) o)
                    .collect(toList());
            if (lRoles.size() == lList.size()) {
                // only consider if all strings
                return lRoles;
            }
        }
        String lUser = pParam.getPrincipal();
        String lType = pObject.getClass().getName();
        String lValue = pObject.toString();
        String lMsg = "Role extraction failed. User '%s', roleSource=%s: Type %s is not supported.";
        lMsg += " Value: %s";
        LOGGER.log(SEVERE, format(lMsg, lUser, pParam.getRoleSource(), lType, lValue));
        return new ArrayList<>();
    }

    public void setUserServiceSupplier(
            Supplier<OAuth2UserService<OAuth2UserRequest, OAuth2User>> pUserServiceSupplier) {
        if (pUserServiceSupplier == null) {
            throw new IllegalArgumentException("Supplier for OAuth2UserService must not be null.");
        }
        this.userServiceSupplier = pUserServiceSupplier;
    }

    public void setMsGraphRolesResolverSupplier(Supplier<MSGraphRolesResolver> pMsGraphRolesResolverSupplier) {
        if (pMsGraphRolesResolverSupplier == null) {
            throw new IllegalArgumentException("Supplier for MSGraphRolesResolver must not be null.");
        }
        this.msGraphRolesResolverSupplier = pMsGraphRolesResolverSupplier;
    }
}
