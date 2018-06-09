/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.io.IOException;
import java.util.Map;
import java.util.logging.Logger;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.vfny.geoserver.util.requests.readers.DispatcherKvpReader;
import org.vfny.geoserver.util.requests.readers.KvpRequestReader;

/**
 * Routes requests made at the top-level URI to appropriate interface servlet. Note that the logic
 * of this method could be generously described as 'loose.' It is not checking for request validity
 * in any way (this is done by the reqeust- specific servlets). Rather, it is attempting to make a
 * reasonable guess as to what servlet to call, given that the client is routing to the top level
 * URI as opposed to the request-specific URI, as specified in the GetCapabilities response. Thus,
 * this is a convenience method, which allows for some slight client laziness and helps explain to
 * lost souls/spiders what lives at the URL. Due to the string parsing, it is much faster (and
 * recommended) to use the URIs specified in the GetCapabablities response.
 *
 * @author Rob Hranac, Vision for New York
 * @author Chris Holmes, TOPP
 * @version $Id$
 * @task TODO: rework to work too for WMS servlets, and to get the servlets from ServletContext
 *     instead of having them hardcoded
 */

// JD: kill this class
public class Dispatcher extends HttpServlet {
    /** Class logger */
    private static Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** Map metadata request type */
    public static String META_REQUEST = "GetMeta";

    /** Map get capabilities request type */
    public static final int GET_CAPABILITIES_REQUEST = 1;

    /** Map describe feature type request type */
    public static final int DESCRIBE_FEATURE_TYPE_REQUEST = 2;

    /** Map get feature request type */
    public static final int GET_FEATURE_REQUEST = 3;

    /** Map get feature request type */
    public static final int TRANSACTION_REQUEST = 4;

    /** Map get feature with lock request type */
    public static final int GET_FEATURE_LOCK_REQUEST = 5;

    /** WMS get feature info request type */
    public static final int GET_FEATURE_INFO_REQUEST = 6;

    /** int representation of a lock request type */
    public static final int LOCK_REQUEST = 6;

    /** Map get capabilities request type */
    public static final int GET_MAP_REQUEST = 7;

    /** WMS DescribeLayer request type */
    public static final int DESCRIBE_LAYER_REQUEST = 8;

    /** WMS GetLegendGraphic request type */
    public static final int GET_LEGEND_GRAPHIC_REQUEST = 9;

    public static final short WMS_SERVICE = 101;
    public static final short WFS_SERVICE = 102;

    /** Map get feature request type */
    public static final int UNKNOWN = -1;

    /** Map get feature request type */
    public static final int ERROR = -2;

    protected ServletConfig servletConfig;

    // HACK! This is just to fix instances where the first request is a
    // dispatcher, and the strategy hasn't been inited yet.  This can be
    // fixed in two ways, one by having Dispatcher extend Abstract ServiceConfig,
    // which it should do, and two by having the configuration of the strategy
    // done in user configuration instead of in the web.xml file.  Both should
    // be done.
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.servletConfig = config;
    }

    /**
     * Handles all Get requests. This method implements the main matching logic for the class.
     *
     * @param request The servlet request object.
     * @param response The servlet response object.
     * @throws ServletException For any servlet problems.
     * @throws IOException For any io problems.
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        int targetRequest = 0;

        // Examine the incoming request and create appropriate server objects
        //  to deal with each request
        //              try {
        if (request.getQueryString() != null) {
            Map kvPairs = KvpRequestReader.parseKvpSet(request.getQueryString());
            targetRequest = DispatcherKvpReader.getRequestType(kvPairs);
        } else {
            targetRequest = UNKNOWN;

            // throw exception
        }
    }
}
