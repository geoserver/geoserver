/* Copyright (c) 2001 - 2008 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.platform;

import java.util.HashSet;
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
 * Listeners can be added/removed during runtime and be injected
 * using the spring context
 *
 * 
 * @author christian
 *
 */
public class GeoServerHttpSessionListenerProxy implements HttpSessionListener {

    protected Set<HttpSessionListener> listeners;
    protected boolean initialized=false;
    
    static protected GeoServerHttpSessionListenerProxy _singleton;
    
    public static GeoServerHttpSessionListenerProxy getInstance() {
        return _singleton;
    }
    
    /**
     * This constructor should be called only once 
     * by the J2EE container.
     * 
     * No further objects of this types should be created,
     * the singleton can be accessed using {@link #getInstance()}
     * 
     */
    public GeoServerHttpSessionListenerProxy() {
        if (_singleton==null) {
            listeners=new HashSet<HttpSessionListener>();
            initialized=false;
        }
        else {
            listeners=_singleton.listeners;
            initialized=_singleton.initialized;
        }
        
        _singleton=this;
    }
    
    @Override
    public void sessionCreated(HttpSessionEvent se) {
        initialize();
        for (HttpSessionListener listener : listeners) {
            listener.sessionCreated(se);
        }
    }

    @Override
    public void sessionDestroyed(HttpSessionEvent se) {
        initialize();
        for (HttpSessionListener listener : listeners) {
            listener.sessionDestroyed(se);
        }
    }

    public boolean contains(HttpSessionListener listener) {
        return listeners.contains(listener);
    }
    
    /**
     * Adds a listener, return false if
     * listener is already registered
     * 
     * @param listener
     * @return
     */
    public boolean add(HttpSessionListener listener) {
        if (contains(listener)) 
            return false;
        listeners.add(listener);
        return true;
    }
    
    /**
     * remove listener
     * 
     * @param listener
     * @return
     */
    public boolean remove(HttpSessionListener listener) {
        return listeners.remove(listener);
    }
    
    /**
     * Register {@link HttpSessionListener} beans registered
     * in the Spring context 
     */
    void initialize() {        
        if (initialized) return;
        listeners.addAll(GeoServerExtensions.extensions(HttpSessionListener.class));
        initialized=true;
    }
}
