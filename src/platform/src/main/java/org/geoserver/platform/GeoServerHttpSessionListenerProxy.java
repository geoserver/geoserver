/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.LinkedHashSet;
import java.util.Set;
import javax.servlet.http.HttpSessionEvent;
import javax.servlet.http.HttpSessionListener;

/**
 * This class has to be registered as a listener in the web.xml file.
 *
 * <p>The class holds a set of {@link HttpSessionListener} objects dispatching session
 * creation/termination events to each registered listeners.
 *
 * <p>Listeners can be injected using the Spring context
 *
 * @author christian
 */
public class GeoServerHttpSessionListenerProxy implements HttpSessionListener {

    protected volatile Set<HttpSessionListener> listeners;

    /**
     * This constructor should be called only once by the J2EE container.
     *
     * <p>No further objects of this types should be created,
     */
    public GeoServerHttpSessionListenerProxy() {}

    @Override
    public void sessionCreated(HttpSessionEvent se) {
        for (HttpSessionListener listener : listeners()) {
            listener.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        for (HttpSessionListener listener : listeners()) {
            listener.sessionDestroyed(se);
        }
    }

    public boolean contains(HttpSessionListener listener) {
        return listeners.contains(listener);
    }

    protected Set<HttpSessionListener> listeners() {
        if (listeners == null) {
            synchronized (this) {
                if (listeners == null) {
                    listeners =
                            new LinkedHashSet<HttpSessionListener>(
                                    GeoServerExtensions.extensions(HttpSessionListener.class));
                }
            }
        }
        return listeners;
    }
}
