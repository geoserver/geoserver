/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Message converter implementation for XML serialization via XStream */
public class XStreamXMLMessageConverter extends XStreamMessageConverter<Object> {

    public XStreamXMLMessageConverter() {
        super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // we can only read RestWrapper, not RestListWrapper
        return !RestWrapper.class.isAssignableFrom(clazz)
                || !RestListWrapper.class.isAssignableFrom(clazz);
    }

    //
    // reading
    //
    //    @Override
    //    public boolean canRead(Class<?> clazz, MediaType mediaType) {
    //        return !RestListWrapper.class.isAssignableFrom(clazz) && canRead(mediaType);
    //    }

    @Override
    protected Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createXMLPersister();
        p.setUnwrapNulls(false);
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
        // we can only write RestWrapper, not RestListWrapper
        return !RestListWrapper.class.isAssignableFrom(clazz)
                && RestWrapper.class.isAssignableFrom(clazz)
                && canWrite(mediaType);
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
