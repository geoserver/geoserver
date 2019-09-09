/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.api;

import java.io.IOException;
import java.lang.reflect.Type;
import org.geoserver.platform.ExtensionPriority;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.AbstractGenericHttpMessageConverter;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Because binary means binary (aka dump the damn thing as is) * */
@Component
public class ByteArrayMessageConverter extends AbstractGenericHttpMessageConverter<byte[]>
        implements ExtensionPriority {

    public static final String OPEN_API_VALUE = "application/openapi+json;version=3.0";
    public static final MediaType OPEN_API = MediaType.parseMediaType(OPEN_API_VALUE);

    public ByteArrayMessageConverter() {
        super(MediaType.ALL);
    }

    @Override
    protected void writeInternal(byte[] bytes, Type type, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        outputMessage.getBody().write(bytes);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return byte[].class.equals(clazz);
    }

    @Override
    protected byte[] readInternal(Class<? extends byte[]> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading is not supported");
    }

    @Override
    public byte[] read(Type type, Class<?> contextClass, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Reading is not supported");
    }

    @Override
    public int getPriority() {
        // dodge other converters that are based on string conversion
        return ExtensionPriority.HIGHEST;
    }
}
