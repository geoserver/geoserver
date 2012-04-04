package org.geoserver.security.validation;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.MasterPasswordConfig;

public class MasterPasswordConfigValidator extends AbstractSecurityValidator {

    public MasterPasswordConfigValidator( GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

    public void validateMasterPasswordConfig(MasterPasswordConfig config) 
        throws SecurityConfigException {
        
    }
}
