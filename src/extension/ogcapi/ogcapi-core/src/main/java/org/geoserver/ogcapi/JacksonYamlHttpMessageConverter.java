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
import java.io.IOException;
import java.util.Map;
import org.geoserver.rest.wrapper.RestWrapper;
import org.jspecify.annotations.Nullable;
import org.springframework.core.ResolvableType;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.dataformat.yaml.YAMLWriteFeature;

/**
 * GeoServer extension of MappingJackson2HttpMessageConverter allowing to mark a bean so that it won't get serialized
 */
public class JacksonYamlHttpMessageConverter
        extends org.springframework.http.converter.yaml.JacksonYamlHttpMessageConverter {

    public JacksonYamlHttpMessageConverter() {
        super(buildMapper());
    }

    private static YAMLMapper buildMapper() {
        return YAMLMapper.builder()
                .addModule(new JtsModule())
                .addModule(new CloseableIteratorModule())
                .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                .disable(YAMLWriteFeature.WRITE_DOC_START_MARKER)
                .enable(YAMLWriteFeature.MINIMIZE_QUOTES)
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

    @Override
    protected void writeInternal(
            Object object,
            ResolvableType resolvableType,
            HttpOutputMessage outputMessage,
            @Nullable Map<String, Object> hints)
            throws IOException, HttpMessageNotWritableException {
        ContentDisposition disposition = ContentDisposition.builder("inline")
                .filename(object.getClass().getSimpleName() + ".yaml")
                .build();
        outputMessage.getHeaders().setContentDisposition(disposition);

        super.writeInternal(object, resolvableType, outputMessage, hints);
    }
}
