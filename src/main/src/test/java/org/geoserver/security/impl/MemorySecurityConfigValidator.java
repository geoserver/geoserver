package org.geoserver.security.impl;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.validation.SecurityConfigValidator;

public class MemorySecurityConfigValidator extends SecurityConfigValidator {

    public MemorySecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

}
