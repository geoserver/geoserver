/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2012 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.AuthenticationCachingFilter;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.Http403ForbiddenEntryPoint;
import org.springframework.util.StringUtils;

/**
 * Filter extending {@link GeoServerSecurityFilter}.
 *
 * <p>The encoded user name is passed as an URL parameter named {@link #authKeyParamName}.
 *
 * <p>The real user name is retrieved by querying an {@link AuthenticationKeyMapper} object stored
 * in {@link #authKeyMapperName}
 *
 * <p>This filter needs a {@link GeoServerUserGroupService} for authentication
 *
 * @author christian
 */
public class GeoServerAuthenticationKeyFilter extends GeoServerSecurityFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private String authKeyMapperName, authKeyParamName;

    private AuthenticationKeyMapper mapper;

    private String userGroupServiceName;

    protected AuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        aep = new Http403ForbiddenEntryPoint();

        AuthenticationKeyFilterConfig authConfig = (AuthenticationKeyFilterConfig) config;
        setAuthKeyParamName(authConfig.getAuthKeyParamName());
        setUserGroupServiceName(authConfig.getUserGroupServiceName());
        setAuthKeyMapperName(authConfig.getAuthKeyMapperName());
        mapper = (AuthenticationKeyMapper) GeoServerExtensions.bean(authKeyMapperName);
        mapper.setUserGroupServiceName(userGroupServiceName);
        mapper.setSecurityManager(getSecurityManager());
        mapper.configureMapper(authConfig.getMapperParameters());
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // String authKey = getAuthKey((HttpServletRequest) request);
        // if (authKey==null) { // nothing to do
        // chain.doFilter(request, response);
        // return;
        // }

        String cacheKey = authenticateFromCache(this, (HttpServletRequest) request);

        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate((HttpServletRequest) request, (HttpServletResponse) response, cacheKey);

            Authentication postAuthentication =
                    SecurityContextHolder.getContext().getAuthentication();
            if (postAuthentication != null && cacheKey != null) {
                if (cacheAuthentication(postAuthentication, (HttpServletRequest) request)) {
                    getSecurityManager()
                            .getAuthenticationCache()
                            .put(getName(), cacheKey, postAuthentication);
                }
            }
        }

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        chain.doFilter(request, response);
    }

    public String getAuthKeyMapperName() {
        return authKeyMapperName;
    }

    public void setAuthKeyMapperName(String authKeyMapperName) {
        this.authKeyMapperName = authKeyMapperName;
    }

    public String getAuthKeyParamName() {
        return authKeyParamName;
    }

    public void setAuthKeyParamName(String authKeyParamName) {
        this.authKeyParamName = authKeyParamName;
    }

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    /**
     * Try to authenticate and adds {@link GeoServerRole#AUTHENTICATED_ROLE} Does NOT authenticate
     * {@link GeoServerUser#ROOT_USERNAME}
     */
    protected void doAuthenticate(
            HttpServletRequest request, HttpServletResponse response, String authKey)
            throws IOException {

        if (authKey == null) return;

        GeoServerUser user = mapper.getUser(authKey);
        if (user == null) {
            return;
        }

        // no support for root login
        if (GeoServerUser.ROOT_USERNAME.equals(user.getUsername())) {
            LOGGER.warning("Authentication key login does accept the root user");
            return;
        }

        LOGGER.log(Level.FINE, "found user: = " + user.getUsername() + ", trying to authenticate");

        Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
        for (GrantedAuthority auth : user.getAuthorities()) {
            roles.add((GeoServerRole) auth);
        }
        if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE) == false)
            roles.add(GeoServerRole.AUTHENTICATED_ROLE);

        KeyAuthenticationToken result =
                new KeyAuthenticationToken(authKey, authKeyParamName, user, roles);

        SecurityContextHolder.getContext().setAuthentication(result);
    }

    public String getAuthKey(HttpServletRequest req) {
        String authKey = getAuthKeyParamValue(req);
        if (StringUtils.hasLength(authKey) == false) return null;
        return authKey;
    }

    /** Extracts authkey value from the request. */
    private String getAuthKeyParamValue(HttpServletRequest req) {
        String keyParamName = getAuthKeyParamName();
        for (Enumeration<String> a = req.getParameterNames(); a.hasMoreElements(); ) {
            String paramName = a.nextElement();

            if (keyParamName.equalsIgnoreCase(paramName)) {
                return req.getParameter(paramName);
            }
        }

        return null;
    }

    /** The cache key is the authentication key (global identifier) */
    @Override
    public String getCacheKey(HttpServletRequest req) {
        return getAuthKey(req);
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return true;
    }

    protected boolean cacheAuthentication(Authentication auth, HttpServletRequest request) {
        // only cache if no HTTP session is available
        if (request.getSession(false) != null) return false;

        return true;
    }

    public AuthenticationKeyMapper getMapper() {
        return mapper;
    }
}
