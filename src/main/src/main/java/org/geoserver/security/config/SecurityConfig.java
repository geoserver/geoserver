/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.config;

import java.io.Serializable;

/**
 * Common base interface for all security configuration classes.
 *
 * @author christian
 */
public interface SecurityConfig extends Serializable {

    /**
     * Clone of a Security Config allowing expansion of placeholders through GeoServerEnvironment
     * helper.
     */
    public SecurityConfig clone(boolean allowEnvParametrization);
}
