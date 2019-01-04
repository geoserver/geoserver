/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.flow.controller;

import org.geoserver.ows.Request;

/**
 * Associates a request to a unique key that identifies the user making it (could use auth, cookie,
 * ip address)
 *
 * @author Andrea Aime - GeoSolutions
 */
public interface KeyGenerator {

    public String getUserKey(Request request);
}
