/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.lang.Nullable;
import org.springframework.oxm.jaxb.Jaxb2Marshaller;

public class MapMLMessageConverter extends BaseMessageConverter<Object> {

    @Autowired private Jaxb2Marshaller mapmlMarshaller;

    public MapMLMessageConverter() {
        super(MapMLConstants.MAPML_MEDIA_TYPE);
    }

    @Override
    public boolean canRead(Class<?> clazz, @Nullable MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class<?> clazz, @Nullable MediaType mediaType) {
        return (canWrite(mediaType) && mapmlMarshaller.supports(clazz));
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // should not be called, since we override canRead()/canWrite()
        throw new UnsupportedOperationException();
    }

    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws UnsupportedEncodingException, IOException {
        OutputStreamWriter osw =
                new OutputStreamWriter(
                        outputMessage.getBody(), geoServer.getSettings().getCharset());
        Result result = new StreamResult(osw);
        mapmlMarshaller.marshal(o, result);
        osw.flush();
    }
}
