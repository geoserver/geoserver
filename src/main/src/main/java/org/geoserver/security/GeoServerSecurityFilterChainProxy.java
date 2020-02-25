/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.filter.GeoServerSecurityContextPersistenceFilter;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.web.DefaultSecurityFilterChain;
import org.springframework.security.web.FilterChainProxy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.firewall.DefaultHttpFirewall;
import org.springframework.security.web.util.matcher.RequestMatcher;

public class GeoServerSecurityFilterChainProxy
        implements SecurityManagerListener, ApplicationContextAware, InitializingBean, Filter {

    static Logger LOGGER = Logging.getLogger("org.geoserver.security");

    static ThreadLocal<HttpServletRequest> REQUEST = new ThreadLocal<HttpServletRequest>();

    /**
     * Request header attribute indicating if the request was running through a Geoserver security
     * filter chain. The default is <code>false</code>.
     *
     * <p>The mandatory {@link GeoServerSecurityContextPersistenceFilter} object sets this attribute
     * to <code>true</code>
     */
    public static final String SECURITY_ENABLED_ATTRIBUTE = "org.geoserver.security.enabled";

    private boolean chainsInitialized;

    // security manager
    GeoServerSecurityManager securityManager;

    FilterChainProxy proxy;

    // app context
    ApplicationContext appContext;

    public GeoServerSecurityFilterChainProxy(GeoServerSecurityManager securityManager) {
        this.securityManager = securityManager;
        this.securityManager.addListener(this);
        chainsInitialized = false;
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

    /**
     * Returns <code>true</code> if the current {@link HttpServletRequest} has traveled through a
     * security filter chain.
     */
    public static boolean isSecurityEnabledForCurrentRequest() {

        if (REQUEST.get() == null) {
            return true;
        }

        if (Boolean.TRUE.equals(REQUEST.get().getAttribute(SECURITY_ENABLED_ATTRIBUTE)))
            return true;

        return false;
    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.appContext = applicationContext;
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (proxy != null) {
            proxy.init(filterConfig);
        } else {
            // FilterChainProxy doesn't to anything in it's init() method so i believe it's ok
            // if it doesn't get called
            LOGGER.warning("init() called but proxy not yet configured");
        }
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        // assume security is disabled
        request.setAttribute(SECURITY_ENABLED_ATTRIBUTE, Boolean.FALSE);
        // set the request thread local
        REQUEST.set((HttpServletRequest) request);
        try {
            proxy.doFilter(request, response, chain);
        } finally {
            REQUEST.remove();
        }
    }

    @Override
    public void handlePostChanged(GeoServerSecurityManager securityManager) {
        createFilterChain();
    }

    public void afterPropertiesSet() {
        createFilterChain();
    };

    void createFilterChain() {

        if (!securityManager.isInitialized()) {
            // nothing to do
            return;
        }

        SecurityManagerConfig config = securityManager.getSecurityConfig();
        GeoServerSecurityFilterChain filterChain =
                new GeoServerSecurityFilterChain(config.getFilterChain());

        // similar to the list of authentication providers
        // adding required providers like GeoServerRootAuthenticationProvider
        filterChain.postConfigure(securityManager);

        //        Map<RequestMatcher,List<Filter>> filterChainMap =
        //                new LinkedHashMap<RequestMatcher,List<Filter>>();

        List<SecurityFilterChain> filterChains = new ArrayList<>();
        for (RequestFilterChain chain : filterChain.getRequestChains()) {
            RequestMatcher matcher = matcherForChain(chain);
            List<Filter> filters = new ArrayList<Filter>();
            for (String filterName : chain.getCompiledFilterNames()) {
                try {
                    Filter filter = lookupFilter(filterName);
                    if (filter == null) {
                        throw new NullPointerException(
                                "No filter named " + filterName + " could " + "be found");
                    }
                    filters.add(filter);
                } catch (Exception ex) {
                    LOGGER.log(Level.SEVERE, "Error loading filter: " + filterName, ex);
                }
            }
            filterChains.add(new DefaultSecurityFilterChain(matcher, filters));
        }

        synchronized (this) {
            // first, call destroy of all current filters
            if (chainsInitialized) {
                for (SecurityFilterChain chain : proxy.getFilterChains()) {
                    for (Filter filter : chain.getFilters()) {
                        filter.destroy();
                    }
                }
            }
            // empty cache since filter config  will change
            securityManager.getAuthenticationCache().removeAll();

            proxy = new FilterChainProxy(filterChains);
            proxy.setFirewall(new DefaultHttpFirewall());
            proxy.afterPropertiesSet();
            chainsInitialized = true;
        }
    }

    /**
     * Creates a {@link GeoServerRequestMatcher} object for the specified {@link RequestFilterChain}
     */
    public GeoServerRequestMatcher matcherForChain(RequestFilterChain chain) {

        Set<HTTPMethod> methods = chain.getHttpMethods();
        if (chain.isMatchHTTPMethod() == false) methods = null;

        List<String> tmp = chain.getPatterns();

        if (tmp == null) return new GeoServerRequestMatcher(methods, (RequestMatcher[]) null);

        // resolve multiple patterns separated by a comma
        List<String> patterns = new ArrayList<String>();
        for (String pattern : tmp) {
            String[] array = pattern.split(",");
            for (String singlePattern : array) patterns.add(singlePattern);
        }

        RequestMatcher[] matchers = new RequestMatcher[patterns.size()];
        for (int i = 0; i < matchers.length; i++) {
            matchers[i] = new IncludeQueryStringAntPathRequestMatcher(patterns.get(i));
        }
        return new GeoServerRequestMatcher(methods, matchers);
    }

    /** looks up a named filter */
    public Filter lookupFilter(String filterName) throws IOException {
        Filter filter = securityManager.loadFilter(filterName);
        if (filter == null) {
            try {
                Object obj = GeoServerExtensions.bean(filterName, appContext);
                if (obj != null && obj instanceof Filter) {
                    filter = (Filter) obj;
                }
            } catch (NoSuchBeanDefinitionException ex) {
                // do nothing
            }
        }
        return filter;
    }

    @Override
    public void destroy() {
        proxy.destroy();

        // do some cleanup
        securityManager.removeListener(this);
    }

    public List<SecurityFilterChain> getFilterChains() {
        return proxy.getFilterChains();
    }
}
