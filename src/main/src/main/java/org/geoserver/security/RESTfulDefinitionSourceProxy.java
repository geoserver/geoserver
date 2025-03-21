/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
public class RESTfulDefinitionSourceProxy implements FilterInvocationSecurityMetadataSource {

    private List<FilterInvocationSecurityMetadataSource> delegates;

    /**
     * Creates a new proxy with the given list of delegate metadata sources.
     *
     * @param delegates the list of metadata sources to delegate to
     */
    public RESTfulDefinitionSourceProxy(List<FilterInvocationSecurityMetadataSource> delegates) {
        this.delegates = delegates;
    }

    /**
     * Returns true if any of the delegate metadata sources support the given class.
     *
     * @param clazz the class to check for support
     * @return true if any delegate supports the class, false otherwise
     */
    @Override
    public boolean supports(Class<?> clazz) {
        for (FilterInvocationSecurityMetadataSource delegate : delegates) {
            if (delegate.supports(clazz)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns the combined security attributes from all delegate metadata sources. Optimizes for the common case of a
     * single delegate.
     *
     * @param object the object to get attributes for
     * @return a collection of security config attributes from all delegates
     * @throws IllegalArgumentException if the object is not supported
     */
    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        if (1 == delegates.size()) return delegates.get(0).getAttributes(object);
        return delegates.stream()
                .map(d -> d.getAttributes(object))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    /**
     * Returns the combined config attributes from all delegate metadata sources. Optimizes for the common case of a
     * single delegate.
     *
     * @return a collection of all config attributes from all delegates
     */
    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        if (1 == delegates.size()) return delegates.get(0).getAllConfigAttributes();
        return delegates.stream()
                .map(d -> d.getAllConfigAttributes())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
