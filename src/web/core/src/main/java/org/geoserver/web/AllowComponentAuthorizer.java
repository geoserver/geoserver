/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.springframework.security.Authentication;;

/**
 * Authorizer that allows allows access.
 * 
 * @author Justin Deoliveira, OpenGeo
 */
public class AllowComponentAuthorizer implements ComponentAuthorizer {

    public boolean isAccessAllowed(Class componentClass, Authentication authentication) {
        return true;
    }

}
