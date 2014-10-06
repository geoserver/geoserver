/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps;

import org.geoserver.platform.ServiceException;

/**
 * WPS Exception class
 *
 * @author Lucas Reed, Refractions Research Inc
 */
public class WPSException extends ServiceException {
    private static final long serialVersionUID = 7833862069590179589L;

    public WPSException(String message) {
        super(message);
    }

    public WPSException(String code, String message) {
        super(message, code);
    }

    public WPSException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public WPSException(String message, Throwable cause) {
        super(message, cause);
    }
}
