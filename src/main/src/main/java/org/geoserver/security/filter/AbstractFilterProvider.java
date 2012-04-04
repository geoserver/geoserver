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
