/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

/**
 * A custom Swagger converter that uses the Jackson 2 libraries configured by Swagger for serialization of OpenAPI
 * objects *
 */
@Component
@SuppressWarnings("removal")
public class SwaggerJSONSchemaMessageConverter extends AbstractJackson2HttpMessageConverter {

    public static final String SCHEMA_TYPE_VALUE = "application/schema+json";
    public static final MediaType SCHEMA_TYPE = MediaType.parseMediaType(SCHEMA_TYPE_VALUE);

    public SwaggerJSONSchemaMessageConverter() {
        super(Json.mapper(), SCHEMA_TYPE);
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return (Schema.class.isAssignableFrom(clazz)) && super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return (Schema.class.isAssignableFrom(clazz)) && super.canWrite(clazz, mediaType);
    }
}
