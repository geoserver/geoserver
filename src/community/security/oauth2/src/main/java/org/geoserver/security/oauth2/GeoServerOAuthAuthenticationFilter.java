/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.PreAuthenticatedUserNameRoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.OAuth2RestOperations;
import org.springframework.security.oauth2.client.filter.OAuth2ClientAuthenticationProcessingFilter;
import org.springframework.security.oauth2.client.resource.UserRedirectRequiredException;
import org.springframework.security.oauth2.client.token.AccessTokenRequest;
import org.springframework.security.oauth2.client.token.grant.code.AuthorizationCodeResourceDetails;
import org.springframework.security.oauth2.common.DefaultOAuth2AccessToken;
import org.springframework.security.oauth2.common.OAuth2AccessToken;
import org.springframework.security.oauth2.provider.token.RemoteTokenServices;
import org.springframework.security.oauth2.provider.token.ResourceServerTokenServices;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.preauth.PreAuthenticatedAuthenticationToken;

/**
 * OAuth2 Authentication filter receiving/validating proxy tickets and service tickets.
 * 
 * @author Alessio Fabiani, GeoSolutions
 *
 */
public class GeoServerOAuthAuthenticationFilter extends GeoServerPreAuthenticatedUserNameFilter
        implements GeoServerAuthenticationFilter, LogoutHandler {

    OAuth2FilterConfig filterConfig;

    OAuth2RestOperations restTemplate;

    OAuth2ClientAuthenticationProcessingFilter filter = new OAuth2ClientAuthenticationProcessingFilter("/");

    ResourceServerTokenServices tokenServices;

    GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration;

    public GeoServerOAuthAuthenticationFilter(SecurityNamedServiceConfig config,
            RemoteTokenServices tokenServices,
            GeoServerOAuth2SecurityConfiguration oauth2SecurityConfiguration,
            OAuth2RestOperations oauth2RestTemplate) {
        this.filterConfig = (OAuth2FilterConfig) config;
        this.tokenServices = tokenServices;
        this.oauth2SecurityConfiguration = oauth2SecurityConfiguration;
        this.restTemplate = oauth2RestTemplate;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        aep = filterConfig.getAuthenticationEntryPoint();
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return this.aep;
    }

    /**
     * Try to authenticate if there is no authenticated principal
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        String cacheKey = authenticateFromCache(this, (HttpServletRequest) request, (HttpServletResponse) response);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {

            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response);

            Authentication postAuthentication = SecurityContextHolder.getContext()
                    .getAuthentication();
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager().getAuthenticationCache().put(getName(), cacheKey,
                            postAuthentication);
                }
            }
        }

        chain.doFilter(request, response);
    }

    protected String authenticateFromCache(AuthenticationCachingFilter filter, HttpServletRequest request, HttpServletResponse response) {
        
        Authentication authFromCache=null;
        String cacheKey=null;
        if (SecurityContextHolder.getContext().getAuthentication()==null) {
            cacheKey = getCacheKey(request, response);
            if (cacheKey!=null) { 
                authFromCache = getSecurityManager().getAuthenticationCache().get(getName(), cacheKey);
                if (authFromCache!=null)
                    SecurityContextHolder.getContext().setAuthentication(authFromCache);
                else
                    return cacheKey;
            }
                
        }
        return null;     
    }
    
    protected String getCacheKey(HttpServletRequest request, HttpServletResponse response) {
        
        if (request.getSession(false)!=null) // no caching if there is an HTTP session
            return null;
        
        String retval;
        try {
            retval = getPreAuthenticatedPrincipal(request, response);
        } catch (Exception e) {
            return null;
        }
        
        if (GeoServerUser.ROOT_USERNAME.equals(retval))
            return null;
        return retval;
    }
    
    @Override
    public void logout(HttpServletRequest request, HttpServletResponse response,
            Authentication authentication) {

        OAuth2AccessToken token = restTemplate.getOAuth2ClientContext().getAccessToken();
        if ((token != null && token.getTokenType().equalsIgnoreCase(OAuth2AccessToken.BEARER_TYPE)) || 
                request.getRequestURI().endsWith(filterConfig.getLogoutEndpoint())) {

            restTemplate.getOAuth2ClientContext().setAccessToken(null);

            final AccessTokenRequest accessTokenRequest = restTemplate.getOAuth2ClientContext()
                    .getAccessTokenRequest();
            if (accessTokenRequest != null && accessTokenRequest.getStateKey() != null) {
                restTemplate.getOAuth2ClientContext()
                        .removePreservedState(accessTokenRequest.getStateKey());
            }

            request.getSession().invalidate();
            response.setStatus(HttpServletResponse.SC_NO_CONTENT);
            Cookie[] allCookies = request.getCookies();

            for (int i = 0; i < allCookies.length; i++) {
                String name = allCookies[i].getName();
                if (name.equalsIgnoreCase("JSESSIONID")) {
                    Cookie cookieToDelete = allCookies[i];
                    cookieToDelete.setMaxAge(-1);
                    cookieToDelete.setPath("/");
                    cookieToDelete.setComment("EXPIRING COOKIE at " + System.currentTimeMillis());
                    response.addCookie(cookieToDelete);
                }
            }
            SecurityContextHolder.getContext().setAuthentication(null);

            request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR,
                    filterConfig.getLogoutUri());
        }
    }

    @Override
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        String principal = null;
        try {
            principal = getPreAuthenticatedPrincipal(request, response);
        } catch (IOException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
            principal = null;
        } catch (ServletException e1) {
            LOGGER.log(Level.FINE, e1.getMessage(), e1);
            principal = null;
        }

        if (principal == null || principal.trim().length() == 0) {
            return;
        }

        LOGGER.log(Level.FINE,
                "preAuthenticatedPrincipal = " + principal + ", trying to authenticate");

        PreAuthenticatedAuthenticationToken result = null;
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            result = new PreAuthenticatedAuthenticationToken(principal, null,
                    Collections.singleton(GeoServerRole.ADMIN_ROLE));
        } else {
            Collection<GeoServerRole> roles = null;
            try {
                roles = getRoles(request, principal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            result = new PreAuthenticatedAuthenticationToken(principal, null, roles);

        }

        result.setDetails(getAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);
    }

    @Override
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        try {
            return getPreAuthenticatedPrincipal(request, null);
        } catch (IOException e) {
            return null;
        } catch (ServletException e) {
            return null;
        }
    }

    protected String getPreAuthenticatedPrincipal(HttpServletRequest req, HttpServletResponse resp)
            throws IOException, ServletException {

        // Make sure the REST Resource Template has been correctly configured
        configureRestTemplate();

        // Avoid retrieving the user name more than once
        if (req.getAttribute(UserNameAlreadyRetrieved) != null)
            return (String) req.getAttribute(UserName);

        // Search for an access_token on the request (simulating SSO)
        String accessToken = req.getParameter("access_token");

        if (accessToken != null && restTemplate.getOAuth2ClientContext().getAccessToken() == null) {
            restTemplate.getOAuth2ClientContext()
                    .setAccessToken(new DefaultOAuth2AccessToken(accessToken));
        }

        // Setting up OAuth2 Filter services and resource template
        filter.setRestTemplate(restTemplate);
        filter.setTokenServices(tokenServices);

        // Validating the access_token
        Authentication authentication = null;
        try {
            authentication = filter.attemptAuthentication(req, null);
        } catch (Exception e) {
            if (e instanceof UserRedirectRequiredException) {
                // Intercepting a "UserRedirectRequiredException" and redirect to the OAuth2 Provider login URI
                this.aep.commence(req, resp, null);
            }
        }

        String principal = (authentication != null ? (String) authentication.getPrincipal() : null);
        if (principal != null && principal.trim().length() == 0)
            principal = null;
        try {
            if (principal != null && PreAuthenticatedUserNameRoleSource.UserGroupService
                    .equals(getRoleSource())) {
                GeoServerUserGroupService service = getSecurityManager()
                        .loadUserGroupService(getUserGroupServiceName());
                GeoServerUser u = service.getUserByUsername(principal);
                if (u != null && u.isEnabled() == false) {
                    principal = null;
                    handleDisabledUser(u, req);
                }

            }
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        req.setAttribute(UserNameAlreadyRetrieved, Boolean.TRUE);
        if (principal != null)
            req.setAttribute(UserName, principal);
        return principal;
    }

    protected void configureRestTemplate() {
        AuthorizationCodeResourceDetails details = (AuthorizationCodeResourceDetails) restTemplate
                .getResource();

        details.setClientId(filterConfig.getCliendId());
        details.setClientSecret(filterConfig.getClientSecret());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setClientId(filterConfig.getCliendId());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setClientSecret(filterConfig.getClientSecret());

        details.setAccessTokenUri(filterConfig.getAccessTokenUri());
        details.setUserAuthorizationUri(filterConfig.getUserAuthorizationUri());
        details.setPreEstablishedRedirectUri(filterConfig.getRedirectUri());
        ((GeoServerOAuthRemoteTokenServices) this.tokenServices)
                .setCheckTokenEndpointUrl(filterConfig.getCheckTokenEndpointUrl());

        details.setScope(parseScopes(filterConfig.getScopes()));
    }

    protected List<String> parseScopes(String commaSeparatedScopes) {
        List<String> scopes = newArrayList();
        Collections.addAll(scopes, commaSeparatedScopes.split(","));
        return scopes;
    }

    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        // Nothing to do; everything is handled by {@link getPreAuthenticatedPrincipal}
        return null;
    }

}
