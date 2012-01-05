/* Copyright (c) 2001 - 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

/**
 * 
 * Maps a unique authentication key to a user name. If any implementation of this interface is
 * defined in the application context it will be used, otherwise the default implementation, based
 * on a property file, will be used instead.
 * 
 * @author Andrea Aime - GeoSolution
 */
public interface AuthenticationKeyMapper {

    /**
     * Maps the key provided in the request to the name of the corresponding user, or returns null
     * if no corresponding user is found
     * 
     * @param key
     * @return
     */
    String getUserName(String key);
}
