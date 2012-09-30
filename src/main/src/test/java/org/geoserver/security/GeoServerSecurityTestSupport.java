/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.test.GeoServerSystemTestSupport;

/**
 * Test support class providing additional accessors for security related beans.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerSecurityTestSupport extends GeoServerSystemTestSupport {

    /**
     * Accessor for the geoserver master password.
     * @return
     */
    protected String getMasterPassword() {
        return new String(getSecurityManager().getMasterPassword());
    }
}
