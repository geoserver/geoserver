/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geofence.server.internal;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import org.geoserver.geofence.spi.UserResolver;
import org.springframework.context.annotation.PropertySource;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Links GeoServer users/roles to internal Geofence server
 *
 * @author Niels Charlier
 */
@PropertySource("classpath*:application.properties")
public class SecurityContextUserResolver implements UserResolver {

    @Override
    @SuppressWarnings("deprecation")
    public boolean existsUser(String username) {
        throw new IllegalStateException("This method is deprecated and should not be invoked");
    }

    @Override
    @SuppressWarnings("deprecation")
    public boolean existsRole(String rolename) {
        throw new IllegalStateException("This method is deprecated and should not be invoked");
    }

    @Override
    public Set<String> getRoles(String username) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return (authentication != null && authentication.getAuthorities() != null)
                ? authentication.getAuthorities().stream()
                        .map(GrantedAuthority::getAuthority)
                        .collect(Collectors.toSet())
                : Collections.emptySet();
    }
}
