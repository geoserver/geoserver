/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.BasicAuthenticationFilterConfig;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.impl.GeoServerUser;
import org.springframework.security.crypto.codec.Hex;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/**
 * Named Basic Authentication Filter
 *
 * @author mcr
 */
public class GeoServerBasicAuthenticationFilter extends GeoServerCompositeFilter
        implements AuthenticationCachingFilter, GeoServerAuthenticationFilter {
    private BasicAuthenticationEntryPoint aep;
    private MessageDigest digest;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("No MD5 algorithm available!");
        }

        aep = new BasicAuthenticationEntryPoint();
        aep.setRealmName(GeoServerSecurityManager.REALM);
        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        BasicAuthenticationFilterConfig authConfig = (BasicAuthenticationFilterConfig) config;

        BasicAuthenticationFilter filter =
                new BasicAuthenticationFilter(getSecurityManager().authenticationManager(), aep);

        if (authConfig.isUseRememberMe()) {
            filter.setRememberMeServices(securityManager.getRememberMeService());
            GeoServerWebAuthenticationDetailsSource s =
                    new GeoServerWebAuthenticationDetailsSource();
            filter.setAuthenticationDetailsSource(s);
        }
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        req.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(req, res, chain);
    }

    /** returns username:md5(password:filtername) */
    @Override
    public String getCacheKey(HttpServletRequest request) {

        if (request.getSession(false) != null) // no caching if there is an HTTP session
        return null;

        String header = request.getHeader("Authorization");
        if ((header != null) && header.startsWith("Basic ")) {
            byte[] base64Token = null;
            try {
                base64Token = header.substring(6).getBytes("UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException(e1);
            }
            String token = new String(Base64.getDecoder().decode(base64Token));

            String username = "";
            String password = "";
            int delim = token.indexOf(":");

            if (delim != -1) {
                username = token.substring(0, delim);
                password = token.substring(delim + 1);
            } else {
                return null;
            }

            if (GeoServerUser.ROOT_USERNAME.equals(username)) return null;

            StringBuffer buff = new StringBuffer(password);
            buff.append(":");
            buff.append(getName());
            String digestString = null;
            try {
                MessageDigest md = (MessageDigest) digest.clone();
                digestString = new String(Hex.encode(md.digest(buff.toString().getBytes("utf-8"))));
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
            buff = new StringBuffer(username);
            buff.append(":");
            buff.append(digestString);
            return buff.toString();
        } else return null;
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
