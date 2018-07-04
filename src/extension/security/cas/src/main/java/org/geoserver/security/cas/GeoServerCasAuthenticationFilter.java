/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2016 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import java.net.URLEncoder;
import java.util.logging.Level;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.LogoutFilterChain;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerLogoutFilter;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.jasig.cas.client.configuration.ConfigurationKeys;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.session.SingleSignOutHandler;
import org.jasig.cas.client.util.CommonUtils;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

/**
 * CAS Authentication filter receiving/validating proxy tickets and service tickets.
 *
 * <p>If {@link #singleSignOut} is <code>true</code>, this filter handles logout requests sent from
 * the CAS server.
 *
 * <p>This filter implements the {@link LogoutHandler} interface for log out requests triggered by
 * GeoServer
 *
 * @author mcr
 */
public class GeoServerCasAuthenticationFilter extends GeoServerPreAuthenticatedUserNameFilter
        implements LogoutHandler {

    protected Cas20ProxyTicketValidator validator;
    protected ServiceAuthenticationDetailsSource casAuthenticationDetailsSource;
    protected String casLogoutURL;
    protected String urlInCasLogoutPage;
    protected boolean singleSignOut;

    protected ProxyGrantingTicketStorage pgtStorageFilter;

    public GeoServerCasAuthenticationFilter(ProxyGrantingTicketStorage pgtStorageFilter) {
        this.pgtStorageFilter = pgtStorageFilter;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        CasAuthenticationFilterConfig authConfig = (CasAuthenticationFilterConfig) config;

        ServiceProperties props = new ServiceProperties();
        props.setSendRenew(authConfig.isSendRenew());
        // TODO, investigate in
        // props.setAuthenticateAllArtifacts(true);
        casAuthenticationDetailsSource =
                new ServiceAuthenticationDetailsSource(
                        props, GeoServerCasConstants.ARTIFACT_PARAMETER);

        // validator = new GeoServerCas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator = new Cas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator.setAcceptAnyProxy(true);
        validator.setProxyGrantingTicketStorage(pgtStorageFilter);

        validator.setRenew(authConfig.isSendRenew());
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrlPrefix()))
            validator.setProxyCallbackUrl(
                    GeoServerCasConstants.createProxyCallBackURl(
                            authConfig.getProxyCallbackUrlPrefix()));

        casLogoutURL =
                GeoServerCasConstants.createCasURl(
                        authConfig.getCasServerUrlPrefix(), GeoServerCasConstants.LOGOUT_URI);
        if (StringUtils.hasLength(authConfig.getUrlInCasLogoutPage()))
            casLogoutURL +=
                    "?"
                            + GeoServerCasConstants.LOGOUT_URL_PARAM
                            + "="
                            + URLEncoder.encode(authConfig.getUrlInCasLogoutPage(), "utf-8");

        singleSignOut = authConfig.isSingleSignOut();
        aep = new GeoServerCasAuthenticationEntryPoint(authConfig);
    }

    protected Assertion getCASAssertion(HttpServletRequest request) {
        String ticket = request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER);

        if (ticket == null) return null;
        if ((ticket.startsWith(GeoServerCasConstants.PROXY_TICKET_PREFIX)
                        || ticket.startsWith(GeoServerCasConstants.SERVICE_TICKET_PREFIX))
                == false) return null;

        try {
            String service = retrieveService(request);
            return validator.validate(ticket, service);

        } catch (TicketValidationException e) {
            LOGGER.warning(e.getMessage());
        }
        return null;
    }

    protected static String retrieveService(HttpServletRequest request) {

        String serviceBaseUrl = null;
        String proxyBaseUrl = GeoServerExtensions.getProperty("PROXY_BASE_URL");
        if (StringUtils.hasLength(proxyBaseUrl)) {
            serviceBaseUrl = proxyBaseUrl;
        } else {
            serviceBaseUrl = request.getRequestURL().toString();
        }
        StringBuffer buff = new StringBuffer(serviceBaseUrl);

        if (StringUtils.hasLength(request.getQueryString())) {
            String query = request.getQueryString();
            String[] params = query.split("&");
            boolean firsttime = true;
            for (String param : params) {

                String[] keyValue = param.split("=");
                if (keyValue.length == 0) continue;
                String name = keyValue[0];

                if (GeoServerCasConstants.ARTIFACT_PARAMETER.equals(name.trim())) continue;
                if (GeoServerCasAuthenticationEntryPoint.CAS_REDIRECT.equals(name.trim())) continue;
                if (firsttime) {
                    buff.append("?");
                    firsttime = false;
                } else {
                    buff.append("&");
                }
                buff.append(param);
            }
        }
        String serviceUrl = buff.toString();
        if (LOGGER.isLoggable(Level.FINE)) LOGGER.fine("CAS Service URL: " + serviceUrl);
        return serviceUrl;
    }

    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {

        String principal = super.getPreAuthenticatedPrincipal(request);

        HttpSession session = request.getSession(false);

        if (principal != null && session != null) {
            session.setAttribute(
                    GeoServerCasConstants.CAS_ASSERTION_KEY,
                    request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
            request.removeAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY);
            getHandler().process(request, null);
        }

        if (principal == null) {
            request.removeAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY);
        }
        return principal;
    }

    /** */
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {

        Assertion assertion = getCASAssertion(request);
        if (assertion == null) return null;
        request.setAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY, assertion);
        return assertion.getPrincipal().getName();
    }

    protected static boolean handlerInitialized = false;

    protected static SingleSignOutHandler getHandler() {
        SingleSignOutHandler handler = GeoServerExtensions.bean(SingleSignOutHandler.class);
        if (!handlerInitialized) {
            handler.init();
            handlerInitialized = true;
        }
        return handler;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        HttpServletRequest httpReq = (HttpServletRequest) req;
        HttpServletResponse httpRes = (HttpServletResponse) res;

        SingleSignOutHandler handler = getHandler();
        // check for sign out request from cas server
        if (isLogoutRequest(httpReq)) {
            if (singleSignOut) { // do we participate
                LOGGER.info("Single Sign Out received from CAS server --> starting log out");
                LogoutFilterChain logOutChain =
                        (LogoutFilterChain)
                                getSecurityManager()
                                        .getSecurityConfig()
                                        .getFilterChain()
                                        .getRequestChainByName("webLogout");
                logOutChain.doLogout(getSecurityManager(), httpReq, httpRes, getName());
                handler.process(httpReq, httpRes);
            } else LOGGER.info("Single Sign Out received from CAS server --> ignoring");
            return;
        }

        super.doFilter(req, res, chain);

        if (SecurityContextHolder.getContext().getAuthentication() != null) {
            HttpSession session = httpReq.getSession(false);

            if (session != null
                    && session.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY) != null
                    && singleSignOut) {
                handler.process(httpReq, httpRes);

                if (LOGGER.isLoggable(Level.INFO))
                    LOGGER.info(
                            "Record HTTP Session " + session.getId() + " for CAS single sign out");
            }
        }
    }

    /**
     * Determines whether the given request is a CAS logout request.
     *
     * @param request HTTP request.
     * @return True if request is logout request, false otherwise.
     */
    public boolean isLogoutRequest(final HttpServletRequest request) {
        return "POST".equals(request.getMethod())
                && CommonUtils.isNotBlank(
                        CommonUtils.safeGetParameter(
                                request,
                                ConfigurationKeys.LOGOUT_PARAMETER_NAME.getDefaultValue()));
    }

    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication) {
        request.setAttribute(GeoServerLogoutFilter.LOGOUT_REDIRECT_ATTR, casLogoutURL);
    }
}
