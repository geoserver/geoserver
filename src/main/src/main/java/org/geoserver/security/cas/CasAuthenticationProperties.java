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

    public final static String CAS_PROXY_RECEPTOR_PATTERN = "/j_spring_cas_security_proxyreceptor";

    public abstract String getCasServerUrlPrefix();

    public abstract String getService();

    public abstract boolean isSendRenew();

    public abstract String getProxyCallbackUrl();

}