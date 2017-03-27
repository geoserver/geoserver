package org.geoserver.restng.converters;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.restng.wrapper.RestListWrapper;
import org.geoserver.restng.wrapper.RestWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Message converter implementation for JSON serialization via XStream
 */
public class XStreamJSONMessageConverter extends BaseMessageConverter {

    public XStreamJSONMessageConverter(ApplicationContext applicationContext) {
        super(applicationContext);
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return !RestListWrapper.class.isAssignableFrom(clazz) &&
            MediaType.APPLICATION_JSON.equals(mediaType);
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        /**
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
        return Arrays.asList(MediaType.APPLICATION_JSON);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException
    {
        XStreamPersister p = xpf.createJSONPersister();
        p.setCatalog(catalog);
        return p.load(inputMessage.getBody(), clazz);
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {
        XStreamPersister xmlPersister = xpf.createJSONPersister();
        xmlPersister.setCatalog(catalog);
        xmlPersister.setReferenceByName(true);
        xmlPersister.setExcludeIds();
        if (o instanceof RestWrapper) {
            ((RestWrapper) o).configurePersister(xmlPersister);
            o = ((RestWrapper) o).getObject();
        }
        xmlPersister.save(o, outputMessage.getBody());
    }

}
