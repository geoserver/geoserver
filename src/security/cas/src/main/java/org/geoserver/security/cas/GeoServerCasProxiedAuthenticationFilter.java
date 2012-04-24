/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig.RoleSource;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerPreAuthenticatedUserNameFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.jasig.cas.client.proxy.ProxyGrantingTicketStorage;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    protected ServiceAuthenticationDetailsSource casAuthenticationDetailsSource = new ServiceAuthenticationDetailsSource();

    protected ProxyGrantingTicketStorage pgtStorageFilter;

    public GeoServerCasProxiedAuthenticationFilter(ProxyGrantingTicketStorage pgtStorageFilter) {
        this.pgtStorageFilter = pgtStorageFilter;
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

                
        CasProxiedAuthenticationFilterConfig authConfig = 
                (CasProxiedAuthenticationFilterConfig) config;
        
        validator = new Cas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator.setAcceptAnyProxy(true);
        validator.setProxyGrantingTicketStorage(pgtStorageFilter);
        
        validator.setRenew(authConfig.isSendRenew());
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrlPrefix()))
                validator.setProxyCallbackUrl(GeoServerCasConstants.createProxyCallBackURl(authConfig.getProxyCallbackUrlPrefix()));
                
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
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        Assertion assertion = getCASAssertion(request);
        if (assertion==null) {
            return;
        }
        
        UserDetails details=null;;
        String principal = assertion.getPrincipal().getName();
        // check for disabled user
        if (RoleSource.UserGroupService.equals(getRoleSource())) {
            try {
                details = getSecurityManager().loadUserGroupService(
                        getUserGroupServiceName()).getUserByUsername(principal);
            } catch (IOException e) {
               throw new RuntimeException(e);
            }
            if (details!=null && details.isEnabled()==false) return;
        }
        
        LOGGER.log(Level.FINE,"preAuthenticatedPrincipal = " + assertion.getPrincipal().getName());
        
        CasAuthenticationToken result = null;
        
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            if (details==null) details=GeoServerUser.createRoot();
            result = new CasAuthenticationToken(getName(),details,
                    request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER), Collections.singleton(GeoServerRole.ADMIN_ROLE),
                    details,assertion);
        } else {
            Collection<GeoServerRole> roles=null;
            if (details==null) details = new GeoServerUser(principal);
            try {
                roles = getRoles(request, principal);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE)==false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            result = new CasAuthenticationToken(getName(),details,
                    request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER), roles,
                    details,assertion);
             
        }
                                                
        result.setDetails(casAuthenticationDetailsSource.buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);                        
    }

    

    protected Assertion getCASAssertion(HttpServletRequest request) {
      String ticket = request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER);
      if (ticket==null) return null;
      try {
          return validator.validate(ticket, service);          
          
      } catch (TicketValidationException e) {
          LOGGER.warning(e.getMessage());
      }
      return null;
    }
    
    
    /**
     * not used
     * 
     */
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        return null;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {
        return request.getParameter("ticket");
    }

    @Override
    protected boolean cacheAuthentication(Authentication auth) {
        if (auth instanceof Assertion && 
            GeoServerUser.ROOT_USERNAME.equals(((Assertion)auth).getPrincipal().getName())) {
            return false;
        }
        return super.cacheAuthentication(auth);
    }
}
