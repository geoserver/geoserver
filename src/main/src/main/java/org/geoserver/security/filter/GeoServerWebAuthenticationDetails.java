/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import javax.servlet.http.HttpServletRequest;
import org.springframework.security.web.authentication.WebAuthenticationDetails;

/**
 * extension for adding additional authentication details
 *
 * @author christian
 */
public class GeoServerWebAuthenticationDetails extends WebAuthenticationDetails {

    private static final long serialVersionUID = 1L;
    private String userGroupServiceName;

    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }

    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }

    public GeoServerWebAuthenticationDetails(HttpServletRequest request) {
        super(request);
    }
}
