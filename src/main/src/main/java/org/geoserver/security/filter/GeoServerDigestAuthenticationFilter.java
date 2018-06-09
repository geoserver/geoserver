/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.HttpDigestUserDetailsServiceWrapper;
import org.geoserver.security.config.DigestAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.DigestAuthUtils;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.DigestAuthenticationFilter;
import org.springframework.util.StringUtils;

/**
 * Named Digest Authentication Filter
 *
 * @author mcr
 */
public class GeoServerDigestAuthenticationFilter extends GeoServerCompositeFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {

    private DigestAuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        DigestAuthenticationFilterConfig authConfig = (DigestAuthenticationFilterConfig) config;

        aep = new DigestAuthenticationEntryPoint();
        aep.setKey(config.getName());
        aep.setNonceValiditySeconds(
                authConfig.getNonceValiditySeconds() <= 0
                        ? 300
                        : authConfig.getNonceValiditySeconds());
        aep.setRealmName(GeoServerSecurityManager.REALM);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        DigestAuthenticationFilter filter = new DigestAuthenticationFilter();

        filter.setCreateAuthenticatedToken(true);
        filter.setPasswordAlreadyEncoded(true);

        filter.setAuthenticationEntryPoint(aep);

        HttpDigestUserDetailsServiceWrapper wrapper =
                new HttpDigestUserDetailsServiceWrapper(
                        getSecurityManager()
                                .loadUserGroupService(authConfig.getUserGroupServiceName()),
                        Charset.defaultCharset());
        filter.setUserDetailsService(wrapper);

        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {
        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        Integer validity = aep.getNonceValiditySeconds();
        // upper limits in the cache, makes no sense to cache an expired authentication token
        req.setAttribute(GeoServerCompositeFilter.CACHE_KEY_IDLE_SECS, validity);
        req.setAttribute(GeoServerCompositeFilter.CACHE_KEY_LIVE_SECS, validity);

        super.doFilter(req, res, chain);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public String getCacheKey(HttpServletRequest request) {

        if (request.getSession(false) != null) // no caching if there is an HTTP session
        return null;

        String header = request.getHeader("Authorization");

        if ((header != null) && header.startsWith("Digest ")) {
            String section212response = header.substring(7);

            String[] headerEntries = DigestAuthUtils.splitIgnoringQuotes(section212response, ',');
            Map<String, String> headerMap =
                    DigestAuthUtils.splitEachArrayElementAndCreateMap(headerEntries, "=", "\"");

            String username = headerMap.get("username");
            String realm = headerMap.get("realm");
            String nonce = headerMap.get("nonce");
            String responseDigest = headerMap.get("response");

            if (StringUtils.hasLength(username) == false
                    || StringUtils.hasLength(realm) == false
                    || StringUtils.hasLength(nonce) == false
                    || StringUtils.hasLength(responseDigest) == false) return null;

            if (GeoServerUser.ROOT_USERNAME.equals(username)) return null;

            StringBuffer buff = new StringBuffer();
            buff.append(username).append(":");
            buff.append(realm).append(":");
            buff.append(nonce).append(":");
            buff.append(responseDigest);
            return buff.toString();
        } else {
            return null;
        }
    }
    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    @Override
    public boolean applicableForServices() {
        return true;
    }
}
