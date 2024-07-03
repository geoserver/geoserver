/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.ows.Request;
import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;

/**
 * Utility class performing operations related to http requests.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 *     <p>TODO: this class needs to be merged with org.vfny.geoserver.Requests.
 */
public class RequestUtils {

    /**
     * Pulls out the first IP address from the X-Forwarded-For request header if it was provided;
     * otherwise just gets the client IP address.
     *
     * @return the IP address of the client that sent the request
     */
    public static String getRemoteAddr(HttpServletRequest req) {
        String forwardedFor = req.getHeader("X-Forwarded-For");
        if (forwardedFor != null) {
            String[] ips = forwardedFor.split(", ");
            return ips[0];
        } else {
            return req.getRemoteAddr();
        }
    }

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will return
     * the negotiated version to be used for response according to the pre OWS 1.1 specifications,
     * that is, WMS 1.1, WMS 1.3, WFS 1.0, WFS 1.1 and WCS 1.0
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionPreOws(List<String> providedList, List<String> acceptedList) {
        // first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest
        if (acceptedList == null || acceptedList.isEmpty()) return provided.last().toString();

        // next figure out what the client accepts (and check they are good version numbers)
        TreeSet<Version> accepted = new TreeSet<>();
        for (String v : acceptedList) {
            checkVersionNumber(v, null);

            accepted.add(new Version(v));
        }

        // prune out those not provided
        for (Iterator<Version> v = accepted.iterator(); v.hasNext(); ) {
            Version version = v.next();

            if (!provided.contains(version)) {
                v.remove();
            }
        }

        // lookup a matching version
        String version = null;
        if (!accepted.isEmpty()) {
            // return the highest version provided
            version = accepted.last().toString();
        } else {
            for (String v : acceptedList) {
                accepted.add(new Version(v));
            }

            // if highest accepted less then lowest provided, send lowest
            if ((accepted.last()).compareTo(provided.first()) < 0) {
                version = (provided.first()).toString();
            }

            // if lowest accepted is greater then highest provided, send highest
            if ((accepted.first()).compareTo(provided.last()) > 0) {
                version = (provided.last()).toString();
            }

            if (version == null) {
                // go through from lowest to highest, and return highest provided
                // that is less than the highest accepted
                Iterator<Version> v = provided.iterator();
                Version last = v.next();

                while (v.hasNext()) {
                    Version current = v.next();

                    if (current.compareTo(accepted.last()) > 0) {
                        break;
                    }

                    last = current;
                }

                version = last.toString();
            }
        }

        return version;
    }

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will return
     * the negotiated version to be used for response according to the OWS 1.1 specification (at the
     * time of writing, only WCS 1.1.1 is using it)
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" format)
     * @return the negotiated version to be used for response
     */
    public static String getVersionOws11(List<String> providedList, List<String> acceptedList) {
        // first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest supported version
        if (acceptedList == null || acceptedList.isEmpty()) return provided.last().toString();

        // next figure out what the client accepts (and check they are good version numbers)
        List<Version> accepted = new ArrayList<>();
        for (String v : acceptedList) {
            checkVersionNumber(v, "AcceptVersions");

            accepted.add(new Version(v));
        }

        // from the specification "The server, upon receiving a GetCapabilities request, shall scan
        // through this list and find the first version number that it supports"
        Version negotiated = null;
        for (Version version : accepted) {
            if (provided.contains(version)) {
                negotiated = version;
                break;
            }
        }

        // from the spec: "If the list does not contain any version numbers that the server
        // supports, the server shall return an Exception with
        // exceptionCode="VersionNegotiationFailed"
        if (negotiated == null)
            throw new ServiceException(
                    "Could not find any matching version "
                            + acceptedList
                            + " in supported list: "
                            + acceptedList,
                    "VersionNegotiationFailed");

        return negotiated.toString();
    }

    /**
     * Checks the validity of a version number (the specification version numbers, three dot
     * separated integers between 0 and 99). Throws a ServiceException if the version number is not
     * valid.
     *
     * @param v the version number (in string format)
     * @param locator The locator for the service exception (may be null)
     */
    public static void checkVersionNumber(String v, String locator) throws ServiceException {
        if (!v.matches("[0-9]{1,2}\\.[0-9]{1,2}\\.[0-9]{1,2}")) {
            String msg = v + " is an invalid version number";
            throw new ServiceException(msg, "VersionNegotiationFailed", locator);
        }
    }

    /**
     * Wraps an xml input xstream in a buffered reader specifying a lookahead that can be used to
     * preparse some of the xml document, resetting it back to its original state for actual
     * parsing.
     *
     * @param stream The original xml stream.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of
     *     bytes are preparsed the stream can not be properly reset.
     * @return The buffered reader.
     */
    public static BufferedReader getBufferedXMLReader(InputStream stream, int xmlLookahead)
            throws IOException {

        @SuppressWarnings("PMD.CloseResource")
        Reader reader = XmlCharsetDetector.getCharsetAwareReader(stream);

        return getBufferedXMLReader(reader, xmlLookahead);
    }

    /**
     * Wraps an xml reader in a buffered reader specifying a lookahead that can be used to preparse
     * some of the xml document, resetting it back to its original state for actual parsing.
     *
     * @param reader The original xml reader.
     * @param xmlLookahead The number of bytes to support for parse. If more than this number of
     *     bytes are preparsed the stream can not be properly reset.
     * @return The buffered reader.
     */
    public static BufferedReader getBufferedXMLReader(Reader reader, int xmlLookahead)
            throws IOException {
        // ensure the reader is a buffered reader

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader, xmlLookahead);
        }

        // mark the input stream
        reader.mark(xmlLookahead);

        return (BufferedReader) reader;
    }

    /**
     * Retrieve a language parameter value from request query params.
     *
     * @param request the request object.
     * @param languageParamName the language parameter.
     * @return the value of the language parameter. Might be LANGUAGE or ACCEPTLANGUAGES.
     */
    public static String[] getLanguageValue(Request request, String languageParamName) {
        String[] result = null;
        if (request != null) result = getLanguageValue(request.getRawKvp(), languageParamName);
        return result;
    }

    /**
     * Retrieve a language parameter value from request query params.
     *
     * @param rawKvp the rawKvp holding the request parameters.
     * @param languageParamName the name of the language parameter. Might be LANGUAGE or
     *     ACCEPTLANGUAGES.
     * @return
     */
    public static String[] getLanguageValue(Map<String, Object> rawKvp, String languageParamName) {
        String[] result = null;
        if (rawKvp != null && rawKvp.containsKey(languageParamName)) {
            String acceptLanguages =
                    rawKvp.get(languageParamName) != null
                            ? rawKvp.get(languageParamName).toString()
                            : "";
            if (acceptLanguages != null) {
                String[] langAr = acceptLanguages.split(" ");
                if (langAr.length == 1) {
                    langAr = acceptLanguages.split(",");
                }
                result = langAr;
            }
        }
        return result;
    }
}
