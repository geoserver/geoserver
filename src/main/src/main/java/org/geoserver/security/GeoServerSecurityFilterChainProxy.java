/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.util.AntPathRequestMatcher;
import org.springframework.security.web.util.RequestMatcher;

public class GeoServerSecurityFilterChainProxy extends FilterChainProxy 
    implements SecurityManagerListener, ApplicationContextAware {
    
    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    static ThreadLocal<HttpServletRequest> REQUEST = new ThreadLocal<HttpServletRequest>();

    private boolean chainsInitialized;

    //security manager
    GeoServerSecurityManager securityManager;

    //app context
    ApplicationContext appContext;

    public GeoServerSecurityFilterChainProxy(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
        this.securityManager.addListener(this);
        chainsInitialized=false;
       
    }

/*    
    Map<String,List<String>> createDefaultFilterChain() {
        Map<String,List<String>> filterChain = new LinkedHashMap<String, List<String>>();
        
        filterChain.put("/web/**", Arrays.asList(SECURITY_CONTEXT_ASC_FILTER, LOGOUT_FILTER, 
            FORM_LOGIN_FILTER, SERVLET_API_SUPPORT_FILTER, REMEMBER_ME_FILTER, ANONYMOUS_FILTER, 
            EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR));

        filterChain.put("/j_spring_security_check/**", Arrays.asList(SECURITY_CONTEXT_ASC_FILTER, 
            LOGOUT_FILTER, FORM_LOGIN_FILTER, SERVLET_API_SUPPORT_FILTER, REMEMBER_ME_FILTER, 
            ANONYMOUS_FILTER, EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR));
        
        filterChain.put("/j_spring_security_logout/**", Arrays.asList(SECURITY_CONTEXT_ASC_FILTER, 
            LOGOUT_FILTER, FORM_LOGIN_FILTER, SERVLET_API_SUPPORT_FILTER, REMEMBER_ME_FILTER, 
            ANONYMOUS_FILTER, EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR));
        
        filterChain.put("/rest/**", Arrays.asList(SECURITY_CONTEXT_NO_ASC_FILTER, BASIC_AUTH_FILTER,
            ANONYMOUS_FILTER, EXCEPTION_TRANSLATION_OWS_FILTER, FILTER_SECURITY_REST_INTERCEPTOR));

        filterChain.put("/gwc/rest/web/**", Arrays.asList(ANONYMOUS_FILTER, 
            EXCEPTION_TRANSLATION_FILTER, FILTER_SECURITY_INTERCEPTOR));

        filterChain.put("/gwc/rest/**", Arrays.asList(SECURITY_CONTEXT_NO_ASC_FILTER, 
            BASIC_AUTH_NO_REMEMBER_ME_FILTER, EXCEPTION_TRANSLATION_OWS_FILTER, 
            FILTER_SECURITY_REST_INTERCEPTOR));

        filterChain.put("/**", Arrays.asList(SECURITY_CONTEXT_NO_ASC_FILTER, ROLE_FILTER,BASIC_AUTH_FILTER, 
            ANONYMOUS_FILTER, EXCEPTION_TRANSLATION_OWS_FILTER, FILTER_SECURITY_INTERCEPTOR));

        return filterChain;
    }
*/    

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        //set the request thread local
        REQUEST.set((HttpServletRequest) request);
        try {
            super.doFilter(request, response, chain);
        }
        finally {
            REQUEST.remove();
        }
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        createFilterChain();
    }

    public void afterPropertiesSet() {
        createFilterChain();
        super.afterPropertiesSet();
    };

    void createFilterChain() {

        if (!securityManager.isInitialized()) {
            //nothing to do
            return;
        }

        SecurityManagerConfig config = securityManager.getSecurityConfig(); 
        GeoServerSecurityFilterChain filterChain = 
                new GeoServerSecurityFilterChain(config.getFilterChain());

        // similar to the list of authentication providers
        // adding required providers like GeoServerRootAuthenticationProvider
        filterChain.postConfigure(securityManager);

        //build up the actual filter chain
        Map<String,List<String>> rawFilterChainMap = filterChain.compileFilterMap();

        Map<RequestMatcher,List<Filter>> filterChainMap = 
                new LinkedHashMap<RequestMatcher,List<Filter>>();

        for (String pattern : rawFilterChainMap.keySet()) {
            List<Filter> filters = new ArrayList<Filter>();
            for (String filterName : rawFilterChainMap.get(pattern)) {
                try {
                    Filter filter = lookupFilter(filterName);
                    if (filter == null) {
                        throw new NullPointerException("No filter named " + filterName +" could " +
                            "be found");
                    }
                    filters.add(filter);
                }
                catch(Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error loading filter: " + filterName, ex);
                }
            }
            //JD: we probably want to actually have seperate filter instances for each pattern 
            // component
            for (String p : pattern.split(",")) {
                filterChainMap.put(new AntPathRequestMatcher(p), filters);
            }
        }

        synchronized (this) {
            // first, call destroy of all current filters        
            if (chainsInitialized) {
                for (SecurityFilterChain chain : getFilterChains()) {
                    for (Filter filter: chain.getFilters()) {
                        filter.destroy();
                    }
                }
            }
            // empty cache since filter config  will change
            securityManager.getAuthenticationCache().removeAll();

            // TODO Justin, this method is deprecated without replacement, I fear 
            // this is a show stopper for the next spring security version, any idea
            setFilterChainMap(filterChainMap);
            chainsInitialized=true;
        }
    }

    /**
     * looks up a named filter  
     */
    Filter lookupFilter(String filterName) throws IOException {
        Filter filter = securityManager.loadFilter(filterName);
        if (filter == null) {
            Object obj = GeoServerExtensions.bean(filterName, appContext);
            if (obj != null && obj instanceof Filter) {
                filter = (Filter) obj;
            }
        }
        return filter;
    }

    @Override
    public void destroy() {
        super.destroy();

        //do some cleanup
        securityManager.removeListener(this);
    }
    
    /**
     * Add constant filter chains
     * 
     * @param filterChainMap
     */
    protected final void addConstantFilterChains(GeoServerSecurityFilterChain chain) {
    }
}
