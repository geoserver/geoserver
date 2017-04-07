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
public class XStreamXMLMessageConverter extends XStreamMessageConverter<Object> {

    public XStreamXMLMessageConverter() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        if( RestWrapper.class.isAssignableFrom(clazz) ){
            return !RestListWrapper.class.isAssignableFrom(clazz); // we can only read RestWrapper, not RestListWrapper
        }
        return true; // reading objects is fine
    }
    
    //
    // reading
    //
//    @Override
//    public boolean canRead(Class<?> clazz, MediaType mediaType) {
//        return !RestListWrapper.class.isAssignableFrom(clazz) && canRead(mediaType);
//    }

    
    @Override
    protected Object readInternal(Class<? extends Object> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createXMLPersister();
        if (inputMessage instanceof RestHttpInputWrapper) {
            ((RestHttpInputWrapper) inputMessage).configurePersister(p, this);
        }
        p.setCatalog(catalog);
        return p.load(inputMessage.getBody(), clazz);
    }
    
    //
    // writing
    //
    @Override
    public boolean canWrite(Class<?> clazz, MediaType mediaType) {
        if( RestListWrapper.class.isAssignableFrom(clazz) ){
            return false; // we can only write RestWrapper, not RestListWrapper
        }
        return RestWrapper.class.isAssignableFrom(clazz) && canWrite(mediaType);
    }
    @Override
    protected void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createXMLPersister();
        xmlPersister.setCatalog(catalog);
        xmlPersister.setReferenceByName(true);
        xmlPersister.setExcludeIds();
        if (o instanceof RestWrapper) {
            RestWrapper<?> wrapper = (RestWrapper<?>) o;
            wrapper.configurePersister(xmlPersister, this);
            o = wrapper.getObject();
        }
        xmlPersister.save(o, outputMessage.getBody());
    }

    @Override
    public String getExtension() {
        return "xml";
    }
    
    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_XML_VALUE;
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
