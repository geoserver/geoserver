/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.vfny.geoserver.global;

import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.web.context.WebApplicationContext;

/**
 * Places a GeoServer module into the servlet context.
 *
 * <p>This class is only around to maintain backwards compatability for hte struts ui stuff which
 * requires application modules to be placed into the servlet context.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class GeoServerServletContextInitializer implements ApplicationContextAware {
    /** The key to register the object under. */
    String key;

    /** The object to register. */
    Object object;

    public GeoServerServletContextInitializer(String key, Object object) {
        this.key = key;
        this.object = object;
    }

    public void setApplicationContext(ApplicationContext context) throws BeansException {
        if (context instanceof WebApplicationContext) {
            WebApplicationContext webContext = (WebApplicationContext) context;
            webContext.getServletContext().setAttribute(key, object);
        }
    }
}
