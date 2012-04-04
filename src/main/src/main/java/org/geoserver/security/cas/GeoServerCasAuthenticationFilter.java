/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import org.geoserver.security.GeoServerUserGroupService;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.UserDetailsWrapper;
import org.jasig.cas.client.validation.Cas20ProxyTicketValidator;
import org.springframework.dao.DataAccessException;
import org.springframework.security.cas.ServiceProperties;
import org.springframework.security.cas.authentication.CasAssertionAuthenticationToken;
import org.springframework.security.cas.authentication.CasAuthenticationProvider;
import org.springframework.security.cas.web.CasAuthenticationEntryPoint;
import org.springframework.security.cas.web.CasAuthenticationFilter;
import org.springframework.security.cas.web.authentication.ServiceAuthenticationDetailsSource;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsByNameServiceWrapper;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.util.StringUtils;

/**
 * Named Cas Authentication Filter
 * 
 * @author mcr
 *
 */
public class GeoServerCasAuthenticationFilter extends GeoServerCompositeFilter  {
    
    
    public class CasUserDetails extends UserDetailsWrapper {

        private static final long serialVersionUID = 1L;
        private Collection<GrantedAuthority> roles;

        public CasUserDetails(UserDetails details, Collection<GrantedAuthority> roles) {
            super(details);
            this.roles=roles;
        }

        @Override
        public Collection<GrantedAuthority> getAuthorities() {
            return roles;
        }
        
    }
    
    public class CasUserDetailsWrapper extends UserDetailsByNameServiceWrapper<CasAssertionAuthenticationToken> {

        public CasUserDetailsWrapper(UserDetailsService userDetailsService) {
            super(userDetailsService);            
        }

        @Override
        public UserDetails loadUserDetails(CasAssertionAuthenticationToken authentication)
                throws UsernameNotFoundException, DataAccessException {
            if (GeoServerUser.ROOT_USERNAME.equals(authentication.getName()))
                    return GeoServerUser.createRoot();            
            UserDetails result =  super.loadUserDetails(authentication);
            if ((result!=null) && result.getAuthorities().contains(GeoServerRole.AUTHENTICATED_ROLE)==false) {
                List<GrantedAuthority> roles = new ArrayList<GrantedAuthority>();
                roles.addAll(result.getAuthorities());
                roles.add(GeoServerRole.AUTHENTICATED_ROLE);
                return new CasUserDetails(result, roles); 
            }
            return result;
        }        
    };
    
    
    private CasAuthenticationEntryPoint aep;
    private CasAuthenticationProvider provider;
    
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
        aep.setLoginUrl(authConfig.getCasServerUrlPrefix()+"/login");
        aep.setServiceProperties(sp);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }
        

        provider = new CasAuthenticationProvider();
        provider.setKey(config.getName());
        GeoServerUserGroupService ugService = getSecurityManager().loadUserGroupService(authConfig.getUserGroupServiceName());
        provider.setAuthenticationUserDetailsService(new CasUserDetailsWrapper(ugService));
        provider.setServiceProperties(sp);
        Cas20ProxyTicketValidator ticketValidator = new Cas20ProxyTicketValidator(authConfig.getCasServerUrlPrefix());
        ticketValidator.setAcceptAnyProxy(true);
        ticketValidator.setProxyGrantingTicketStorage(ProxyGrantingTicketStorageProvider.get());
        ticketValidator.setProxyCallbackUrl(authConfig.getProxyCallbackUrl());            
        
        provider.setTicketValidator(ticketValidator);        
        getSecurityManager().getProviders().add(provider);
        
        try {
            provider.afterPropertiesSet();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        
        CasAuthenticationFilter filter = new CasAuthenticationFilter();
        if (StringUtils.hasLength(authConfig.getProxyCallbackUrl())) {
            filter.setProxyGrantingTicketStorage(ProxyGrantingTicketStorageProvider.get());
            filter.setProxyReceptorUrl(CasAuthenticationFilterConfig.CAS_PROXY_RECEPTOR_PATTERN);
            filter.setAuthenticationDetailsSource(new ServiceAuthenticationDetailsSource());            
        }

        filter.setAuthenticationManager(getSecurityManager());
        filter.setAllowSessionCreation(false);
        filter.setFilterProcessesUrl(CasAuthenticationFilterConfig.CAS_CHAIN_PATTERN);
        filter.setServiceProperties(sp);
        
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);        
    }
    
    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(req, res, chain);
    }            

    
    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void destroy() {
        getSecurityManager().getProviders().remove(provider);
    }


    
}
