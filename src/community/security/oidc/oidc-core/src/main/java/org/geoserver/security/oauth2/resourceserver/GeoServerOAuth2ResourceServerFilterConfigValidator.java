/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.oauth2.resourceserver;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.oauth2.common.GeoServerOAuth2FilterConfigException;
import org.geoserver.security.validation.FilterConfigValidator;

/**
 * Validator for {@link GeoServerOAuth2ResourceServerAuthenticationFilter}.
 *
 * <p>Used for the "Resource Server" use case. Implementation is unfinished, because a different GS extension supports
 * this case already. Filter is not offered in UI. This code is never executed.
 */
public class GeoServerOAuth2ResourceServerFilterConfigValidator extends FilterConfigValidator {

    public GeoServerOAuth2ResourceServerFilterConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    @Override
    protected GeoServerOAuth2FilterConfigException createFilterException(String errorid, Object... args) {
        return new GeoServerOAuth2FilterConfigException(errorid, args);
    }
}
