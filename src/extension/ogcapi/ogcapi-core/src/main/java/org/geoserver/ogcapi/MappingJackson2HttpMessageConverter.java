/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import com.bedatadriven.jackson.datatype.jts.JtsModule;
import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import java.lang.reflect.Type;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.MediaType;

/**
 * GeoServer extension of MappingJackson2HttpMessageConverter allowing to mark a bean so that it won't get serialized
 */
public class MappingJackson2HttpMessageConverter
        extends org.springframework.http.converter.json.MappingJackson2HttpMessageConverter {

    public MappingJackson2HttpMessageConverter() {
        ObjectMapper mapper = getObjectMapper();
        mapper.registerModule(new JtsModule());
        mapper.registerModule(new CloseableIteratorModule());
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        mapper.writer(new DefaultPrettyPrinter());
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        if (!canJacksonHandle(clazz)) return false;

        return super.canRead(clazz, mediaType);
    }

    @Override
    public boolean canRead(Type type, Class<?> contextClass, MediaType mediaType) {
        // reading wise, the converters are called with simple types (not wrappers),
        // limit this to the OGC API controllers, while the REST ones handle all the
        // other classes, for backwards compatibility
        if (contextClass != null && !contextClass.getPackage().getName().startsWith("org.geoserver.ogcapi"))
            return false;

        return super.canRead(type, contextClass, mediaType);
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
