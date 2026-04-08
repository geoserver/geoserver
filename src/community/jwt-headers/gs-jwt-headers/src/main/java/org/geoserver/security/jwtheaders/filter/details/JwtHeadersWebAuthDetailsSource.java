/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter.details;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;

/**
 * We are marking our (JWT Headers) security context authentication's with the ID of the header it came from. This
 * allows for easily detecting when to loggout the user. NOTE: we logout a user when the previously logged in (JWT
 * Header), but then the headers are no longer there.
 */
public class JwtHeadersWebAuthDetailsSource extends WebAuthenticationDetailsSource {

    String jwtHeadersConfigId;

    public JwtHeadersWebAuthDetailsSource(String jwtHeadersConfigId) {
        this.jwtHeadersConfigId = jwtHeadersConfigId;
    }

    @Override
    public WebAuthenticationDetails buildDetails(HttpServletRequest context) {
        return new JwtHeadersWebAuthenticationDetails(jwtHeadersConfigId, context);
    }
}
