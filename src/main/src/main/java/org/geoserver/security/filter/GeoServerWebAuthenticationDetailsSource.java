/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import javax.servlet.http.HttpServletRequest;

import org.springframework.security.authentication.AuthenticationDetailsSource;

/**
 * @author christian
 *
 */
public class GeoServerWebAuthenticationDetailsSource implements AuthenticationDetailsSource<HttpServletRequest, GeoServerWebAuthenticationDetails> {

    @Override
    public GeoServerWebAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new GeoServerWebAuthenticationDetails(context);
    }

}
