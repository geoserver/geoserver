/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import javax.servlet.Filter;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.config.SecurityManagerConfig;
import org.geoserver.security.impl.AbstractGeoServerSecurityService;
import org.springframework.beans.factory.BeanNameAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;

/**
 * Extension of {@link Filter} for the geoserver security subsystem.
 *
 * <p>Instances of this class are provided by {@link GeoServerSecurityProvider}, or may also be
 * contribute via a spring context. Filters are configured via name through {@link
 * SecurityManagerConfig#getFilterChain()}.The referenced name will be matched to a named security
 * configuration through {@link GeoServerSecurityManager#loadFilter(String)} or matched to a bean
 * name in the application context.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public abstract class GeoServerSecurityFilter extends AbstractGeoServerSecurityService
        implements Filter, BeanNameAware {

    /**
     * Geoserver authentication filter should set an {@link AuthenticationEntryPoint} using this
     * servlet attribute name.
     *
     * <p>The {@link GeoServerExceptionTranslationFilter} may use the entry point in case of an
     * {@link AuthenticationException}
     */
    public static final String AUTHENTICATION_ENTRY_POINT_HEADER =
            "_AUTHENTICATION_ENTRY_POINT_HEADER";

    private String beanName;

    public String getBeanName() {
        return beanName;
    }

    public void setBeanName(String beanName) {
        this.beanName = beanName;
    }

    /** Not used, these filters are not plugged in via web.xml */
    @Override
    public final void init(FilterConfig filterConfig) throws ServletException {}

    /** Does nothing, subclasses may override. */
    @Override
    public void destroy() {}

    /**
     * Tries to authenticate from cache if a key can be derived and the {@link Authentication}
     * object is not in the cache, the key will be returned.
     *
     * <p>A not <code>null</code> return value indicates a missing cache entry
     */
    protected String authenticateFromCache(
            AuthenticationCachingFilter filter, HttpServletRequest request) {

        Authentication authFromCache = null;
        String cacheKey = null;
        if (SecurityContextHolder.getContext().getAuthentication() == null) {
            cacheKey = filter.getCacheKey(request);
            if (cacheKey != null) {
                authFromCache =
                        getSecurityManager().getAuthenticationCache().get(getName(), cacheKey);
                if (authFromCache != null)
                    SecurityContextHolder.getContext().setAuthentication(authFromCache);
                else return cacheKey;
            }
        }
        return null;
    }

    protected String getRequestPath(HttpServletRequest request) {
        String url = request.getServletPath();

        if (request.getPathInfo() != null) {
            url += request.getPathInfo();
        }

        url = url.toLowerCase();

        return url;
    }
}
