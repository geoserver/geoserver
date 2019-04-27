/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import java.io.IOException;
import java.io.InputStream;
import org.apache.commons.io.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Convert InputStream directly to provided output.
 *
 * @author Torben Barsballe (Boundless)
 */
public class InputStreamConverter extends BaseMessageConverter<InputStream> {

    public InputStreamConverter() {
        super(MediaType.ALL);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return InputStream.class.isAssignableFrom(clazz);
    }

    @Override
    protected InputStream readInternal(
            Class<? extends InputStream> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                getClass().getName() + " does not support deserialization", inputMessage);
    }

    @Override
    protected void writeInternal(InputStream inputStream, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        try {
            IOUtils.copy(inputStream, outputMessage.getBody());
        } finally {
            inputStream.close();
        }
    }
}
