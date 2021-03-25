/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Initializes all logging functions.
 *
 * @author Rob Hranac, Vision for New York
 * @author Chris Holmes, TOPP
 * @version $Id$
 */
public class FreefsLog extends HttpServlet {

    /** Initializes logging and config. */
    @Override
    public void init() throws ServletException {
        // configure log4j, since console logging is configured elsewhere
        // we deny all logging, this is really just to prevent log4j
        // initilization warnings
        // TODO: this is a hack, log config should be cleaner

        // JD: Commenting out
        //    	ConsoleAppender appender = new ConsoleAppender(new PatternLayout());
        //    	appender.addFilter(new DenyAllFilter());
        //
        //    	BasicConfigurator.configure(appender);
        //
        // HACK: java.util.prefs are awful.  See
        // http://www.allaboutbalance.com/disableprefs.  When the site comes
        // back up we should implement their better way of fixing the problem.
        System.setProperty("java.util.prefs.syncInterval", "5000000");
    }

    /**
     * Initializes logging.
     *
     * @param req The servlet request object.
     * @param res The servlet response object.
     */
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
        // BasicConfigurator.configure();
    }
}
