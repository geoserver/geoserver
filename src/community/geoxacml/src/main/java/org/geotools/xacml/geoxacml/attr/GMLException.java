/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2008, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.xacml.geoxacml.attr;

/**
 * @author Christian Mueller
 * 
 *         An exception class for GML parsing errors
 */
public class GMLException extends Exception {

    private static final long serialVersionUID = 1L;

    public GMLException() {
    }

    public GMLException(String message) {
        super(message);
    }

    public GMLException(Throwable cause) {
        super(cause);
    }

    public GMLException(String message, Throwable cause) {
        super(message, cause);
    }

}
