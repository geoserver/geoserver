/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security;

/**
 * Simple enum for HTTMethods
 *
 * @author christian
 */
public enum HTTPMethod {
    GET("GET"),
    POST("POST"),
    HEAD("HEAD"),
    OPTIONS("OPTIONS"),
    PUT("PUT"),
    DELETE("DELETE"),
    TRACE("TRACE");

    private HTTPMethod(String name) {
        this.name = name;
    }

    private final String name;

    @Override
    public String toString() {
        return name;
    }

    public static HTTPMethod fromString(String method) {
        if ("GET".equals(method)) return GET;
        if ("POST".equals(method)) return POST;
        if ("HEAD".equals(method)) return HEAD;
        if ("OPTIONS".equals(method)) return OPTIONS;
        if ("PUT".equals(method)) return PUT;
        if ("DELETE".equals(method)) return DELETE;
        if ("TRACE".equals(method)) return TRACE;
        throw new RuntimeException("Unknown HTTP method: " + method);
    }
}
