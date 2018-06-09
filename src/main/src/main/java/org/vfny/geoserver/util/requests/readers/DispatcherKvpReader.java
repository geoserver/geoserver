/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.util.requests.readers;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import org.geoserver.platform.ServiceException;
import org.vfny.geoserver.servlets.Dispatcher;

/**
 * Reads in a generic request and attempts to determine its type.
 *
 * @author Chris Holmes, TOPP
 * @author Gabriel Rold?n
 * @version $Id$
 */
public class DispatcherKvpReader {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.requests.readers");

    private String queryString;
    private Map requestParams;

    /**
     * Constructor with raw request string. Calls parent.
     *
     * @param reader A reader of the request from the http client.
     * @param req The actual request made.
     * @throws ServiceException DOCUMENT ME!
     * @throws IOException
     */
    public void read(BufferedReader requestReader, HttpServletRequest req)
            throws ServiceException, IOException {
        final StringBuffer output = new StringBuffer();
        int c;

        while (-1 != (c = requestReader.read())) {
            output.append((char) c);
        }

        requestReader.close();
        this.queryString = output.toString();

        this.requestParams = KvpRequestReader.parseKvpSet(this.queryString);
    }

    /**
     * Returns the request type for a given KVP set.
     *
     * @param kvPairs DOCUMENT ME!
     * @return Request type.
     */
    public static int getRequestType(Map kvPairs) {
        String responseType = ((String) kvPairs.get("REQUEST"));
        LOGGER.finer("dispatcher got request " + responseType);

        if (responseType != null) {
            responseType = responseType.toUpperCase();

            if (responseType.equals("GETCAPABILITIES") || responseType.equals("CAPABILITIES")) {
                return Dispatcher.GET_CAPABILITIES_REQUEST;
            } else if (responseType.equals("DESCRIBEFEATURETYPE")) {
                return Dispatcher.DESCRIBE_FEATURE_TYPE_REQUEST;
            } else if (responseType.equals("GETFEATURE")) {
                return Dispatcher.GET_FEATURE_REQUEST;
            } else if (responseType.equals("TRANSACTION")) {
                return Dispatcher.TRANSACTION_REQUEST;
            } else if (responseType.equals("GETFEATUREWITHLOCK")) {
                return Dispatcher.GET_FEATURE_LOCK_REQUEST;
            } else if (responseType.equals("LOCKFEATURE")) {
                return Dispatcher.LOCK_REQUEST;
            } else if (responseType.equals("GETMAP") || responseType.equals("MAP")) {
                return Dispatcher.GET_MAP_REQUEST;
            } else if (responseType.equals("GETFEATUREINFO")) {
                return Dispatcher.GET_FEATURE_INFO_REQUEST;
            } else if (responseType.equals("DESCRIBELAYER")) {
                return Dispatcher.DESCRIBE_LAYER_REQUEST;
            } else if (responseType.equals("GETLEGENDGRAPHIC")) {
                return Dispatcher.GET_LEGEND_GRAPHIC_REQUEST;
            } else {
                return Dispatcher.UNKNOWN;
            }
        } else {
            return Dispatcher.UNKNOWN;
        }
    }

    /**
     * Returns the request type for a given KVP set.
     *
     * @param kvPairs DOCUMENT ME!
     * @return Request type.
     */
    public static int getServiceType(Map kvPairs) {
        String serviceType = ((String) kvPairs.get("SERVICE"));

        if (serviceType != null) {
            serviceType = serviceType.toUpperCase();

            if (serviceType.equals("WFS")) {
                return Dispatcher.WFS_SERVICE;
            } else if (serviceType.equals("WMS")) {
                return Dispatcher.WMS_SERVICE;
            } else {
                return Dispatcher.UNKNOWN;
            }
        } else {
            return Dispatcher.UNKNOWN;
        }
    }

    /** @return The service, WFS,WMS,WCS,etc... */
    public String getService() {
        if (requestParams.containsKey("SERVICE")) {
            return (String) requestParams.get("SERVICE");
        } else {
            return null;
        }
    }

    /** @return The request, GetCapabilities,GetMap,etc... */
    public String getRequest() {
        if (requestParams.containsKey("REQUEST")) {
            return (String) requestParams.get("REQUEST");
        } else {
            return null;
        }
    }

    public String getQueryString() {
        return queryString;
    }
}
