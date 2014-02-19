/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;


/**
 * This class has to be registered as a listener
 * in the web.xml file.
 * 
 * The class holds a set of {@link HttpSessionListener} objects
 * dispatching session creation/termination events to each registered
 * listeners.
 * 
 * Listeners can be injected
 * using the Spring context
 *
 * 
 * @author christian
 *
 */
public class GeoServerHttpSessionListenerProxy implements HttpSessionListener {

    private volatile Set<HttpSessionListener> listeners;
    
    /**
     * This constructor should be called only once 
     * by the J2EE container.
     * 
     * No further objects of this types should be created,
     * 
     */
    public GeoServerHttpSessionListenerProxy() {
    }
    
    @Override
    public void sessionCreated(final HttpSessionEvent se) {
        for (HttpSessionListener listener : listeners()) {
            listener.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(final HttpSessionEvent se) {
        for (HttpSessionListener listener : listeners()) {
            listener.sessionDestroyed(se);
        }
    }

    /**
     *
     * @param listener
     * @return
     */
    public boolean contains(final HttpSessionListener listener) {
        return listeners.contains(listener);
    }

    /**
     *
     * @return
     */
    protected Set<HttpSessionListener> listeners() {
        if (listeners == null) {
           synchronized(this) {
               if (listeners == null) {
                   listeners = new
                       LinkedHashSet<HttpSessionListener>(
                               GeoServerExtensions.extensions(HttpSessionListener.class));
               }
           }
        }
        return listeners;
   }
}
