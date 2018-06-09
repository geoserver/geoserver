/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import javax.servlet.http.HttpServletRequest;

/**
 * A thread local variable for a {@link HttpServletRequest} that was specified as part of an ows
 * request.
 */
public class LocalHttpServletRequest {

    /** the request thread local */
    static ThreadLocal<HttpServletRequest> request = new ThreadLocal<>();

    public static void set(HttpServletRequest req) {
        request.set(req);
    }

    public static HttpServletRequest get() {
        return request.get();
    }

    public static void remove() {
        request.remove();
    }
}
