package com.boundlessgeo.gsr.api;

import com.boundlessgeo.gsr.model.GSRModel;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.MediaType;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.Charset;

/**
 * Abstract class for all {@link GSRModel} converters
 */
public abstract class AbstractGeoServicesConverter extends BaseMessageConverter<GSRModel> {

    protected AbstractGeoServicesConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    protected AbstractGeoServicesConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
        super(defaultCharset, supportedMediaTypes);
    }

    /**
     * Serializes an object in a context-insensitive way, so that it can be called outside of the regular request
     * handling hierarchy.
     * Mainly used for exception handling; see {@link FormatParameterInterceptor} and {@link GeoServicesExceptionResolver}.
     *
     * @param os {@link OutputStream} to write to
     * @param o Object to write
     * @throws IOException
     */
    public abstract void writeToOutputStream(OutputStream os, Object o) throws IOException;
}
