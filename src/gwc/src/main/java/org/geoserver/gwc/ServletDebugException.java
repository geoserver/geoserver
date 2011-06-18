/** 
 * Copyright (c) 2001 - 2009 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Arne Kepp / OpenGeo
 */
package org.geoserver.gwc;

/**
 * This class is just used to detect what methods GeoServer actually invokes
 */
class ServletDebugException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    ServletDebugException() {

    }
}
