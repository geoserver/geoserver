/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.springframework.security.core.Authentication;;


public class DefaultPageAuthorizer extends AdminComponentAuthorizer implements ComponentAuthorizer {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;


    @Override
    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        if (GeoServerSecuredPage.class.isAssignableFrom(componentClass)) {
            return super.isAccessAllowed(componentClass, authentication);
        }
        return true;
    }
}
