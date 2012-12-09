/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
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
