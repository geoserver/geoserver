/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import javax.servlet.http.HttpServletRequest;

/**
 * J2EE Authentication Filter
 *
 * @author mcr
 */
public class GeoServerJ2eeAuthenticationFilter extends GeoServerJ2eeBaseAuthenticationFilter {
    @Override
    protected String getPreAuthenticatedPrincipalName(HttpServletRequest request) {
        return request.getUserPrincipal() == null ? null : request.getUserPrincipal().getName();
    }
}
