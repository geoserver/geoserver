/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.cas;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import org.jasig.cas.client.validation.Assertion;
import org.springframework.security.cas.ServiceProperties;

/**
 * Cas constants and convenience methods used for the Geoserver CAS implementation
 *
 * @author christian
 */
public class GeoServerCasConstants {

    public static final String CAS_PROXY_RECEPTOR_PATTERN = "/j_spring_cas_security_proxyreceptor";
    public static final String ARTIFACT_PARAMETER =
            ServiceProperties.DEFAULT_CAS_ARTIFACT_PARAMETER;
    public static final String LOGIN_URI = "/login";
    public static final String LOGOUT_URI = "/logout";
    public static final String LOGOUT_URL_PARAM = "url";

    public static final String PROXY_TICKET_PREFIX = "PT-";
    public static final String SERVICE_TICKET_PREFIX = "ST-";

    /**
     * The original CAS {@link Assertion} object is needed if geoserver code wants to create CAS
     * Proxy tickets
     *
     * <p>If an HTTP session has been created, the assertion is stored using {@link
     * HttpSession#setAttribute(String, Object)}.
     *
     * <p>If no session has been created, the assertion is stored using {@link
     * HttpServletRequest#setAttribute(String, Object)}
     */
    public static final String CAS_ASSERTION_KEY = "org.geoserver.security.cas.CasAssertion";

    /**
     * creates the proxy callback url using the call back url prefix and {@link
     * #CAS_PROXY_RECEPTOR_PATTERN}
     *
     * <p>if the ulrPrefix is null, the return value is null
     */
    public static String createProxyCallBackURl(String urlPrefix) {
        return createCasURl(urlPrefix, CAS_PROXY_RECEPTOR_PATTERN);
    }

    /** create a CAS url, casUri must start with "/" */
    public static String createCasURl(String casUrlPrefix, String casUri) {
        if (casUrlPrefix == null) return null;

        String resultURL =
                casUrlPrefix.endsWith("/")
                        ? casUrlPrefix.substring(0, casUrlPrefix.length() - 1)
                        : casUrlPrefix;
        return resultURL + casUri;
    }
}
