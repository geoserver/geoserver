/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import javax.servlet.ServletContextEvent;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.ContextLoaderListener;

/**
 * Custom context loader listener that emits a {@link ContextLoadedEvent} once the application
 * context has been successfully loaded.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerContextLoaderListener extends ContextLoaderListener {

    @Override
    public void contextInitialized(ServletContextEvent event) {
        super.contextInitialized(event);
        ApplicationContext appContext = getCurrentWebApplicationContext();
        if (appContext != null) {
            appContext.publishEvent(new ContextLoadedEvent(appContext));
        }
    }
}
