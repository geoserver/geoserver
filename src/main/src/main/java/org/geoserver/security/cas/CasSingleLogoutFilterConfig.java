/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import org.geoserver.security.config.SecurityFilterConfig;



/**
 * Configuration for cas Single logout
 * 
 * @author mcr
 *
 */
public class CasSingleLogoutFilterConfig extends SecurityFilterConfig  {

    public final static String CAS_CHAIN_PATTERN = "/j_spring_cas_security_logout";
    private static final long serialVersionUID = 1L;
    
    /**
     * Url to be shown in the case single sign out page
     */
    private String urlInCaseLogoutPage;

    public String getUrlInCaseLogoutPage() {
        return urlInCaseLogoutPage;
    }

    public void setUrlInCaseLogoutPage(String urlInCaseLogoutPage) {
        this.urlInCaseLogoutPage = urlInCaseLogoutPage;
    }
}
