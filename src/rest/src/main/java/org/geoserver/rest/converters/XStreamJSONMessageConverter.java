/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.io.IOException;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/** Message converter implementation for JSON serialization via XStream */
public class XStreamJSONMessageConverter extends XStreamMessageConverter<Object> {

    static final MediaType TEXT_JSON = MediaType.valueOf("text/json");

    public XStreamJSONMessageConverter() {
        super(MediaType.APPLICATION_JSON, TEXT_JSON);
    }

    @Override
    public String getExtension() {
        return "json";
    }

    @Override
    public String getMediaType() {
        return MediaType.APPLICATION_JSON_VALUE;
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        // this converter is actually used for classes that are not RestWrapper, but we need to
        // exclude OGC API Objects that need to be managed by the Jackson converter
        return clazz != null && (RestWrapper.class.isAssignableFrom(clazz) || !isOGCAPIObject(clazz));
    }

    private boolean isOGCAPIObject(Class<?> clazz) {
        return clazz.getPackage().getName().startsWith("org.geoserver.ogcapi");
    }

    //
    // reading
    //
    @Override
    public Object readInternal(Class<?> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        XStreamPersister p = xpf.createJSONPersister();
        p.setCatalog(catalog);
        if (inputMessage instanceof RestHttpInputWrapper wrapper) {
            wrapper.configurePersister(p, this);
        }
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
    public void writeInternal(Object o, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createJSONPersister();
        xmlPersister.setCatalog(catalog);
        xmlPersister.setReferenceByName(true);
        xmlPersister.setExcludeIds();
        if (o instanceof RestWrapper<?> wrapper) {
            wrapper.configurePersister(xmlPersister, this);
            o = wrapper.getObject();
        }
        xmlPersister.save(o, outputMessage.getBody());
    }

    @Override
    public void encodeLink(String link, HierarchicalStreamWriter writer) {
        writer.startNode("href");
        writer.setValue(href(link));
        writer.endNode();
    }

    @Override
    public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
        writer.setValue(href(link));
    }

    @Override
    protected XStream createXStreamInstance() {
        throw new UnsupportedOperationException("unused");
    }
}
