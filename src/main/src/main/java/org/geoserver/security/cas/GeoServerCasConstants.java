/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

/**
 * Cas constants and convenience methods used
 * for the Geoserver CAS implementation
 * 
 * @author christian
 *
 */
public class GeoServerCasConstants {

    public final static String CAS_PROXY_RECEPTOR_PATTERN = "/j_spring_cas_security_proxyreceptor";
    public final static String ARTIFACT_PARAMETER = "ticket";
    public final static String LOGIN_URI = "/login";
    public final static String LOGOUT_URI = "/logout";
    public final static String LOGOUT_URL_PARAM = "url";
    
    /**
     * creates the proxy callback url using the call back url prefix
     * and {@link #CAS_PROXY_RECEPTOR_PATTERN}
     * 
     * if the ulrPrefix is null, the return value is null
     * 
     * @param urlPrefix
     * @return
     */
    public static String createProxyCallBackURl(String urlPrefix) {
        return createCasURl(urlPrefix, CAS_PROXY_RECEPTOR_PATTERN);
    }
    
    /**
     * create a CAS url, casUri must start with "/"
     * 
     * @param casUrlPrefix
     * @param casUri
     * @return
     */
    public static String createCasURl(String casUrlPrefix, String casUri) {
        if (casUrlPrefix==null)
            return null;
        
        String resultURL = casUrlPrefix.endsWith("/") ? casUrlPrefix.substring(0, casUrlPrefix.length()-1) : casUrlPrefix;
        return resultURL+casUri;
    }


}
