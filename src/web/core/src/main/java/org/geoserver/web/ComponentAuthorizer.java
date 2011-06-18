/* Copyright (c) 2001 - 2010 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.io.Serializable;

import org.springframework.security.Authentication;;

/**
 * Controls access to a component.
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public interface ComponentAuthorizer extends Serializable {

    /**
     * authorizer that always allows access to the component 
     */
    static ComponentAuthorizer ALLOW = new AllowComponentAuthorizer();
    
    /**
     * authorizer that grants access if the user has admin credentials
     */
    static ComponentAuthorizer ADMIN = new AdminComponentAuthorizer();
    
    /**
     * Determines if access is allowed to the component given the specified credentials.
     */
    boolean isAccessAllowed(Class componentClass, Authentication authentication);
}
