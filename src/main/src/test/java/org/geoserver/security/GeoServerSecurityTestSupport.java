/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.security;

import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.test.GeoServerTestSupport;

/**
 * Test support class providing additional accessors for security related beans.
 *  
 * @author Justin Deoliveira, OpenGeo
 */
public class GeoServerSecurityTestSupport extends GeoServerTestSupport {

    /**
     * Accessor for plain text password encoder.
     */
    protected GeoServerPlainTextPasswordEncoder getPlainTextPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPlainTextPasswordEncoder.class);
    }

    /**
     * Accessor for digest password encoder.
     */
    protected GeoServerDigestPasswordEncoder getDigestPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerDigestPasswordEncoder.class);
    }

    /**
     * Accessor for regular (weak encryption) pbe password encoder.
     */
    protected GeoServerPBEPasswordEncoder getPBEPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, false);
    }

    /**
     * Accessor for strong encryption pbe password encoder.
     */
    protected GeoServerPBEPasswordEncoder getStrongPBEPasswordEncoder() {
        return getSecurityManager().loadPasswordEncoder(GeoServerPBEPasswordEncoder.class, null, true);
    }

    /**
     * Accessor for the geoserver master password.
     * @return
     */
    protected String getMasterPassword() {
        return new String(getSecurityManager().getMasterPassword());
    }
}
