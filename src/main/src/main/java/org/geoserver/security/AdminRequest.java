/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * Thread local that maintains the state of an administrative request.
 *
 * <p>Such requests are typically used to configure the server, be it via the web ui, restconfig,
 * etc...
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class AdminRequest {

    static ThreadLocal<Object> REQUEST = new ThreadLocal<Object>();

    public static void start(Object request) {
        REQUEST.set(request);
    }

    public static void abort() {
        REQUEST.remove();
    }

    public static Object get() {
        return REQUEST.get();
    }

    public static void finish() {
        REQUEST.remove();
    }
}
