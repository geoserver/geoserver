/*
 *  Copyright (C) 2007 - 2013 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * 
 *  GPLv3 + Classpath exception
 * 
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 * 
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 * 
 *  You should have received a copy of the GNU General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.geoserver.geoserver.authentication.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.codec.binary.Base64;
import org.geoserver.geofence.services.RuleReaderService;
import org.geoserver.geofence.services.dto.AuthUser;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geotools.util.logging.Logging;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 *
 * @author ETj (etj at geo-solutions.it)
 */
public class GeoFenceAuthFilter
        // extends GeoServerSecurityFilter
        extends GeoServerCompositeFilter implements GeoServerAuthenticationFilter {

    static final Logger LOGGER = Logging.getLogger(GeoFenceAuthFilter.class);

    private RuleReaderService ruleReaderService;

    private GeoFenceSecurityProvider geofenceAuth;

    // static final String ROOT_ROLE = "ROLE_ADMINISTRATOR";
    // static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";
    static final String USER_ROLE = "ROLE_USER";

    private BasicAuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        GeoFenceAuthFilterConfig cfg = (GeoFenceAuthFilterConfig) config;
        // anything to set here? maybe the cache config

        aep = new BasicAuthenticationEntryPoint();
        aep.setRealmName(GeoServerSecurityManager.REALM);

        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        // BasicAuthenticationFilterConfig authConfig = (BasicAuthenticationFilterConfig) config;
        SecurityNamedServiceConfig authCfg = securityManager
                .loadAuthenticationProviderConfig("geofence");
        GeoFenceAuthenticationProvider geofenceAuthProvider = geofenceAuth
                .createAuthenticationProvider(authCfg);
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(geofenceAuthProvider, aep);

        // if (authConfig.isUseRememberMe()) {
        // filter.setRememberMeServices(securityManager.getRememberMeService());
        // GeoServerWebAuthenticationDetailsSource s = new GeoServerWebAuthenticationDetailsSource();
        // filter.setAuthenticationDetailsSource(s);
        // }
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(request, response, chain);

        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth == null) {
        // doAuth(request, response);
        // } else {
        // LOGGER.fine("Found existing Authentication in context: " + auth);
        // }
        //
        // chain.doFilter(request, response);
    }

    /**
     * Simple username+password container
     */
    class BasicUser {
        String name;

        String pw;

        public BasicUser(String name, String pw) {
            this.name = name;
            this.pw = pw;
        }
    }

    /**
     * @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml()
     */
    // @Override
    public boolean applicableForHtml() {
        return true;
    }

    /**
     * @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices()
     */
    // @Override
    public boolean applicableForServices() {
        return true;
    }

    public void setRuleReaderService(RuleReaderService ruleReaderService) {
        this.ruleReaderService = ruleReaderService;
    }

    public void setGeofenceAuth(GeoFenceSecurityProvider geofenceAuth) {
        this.geofenceAuth = geofenceAuth;
    }

}
