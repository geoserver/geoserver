/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import org.geoserver.security.filter.GeoServerLogoutFilter;

/**
 * {@link GeoServerLogoutFilter} configuration object.
 *
 * @author mcr
 */
public class LogoutFilterConfig extends SecurityFilterConfig {

    private static final long serialVersionUID = 1L;

    /**
     * Optional, redirect URL after a successful logout If empty, the client will receive an HTTP
     * 200 response.
     *
     * <p>This URL can be an absolute URL or relative to the GeoServer root context like the default
     * {@link GeoServerLogoutFilter#URL_AFTER_LOGOUT}
     */
    private String redirectURL;

    private String formLogoutChain = "/j_spring_security_logout,/j_spring_security_logout/,/logout";

    public String getRedirectURL() {
        return redirectURL;
    }

    public void setRedirectURL(String redirectURL) {
        this.redirectURL = redirectURL;
    }

    /** @return the formLogoutChain */
    public String getFormLogoutChain() {
        return formLogoutChain;
    }

    /** @param formLogoutChain the formLogoutChain to set */
    public void setFormLogoutChain(String formLogoutChain) {
        this.formLogoutChain = formLogoutChain;
    }
}
