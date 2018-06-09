/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.security;

import java.io.IOException;
import java.util.Map;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.catalog.MapJSONConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

/** Converts a RuleMap into JSON and back */
@Component
public class RuleMapJSONConverter extends MapJSONConverter {

    @Override
    public int getPriority() {
        // pretty specific, but leave some room for more specific converters just in case
        return (ExtensionPriority.HIGHEST + ExtensionPriority.LOWEST) / 2;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RuleMap.class.isAssignableFrom(clazz);
    }

    @Override
    public Map<?, ?> readInternal(Class<? extends Map<?, ?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // superclass generates a generic JSON map, we need a specific one to please Spring

        @SuppressWarnings("unchecked")
        Map<String, String> source = (Map<String, String>) super.readInternal(clazz, inputMessage);
        return new RuleMap<>(source);
    }
}
