/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.RequestFilterChain;
import org.geoserver.security.config.LogoutFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.security.web.authentication.logout.SecurityContextLogoutHandler;
import org.springframework.security.web.authentication.logout.SimpleUrlLogoutSuccessHandler;
import org.springframework.util.StringUtils;

/**
 * Logout filter
 *
 * @author christian
 */
public class GeoServerLogoutFilter extends GeoServerSecurityFilter {

    public static final String URL_AFTER_LOGOUT = "/web/";
    public static final String LOGOUT_REDIRECT_ATTR = "_logout_redirect";

    private String redirectUrl;

    SecurityContextLogoutHandler logoutHandler;
    SimpleUrlLogoutSuccessHandler logoutSuccessHandler;
    String[] pathInfos;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        logoutHandler = new SecurityContextLogoutHandler();
        redirectUrl = ((LogoutFilterConfig) config).getRedirectURL();
        logoutSuccessHandler = new SimpleUrlLogoutSuccessHandler();
        if (StringUtils.hasLength(redirectUrl))
            logoutSuccessHandler.setDefaultTargetUrl(redirectUrl);
        String formLogoutChain =
                (((LogoutFilterConfig) config).getFormLogoutChain() != null
                        ? ((LogoutFilterConfig) config).getFormLogoutChain()
                        : GeoServerSecurityFilterChain.FORM_LOGOUT_CHAIN);
        pathInfos = formLogoutChain.split(",");
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;
        boolean doLogout = false;
        for (String pathInfo : pathInfos) {
            if (getRequestPath(request).startsWith(pathInfo)) {
                doLogout = true;
                break;
            }
        }
        if (doLogout) doLogout(request, response);
    }

    public void doLogout(
            HttpServletRequest request, HttpServletResponse response, String... skipHandlerName)
            throws IOException, ServletException {

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication != null) {
            List<LogoutHandler> logoutHandlers = calculateActiveLogoutHandlers(skipHandlerName);
            for (LogoutHandler h : logoutHandlers) {
                h.logout(request, response, authentication);
            }

            RememberMeServices rms = securityManager.getRememberMeService();
            ((LogoutHandler) rms).logout(request, response, authentication);

            logoutHandler.logout(request, response, authentication);
        }

        String redirectUrl = (String) request.getAttribute(LOGOUT_REDIRECT_ATTR);
        if (StringUtils.hasLength(redirectUrl)) {
            SimpleUrlLogoutSuccessHandler h = new SimpleUrlLogoutSuccessHandler();
            h.setDefaultTargetUrl(redirectUrl);
            h.onLogoutSuccess(request, response, authentication);
            return;
        }

        logoutSuccessHandler.onLogoutSuccess(request, response, authentication);
    }

    /**
     * Search for filters implementing {@link LogoutHandler}. If such a filter is on an active
     * filter chain and is not enlisted in the parameter skipHandlerName, add it to the result
     *
     * <p>The skipHandlerName parameter gives other LogoutHandler the chance to trigger a using
     * {@link #doLogout(HttpServletRequest, HttpServletResponse, String...)} without receiving an
     * unnecessary callback.
     */
    List<LogoutHandler> calculateActiveLogoutHandlers(String... skipHandlerName)
            throws IOException {
        List<LogoutHandler> result = new ArrayList<LogoutHandler>();
        SortedSet<String> logoutFilterNames = getSecurityManager().listFilters(LogoutHandler.class);
        logoutFilterNames.removeAll(Arrays.asList(skipHandlerName));
        Set<String> handlerNames = new HashSet<String>();

        GeoServerSecurityFilterChain chain =
                getSecurityManager().getSecurityConfig().getFilterChain();
        for (RequestFilterChain requestChain : chain.getRequestChains()) {
            for (String filterName : requestChain.getFilterNames()) {
                if (logoutFilterNames.contains(filterName)) handlerNames.add(filterName);
            }
        }

        for (String handlerName : handlerNames) {
            result.add((LogoutHandler) getSecurityManager().loadFilter(handlerName));
        }
        return result;
    }
}
