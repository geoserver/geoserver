/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geoserver.authentication.filter;

import java.io.IOException;
import java.util.logging.Logger;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import org.geoserver.geoserver.authentication.auth.GeoFenceAuthenticationProvider;
import org.geoserver.geoserver.authentication.auth.GeoFenceSecurityProvider;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geoserver.security.filter.GeoServerSecurityFilter;
import org.geotools.util.logging.Logging;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationEntryPoint;
import org.springframework.security.web.authentication.www.BasicAuthenticationFilter;

/** @author ETj (etj at geo-solutions.it) */
public class GeoFenceAuthFilter
        // extends GeoServerSecurityFilter
        extends GeoServerCompositeFilter implements GeoServerAuthenticationFilter {

    static final Logger LOGGER = Logging.getLogger(GeoFenceAuthFilter.class);

    private GeoFenceSecurityProvider geofenceAuth;

    // static final String ROOT_ROLE = "ROLE_ADMINISTRATOR";
    // static final String ANONYMOUS_ROLE = "ROLE_ANONYMOUS";
    static final String USER_ROLE = "ROLE_USER";

    private BasicAuthenticationEntryPoint aep;

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig config) throws IOException {
        super.initializeFromConfig(config);

        // anything to set here? maybe the cache config

        aep = new BasicAuthenticationEntryPoint();
        aep.setRealmName(GeoServerSecurityManager.REALM);

        try {
            aep.afterPropertiesSet();
        } catch (Exception e) {
            throw new IOException(e);
        }

        // BasicAuthenticationFilterConfig authConfig = (BasicAuthenticationFilterConfig) config;
        SecurityNamedServiceConfig authCfg =
                securityManager.loadAuthenticationProviderConfig("geofence");
        GeoFenceAuthenticationProvider geofenceAuthProvider =
                geofenceAuth.createAuthenticationProvider(authCfg);
        BasicAuthenticationFilter filter = new BasicAuthenticationFilter(geofenceAuthProvider, aep);

        // if (authConfig.isUseRememberMe()) {
        // filter.setRememberMeServices(securityManager.getRememberMeService());
        // GeoServerWebAuthenticationDetailsSource s = new
        // GeoServerWebAuthenticationDetailsSource();
        // filter.setAuthenticationDetailsSource(s);
        // }
        filter.afterPropertiesSet();
        getNestedFilters().add(filter);
    }

    @Override
    public AuthenticationEntryPoint getAuthenticationEntryPoint() {
        return aep;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {

        request.setAttribute(GeoServerSecurityFilter.AUTHENTICATION_ENTRY_POINT_HEADER, aep);
        super.doFilter(request, response, chain);

        // Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        // if (auth == null) {
        // doAuth(request, response);
        // } else {
        // LOGGER.fine("Found existing Authentication in context: " + auth);
        // }
        //
        // chain.doFilter(request, response);
    }

    /** Simple username+password container */
    class BasicUser {
        String name;

        String pw;

        public BasicUser(String name, String pw) {
            this.name = name;
            this.pw = pw;
        }
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForHtml() */
    // @Override
    public boolean applicableForHtml() {
        return true;
    }

    /** @see org.geoserver.security.filter.GeoServerAuthenticationFilter#applicableForServices() */
    // @Override
    public boolean applicableForServices() {
        return true;
    }

    public void setGeofenceAuth(GeoFenceSecurityProvider geofenceAuth) {
        this.geofenceAuth = geofenceAuth;
    }
}
