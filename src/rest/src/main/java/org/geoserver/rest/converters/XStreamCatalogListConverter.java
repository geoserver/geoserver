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
import org.codehaus.jettison.mapped.Configuration;
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

    protected void configureXStream(XStream xstream, Class<?> clazz, RestListWrapper<?> wrapper) {
        XStreamPersister xp = xpf.createXMLPersister();
        wrapper.configurePersister(xp, this);
        final String name = getItemName(xp, clazz);
        xstream.alias(name, clazz);

        xstream.registerConverter(
                new CollectionConverter(xstream.getMapper()) {
                    @Override
                    public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
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
                    @Override
                    public boolean canConvert(Class type) {
                        return clazz.isAssignableFrom(type);
                    }

                    @Override
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

                    @Override
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

        /**
         * Calls {@link #writeSingleElementCollection} for single-element collections, to ensure
         * backwards compatibility with Jettison 1.0 output, and {@code super.writeInternal()}
         * otherwise
         */
        @Override
        public void writeInternal(RestListWrapper<?> wrapper, HttpOutputMessage outputMessage)
                throws IOException, HttpMessageNotWritableException {

            if (wrapper.getCollection().size() == 1) {
                writeSingleElementCollection(wrapper, outputMessage);
            } else {
                super.writeInternal(wrapper, outputMessage);
            }
        }

        @Override
        protected XStream createXStreamInstance() {
            // preserve legacy single-element-array-as-object serialization
            // called by super.writeInternal() for non-single element collections
            boolean useSerializeAsArray = false;
            return createXStreamInstance(useSerializeAsArray);
        }

        @Override
        public String getExtension() {
            return "json";
        }

        @Override
        public String getMediaType() {
            return MediaType.APPLICATION_JSON_VALUE;
        }

        private XStream createXStreamInstance(boolean useSerializeAsArray) {
            // needed for Jettison 1.4.1
            Configuration configuration = new Configuration();
            configuration.setRootElementArrayWrapper(false);
            return new SecureXStream(
                    new JettisonMappedXmlDriver(configuration, useSerializeAsArray));
        }

        /**
         * Special treatment for single-element collections in {@link
         * RestListWrapper#getCollection()} to ensure encoding matches Jettison 1.0.1 (prior to
         * XStream 1.4.18 / Jettison 1.4.1 upgrade).
         *
         * <p>Expected output being like:
         *
         * <pre>
         * <code>
         * {"coverages": {"coverage": [{ "name": "tazdem", ... }]}}
         *  </code>
         *  </pre>
         *
         * Otherwise we'd get one of:
         *
         * <pre>
         * <code>
         *  {"coverages": {"coverage": { "name": "tazdem", ... }}}
         *  </code>
         *  </pre>
         *
         * or
         *
         * <pre>
         * <code>
         *  {"coverages": [{"coverage": { "name": "tazdem", ... }}]}
         *  </code>
         *  </pre>
         *
         * instead.
         */
        private void writeSingleElementCollection(
                RestListWrapper<?> wrapper, HttpOutputMessage outputMessage) throws IOException {

            final boolean useSerializeAsArray = true;
            XStream xstream = this.createXStreamInstance(useSerializeAsArray);
            XStreamPersister xp = xpf.createXMLPersister();
            wrapper.configurePersister(xp, this);
            final Class<?> targetClass = wrapper.getObjectClass();
            final String itemName = getItemName(xp, targetClass);
            final String collectionAlias = itemName + "s";
            this.configureSingleElementCollectionXStream(xstream, targetClass, wrapper);

            ListRoot data = new ListRoot(wrapper.getCollection());
            xstream.alias(collectionAlias, ListRoot.class);
            xstream.aliasField(itemName, ListRoot.class, "values");
            xstream.alias(itemName, targetClass);

            // do not generate an @class: list JSON attribute
            xstream.aliasSystemAttribute(null, "class");
            xstream.toXML(data, outputMessage.getBody());
        }

        static class ListRoot {
            private Collection<?> values;

            ListRoot(Collection<?> values) {
                this.values = values;
            }

            public Collection<?> getValues() {
                return values;
            }
        }

        /**
         * Uses a {@link CollectionConverter} for single-element collection JSON encoding without
         * calling {@code writer.start/endNode(name)}, but a single call to {@link
         * CollectionConverter#writeBareItem writeBareItem()}, since the element name is handled by
         * the encoding of {@link ListRoot}.
         */
        protected void configureSingleElementCollectionXStream(
                XStream xstream, Class<?> clazz, RestListWrapper<?> wrapper) {

            super.configureXStream(xstream, clazz, wrapper);
            xstream.registerConverter(
                    new CollectionConverter(xstream.getMapper()) {
                        @Override
                        public boolean canConvert(@SuppressWarnings("rawtypes") Class type) {
                            return Collection.class.isAssignableFrom(type);
                        }

                        @Override
                        protected void writeCompleteItem(
                                Object item,
                                MarshallingContext context,
                                HierarchicalStreamWriter writer) {

                            super.writeBareItem(item, context, writer);
                        }
                    });
        }
    }
}
