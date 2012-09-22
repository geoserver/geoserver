/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.session.SingleSignOutHandler;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.TicketValidationException;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

/**
 * Named Cas Authentication filter receiving proxy tickets
 * This filter needs a special entry point sending back 401 
 * {@link HttpServletResponse#SC_UNAUTHORIZED}  
 * 
 * @author mcr
 *
 */
public class GeoServerCasProxiedAuthenticationFilter extends GeoServerPreAuthenticatedUserNameFilter  {
    
    
    protected GeoServerCas20ProxyTicketValidator validator;
    protected ServiceAuthenticationDetailsSource casAuthenticationDetailsSource = new ServiceAuthenticationDetailsSource();
    protected boolean createSession;
    
    protected ProxyGrantingTicketStorage pgtStorageFilter;

    public GeoServerCasProxiedAuthenticationFilter(ProxyGrantingTicketStorage pgtStorageFilter) {
        this.pgtStorageFilter = pgtStorageFilter;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

                
        CasProxiedAuthenticationFilterConfig authConfig = 
                (CasProxiedAuthenticationFilterConfig) config;
        
        validator = new GeoServerCas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator.setAcceptAnyProxy(true);
        validator.setProxyGrantingTicketStorage(pgtStorageFilter);
        
        validator.setRenew(authConfig.isSendRenew());
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrlPrefix()))
                validator.setProxyCallbackUrl(GeoServerCasConstants.createProxyCallBackURl(authConfig.getProxyCallbackUrlPrefix()));
        createSession=authConfig.isCreateHTTPSessionForValidTicket();
                
        aep = new AuthenticationEntryPoint() {            
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

            }
        };
    }
    

        
    protected Assertion getCASAssertion(HttpServletRequest request) {
      String ticket = request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER);
      
      if (ticket==null) return null;
      if ((ticket.startsWith(GeoServerCasConstants.PROXY_TICKET_PREFIX) ||
              ticket.startsWith(GeoServerCasConstants.SERVICE_TICKET_PREFIX))==false)
          return null;
      
      try {
          String service = retrieveService(request); 
          return validator.validate(ticket,service );          
          
      } catch (TicketValidationException e) {
          LOGGER.warning(e.getMessage());
      }
      return null;
    }
    
    protected String retrieveService(HttpServletRequest request) {
        StringBuffer buff  = new StringBuffer(request.getRequestURL().toString());
        if (StringUtils.hasLength(request.getQueryString())) {
            String query = request.getQueryString();
            String[] params = query.split("&");
            boolean firsttime=true;
            for (String param : params)  
            {  
                String name = param.split("=")[0];  
                String value = param.split("=")[1];  
                if (GeoServerCasConstants.ARTIFACT_PARAMETER.equals(name.trim()))
                    continue;
                if (firsttime) {
                    buff.append("?");
                    firsttime=false;
                } else {
                    buff.append("&");
                }                                    
                buff.append(name).append("=").append(value);
            }                            
        }
        return buff.toString();
    }
    
    
    protected String getPreAuthenticatedPrincipal(HttpServletRequest request) {
        
        String principal = super.getPreAuthenticatedPrincipal(request);
        
        if (principal!=null && createSession) {
            HttpSession session = request.getSession();
            session.setAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY, 
                    request.getAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY));
            request.removeAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY);
            getHandler().recordSession(request);
        }
        
        if (principal==null) {
            request.removeAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY);
        }
        
        
        return principal;
        
    }
    
    /**
     */
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        
        
        Assertion assertion = getCASAssertion(request);
        if (assertion==null) return null;                
        request.setAttribute(GeoServerCasConstants.CAS_ASSERTION_KEY,assertion);        
        return assertion.getPrincipal().getName();
    }


    @Override
    public String getCacheKey(HttpServletRequest request) {
        
        if (createSession) // no caching if there is an HTTP session
            return null;
        return super.getCacheKey(request);
    }
    
    protected static SingleSignOutHandler getHandler() {
        return GeoServerExtensions.bean(SingleSignOutHandler.class);
    }

    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpReq= (HttpServletRequest) req;
        HttpServletResponse httpRes= (HttpServletResponse) res;
        
        if (httpReq.getSession(false)!=null) {
            SingleSignOutHandler handler = getHandler();
            // check for sign out request from cas server
            if (handler.isLogoutRequest(httpReq)) {
                handler.destroySession(httpReq);
                RememberMeServices rms = securityManager.getRememberMeService();
                Authentication auth = SecurityContextHolder.getContext().getAuthentication();
                if (auth!=null)
                    ((LogoutHandler)rms).logout(httpReq, httpRes, auth);
                return;
            }
        }
        
        super.doFilter(req, res, chain);
    }

}
