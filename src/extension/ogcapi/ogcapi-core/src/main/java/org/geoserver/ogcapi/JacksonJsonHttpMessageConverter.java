/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.MediaType;
import tools.jackson.databind.json.JsonMapper;

/**
 * GeoServer extension of MappingJackson2HttpMessageConverter allowing to mark a bean so that it won't get serialized
 */
public class JacksonJsonHttpMessageConverter
        extends org.springframework.http.converter.json.JacksonJsonHttpMessageConverter {

    public JacksonJsonHttpMessageConverter() {
        super(buildMapper());
    }

    private static JsonMapper buildMapper() {
        return JsonMapper.builder()
                .addModule(new JtsModule())
                .addModule(new CloseableIteratorModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .build();
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        if (!canJacksonHandle(clazz)) return false;

        return super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if (!canJacksonHandle(clazz)) return false;

        return super.canWrite(clazz, mediaType);
    }

    static boolean canJacksonHandle(Class<?> clazz) {
        return clazz.getAnnotation(JsonIgnoreType.class) == null
                && !RestWrapper.class.isAssignableFrom(clazz)
                && !OpenAPI.class.isAssignableFrom(clazz)
                && !Schema.class.isAssignableFrom(clazz);
    }
}
