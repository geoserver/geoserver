/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import org.geoserver.security.config.SecurityFilterConfig;



/**
 * Configuration for cas authentication, acting
 * as replacement for a form login and  handling
 * the proxy receptor url
 * 
 * 
 * @author mcr
 *
 */
public class CasAuthenticationFilterConfig extends SecurityFilterConfig implements CasAuthenticationProperties {

    private static final long serialVersionUID = 1L;
    
    private String userGroupServiceName;
     
    /**
     * The geoserver url where the case server sends credential information
     * 
     * example:
     * http://localhost:8080/geoserver/j_spring_cas_security_check
     */
    private String service;
    
    
    /**
     * if true, no single sign on possible
     */
    private boolean sendRenew;
    
    /**
     * The CAS server URL including context root
     * 
     * example
     * "https://localhost:9443/cas" 
     */
    private String casServerUrlPrefix;
    
    
    /**
     * The geoserver url for the proxy callback 
     * 
     * example:
     * https://myhost:8443/geoserver
     */
    
    private String proxyCallbackUrlPrefix;


    /**
     * Optional:
     * 
     * After a successful CAS logout triggered by geoserver,
     * a cas response page is rendered.
     * 
     * This url should be rendered as a link in the CAS response
     * page.
     * 
     * example:
     * https://myhost:8443/geoserver
     */
    private String urlInCasLogoutPage;

    
    
    public String getUrlInCasLogoutPage() {
        return urlInCasLogoutPage;
    }

    public void setUrlInCasLogoutPage(String urlInCasLogoutPage) {
        this.urlInCasLogoutPage = urlInCasLogoutPage;
    }




    /* (non-Javadoc)
     * @see org.geoserver.security.cas.CasAuthenticationProperties#getCasServerUrlPrefix()
     */
    @Override
    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }



    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }



    /* (non-Javadoc)
     * @see org.geoserver.security.cas.CasAuthenticationProperties#getService()
     */
    @Override
    public String getService() {
        return service;
    }



    public void setService(String service) {
        this.service = service;
    }



    /* (non-Javadoc)
     * @see org.geoserver.security.cas.CasAuthenticationProperties#isSendRenew()
     */
    @Override
    public boolean isSendRenew() {
        return sendRenew;
    }



    public void setSendRenew(boolean sendRenew) {
        this.sendRenew = sendRenew;
    }


    public String getUserGroupServiceName() {
        return userGroupServiceName;
    }



    public void setUserGroupServiceName(String userGroupServiceName) {
        this.userGroupServiceName = userGroupServiceName;
    }
    
    @Override
    public  boolean providesAuthenticationEntryPoint() {
        return true;
    }


    public String getProxyCallbackUrlPrefix() {
        return proxyCallbackUrlPrefix;
    }



    public void setProxyCallbackUrlPrefix(String proxyCallbackUrlPrefix) {
        this.proxyCallbackUrlPrefix = proxyCallbackUrlPrefix;
    }
 
    
}
