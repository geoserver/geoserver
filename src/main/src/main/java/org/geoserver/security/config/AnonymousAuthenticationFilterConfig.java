/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.config;

import java.io.Serial;
import org.geoserver.security.filter.GeoServerAnonymousAuthenticationFilter;

/**
 * {@link GeoServerAnonymousAuthenticationFilter} configuration object.
 *
 * @author mcr
 */
public class AnonymousAuthenticationFilterConfig extends SecurityFilterConfig implements SecurityAuthFilterConfig {

    @Serial
    private static final long serialVersionUID = 1L;
}
