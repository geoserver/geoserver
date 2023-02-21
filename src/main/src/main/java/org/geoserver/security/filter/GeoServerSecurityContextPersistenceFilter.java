/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.config.SecurityContextPersistenceFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * Security context persitence filter
 *
 * @author mcr
 */
public class GeoServerSecurityContextPersistenceFilter extends GeoServerCompositeFilter {

    public static final String ALLOWSESSIONCREATION_ATTR = "_allowSessionCreation";
    Boolean isAllowSessionCreation;

    static final String FILTER_APPLIED = "__spring_security_scpf_applied";

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        SecurityContextPersistenceFilterConfig pConfig =
                (SecurityContextPersistenceFilterConfig) config;

        HttpSessionSecurityContextRepository repo = new HttpSessionSecurityContextRepository();
        // Previous implementation
        // (https://github.com/geoserver/geoserver/blob/2.22.x/src/main/src/main/java/org/geoserver/security/filter/GeoServerSecurityContextPersistenceFilter.java#L39)
        // migrated to use OncePerRequestFilter due to deprecation of
        // SecurityContextPersistenceFilter
        OncePerRequestFilter eagerFilter =
                new OncePerRequestFilter() {
                    @Override
                    public void doFilterInternal(
                            HttpServletRequest request,
                            HttpServletResponse response,
                            FilterChain chain)
                            throws IOException, ServletException {
                        // set the hint for authentcation servlets
                        request.setAttribute(ALLOWSESSIONCREATION_ATTR, isAllowSessionCreation);
                        if (isAllowSessionCreation) {
                            request.getSession();
                        }
                        // set the hint for other components
                        request.setAttribute(
                                GeoServerSecurityFilterChainProxy.SECURITY_ENABLED_ATTRIBUTE,
                                Boolean.TRUE);
                        // ensure that filter is only applied once per request
                        if (request.getAttribute(FILTER_APPLIED) != null) {
                            chain.doFilter(request, response);
                            return;
                        }
                        request.setAttribute(FILTER_APPLIED, Boolean.TRUE);
                        try {
                            SecurityContext securityContext = repo.loadContext(request).get();
                            SecurityContextHolder.setContext(securityContext);
                            chain.doFilter(request, response);
                        } finally {
                            SecurityContext contextAfterChainExecution =
                                    SecurityContextHolder.getContext();
                            // Crucial removal of SecurityContextHolder contents before anything
                            // else.
                            SecurityContextHolder.clearContext();
                            repo.saveContext(contextAfterChainExecution, request, response);
                            request.removeAttribute(FILTER_APPLIED);
                        }
                    }
                };
        isAllowSessionCreation = pConfig.isAllowSessionCreation();
        repo.setAllowSessionCreation(pConfig.isAllowSessionCreation());
        try {
            eagerFilter.afterPropertiesSet();
        } catch (ServletException e) {
            throw new RuntimeException(e);
        }
        getNestedFilters().add(eagerFilter);
    }
}
