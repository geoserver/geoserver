/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.platform;

import org.springframework.context.ApplicationContext;
import org.springframework.context.event.ApplicationContextEvent;

/**
 * Custom application context event that is fired after the application context is 
 * successfully loaded.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ContextLoadedEvent extends ApplicationContextEvent {

    public ContextLoadedEvent(final ApplicationContext source) {
        super(source);
    }

}
