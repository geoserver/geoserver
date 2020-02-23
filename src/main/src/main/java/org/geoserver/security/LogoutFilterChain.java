/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

import java.io.IOException;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.security.filter.GeoServerLogoutFilter;

/**
 * Filter chain for logout requests
 *
 * @author christian
 */
public class LogoutFilterChain extends ConstantFilterChain {

    /** */
    private static final long serialVersionUID = 1L;

    public LogoutFilterChain(String... patterns) {
        super(patterns);
    }

    /**
     * Convenience method for {@link GeoServerLogoutFilter#doLogout(HttpServletRequest,
     * HttpServletResponse, String...)}
     */
    public void doLogout(
            GeoServerSecurityManager manager,
            HttpServletRequest request,
            HttpServletResponse response,
            String... skipHandlerName)
            throws IOException, ServletException {
        GeoServerLogoutFilter filter =
                (GeoServerLogoutFilter)
                        manager.loadFilter(GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
        if (filter == null) {
            LOGGER.warning(
                    "Cannot find filter: " + GeoServerSecurityFilterChain.FORM_LOGOUT_FILTER);
            return;
        }
        filter.doLogout(request, response, skipHandlerName);
    }
}
