/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerUser;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
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
    
    protected Cas20ProxyTicketValidator validator;
    protected String service;
    
    
    
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

                
        CasProxiedAuthenticationFilterConfig authConfig = 
                (CasProxiedAuthenticationFilterConfig) config;
        
        validator = new Cas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator.setAcceptAnyProxy(true);
        validator.setProxyGrantingTicketStorage(ProxyGrantingTicketStorageProvider.get());
        
        validator.setRenew(authConfig.isSendRenew());
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrl()))
                validator.setProxyCallbackUrl(authConfig.getProxyCallbackUrl());
        
        service=authConfig.getService();
            
        aep = new AuthenticationEntryPoint() {            
            @Override
            public void commence(HttpServletRequest request, HttpServletResponse response,
                    AuthenticationException authException) throws IOException, ServletException {
                HttpServletResponse httpResponse = (HttpServletResponse) response;
                httpResponse.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Unauthorized");

            }
        };
    }
        
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        String ticket = request.getParameter("ticket");
        if (ticket==null) return null;
        try {
            Assertion assertion = validator.validate(ticket, service);
            return assertion.getPrincipal().getName();
            
        } catch (TicketValidationException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {
        return request.getParameter("ticket");
    }

    
}
