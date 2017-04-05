/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class StringsListXMLConverter extends BaseMessageConverter {

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return StringsList.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public List getSupportedMediaTypes() {
        return Arrays.asList(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage) throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void write(Object t, MediaType contentType, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        StringsList stringsList = (StringsList) t;
        XStream xstream = new SecureXStream();
        xstream.alias(stringsList.getAlias(), String.class);
        xstream.toXML(stringsList.getValues(), outputMessage.getBody());
    }
}
