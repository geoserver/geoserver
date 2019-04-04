/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import org.springframework.security.core.AuthenticationException;

/**
 * Exception thrown by the brute force attack prevention when too many threads are blocked already
 *
 * @author Andrea Aime - GeoSolutions
 */
public class MaxBlockedThreadsException extends AuthenticationException {

    private static final long serialVersionUID = -9016181675222375267L;
    private int count;

    public MaxBlockedThreadsException(int count) {
        super(
                "Too many failed logins waiting on delay already: "
                        + count
                        + ". Please wait a bit and try again."
                        + ". A brute force attack to crack user's passwords may be underway.");
        this.count = count;
    }

    public int getCount() {
        return count;
    }
}
