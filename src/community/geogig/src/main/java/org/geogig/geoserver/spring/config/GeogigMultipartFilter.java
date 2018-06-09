/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import java.io.IOException;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.filters.GeoServerFilter;
import org.springframework.web.multipart.support.MultipartFilter;

public class GeogigMultipartFilter extends MultipartFilter implements GeoServerFilter {

    public GeogigMultipartFilter() {
        setMultipartResolverBeanName(GeogigSpringConfig.FILTER_MULTIPART_RESOLVER_NAME);
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path.startsWith("/geoserver/geogig")) {
            super.doFilterInternal(request, response, filterChain);
        } else {
            filterChain.doFilter(request, response);
        }
    }
}
