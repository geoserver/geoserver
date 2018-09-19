/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import com.thoughtworks.xstream.mapper.Mapper;
import com.thoughtworks.xstream.mapper.PackageAliasingMapper;

/**
 * Variant of {@link PackageAliasingMapper} that only applies aliases when reading a serialized
 * representation. When writing to a serialized representation it delegates to the wrapped {@link
 * Mapper}
 */
public class OneWayPackageAliasingMapper extends PackageAliasingMapper {
    private final Mapper wrapped;

    public OneWayPackageAliasingMapper(Mapper wrapped) {
        super(wrapped);
        this.wrapped = wrapped;
    }

    @Override
    public String serializedClass(final Class type) {
        return wrapped.serializedClass(type);
    }
}
