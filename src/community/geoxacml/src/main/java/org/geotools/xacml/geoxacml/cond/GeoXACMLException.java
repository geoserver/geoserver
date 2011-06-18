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

package org.geotools.xacml.geoxacml.cond;

/**
 * @author Christian Mueller
 * 
 *         Exception class for proccessing erros
 */
public class GeoXACMLException extends Exception {

    private static final long serialVersionUID = 1L;

    public GeoXACMLException() {
    }

    public GeoXACMLException(String message) {
        super(message);
    }

    public GeoXACMLException(Throwable cause) {
        super(cause);
    }

    public GeoXACMLException(String message, Throwable cause) {
        super(message, cause);
    }

}
