/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.config.UsernamePasswordAuthenticationFilterConfig;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationFailureHandler;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionFixationProtectionStrategy;

/**
 * User name / password authentication filter
 *
 * @author christian
 */
public class GeoServerUserNamePasswordAuthenticationFilter extends GeoServerCompositeFilter
        implements GeoServerAuthenticationFilter {

    // public static final String URL_FOR_LOGIN = "/j_spring_security_check";
    public static final String URL_LOGIN_SUCCCESS = "/web";
    public static final String URL_LOGIN_FAILURE =
            "/web/wicket/bookmarkable/org.geoserver.web.GeoServerLoginPage?error=true";
    public static final String URL_LOGIN_FORM =
            "/web/wicket/bookmarkable/org.geoserver.web.GeoServerLoginPage?error=false";
    // public static final String URL_LOGIN_FORM="/admin/login.do";

    private LoginUrlAuthenticationEntryPoint aep;
    String[] pathInfos;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        pathInfos = GeoServerSecurityFilterChain.FORM_LOGIN_CHAIN.split(",");

        UsernamePasswordAuthenticationFilterConfig upConfig =
                (UsernamePasswordAuthenticationFilterConfig) config;

        aep = new LoginUrlAuthenticationEntryPoint(URL_LOGIN_FORM);
        aep.setForceHttps(false);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e2) {
            throw new IOException(e2);
        }

        RememberMeServices rms = securityManager.getRememberMeService();

        // add login filter
        UsernamePasswordAuthenticationFilter filter =
                new UsernamePasswordAuthenticationFilter() {
                    @Override
                    protected boolean requiresAuthentication(
                            HttpServletRequest request, HttpServletResponse response) {

                        for (String pathInfo : pathInfos) {
                            if (getRequestPath(request).startsWith(pathInfo)) return true;
                        }
                        return false;
                    }
                };

        filter.setPasswordParameter(upConfig.getPasswordParameterName());
        filter.setUsernameParameter(upConfig.getUsernameParameterName());
        filter.setAuthenticationManager(getSecurityManager().authenticationManager());

        filter.setRememberMeServices(rms);
        GeoServerWebAuthenticationDetailsSource s = new GeoServerWebAuthenticationDetailsSource();
        filter.setAuthenticationDetailsSource(s);

        try {
            // Prevent session fixation when using Servlet 3.1+
            filter.setSessionAuthenticationStrategy(new ChangeSessionIdAuthenticationStrategy());
        } catch (IllegalStateException e) {
            // Prevent session fixation when using < Servlet 3.1
            filter.setSessionAuthenticationStrategy(new SessionFixationProtectionStrategy());
        }

        filter.setAllowSessionCreation(false);
        // filter.setFilterProcessesUrl(URL_FOR_LOGIN);

        SimpleUrlAuthenticationSuccessHandler successHandler =
                new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl(URL_LOGIN_SUCCCESS);
        filter.setAuthenticationSuccessHandler(successHandler);

        SimpleUrlAuthenticationFailureHandler failureHandler =
                new SimpleUrlAuthenticationFailureHandler();
        // TODO, check this when using encrypting of URL parameters
        failureHandler.setDefaultFailureUrl(URL_LOGIN_FAILURE);
        filter.setAuthenticationFailureHandler(failureHandler);

        // filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(req, res, chain);
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return false;
    }
}
