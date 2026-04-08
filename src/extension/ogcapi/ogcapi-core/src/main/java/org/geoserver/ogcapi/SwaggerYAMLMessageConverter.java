/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.http.converter.yaml.MappingJackson2YamlHttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * A custom YAML converter that uses the Jackson 2 libraries configured by Swagger for serialization of the main Swagger
 * objects
 */
@Component
@SuppressWarnings("removal")
public class SwaggerYAMLMessageConverter extends MappingJackson2YamlHttpMessageConverter {

    public SwaggerYAMLMessageConverter() {
        super(Yaml.mapper());
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return (Schema.class.isAssignableFrom(clazz) || OpenAPI.class.isAssignableFrom(clazz))
                && super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return (Schema.class.isAssignableFrom(clazz) || OpenAPI.class.isAssignableFrom(clazz))
                && super.canWrite(clazz, mediaType);
    }
}
