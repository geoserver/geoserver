/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.rest;

import java.io.IOException;
import java.util.Map;

import org.geoserver.rest.catalog.MapJSONConverter;
import org.geoserver.platform.ExtensionPriority;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

/**
 * Converts a RuleMap into JSON and back
 */
@Component
public class RuleMapJSONConverter extends MapJSONConverter {

    @Override
    public int getPriority() {
        // pretty specific, but leave some room for more specific converters just in case
        return (ExtensionPriority.HIGHEST + ExtensionPriority.LOWEST) / 2;
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return RuleMap.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return RuleMap.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        // superclass generates a generic JSON map, we need a specific one to please Spring
        Map<String, String> source = (Map<String, String>) super.read(clazz, inputMessage);
        return new RuleMap(source);
    }
}
