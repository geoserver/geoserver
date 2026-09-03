/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.security.workspaceadmin.WorkspaceAdminRestfulDefinitionSource;
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;

/**
 * A proxy implementation of {@link FilterInvocationSecurityMetadataSource} that delegates to a list of other
 * FilterInvocationSecurityMetadataSource instances.
 *
 * <p>This allows combining multiple security metadata sources, such as the standard {@link RESTfulDefinitionSource} and
 * the {@link WorkspaceAdminRestfulDefinitionSource}, to handle different authorization scenarios.
 */
@SuppressWarnings({"deprecation", "removal"})
public class RESTfulDefinitionSourceProxy implements FilterInvocationSecurityMetadataSource {

    private List<FilterInvocationSecurityMetadataSource> delegates;

    public RESTfulDefinitionSourceProxy(List<FilterInvocationSecurityMetadataSource> delegates) {
        this.delegates = delegates;
    }

    @Override
    public boolean supports(Class<?> clazz) {
        for (FilterInvocationSecurityMetadataSource delegate : delegates) {
            if (delegate.supports(clazz)) {
                return true;
            }
        }
        return false;
    }

    /** Returns the combined security attributes from all delegates. */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        if (1 == delegates.size()) return delegates.get(0).getAttributes(object);
        return delegates.stream()
                .map(d -> d.getAttributes(object))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /** Returns the combined config attributes from all delegates. */
    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        if (1 == delegates.size()) return delegates.get(0).getAllConfigAttributes();
        return delegates.stream()
                .map(d -> d.getAllConfigAttributes())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
