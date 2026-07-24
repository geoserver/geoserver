/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.security;

import org.geotools.parameter.DefaultParameterDescriptor;

/** Test serializer for {@link CustomParam} - registered as a Spring bean in integration tests. */
public class CustomParamSerializer implements ParameterValueKeySerializer<CustomParam> {

    public static final DefaultParameterDescriptor<CustomParam> DESCRIPTOR =
            new DefaultParameterDescriptor<>("CUSTOM_PARAM", CustomParam.class, null, null);

    @Override
    public Class<CustomParam> getValueType() {
        return CustomParam.class;
    }

    @Override
    public String toKey(CustomParam value) {
        return value.value();
    }
}
