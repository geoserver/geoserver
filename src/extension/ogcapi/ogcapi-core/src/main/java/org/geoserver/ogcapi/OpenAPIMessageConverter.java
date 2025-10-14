/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.core.util.Json;
import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;
import org.springframework.stereotype.Component;

/** A custom JSON converter that uses the Open API mime type */
@Component
public class OpenAPIMessageConverter extends AbstractJackson2HttpMessageConverter {

    public static final String OPEN_API_MEDIA_TYPE_VALUE = "application/vnd.oai.openapi+json;version=3.0";
    public static final MediaType OPEN_API_MEDIA_TYPE = MediaType.parseMediaType(OPEN_API_MEDIA_TYPE_VALUE);

    public OpenAPIMessageConverter() {
        super(getMapper(), OPEN_API_MEDIA_TYPE);
    }

    public static ObjectMapper getMapper() {
        ObjectMapper mapper = Json.mapper();
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writer(new DefaultPrettyPrinter());
        return mapper;
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return OPEN_API_MEDIA_TYPE.isCompatibleWith(mediaType) && OpenAPI.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (mediaType == null || OPEN_API_MEDIA_TYPE.isCompatibleWith(mediaType))
                && OpenAPI.class.isAssignableFrom(clazz);
    }
}
