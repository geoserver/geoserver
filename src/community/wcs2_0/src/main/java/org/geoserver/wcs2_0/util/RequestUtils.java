/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs2_0.util;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;

import org.geoserver.platform.OWS20Exception;

import org.geoserver.platform.ServiceException;
import org.geotools.util.Version;


/**
 * Utility class performing operations related to http requests.
 *
 *
 * TODO: these methods should be put back into org.geoserver.ows.util.RequestUtils
 */
public class RequestUtils {
    

    /**
     * Given a list of provided versions, and a list of accepted versions, this method will
     * return the negotiated version to be used for response according to the OWS 2.0 specification.
     *
     * The difference from the 11 version is that here versions can have format "x.y".
     *
     * @param providedList a non null, non empty list of provided versions (in "x.y.z" or "x.y" format)
     * @param acceptedList a list of accepted versions, eventually null or empty (in "x.y.z" or "x.y" format)
     * @return the negotiated version to be used for response
     *
     * @see org.geoserver.ows.util.RequestUtils#getVersionOws11(java.util.List, java.util.List) 
     */
    public static String getVersionOws20(List<String> providedList, List<String> acceptedList) {

        //first figure out which versions are provided
        TreeSet<Version> provided = new TreeSet<Version>();
        for (String v : providedList) {
            provided.add(new Version(v));
        }

        // if no accept list provided, we return the biggest supported version
        if(acceptedList == null || acceptedList.isEmpty())
            return provided.last().toString();


        // next figure out what the client accepts (and check they are good version numbers)
        List<Version> accepted = new ArrayList<Version>();
        for (String v : acceptedList) {
            checkVersionNumber20(v, "AcceptVersions");

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
        if(negotiated == null)
            throw new OWS20Exception("Could not find any matching version", OWS20Exception.OWSExceptionCode.VersionNegotiationFailed);

        return negotiated.toString();
    }

    /**
     * Checks the validity of a version number (the specification version numbers, two or three dot
     * separated integers between 0 and 99). Throws a ServiceException if the version number
     * is not valid.
     * @param v the version number (in string format)
     * @param the locator for the service exception (may be null)
     */
    public static void checkVersionNumber20(String v, String locator) throws ServiceException {
        if (!v.matches("[0-9]{1,2}\\.[0-9]{1,2}(\\.[0-9]{1,2})?")) {
            String msg = v + " is an invalid version number";
            throw new OWS20Exception("Could not find any matching version", OWS20Exception.OWSExceptionCode.VersionNegotiationFailed, locator);
        }
    }

}
