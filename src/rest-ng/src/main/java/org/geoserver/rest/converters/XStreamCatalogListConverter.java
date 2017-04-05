package org.geoserver.rest.converters;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.springframework.context.ApplicationContext;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;

/**
 * Converter to handle the serialization of lists of catalog resources, which need some special
 * handling
 */
public abstract class XStreamCatalogListConverter extends XStreamMessageConverter<RestListWrapper> {

    public XStreamCatalogListConverter() {
        super();
    }

    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return RestListWrapper.class.isAssignableFrom(clazz) &&
                isSupportedMediaType(mediaType);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
        throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException("Can't marshal catalog lists");
    }

    @Override
    public void write(Object o, MediaType contentType, HttpOutputMessage outputMessage)
        throws IOException, HttpMessageNotWritableException {

        XStream xstream = this.createXStreamInstance();
        RestListWrapper wrapper = (RestListWrapper)o;

        Class targetClass = wrapper.getObjectClass();
        Collection data = wrapper.getCollection();
        this.aliasCollection(data, xstream, targetClass);
        this.configureXStream(xstream, targetClass, wrapper);
        xstream.toXML(data, outputMessage.getBody());
    }

    private void configureXStream(XStream xstream, Class clazz, RestListWrapper wrapper) {
        XStreamPersister xp = xpf.createXMLPersister();
        wrapper.configurePersister(xp, this);
        final String name = getItemName(xp, clazz);
        xstream.alias( name, clazz );

        xstream.registerConverter(
            new CollectionConverter(xstream.getMapper()) {
                public boolean canConvert(Class type) {
                    return Collection.class.isAssignableFrom(type);
                };
                @Override
                protected void writeItem(Object item,
                    MarshallingContext context,
                    HierarchicalStreamWriter writer) {

                    writer.startNode( name );
                    context.convertAnother( item );
                    writer.endNode();
                }
            }
        );
        xstream.registerConverter(
            new Converter() {

                public boolean canConvert(Class type) {
                    return clazz.isAssignableFrom( type );
                }

                public void marshal(Object source,
                    HierarchicalStreamWriter writer,
                    MarshallingContext context) {

                    String ref = null;
                    if ( OwsUtils.getter( clazz, "name", String.class ) != null ) {
                        ref = (String) OwsUtils.get( source, "name");
                    }
                    else if ( OwsUtils.getter( clazz, "id", String.class ) != null ) {
                        ref = (String) OwsUtils.get( source, "id");
                    } else if ( OwsUtils.getter( clazz, "id", Long.class ) != null ) {
                        //For some reason Importer objects have Long ids so this catches that case
                        ref = (String) OwsUtils.get( source, "id").toString();
                    }
                    
                    else {
                        throw new RuntimeException( "Could not determine identifier for: " + clazz.getName());
                    }
                    writer.startNode( "name" );
                    writer.setValue(ref);
                    writer.endNode();

                    encodeLink(encode(ref), writer);
                }

                public Object unmarshal(HierarchicalStreamReader reader,
                    UnmarshallingContext context) {
                    return null;
                }
            }
        );
    }

    /**
     * Template method to alias the type of the collection.
     * <p>
     * The default works with list, subclasses may override for instance
     * to work with a Set.
     * </p>
     */
    protected void aliasCollection( Object data, XStream xstream, Class clazz) {
        XStreamPersister xp = xpf.createXMLPersister();
        final String alias = getItemName(xp, clazz);
        xstream.alias(alias + "s", Collection.class, data.getClass());
    }

    protected String getItemName(XStreamPersister xp, Class clazz) {
        return xp.getClassAliasingMapper().serializedClass( clazz );
    }

    /**
     * XML handling for catalog lists
     */
    public static class XMLXStreamListConverter extends XStreamCatalogListConverter {

        public XMLXStreamListConverter() {
            super();
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

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Arrays.asList(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_ATOM_XML_VALUE;
        }

        @Override
        public String getExtension() {
            return "xml";
        }
    }

    public static class JSONXStreamListConverter extends XStreamCatalogListConverter {

        public JSONXStreamListConverter() {
            super();
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

        @Override
        public String getExtension() {
            return "json";
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        @Override
        public List<MediaType> getSupportedMediaTypes() {
            return Arrays.asList(MediaType.APPLICATION_JSON);
        }
    }
}
