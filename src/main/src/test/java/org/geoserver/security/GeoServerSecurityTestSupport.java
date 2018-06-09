/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
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

    /** Accessor for the geoserver master password. */
    protected String getMasterPassword() {
        return new String(getSecurityManager().getMasterPassword());
    }
}
