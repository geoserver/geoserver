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
import org.springframework.security.access.ConfigAttribute;
import org.springframework.security.web.access.intercept.FilterInvocationSecurityMetadataSource;

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

    @Override
    public Collection<ConfigAttribute> getAttributes(Object object) throws IllegalArgumentException {
        if (1 == delegates.size()) return delegates.get(0).getAttributes(object);
        return delegates.stream()
                .map(d -> d.getAttributes(object))
                .filter(Objects::nonNull)
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }

    @Override
    public Collection<ConfigAttribute> getAllConfigAttributes() {
        if (1 == delegates.size()) return delegates.get(0).getAllConfigAttributes();
        return delegates.stream()
                .map(d -> d.getAllConfigAttributes())
                .flatMap(Collection::stream)
                .collect(Collectors.toList());
    }
}
