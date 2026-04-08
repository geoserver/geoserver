/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.jwtheaders.filter.details;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * This will be in Authentication#getDetails() This contains the jwtHeadersConfigId. See JwtHeadersWebAuthDetailsSource
 */
public class JwtHeadersWebAuthenticationDetails extends WebAuthenticationDetails {

    public String jwtHeadersConfigId;

    public JwtHeadersWebAuthenticationDetails(String jwtHeadersConfigId, HttpServletRequest request) {
        super(request);
        this.jwtHeadersConfigId = jwtHeadersConfigId;
    }

    public String getJwtHeadersConfigId() {
        return jwtHeadersConfigId;
    }
}
