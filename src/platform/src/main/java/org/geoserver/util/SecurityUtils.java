/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */

package org.geoserver.util;

/**
 * Common security utility mehtods 
 * 
 * @author mcr
 *
 */
public class SecurityUtils {

    /**
     * Spring Secruity 3.x drops the common base security exception
     * class SpringSecurityException, now the test is based on the package
     * name
     * 
     * @param t, the exception to check
     * @return true if the exception is caused by Spring Security
     */
    public static boolean  isSecurityException(Throwable t) {
        return t != null && t.getClass().getPackage().getName()
            .startsWith("org.springframework.security");
    }

}
