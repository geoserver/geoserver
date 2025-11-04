/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.security.config.SecurityNamedServiceConfig;
import org.geoserver.security.filter.GeoServerAuthenticationFilter;
import org.geoserver.security.filter.GeoServerCompositeFilter;
import org.geotools.util.logging.Logging;

/**
 * {@link Filter} supports OAuth2 resource server scenarios by delegating to the nested Spring filter implementations.
 *
 * <p>Used for the "Resource Server" use case. Implementation is unfinished, because a different GS extension supports
 * this case already. Filter is not offered in UI. This code is never executed.
 *
 * @author awaterme
 */
public class GeoServerOAuth2ResourceServerAuthenticationFilter extends GeoServerCompositeFilter
        implements GeoServerAuthenticationFilter {

    private static final Logger LOGGER = Logging.getLogger(GeoServerOAuth2ResourceServerAuthenticationFilter.class);

    public GeoServerOAuth2ResourceServerAuthenticationFilter() {
        super();
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        LOGGER.log(Level.FINER, "Running filter.");
        super.doFilter(request, response, chain);
    }

    @Override
    public void initializeFromConfig(SecurityNamedServiceConfig pConfig) throws IOException {
        LOGGER.log(Level.FINE, "Initializing filter.");
        super.initializeFromConfig(pConfig);
    }

    @Override
    public boolean applicableForHtml() {
        return true;
    }

    @Override
    public boolean applicableForServices() {
        return true;
    }
}
