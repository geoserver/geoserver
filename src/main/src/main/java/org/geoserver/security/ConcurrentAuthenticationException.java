/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown by concurrent login attempts during the quiet period of a failed login
 *
 * @author Andrea Aime - GeoSolutions
 */
public class ConcurrentAuthenticationException extends AuthenticationException {

    private static final long serialVersionUID = 6692144768515314827L;

    private String user;

    private int count;

    public ConcurrentAuthenticationException(String user, int count) {
        super(
                "Concurrent login attempts during delay period not allowed, stopped "
                        + count
                        + " attempts. If you see a large number of blocked attempts, a brute force attack to crack this user's password may be underway.");
        this.user = user;
        this.count = count;
    }

    public String getUser() {
        return user;
    }

    public int getCount() {
        return count;
    }
}
