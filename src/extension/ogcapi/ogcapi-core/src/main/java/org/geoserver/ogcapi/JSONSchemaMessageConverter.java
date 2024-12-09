/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.media.Schema;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

/** A custom JSON converter that uses the JSON schema mime type */
@Component
public class JSONSchemaMessageConverter extends AbstractJackson2HttpMessageConverter {

    public static final String SCHEMA_TYPE_VALUE = "application/schema+json";
    public static final MediaType SCHEMA_TYPE = MediaType.parseMediaType(SCHEMA_TYPE_VALUE);

    public JSONSchemaMessageConverter() {
        super(getMapper(), SCHEMA_TYPE);
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = Json.mapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writer(new DefaultPrettyPrinter());
        return mapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return SCHEMA_TYPE.isCompatibleWith(mediaType) && Schema.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (mediaType == null || SCHEMA_TYPE.isCompatibleWith(mediaType))
                && Schema.class.isAssignableFrom(clazz);
    }
}
