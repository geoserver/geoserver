/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.threadlocals;

import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Moves the Spring Authentication thread local to another thread
 *
 * @author Andrea Aime - GeoSolutions
 */
public class AuthenticationThreadLocalTransfer implements ThreadLocalTransfer {

    private static final String KEY = SecurityContext.class.getName() + "#Authentication";

    @Override
    public void collect(Map<String, Object> storage) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        storage.put(KEY, authentication);
    }

    @Override
    public void apply(Map<String, Object> storage) {
        Authentication authentication = (Authentication) storage.get(KEY);
        if (authentication != null) {
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }
    }

    @Override
    public void cleanup() {
        SecurityContextHolder.getContext().setAuthentication(null);
    }
}
