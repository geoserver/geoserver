package org.geoserver.rest.converters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;

/**
 * Message converter implementation for XML serialization via XStream
 */
public class XStreamXMLMessageConverter extends XStreamMessageConverter {

    public XStreamXMLMessageConverter() {
        super();
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return !RestListWrapper.class.isAssignableFrom(clazz) && isSupportedMediaType(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return !RestListWrapper.class.isAssignableFrom(clazz) && RestWrapper.class.isAssignableFrom(clazz)
                && isSupportedMediaType(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createXMLPersister();
        if (inputMessage instanceof RestHttpInputWrapper) {
            ((RestHttpInputWrapper) inputMessage).configurePersister(p, this);
        }
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
        if (o instanceof RestWrapper) {
            ((RestWrapper) o).configurePersister(xmlPersister, this);
            o = ((RestWrapper) o).getObject();
        }
        xmlPersister.save(o, outputMessage.getBody());
    }
    
    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_XML_VALUE;
    }

    @Override
    public String getExtension() {
        return "xml";
    }
    
    @Override
    protected XStream createXStreamInstance() {
        return new SecureXStream();
    }

    @Override
    public void encodeLink(String link, HierarchicalStreamWriter writer) {
        encodeAlternateAtomLink(link, writer);
    }
    
    @Override
    public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
        encodeAlternateAtomLink(link, writer);
    }
}
