/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.ogcapi;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.media.Schema;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/** A custom JSON converter that uses the JSON schema mime type */
@Component
public class JSONSchemaMessageConverter extends JacksonJsonHttpMessageConverter {

    public static final String SCHEMA_TYPE_VALUE = "application/schema+json";
    public static final MediaType SCHEMA_TYPE = MediaType.parseMediaType(SCHEMA_TYPE_VALUE);

    public JSONSchemaMessageConverter() {
        super(buildMapper());
        setSupportedMediaTypes(List.of(SCHEMA_TYPE));
    }

    public static JsonMapper buildMapper() {
        return JsonMapper.builder()
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return SCHEMA_TYPE.isCompatibleWith(mediaType) && Schema.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return (mediaType == null || SCHEMA_TYPE.isCompatibleWith(mediaType)) && Schema.class.isAssignableFrom(clazz);
    }
}
