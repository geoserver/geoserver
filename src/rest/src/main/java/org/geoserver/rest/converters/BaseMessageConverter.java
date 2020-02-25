/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import java.io.IOException;
import java.nio.charset.Charset;
import org.geoserver.catalog.Catalog;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersisterFactory;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.platform.GeoServerExtensions;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractHttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Base message converter behavior for XStream XML or JSON converters.
 *
 * <p>Local fields have been provided for {@link #catalog} and {@link #geoServer} access.
 */
public abstract class BaseMessageConverter<T> extends AbstractHttpMessageConverter<T>
        implements HttpMessageConverter<T>, ExtensionPriority {

    protected final Catalog catalog;

    protected final XStreamPersisterFactory xpf;

    protected final GeoServer geoServer;

    //    /**
    //     * Construct an {@code BaseMessageConverter} with no supported media types.
    //     * @see #setSupportedMediaTypes
    //     */
    //    public BaseMessageConverter() {
    //        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
    //        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
    //        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    //    }

    /**
     * Construct an {@code BaseMessageConverter} with supported media types.
     *
     * @param supportedMediaTypes the supported media types
     */
    protected BaseMessageConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }

    /**
     * Construct an {@code BaseMessageConverter} with a default charset and supported media types.
     *
     * @param defaultCharset the default character set
     * @param supportedMediaTypes the supported media types
     */
    protected BaseMessageConverter(Charset defaultCharset, MediaType... supportedMediaTypes) {
        super(defaultCharset, supportedMediaTypes);
        this.catalog = (Catalog) GeoServerExtensions.bean("catalog");
        this.xpf = GeoServerExtensions.bean(XStreamPersisterFactory.class);
        this.geoServer = GeoServerExtensions.bean(GeoServer.class);
    }

    /** Returns the priority of the {@link BaseMessageConverter}. */
    public int getPriority() {
        return ExtensionPriority.LOWEST;
    }

    //    /**
    //     * Checks if the media type provided is "included" by one of the media types declared
    //     * in {@link #getSupportedMediaTypes()}
    //     */
    //    protected boolean isSupportedMediaType(MediaType mediaType) {
    //        for (MediaType supported : getSupportedMediaTypes()) {
    //            if(supported.includes(mediaType)) {
    //                return true;
    //            }
    //        }
    //        return false;
    //    }

    //    @Override
    //    protected T readInternal(Class<? extends T> clazz,
    //            HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException
    // {
    //        throw new HttpMessageNotReadableException(getClass().getName()+" does not support
    // deserialization");
    //    }
    //
    //    @Override
    //    protected void writeInternal(T t, HttpOutputMessage outputMessage)
    //            throws IOException, HttpMessageNotWritableException {
    //        throw new HttpMessageNotReadableException(getClass().getName()+" does not support
    // serialization");
    //    }

    /* Default implementation provided for consistent not-implemented message */
    @Override
    protected T readInternal(Class<? extends T> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                getClass().getName() + " does not support deserialization", inputMessage);
    }

    /* Default implementation provided for consistent not-implemented message */
    @Override
    protected void writeInternal(T t, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        throw new HttpMessageNotWritableException(
                getClass().getName() + " does not support serialization");
    }
}
