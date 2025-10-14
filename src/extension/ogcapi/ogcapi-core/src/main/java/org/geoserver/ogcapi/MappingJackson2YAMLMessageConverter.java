/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi;

import static org.geoserver.ogcapi.MappingJackson2HttpMessageConverter.canJacksonHandle;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import io.swagger.v3.core.util.Yaml;
import java.io.IOException;
import java.lang.reflect.Type;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.http.converter.json.AbstractJackson2HttpMessageConverter;

/** Message converter encoding a Java bean into YAML using Jackson */
public class MappingJackson2YAMLMessageConverter extends AbstractJackson2HttpMessageConverter {

    public static final String APPLICATION_YAML_VALUE = "application/yaml";

    public static final MediaType APPLICATION_YAML = MediaType.parseMediaType(APPLICATION_YAML_VALUE);

    protected MappingJackson2YAMLMessageConverter() {
        super(Yaml.mapper(), APPLICATION_YAML);
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
        return clazz.getAnnotation(JsonIgnoreType.class) == null && !RestWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    protected void writeInternal(Object object, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        ContentDisposition disposition = ContentDisposition.builder("inline")
                .filename(object.getClass().getSimpleName() + ".yaml")
                .build();
        outputMessage.getHeaders().setContentDisposition(disposition);

        super.writeInternal(object, type, outputMessage);
    }
}
