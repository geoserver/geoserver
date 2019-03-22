/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.servlets;

import java.util.logging.Logger;
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
    /** Standard logging instance for class */
    private static final Logger LOGGER =
            org.geotools.util.logging.Logging.getLogger("org.vfny.geoserver.servlets");

    /** Initializes logging and config. */
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
    public void doGet(HttpServletRequest req, HttpServletResponse res) {
        // BasicConfigurator.configure();
    }

    /**
     * Closes down the zserver if it is running, and frees up resources.
     *
     * @task REVISIT: what we should consider is having geotools provide a nicer way to clean up
     *     datastores's resources, something like a close, so that we could just iterate through all
     *     the datastores calling close. Once that's done we can clean up this method a bit.
     */
    public void destroy() {
        super.destroy();
        // ConnectionPoolManager.getInstance().closeAll();

        /*
          HACK: we must get a standard API way for releasing resources...
        */
        try {
            Class sdepfClass = Class.forName("org.geotools.data.arcsde.ConnectionPoolFactory");

            LOGGER.fine("SDE datasource found, releasing resources");

            java.lang.reflect.Method m = sdepfClass.getMethod("getInstance", new Class[0]);
            Object pfInstance = m.invoke(sdepfClass, new Object[0]);

            LOGGER.fine("got sde connection pool factory instance: " + pfInstance);

            java.lang.reflect.Method closeMethod =
                    pfInstance.getClass().getMethod("closeAll", new Class[0]);

            closeMethod.invoke(pfInstance, new Object[0]);
            LOGGER.info("just asked SDE datasource to release connections");
        } catch (ClassNotFoundException cnfe) {
            LOGGER.fine("No SDE datasource found");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
