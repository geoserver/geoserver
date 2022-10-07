/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.rest;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.impl.GeoServerRole;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.servlet.handler.HandlerInterceptorAdapter;

/** @author Niels Charlier */
public class GeofenceSecurityInterceptor extends HandlerInterceptorAdapter {

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (request.getServletPath().equalsIgnoreCase("/geofence")) {
            if (!SecurityContextHolder.getContext()
                    .getAuthentication()
                    .getAuthorities()
                    .contains(GeoServerRole.ADMIN_ROLE)) {
                throw new AccessDeniedException("You must be administrator.");
            }
        }

        return true;
    }
}
