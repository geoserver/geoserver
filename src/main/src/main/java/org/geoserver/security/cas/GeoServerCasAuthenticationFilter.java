/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.logging.Level;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.filter.GeoServerUserNamePasswordAuthenticationFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.jasig.cas.client.session.SessionMappingStorage;
import org.jasig.cas.client.session.SingleSignOutHandler;
import org.jasig.cas.client.validation.Assertion;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.jasig.cas.client.validation.TicketValidationException;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAuthenticationToken;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.RememberMeServices;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.util.StringUtils;

/**
 * Named Cas authentication filter for form based login 
 * with 3 functionalities 
 * 
 * 1) called wit a logout parameter 
 * 
 * http://geoserver/j_spring_security_check?logout=true
 * 
 * logs out the user from the CAS server and shows the CAS lougout page
 * no valid {@link HttpSession} is necessary
 * 
 * 2) called using HTTP POST with a logoutRequest parameter
 * 
 * The CAS server sends a request for a single sign out, the {@link HttpSession}
 * is invalidated
 * 
 * 3) called with a ticket parameter
 * 
 * http://geoserver/j_spring_security_check?ticket=ST-?????-cas
 * 
 * Logs in the user and creates a {@link HttpSession}  
 * 
 * @author mcr
 *
 */
public class GeoServerCasAuthenticationFilter extends GeoServerSecurityFilter  {
    
    protected static HttpSessionListener SingleSignOutHttpSessionListener = new  HttpSessionListener() {

        private SessionMappingStorage sessionMappingStorage;
        
        public void sessionCreated(final HttpSessionEvent event) {
            // nothing to do at the moment
        }
    
        public void sessionDestroyed(final HttpSessionEvent event) {
            if (sessionMappingStorage == null) {
                sessionMappingStorage = Handler.getSessionMappingStorage();
            }
            final HttpSession session = event.getSession();
            sessionMappingStorage.removeBySessionById(session.getId());
        }
    
    };


    public static final String LOGOUT_PARAM="logout";
    
    protected Cas20ProxyTicketValidator validator;
    protected String service, userGroupServiceName;
    protected ServiceAuthenticationDetailsSource casAuthenticationDetailsSource = new ServiceAuthenticationDetailsSource();
    protected SimpleUrlAuthenticationSuccessHandler successHandler;
    protected String urlInCasLogoutPage;
    protected String casLogoutURL;
    
    protected static final SingleSignOutHandler Handler = new SingleSignOutHandler();
        
    private CasAuthenticationEntryPoint aep;
    
    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

                
        CasAuthenticationFilterConfig authConfig = 
                (CasAuthenticationFilterConfig) config;
        
        ServiceProperties sp = new ServiceProperties();
        sp.setSendRenew(authConfig.isSendRenew());
        sp.setService(authConfig.getService());
        sp.setAuthenticateAllArtifacts(true);
        
        try {
            sp.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
        
        aep= new CasAuthenticationEntryPoint();
        aep.setLoginUrl(authConfig.getCasServerUrlPrefix()+GeoServerCasConstants.LOGIN_URI);
        aep.setServiceProperties(sp);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
                        
        validator = new Cas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        validator.setAcceptAnyProxy(true);
        validator.setProxyGrantingTicketStorage(ProxyGrantingTicketCallbackFilter.getPGTStorage());
        
        validator.setRenew(authConfig.isSendRenew());
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrlPrefix()))
                validator.setProxyCallbackUrl(GeoServerCasConstants.createProxyCallBackURl(authConfig.getProxyCallbackUrlPrefix()));
                
        service=authConfig.getService();
        userGroupServiceName=authConfig.getUserGroupServiceName();
        urlInCasLogoutPage=authConfig.getUrlInCasLogoutPage();
        casLogoutURL=GeoServerCasConstants.createCasURl(authConfig.getCasServerUrlPrefix(), GeoServerCasConstants.LOGOUT_URI);
        if (StringUtils.hasLength(urlInCasLogoutPage)) {
            casLogoutURL+="?"+GeoServerCasConstants.LOGOUT_URL_PARAM+"="+urlInCasLogoutPage;
        }

        successHandler = new SimpleUrlAuthenticationSuccessHandler();
        successHandler.setDefaultTargetUrl(GeoServerUserNamePasswordAuthenticationFilter.URL_LOGIN_SUCCCESS);

        
        
        // TODO register sessionListener
        
        
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

    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpReq= (HttpServletRequest) req;
        HttpServletResponse httpRes= (HttpServletResponse) res;
        
        // check for logout triggered by geoserver
        String booleanString = httpReq.getParameter(LOGOUT_PARAM);
        if (StringUtils.hasLength(booleanString)) {
            Boolean blogout = Boolean.parseBoolean(booleanString);
            if (blogout) {
                httpRes.sendRedirect(casLogoutURL);
                return;
            }
        }
        
        // check for sign out request from cas server
        if (Handler.isLogoutRequest(httpReq)) {
            Handler.destroySession(httpReq);
            RememberMeServices rms = securityManager.getRememberMeService();
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth!=null)
                ((LogoutHandler)rms).logout(httpReq, httpRes, auth);
            return;
        }
        
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            doAuthenticate((HttpServletRequest) req,(HttpServletResponse)res);
            if (SecurityContextHolder.getContext().getAuthentication()==null) {
                aep.commence(httpReq, httpRes, null);
            }                
            else {
                Handler.recordSession(httpReq);
                successHandler.onAuthenticationSuccess(httpReq, httpRes,
                        SecurityContextHolder.getContext().getAuthentication());
            }
        }
                
//        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        //chain.doFilter(req, res);          
    }            

    
    protected void doAuthenticate(HttpServletRequest request, HttpServletResponse response) {

        Assertion assertion = getCASAssertion(request);
        if (assertion==null) {
            return;
        }
        
        UserDetails details=null;;
        String principal = assertion.getPrincipal().getName();
        // check for disabled user
        try {
            details = getSecurityManager().loadUserGroupService(
                    userGroupServiceName).loadUserByUsername(principal);
        } catch (UsernameNotFoundException ex) { // authenticated, but not in user group service
            details = new GeoServerUser(principal);            
        } catch (IOException e) {
           throw new RuntimeException(e);
        }
        if (details.isEnabled()==false) return;
        
        LOGGER.log(Level.FINE,"preAuthenticatedPrincipal = " + assertion.getPrincipal().getName());
        
        CasAuthenticationToken result = null;
        
        if (GeoServerUser.ROOT_USERNAME.equals(principal)) {
            details=GeoServerUser.createRoot();
            result = new CasAuthenticationToken(getName(),details,
                    request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER), Collections.singleton(GeoServerRole.ADMIN_ROLE),
                    details,assertion);
        } else {            
            Collection<GeoServerRole> roles = new ArrayList<GeoServerRole>();
            for (GrantedAuthority role : details.getAuthorities())
                roles.add((GeoServerRole)role);

            if (roles.contains(GeoServerRole.AUTHENTICATED_ROLE)==false)
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
            
            result = new CasAuthenticationToken(getName(),details,
                    request.getParameter(GeoServerCasConstants.ARTIFACT_PARAMETER), roles,
                    details,assertion);
             
        }                                                
        result.setDetails(casAuthenticationDetailsSource.buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(result);                        
    }

    
    
    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void destroy() {
        // TODO
        // deregister session listener
    }


    
}
