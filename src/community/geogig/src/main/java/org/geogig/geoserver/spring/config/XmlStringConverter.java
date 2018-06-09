/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.config;

import java.io.IOException;
import org.geoserver.platform.ExtensionPriority;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.geoserver.rest.util.IOUtils;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.stereotype.Component;

/** Converter that supports the conversion of an xml request body to String. */
@Component
public class XmlStringConverter extends BaseMessageConverter<String> {

    public XmlStringConverter() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @SuppressWarnings("rawtypes")
    @Override
    protected boolean supports(Class clazz) {
        return String.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return String.class.isAssignableFrom(clazz) && canRead(mediaType);
    }

    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    protected String readInternal(Class<? extends String> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        return IOUtils.getStringFromStream(inputMessage.getBody());
    }

    public int getPriority() {
        return ExtensionPriority.HIGHEST;
    }
}
