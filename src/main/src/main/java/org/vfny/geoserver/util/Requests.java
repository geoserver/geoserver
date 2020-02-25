/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.servlet.http.HttpServletRequest;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Utility methods helpful when processing GeoServer Requests.
 *
 * <p>Provides helper functions and classes useful when implementing your own Response classes. Of
 * significant importantance are the Request processing functions that allow access to the
 * WebContainer, GeoServer and the User's Session.
 *
 * <p>If you are working with the STRUTS API the Action method is the direct paralle of the Response
 * classes. You may whish to look at how ConfigAction is implemented, it is a super class which
 * delegates to these Request processing methods.
 *
 * @author Jody Garnett
 */
public final class Requests {
    static Logger LOGGER = org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver");

    /*
    * This is the parameter used to get the proxy from the
    * web.xml file.  This is a bit hacky, it should be moved to
    * GeoServer.java and be a normal config parameter, but the
    * overhead of making a new config param is just too high,
    * so we're allowing this to just be read from the web.xml
    ( See GEOS-598 for more information
    */
    public static final String PROXY_PARAM = "PROXY_BASE_URL";

    /**
     * Appends a context path to a base url.
     *
     * @param url The base url.
     * @param contextPath The context path to be appended.
     * @return A full url with the context path appended.
     */
    public static String appendContextPath(String url, String contextPath) {
        if (url.endsWith("/")) {
            url = url.substring(0, url.length() - 1);
        }
        if (contextPath == null) {
            return url;
        }

        if (contextPath.startsWith("/")) {
            contextPath = contextPath.substring(1);
        }

        return url + "/" + contextPath;
    }

    /**
     * Appends a query string to a url.
     *
     * <p>This method checks <code>url</code> to see if the appended query string requires a '?' or
     * '&' to be prepended.
     *
     * @param url The base url.
     * @param queryString The query string to be appended, should not contain the '?' character.
     * @return A full url with the query string appended.
     */
    public static String appendQueryString(String url, String queryString) {
        if (url.endsWith("?") || url.endsWith("&")) {
            return url + queryString;
        }

        if (url.indexOf('?') != -1) {
            return url + "&" + queryString;
        }

        return url + "?" + queryString;
    }

    /**
     * Tests is user is loggin in.
     *
     * <p>True if UserContainer exists has been created.
     *
     * @param request HttpServletRequest providing current Session
     */
    public static boolean isLoggedIn(HttpServletRequest request) {
        // check the user is not the anonymous one
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        return (authentication != null)
                && !(authentication instanceof AnonymousAuthenticationToken);
    }

    /**
     * This method gets the correct input stream for a URL. If the URL is a http/https connection,
     * the Accept-Encoding: gzip, deflate is added. It the paramter is added, the response is
     * checked to see if the response is encoded in gzip, deflate or plain bytes. The correct input
     * stream wrapper is then selected and returned.
     *
     * <p>This method was added as part of GEOS-420
     *
     * @param url The url to the sld file
     * @return The InputStream used to validate and parse the SLD xml.
     */
    public static InputStream getInputStream(URL url) throws IOException {
        // Open the connection
        URLConnection conn = url.openConnection();

        // If it is the http or https scheme, then ask for gzip if the server supports it.
        if (conn instanceof HttpURLConnection) {
            // Send the requested encoding to the remote server.
            conn.setRequestProperty("Accept-Encoding", "gzip, deflate");
        }

        // Conect to get the response headers
        conn.connect();

        // Return the correct inputstream
        // If the connection is a url, connection, check the response encoding.
        if (conn instanceof HttpURLConnection) {
            // Get the content encoding of the server response
            String encoding = conn.getContentEncoding();

            // If null, set it to a emtpy string
            if (encoding == null) {
                encoding = "";
            }

            if (encoding.equalsIgnoreCase("gzip")) {
                // For gzip input stream, use a GZIPInputStream
                return new GZIPInputStream(conn.getInputStream());
            } else if (encoding.equalsIgnoreCase("deflate")) {
                // If it is encoded as deflate, then select the inflater inputstream.
                return new InflaterInputStream(conn.getInputStream(), new Inflater(true));
            } else {
                // Else read the raw bytes
                return conn.getInputStream();
            }
        } else {
            // Else read the raw bytes.
            return conn.getInputStream();
        }
    }

    /**
     * Parses an 'option-holding' parameters in the following form
     * FORMAT_OPTIONS=multiKey:val1,val2,val3;singleKey:val
     *
     * <p>Useful for parsing out the FORMAT_OPTIONS and LEGEND_OPTIONS parameters
     */
    public static Map parseOptionParameter(String rawOptionString) throws IllegalArgumentException {
        HashMap map = new HashMap();
        if (rawOptionString == null) {
            return map;
        }

        StringTokenizer semiColonSplitter = new StringTokenizer(rawOptionString, ";");
        while (semiColonSplitter.hasMoreElements()) {
            String curKVP = semiColonSplitter.nextToken();

            final int cloc = curKVP.indexOf(":");
            if (cloc <= 0) {
                throw new IllegalArgumentException(
                        "Key-value-pair: '"
                                + curKVP
                                + "' isn't properly formed.  It must be of the form 'Key:Value1,Value2...'");
            }
            String key = curKVP.substring(0, cloc);
            String values = curKVP.substring(cloc + 1, curKVP.length());
            if (values.indexOf(",") != -1) {
                List valueList = new ArrayList();
                StringTokenizer commaSplitter = new StringTokenizer(values, ",");
                while (commaSplitter.hasMoreElements()) valueList.add(commaSplitter.nextToken());

                map.put(key, valueList);
            } else {
                map.put(key, values);
            }
        }

        return map;
    }
}
