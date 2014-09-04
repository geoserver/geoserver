/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import org.jasig.cas.client.validation.Assertion;

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
    
    public final static String PROXY_TICKET_PREFIX = "PT-";
    public final static String SERVICE_TICKET_PREFIX = "ST-";
    
    /**
     * The original CAS {@link Assertion} object is needed if geoserver
     * code wants to create CAS Proxy tickets
     * 
     * If an HTTP session has been created, the assertion is stored using
     * {@link HttpSession#setAttribute(String, Object)}.
     * 
     * If no session has been created, the assertion is stored using   
     * {@link HttpServletRequest#setAttribute(String, Object)}
     * 
     */
    public final static String CAS_ASSERTION_KEY = "org.geoserver.security.cas.CasAssertion";
    
    
    /**
     * This key is used to store the proxy list in {@link Assertion#getAttributes()}.
     * The main reason is to determine if the ticket was a proxy ticket or a service ticket.
     * 
     */
    public final static String CAS_PROXYLIST_KEY = "_CasProxyListKey";
    
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
