/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.converters;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.converters.collections.CollectionConverter;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import com.thoughtworks.xstream.io.json.JettisonMappedXmlDriver;
import java.io.IOException;
import java.util.Collection;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.config.util.SecureXStream;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.rest.RequestInfo;
import org.geoserver.rest.wrapper.RestListWrapper;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpOutputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.HttpMessageNotWritableException;

/**
 * Converter to handle the serialization of lists of catalog resources, which need some special
 * handling
 */
public abstract class XStreamCatalogListConverter
        extends XStreamMessageConverter<RestListWrapper<?>> {

    public XStreamCatalogListConverter(MediaType... supportedMediaTypes) {
        super(supportedMediaTypes);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return RestListWrapper.class.isAssignableFrom(clazz); // can write RestListWrapper
    }

    //
    // reading
    //
    @Override
    public boolean canRead(Class<?> clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public RestListWrapper<?> readInternal(
            Class<? extends RestListWrapper<?>> clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new HttpMessageNotReadableException(
                getClass().getName() + " does not support deserialization of catalog lists",
                inputMessage);
    }
    //
    // writing
    //
    @Override
    public void writeInternal(RestListWrapper<?> wrapper, HttpOutputMessage outputMessage)
            throws IOException, HttpMessageNotWritableException {
        XStream xstream = this.createXStreamInstance();

        Class<?> targetClass = wrapper.getObjectClass();
        Collection<?> data = wrapper.getCollection();
        this.aliasCollection(data, xstream, targetClass, wrapper);
        this.configureXStream(xstream, targetClass, wrapper);
        xstream.toXML(data, outputMessage.getBody());
    }

    private void configureXStream(XStream xstream, Class<?> clazz, RestListWrapper<?> wrapper) {
        XStreamPersister xp = xpf.createXMLPersister();
        wrapper.configurePersister(xp, this);
        final String name = getItemName(xp, clazz);
        xstream.alias(name, clazz);

        xstream.registerConverter(
                new CollectionConverter(xstream.getMapper()) {
                    @SuppressWarnings("rawtypes")
                    @Override
                    public boolean canConvert(Class type) {
                        return Collection.class.isAssignableFrom(type);
                    }

                    @Override
                    protected void writeCompleteItem(
                            Object item,
                            MarshallingContext context,
                            HierarchicalStreamWriter writer) {

                        writer.startNode(name);
                        context.convertAnother(item);
                        writer.endNode();
                    }
                });
        xstream.registerConverter(
                new Converter() {
                    @SuppressWarnings("rawtypes")
                    public boolean canConvert(Class type) {
                        return clazz.isAssignableFrom(type);
                    }

                    public void marshal(
                            Object source,
                            HierarchicalStreamWriter writer,
                            MarshallingContext context) {

                        String ref;
                        // Special case for layer list, to handle the non-workspace-specific
                        // endpoint for layers
                        if (clazz.equals(LayerInfo.class)
                                && OwsUtils.getter(clazz, "prefixedName", String.class) != null
                                && RequestInfo.get() != null
                                && !RequestInfo.get().getPagePath().contains("/workspaces/")) {

                            ref = (String) OwsUtils.get(source, "prefixedName");
                        } else if (OwsUtils.getter(clazz, "name", String.class) != null) {
                            ref = (String) OwsUtils.get(source, "name");
                        } else if (OwsUtils.getter(clazz, "id", String.class) != null) {
                            ref = (String) OwsUtils.get(source, "id");
                        } else if (OwsUtils.getter(clazz, "id", Long.class) != null) {
                            // For some reason Importer objects have Long ids so this catches that
                            // case
                            ref = OwsUtils.get(source, "id").toString();
                        } else {
                            throw new RuntimeException(
                                    "Could not determine identifier for: " + clazz.getName());
                        }
                        writer.startNode(wrapper.getItemAttributeName());
                        writer.setValue(ref);
                        writer.endNode();

                        encodeLink(encode(ref), writer);
                    }

                    public Object unmarshal(
                            HierarchicalStreamReader reader, UnmarshallingContext context) {
                        return null;
                    }
                });
    }

    /**
     * Template method to alias the type of the collection.
     *
     * <p>The default works with list, subclasses may override for instance to work with a Set.
     */
    protected void aliasCollection(
            Object data, XStream xstream, Class<?> clazz, RestListWrapper<?> wrapper) {
        XStreamPersister xp = xpf.createXMLPersister();
        wrapper.configurePersister(xp, this);
        final String alias = getItemName(xp, clazz);
        xstream.alias(alias + "s", Collection.class, data.getClass());
    }

    protected String getItemName(XStreamPersister xp, Class<?> clazz) {
        return xp.getClassAliasingMapper().serializedClass(clazz);
    }

    /** XML handling for catalog lists */
    public static class XMLXStreamListConverter extends XStreamCatalogListConverter {

        public XMLXStreamListConverter() {
            super(MediaType.APPLICATION_XML, MediaType.TEXT_XML);
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
            super(MediaType.APPLICATION_JSON, XStreamJSONMessageConverter.TEXT_JSON);
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
            return new SecureXStream(new JettisonMappedXmlDriver());
        }

        @Override
        public String getExtension() {
            return "json";
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }
    }
}
