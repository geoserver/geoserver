package org.geoserver.rest.converters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.rest.wrapper.RestHttpInputWrapper;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.geoserver.rest.wrapper.RestWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Message converter implementation for JSON serialization via XStream
 */
public class XStreamJSONMessageConverter extends XStreamMessageConverter {
    
    static final MediaType TEXT_JSON = MediaType.valueOf("text/json");

    public XStreamJSONMessageConverter() {
        super(MediaType.APPLICATION_JSON);
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
    protected boolean supports(Class clazz) {
        return !RestListWrapper.class.isAssignableFrom(clazz);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        /*
         * Actually, this should largely be dependent on clazz and not by the passed in media type.
         *
         * During my research I found that:
         *
         * - Unless "produces" was set on the controller object, the passed media type was null
         * - So, you can't actually rely on media type not being null
         * - BUT, this method is only called anyway if they requested media type (via Accepts header) is in the list of getSupportedMediaTypes
         */
        return !RestListWrapper.class.isAssignableFrom(clazz) && RestWrapper.class.isAssignableFrom(clazz) &&
            MediaType.APPLICATION_JSON.equals(mediaType);
    }

    @Override
    public List<MediaType> getSupportedMediaTypes() {
        return Arrays.asList(MediaType.APPLICATION_JSON, TEXT_JSON);
    }

    @Override
    public Object readInternal(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException
    {
        XStreamPersister p = xpf.createJSONPersister();
        p.setCatalog(catalog);
        if (inputMessage instanceof RestHttpInputWrapper) {
            ((RestHttpInputWrapper) inputMessage).configurePersister(p, this);
        }
        return p.load(inputMessage.getBody(), clazz);
    }

    @Override
    public void writeInternal(Object o, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createJSONPersister();
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
    public void encodeLink(String link, HierarchicalStreamWriter writer) {
        writer.startNode( "href" );
        writer.setValue(href(link));
        writer.endNode();
    }
    
    @Override
    public void encodeCollectionLink(String link, HierarchicalStreamWriter writer) {
        writer.setValue(href(link));
    }

    @Override
    protected XStream createXStreamInstance() {
        return new XStream(new JettisonMappedXmlDriver());
    }

}
