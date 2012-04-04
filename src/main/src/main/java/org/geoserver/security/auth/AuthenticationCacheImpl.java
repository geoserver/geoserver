/* Copyright (c) 2001 - 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.security.auth;

import org.springframework.security.core.Authentication;

/**
 * @author mcr
 *
 * Null implementation doing nothing
 */
public class AuthenticationCacheImpl implements AuthenticationCache {

    @Override
    public void removeAll() {
    }

    @Override
    public void removeAll(String filterName) {
    }

    @Override
    public void remove(String filterName, String cacheKey) {
    }

    @Override
    public Authentication get(String filterName, String cacheKey) {
        return null;
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth,
            Integer timeToIdleSeconds, Integer timeToLiveSeconds) {
    }

    @Override
    public void put(String filterName, String cacheKey, Authentication auth) {
    }
}
