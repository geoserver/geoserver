/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import org.springframework.security.core.Authentication;

/**
 * Cache entry implementation for {@link Authentication} objects
 *
 * @author christian
 */
public class AuthenticationCacheEntry {
    /** The Spring authentication to cache */
    private Authentication authentication;
    /** Time in seconds. The entry expires if (last accessed time + idle time) < current time */
    private int timeToIdleSeconds;

    /** Time in seconds, The entry expires if (creation time + live time) < current time */
    private int timeToLiveSeconds;

    /** Time stamp of last access in milliseconds */
    private long lastAccessed;
    /** Time stamp of creation in milliseconds */
    private long created;

    public AuthenticationCacheEntry(
            Authentication authentication, int timeToIdleSeconds, int timeToLiveSeconds) {
        super();

        this.authentication = authentication;
        this.timeToIdleSeconds = timeToIdleSeconds;
        this.timeToLiveSeconds = timeToLiveSeconds;
        created = lastAccessed = System.currentTimeMillis();
    }

    public Authentication getAuthentication() {
        return authentication;
    }

    public int getTimeToIdleSeconds() {
        return timeToIdleSeconds;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public long getCreated() {
        return created;
    }

    public long getLastAccessed() {
        return lastAccessed;
    }

    public void setLastAccessed(long lastAccessed) {
        this.lastAccessed = lastAccessed;
    }

    /** returns true if the entry has expired, false otherwise */
    public boolean hasExpired(long timeInMilliSecs) {
        if (lastAccessed + timeToIdleSeconds * 1000 < timeInMilliSecs) return true;
        if (created + timeToLiveSeconds * 1000 < timeInMilliSecs) return true;
        return false;
    }

    @Override
    public int hashCode() {
        return authentication == null ? 0 : authentication.hashCode();
    }

    @Override
    public boolean equals(Object o) {

        if (this == o) return true;

        if (o instanceof AuthenticationCacheEntry == false) return false;

        AuthenticationCacheEntry other = (AuthenticationCacheEntry) o;
        if (authentication == other.authentication) return true;
        if (authentication == null || other.authentication == null) return false;
        return authentication.equals(other.authentication);
    }
}
