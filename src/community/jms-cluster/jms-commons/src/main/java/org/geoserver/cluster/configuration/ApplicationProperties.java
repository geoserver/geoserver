/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.configuration;

import java.util.logging.Logger;
import javax.servlet.ServletContext;
import org.geotools.util.logging.Logging;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

/**
 * Utility class
 *
 * @author Justin Deoliveira, The Open Planning Project
 * @author Andrea Aime, The Open Planning Project
 */
public class ApplicationProperties implements ApplicationContextAware {

    /** logger */
    public static Logger LOGGER = Logging.getLogger("org.geoserver");

    /** A static application context */
    static ApplicationContext context;

    /**
     * Sets the web application context to be used for looking up extensions.
     *
     * <p>This method is called by the spring container, and should never be called by client code.
     * If client needs to supply a particular context, methods which take a context are available.
     *
     * <p>This is the context that is used for methods which dont supply their own context.
     */
    public void setApplicationContext(ApplicationContext context) throws BeansException {
        ApplicationProperties.context = context;
    }

    /** Checks the context, if null will issue a warning. */
    static void checkContext(ApplicationContext context) {
        if (context == null) {
            LOGGER.severe("Extension lookup occured, but ApplicationContext is unset.");
        }
    }

    /**
     * Looks up for a named string property in the order defined by {@link #getProperty(String,
     * ApplicationContext)} using the internally cached spring application context.
     *
     * <p>Care should be taken when using this method. It should not be called during startup or
     * from tests cases as the internal context will not have been set.
     *
     * @param propertyName The property name to lookup.
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName) {
        return getProperty(propertyName, context);
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters (only works if the context is a {@link WebApplicationContext}
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The Spring context (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ApplicationContext context) {
        if (context instanceof WebApplicationContext) {
            return getProperty(propertyName, ((WebApplicationContext) context).getServletContext());
        } else {
            return getProperty(propertyName, (ServletContext) null);
        }
    }

    /**
     * Looks up for a named string property into the following contexts (in order):
     *
     * <ul>
     *   <li>System Property
     *   <li>web.xml init parameters
     *   <li>Environment variable
     * </ul>
     *
     * and returns the first non null, non empty value found.
     *
     * @param propertyName The property name to be searched
     * @param context The servlet context used to look into web.xml (may be null)
     * @return The property value, or null if not found
     */
    public static String getProperty(String propertyName, ServletContext context) {
        // TODO: this code comes from the data directory lookup and it's useful
        // until we provide a way for the user to manually inspect the three contexts
        // (when trying to debug why the variable they thing they've set, and so on, see also
        // https://osgeo-org.atlassian.net/browse/GEOS-2343
        // Once that is fixed, we can remove the logging code that makes this method more complex
        // than strictly necessary

        final String[] typeStrs = {
            "Java environment variable ",
            "Servlet context parameter ",
            "System environment variable "
        };

        String result = null;
        for (int j = 0; j < typeStrs.length; j++) {
            // Lookup section
            switch (j) {
                case 0:
                    result = System.getProperty(propertyName);
                    break;
                case 1:
                    if (context != null) {
                        result = context.getInitParameter(propertyName);
                    }
                    break;
                case 2:
                    result = System.getenv(propertyName);
                    break;
            }

            if (result == null || result.equalsIgnoreCase("")) {
                LOGGER.finer("Found " + typeStrs[j] + ": '" + propertyName + "' to be unset");
            } else {
                break;
            }
        }

        return result;
    }
}
