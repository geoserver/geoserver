/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/**
 * Provides {@link PriorityThreadBlocker} with a priority for the current request, based on some
 * logic (e.g., user roles, HTTP header, request parameter or request type)
 */
public interface PriorityProvider {

    /**
     * Returns the priority associated with the request. Higher values imply higher priority
     *
     * @param request The request whose priority needs to be evaluated
     * @return A priority value (any number will do, higher number, higher priority, this is a user
     *     visible parameter so using a obvious order instead of a computer friendly one)
     */
    int getPriority(Request request);
}
