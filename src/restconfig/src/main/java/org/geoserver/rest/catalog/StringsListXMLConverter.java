/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import com.thoughtworks.xstream.XStream;
import java.io.IOException;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.stereotype.Component;

/** Write a named {@link StringsList} to XML. */
@Component
public class StringsListXMLConverter extends BaseMessageConverter<StringsList> {
    public StringsListXMLConverter() {
        super(MediaType.TEXT_XML, MediaType.APPLICATION_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return StringsList.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    //
    // writing
    //
    @Override
    public void writeInternal(StringsList stringsList, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        XStream xstream = new SecureXStream();
        xstream.alias(stringsList.getAlias(), String.class);
        xstream.toXML(stringsList.getValues(), outputMessage.getBody());
    }
}
