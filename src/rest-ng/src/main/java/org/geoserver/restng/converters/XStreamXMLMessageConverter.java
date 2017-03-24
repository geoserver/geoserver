package org.geoserver.restng.converters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.restng.catalog.wrapper.XStreamListWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Message converter implementation for XML serialization via XStream
 */
public class XStreamXMLMessageConverter extends BaseMessageConverter {

    public XStreamXMLMessageConverter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return !XStreamListWrapper.class.isAssignableFrom(clazz) &&
            MediaType.APPLICATION_XML.equals(mediaType) || MediaType.TEXT_XML.equals(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return !XStreamListWrapper.class.isAssignableFrom(clazz)
            && MediaType.APPLICATION_XML.equals(mediaType) || MediaType.TEXT_XML.equals(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createXMLPersister();
        p.setCatalog(catalog);
        return p.load(inputMessage.getBody(), clazz);
    }


    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createXMLPersister();
        xmlPersister.setCatalog(catalog);
        xmlPersister.setReferenceByName(true);
        xmlPersister.setExcludeIds();
        xmlPersister.save(o, outputMessage.getBody());
    }
}
