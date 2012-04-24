/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */


package org.geoserver.security.cas;


/**
 * Common interface for cas authentication configurations
 * 
 * @author christian
 *
 */
public interface CasAuthenticationProperties {

    public abstract String getCasServerUrlPrefix();
    public abstract void setCasServerUrlPrefix(String url);

    public abstract String getService();
    public abstract void setService(String url);

    public abstract boolean isSendRenew();
    public abstract void setSendRenew(boolean renew);

    public abstract String getProxyCallbackUrlPrefix();
    public abstract void setProxyCallbackUrlPrefix(String url);

}