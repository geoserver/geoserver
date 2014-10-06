/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw;

import org.geoserver.platform.ServiceException;

/**
 * CSW Exception class
 *
 * @author Alessio Fabiani, GeoSolutions
 */
public class CSWException extends ServiceException {

    /** serialVersionUID */
    private static final long serialVersionUID = 4265653177643108445L;

    public CSWException(String message) {
        super(message);
    }

    public CSWException(String code, String message) {
        super(message, code);
    }

    public CSWException(String message, String code, String locator) {
        super(message, code, locator);
    }

    public CSWException(String message, Throwable cause) {
        super(message, cause);
    }
}
