/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.login;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.security.GeoServerSecurityFilterChainProxy;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geotools.util.logging.Logging;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.AbstractOAuth2TokenAuthenticationToken;
import org.springframework.security.oauth2.server.resource.web.BearerTokenAuthenticationEntryPoint;
import org.springframework.security.oauth2.server.resource.web.authentication.BearerTokenAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler;
import org.springframework.security.web.authentication.AuthenticationFailureHandler;

/**
 * Validates the Bearer tokens of one OIDC filter, in a chain where other OIDC filters may validate Bearer tokens too.
 *
 * <p>Spring's {@link BearerTokenAuthenticationFilter} answers 401 when it cannot validate a token, and validates a
 * token again after another filter accepted it. Two of them in a chain, on two identity providers, reject the tokens of
 * each other. This filter gives each OIDC filter of the chain a turn instead:
 *
 * <ul>
 *   <li>A request already authenticated with its Bearer token is passed on untouched.
 *   <li>A token rejected by this filter is passed on if a later filter of the chain validates Bearer tokens.
 *   <li>The last of these filters has the final word and answers 401, as a single filter does.
 * </ul>
 */
public class ChainedBearerTokenFilter implements Filter {

    private static final Logger LOGGER = Logging.getLogger(ChainedBearerTokenFilter.class);

    private static final String LEFT_TO_LATER_FILTER = ChainedBearerTokenFilter.class.getName() + ".leftToLaterFilter";

    private final BearerTokenAuthenticationFilter delegate;
    private final Supplier<List<SecurityFilterChain>> securityChains;
    private final AuthenticationFailureHandler rejection =
            new AuthenticationEntryPointFailureHandler(new BearerTokenAuthenticationEntryPoint());

    public ChainedBearerTokenFilter(BearerTokenAuthenticationFilter delegate) {
        this(delegate, ChainedBearerTokenFilter::chainsOfGeoServer);
    }

    ChainedBearerTokenFilter(
            BearerTokenAuthenticationFilter delegate, Supplier<List<SecurityFilterChain>> securityChains) {
        this.delegate = delegate;
        this.securityChains = securityChains;
        delegate.setAuthenticationFailureHandler(this::rejectOrLeaveToLaterFilter);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (isAuthenticatedWithBearerToken()) {
            chain.doFilter(request, response);
            return;
        }
        delegate.doFilter(request, response, chain);
        // the delegate does not continue the chain after a rejected token
        if (wasLeftToLaterFilter(request)) {
            chain.doFilter(request, response);
        }
    }

    private boolean isAuthenticatedWithBearerToken() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication instanceof AbstractOAuth2TokenAuthenticationToken<?> && authentication.isAuthenticated();
    }

    private void rejectOrLeaveToLaterFilter(
            HttpServletRequest request, HttpServletResponse response, AuthenticationException failure)
            throws IOException, ServletException {
        if (laterFilterValidatesBearerTokens(request)) {
            LOGGER.log(Level.FINE, "Bearer token left to a later filter of the chain: {0}", failure.getMessage());
            request.setAttribute(LEFT_TO_LATER_FILTER, Boolean.TRUE);
            return;
        }
        rejection.onAuthenticationFailure(request, response, failure);
    }

    private boolean wasLeftToLaterFilter(ServletRequest request) {
        boolean left = request.getAttribute(LEFT_TO_LATER_FILTER) != null;
        request.removeAttribute(LEFT_TO_LATER_FILTER);
        return left;
    }

    private boolean laterFilterValidatesBearerTokens(HttpServletRequest request) {
        List<Filter> filters = filtersOfChainFor(request);
        int owner = indexOfOwner(filters);
        if (owner < 0) {
            return false;
        }
        List<Filter> laterFilters = filters.subList(owner + 1, filters.size());
        return laterFilters.stream().anyMatch(ChainedBearerTokenFilter::validatesBearerTokens);
    }

    private List<Filter> filtersOfChainFor(HttpServletRequest request) {
        for (SecurityFilterChain chain : securityChains.get()) {
            if (chain.matches(request)) {
                return chain.getFilters();
            }
        }
        return List.of();
    }

    /** The position, in a chain, of the GeoServer filter holding this filter among its nested filters. */
    private int indexOfOwner(List<Filter> filters) {
        for (int i = 0; i < filters.size(); i++) {
            if (nestedFiltersOf(filters.get(i)).contains(this)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean validatesBearerTokens(Filter filter) {
        return nestedFiltersOf(filter).stream().anyMatch(ChainedBearerTokenFilter.class::isInstance);
    }

    private static List<Filter> nestedFiltersOf(Filter filter) {
        if (filter instanceof GeoServerCompositeFilter composite && composite.getNestedFilters() != null) {
            return composite.getNestedFilters();
        }
        return List.of();
    }

    private static List<SecurityFilterChain> chainsOfGeoServer() {
        GeoServerSecurityFilterChainProxy proxy = GeoServerExtensions.bean(GeoServerSecurityFilterChainProxy.class);
        return proxy == null ? List.of() : proxy.getFilterChains();
    }
}
