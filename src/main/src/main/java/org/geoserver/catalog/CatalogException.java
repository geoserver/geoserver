/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * Exception class for catalog.
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class CatalogException extends RuntimeException {

    public CatalogException() {
        super();
    }

    public CatalogException(String message) {
        super(message);
    }

    public CatalogException(Throwable cause) {
        super(cause);
    }

    public CatalogException(String message, Throwable cause) {
        super(message, cause);
    }
}
