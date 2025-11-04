/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.filter;

import jakarta.servlet.http.HttpServletRequest;
import org.geoserver.security.auth.AuthenticationCache;

/**
 * Filters implementing this interface my use an {@link AuthenticationCache}
 *
 * @author mcr
 */
public interface AuthenticationCachingFilter {

    /**
     * Tries to extract a unique key for the principal If this is not possible, return {@code null} if the principal
     * equals {@link org.geoserver.security.impl.GeoServerUser#ROOT_USERNAME} than {@code null} must be returned (Never
     * cache ROOT_USERNAME user).
     *
     * <p>For pre-authentication filters, the name of the principal is sufficient. All other filters should include some
     * information derived from the credentials, otherwise an attacker could authenticate using only the principal
     * information.
     *
     * <p>As an example, the derived information could be an md5 checksum of the credentials
     *
     * <p>If there is an already existing HTTP Session, this method should return {@code null} If the HTTP request
     * attribute named GeoServerSecurityContextPersistenceFilter.ALLOWSESSIONCREATION_ATTR is true, this method should
     * return {@code null}
     */
    public String getCacheKey(HttpServletRequest request);
}
