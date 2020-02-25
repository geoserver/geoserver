/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.http.MediaType;

/**
 * GeoServer extension of MappingJackson2HttpMessageConverter allowing to mark a bean so that it
 * won't get serialized
 */
public class MappingJackson2HttpMessageConverter
        extends org.springframework.http.converter.json.MappingJackson2HttpMessageConverter {

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (clazz.getAnnotation(JsonIgnoreType.class) != null
                || OpenAPI.class.isAssignableFrom(clazz)) {
            return false;
        }
        return super.canWrite(clazz, mediaType);
    }
}
