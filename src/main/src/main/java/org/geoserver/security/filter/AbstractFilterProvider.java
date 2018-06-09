/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.filter;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerSecurityProvider;
import org.geoserver.security.validation.FilterConfigValidator;
import org.geoserver.security.validation.SecurityConfigValidator;

/**
 * Base provider class for filters
 *
 * @author mcr
 */
public class AbstractFilterProvider extends GeoServerSecurityProvider {

    @Override
    public SecurityConfigValidator createConfigurationValidator(
            GeoServerSecurityManager securityManager) {
        return new FilterConfigValidator(securityManager);
    }
}
