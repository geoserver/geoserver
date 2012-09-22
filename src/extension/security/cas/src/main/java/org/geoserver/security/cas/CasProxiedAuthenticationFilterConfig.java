/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.cas;

import org.geoserver.security.config.PreAuthenticatedUserNameFilterConfig;



/**
 * Configuration for cas authentication receiving proxy tickets
 * 
 * 
 * @author mcr
 *
 */
public class CasProxiedAuthenticationFilterConfig extends PreAuthenticatedUserNameFilterConfig  implements CasAuthenticationProperties {



    private static final long serialVersionUID = 1L;
    
    
    
    
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
     * http://localhost:8080/geoserver
     */
    
    private String proxyCallbackUrlPrefix;
    
    /**
     * if <code>true</code>, an HTTP session is created after
     * a successful ticket validation
     */
    private boolean createHTTPSessionForValidTicket;
    

    public boolean isSendRenew() {
        return sendRenew;
    }


    public void setSendRenew(boolean sendRenew) {
        this.sendRenew = sendRenew;
    }


    public String getCasServerUrlPrefix() {
        return casServerUrlPrefix;
    }


    public void setCasServerUrlPrefix(String casServerUrlPrefix) {
        this.casServerUrlPrefix = casServerUrlPrefix;
    }


    public String getProxyCallbackUrlPrefix() {
        return proxyCallbackUrlPrefix;
    }



    public void setProxyCallbackUrlPrefix(String proxyCallbackUrlPrefix) {
        this.proxyCallbackUrlPrefix = proxyCallbackUrlPrefix;
    }

      
    @Override
    public boolean providesAuthenticationEntryPoint() {
        return false;
    }


    public boolean isCreateHTTPSessionForValidTicket() {
        return createHTTPSessionForValidTicket;
    }


    public void setCreateHTTPSessionForValidTicket(boolean createHTTPSessionForValidTicket) {
        this.createHTTPSessionForValidTicket = createHTTPSessionForValidTicket;
    }

}
