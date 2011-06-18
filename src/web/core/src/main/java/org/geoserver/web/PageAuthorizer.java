/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import org.springframework.security.Authentication;;

/**
 * 
 * @deprecated use {@link ComponentAuthorizer}
 */
public interface PageAuthorizer extends ComponentAuthorizer {
    
    /**
     * Checks if the (bookmarkable) page can be accessed
     * @param pageClass The page class to be checked
     * @param authentication The current user
     * @return
     */
    public boolean isAccessAllowed(Class pageClass, Authentication authentication);
}
