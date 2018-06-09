/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc;

/** This class is just used to detect what methods GeoServer actually invokes */
class ServletDebugException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ServletDebugException() {}
}
