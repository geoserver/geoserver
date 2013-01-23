/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.validation.SecurityConfigValidator;

public class MemorySecurityConfigValidator extends SecurityConfigValidator {

    public MemorySecurityConfigValidator(GeoServerSecurityManager securityManager) {
        super(securityManager);
    }

}
