/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.rest;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.lang.reflect.Array;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.function.Consumer;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.io.IOUtils;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.opensearch.eo.ListComplexFeatureCollection;
import org.geoserver.opensearch.eo.OpenSearchAccessProvider;
import org.geoserver.opensearch.eo.ProductClass;
import org.geoserver.opensearch.eo.response.LinkFeatureComparator;
import org.geoserver.opensearch.eo.store.OpenSearchAccess;
import org.geoserver.opensearch.rest.CollectionsController.IOConsumer;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.collection.BaseSimpleFeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geojson.feature.FeatureJSON;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.FilterFactory2;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.HttpStatus;
import org.w3c.dom.Document;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Base class for OpenSearch related REST controllers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractOpenSearchController extends RestBaseController {

    interface ZipPart {

        /** Returns true if the part matches the provided name */
        public boolean matches(String name);
    }

    static final Logger LOGGER = Logging.getLogger(CollectionsController.class);

    static final String SOURCE_NAME = "SourceName";

    static final FeatureFactory FEATURE_FACTORY = CommonFactoryFinder.getFeatureFactory(null);

    static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    protected OpenSearchAccessProvider accessProvider;

    protected OseoJSONConverter jsonConverter;

    public AbstractOpenSearchController(
            OpenSearchAccessProvider accessProvider, OseoJSONConverter jsonConverter) {
        this.accessProvider = accessProvider;
        this.jsonConverter = jsonConverter;
    }

    protected OpenSearchAccess getOpenSearchAccess() throws IOException {
        return accessProvider.getOpenSearchAccess();
    }

    protected DataStoreInfo getOpenSearchStoreInfo() throws IOException {
        return accessProvider.getDataStoreInfo();
    }

    protected void validateMin(Integer value, int min, String name) {
        if (value != null && value < min) {
            throw new RestException(
                    "Invalid parameter " + name + ", should be at least " + min,
                    HttpStatus.BAD_REQUEST);
        }
    }

    protected void validateMax(Integer value, int max, String name) {
        if (value != null && value > max) {
            throw new RestException(
                    "Invalid parameter " + name + ", should be at most " + max,
                    HttpStatus.BAD_REQUEST);
        }
    }

    protected void setupQueryPaging(Query query, Integer offset, Integer limit) {
        if (offset != null) {
            validateMin(offset, 0, "offset");
            query.setStartIndex(offset);
        }
        final int maximumRecordsPerPage = accessProvider.getService().getMaximumRecordsPerPage();
        if (limit != null) {
            validateMin(limit, 0, "limit");
            validateMax(limit, maximumRecordsPerPage, "limit");
            query.setMaxFeatures(limit);
        } else {
            query.setMaxFeatures(maximumRecordsPerPage);
        }
    }

    protected FeatureCollection<FeatureType, Feature> queryCollections(Query query)
            throws IOException {
        OpenSearchAccess access = accessProvider.getOpenSearchAccess();
        FeatureSource<FeatureType, Feature> fs = access.getCollectionSource();
        FeatureCollection<FeatureType, Feature> fc = fs.getFeatures(query);
        return fc;
    }

    protected Feature queryCollection(String collectionName, Consumer<Query> queryDecorator)
            throws IOException {
        Query query = new Query();
        query.setFilter(FF.equal(FF.property("name"), FF.literal(collectionName), true));
        queryDecorator.accept(query);
        FeatureCollection<FeatureType, Feature> fc = queryCollections(query);
        Feature feature = DataUtilities.first(fc);
        if (feature == null) {
            throw new ResourceNotFoundException(
                    "Could not find a collection named '" + collectionName + "'");
        }

        return feature;
    }

    protected SimpleFeatureType mapFeatureTypeToSimple(
            FeatureType schema, Consumer<SimpleFeatureTypeBuilder> extraAttributeBuilder) {
        SimpleFeatureTypeBuilder tb = new SimpleFeatureTypeBuilder();
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            // skip multivalue, metadata and quicklook
            final Name propertyName = pd.getName();
            if (pd.getMaxOccurs() > 1
                    || OpenSearchAccess.METADATA_PROPERTY_NAME.equals(propertyName)
                    || OpenSearchAccess.QUICKLOOK_PROPERTY_NAME.equals(propertyName)
                    || "htmlDescription".equalsIgnoreCase(propertyName.getLocalPart())
                    || "id".equals(propertyName.getLocalPart())) {
                continue;
            }
            Name name = propertyName;
            String uri = name.getNamespaceURI();
            String prefix = null;
            if (OpenSearchAccess.EO_NAMESPACE.equals(uri)) {
                prefix = "eo";
            } else {
                for (ProductClass pc : accessProvider.getProductClasses()) {
                    if (pc.getNamespace().equals(uri)) {
                        prefix = pc.getPrefix();
                        break;
                    }
                }
            }
            String mappedName;
            if (prefix != null) {
                mappedName = prefix + ":" + name.getLocalPart();
            } else {
                mappedName = name.getLocalPart();
            }
            tb.userData(SOURCE_NAME, name);
            tb.add(mappedName, pd.getType().getBinding());
        }
        tb.setName(schema.getName());
        extraAttributeBuilder.accept(tb);
        SimpleFeatureType targetSchema = tb.buildFeatureType();
        return targetSchema;
    }

    protected OgcLinks buildOgcLinksFromFeature(Feature feature, boolean notFoundOnEmpty) {
        // map to a list of beans
        List<OgcLink> links = Collections.emptyList();
        Collection<Property> linkProperties =
                feature.getProperties(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
        if (linkProperties != null) {
            links =
                    linkProperties
                            .stream()
                            .map(p -> (SimpleFeature) p)
                            .sorted(LinkFeatureComparator.INSTANCE)
                            .map(
                                    sf -> {
                                        String offering = (String) sf.getAttribute("offering");
                                        String method = (String) sf.getAttribute("method");
                                        String code = (String) sf.getAttribute("code");
                                        String type = (String) sf.getAttribute("type");
                                        String href = (String) sf.getAttribute("href");
                                        return new OgcLink(offering, method, code, type, href);
                                    })
                            .collect(Collectors.toList());
        }
        if (links.isEmpty() && notFoundOnEmpty) {
            throw new ResourceNotFoundException();
        }
        return new OgcLinks(links);
    }

    protected SimpleFeature mapFeatureToSimple(
            Feature f,
            SimpleFeatureType targetSchema,
            Consumer<SimpleFeatureBuilder> extraValueBuilder) {
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(targetSchema);
        List<AttributeDescriptor> attributeDescriptors = targetSchema.getAttributeDescriptors();
        String identifier = f.getIdentifier().getID();
        for (AttributeDescriptor ad : attributeDescriptors) {
            Name sourceName = (Name) ad.getUserData().get(SOURCE_NAME);
            Property p = f.getProperty(sourceName);
            if (p != null) {
                Object value = p.getValue();
                if (value != null) {
                    fb.set(ad.getLocalName(), value);
                    if (("eo:identifier".equals(ad.getLocalName())
                                    || "eop:identifier".equals(ad.getLocalName()))
                            && value instanceof String) {
                        identifier = (String) value;
                    }
                }
            }
        }
        extraValueBuilder.accept(fb);

        return fb.buildFeature(identifier);
    }

    /** Un-maps OSEO related attributes to a prefix:name for for json encoding */
    protected SimpleFeatureCollection toSimpleFeatureCollection(
            FeatureCollection<FeatureType, Feature> fc,
            Consumer<SimpleFeatureTypeBuilder> extraAttributeBuilder,
            Consumer<SimpleFeatureBuilder> extraValuesBuilder) {
        SimpleFeatureType targetSchema =
                mapFeatureTypeToSimple(fc.getSchema(), extraAttributeBuilder);

        return new BaseSimpleFeatureCollection(targetSchema) {

            @Override
            public SimpleFeatureIterator features() {
                FeatureIterator<Feature> features = fc.features();
                return new SimpleFeatureIterator() {

                    @Override
                    public SimpleFeature next() throws NoSuchElementException {
                        Feature f = features.next();
                        return mapFeatureToSimple(f, targetSchema, extraValuesBuilder);
                    }

                    @Override
                    public boolean hasNext() {
                        return features.hasNext();
                    }

                    @Override
                    public void close() {
                        features.close();
                    }
                };
            }
        };
    }

    /** Checks XML well formedness (TODO: check against actual schemas) */
    protected void checkWellFormedXML(String xml) {
        DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder dBuilder;
        try {
            dBuilder = dbFactory.newDocumentBuilder();
            Document doc = dBuilder.parse(new InputSource(new StringReader(xml)));
        } catch (ParserConfigurationException | SAXException | IOException e) {
            throw new RestException(
                    "XML document is not well formed: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }

    /**
     * Factors out the boilerplate to create a transaction, run it, commit it if successful, revert
     * otherwise, and finally close it
     */
    protected void runTransactionOnStore(
            FeatureStore store, IOConsumer<FeatureStore> featureStoreConsumer) throws IOException {
        try (Transaction t = new DefaultTransaction()) {
            store.setTransaction(t);
            try {
                featureStoreConsumer.accept(store);
                t.commit();
            } catch (Exception e) {
                t.rollback();
                throw new IOException("Failed to run modification on storage:" + e.getMessage(), e);
            }
        }
    }

    /** Turns a complex feature into a single item feature collection */
    protected FeatureCollection singleton(Feature f) {
        ListComplexFeatureCollection fc = new ListComplexFeatureCollection(f);
        return fc;
    }

    /**
     * Converts the simple feature representatin of a collection into a complex feature suitable for
     * OpenSearchAccess usage
     */
    protected Feature simpleToComplex(
            SimpleFeature feature, FeatureType targetSch, Collection<String> ignoredAttributes)
            throws IOException {
        ComplexFeatureBuilder builder = new ComplexFeatureBuilder(targetSch);
        AttributeBuilder ab = new AttributeBuilder(FEATURE_FACTORY);
        for (AttributeDescriptor ad : feature.getType().getAttributeDescriptors()) {
            String sourceName = ad.getLocalName();
            // ignore links
            if (ignoredAttributes.contains(sourceName)) {
                continue;
            }
            // map to complex feature attribute and check
            Name pname = toName(sourceName, targetSch.getName().getNamespaceURI());
            PropertyDescriptor pd = targetSch.getDescriptor(pname);
            if (pd == null) {
                throw new RestException(
                        "Unexpected attribute found: '" + sourceName + "'", HttpStatus.BAD_REQUEST);
            }

            ab.setDescriptor((AttributeDescriptor) pd);
            Object originalValue = feature.getAttribute(sourceName);
            Object converted = convert(originalValue, pd.getType().getBinding());
            Attribute attribute = ab.buildSimple(null, converted);
            builder.append(pd.getName(), attribute);
        }
        Feature collectionFeature = builder.buildFeature(feature.getID());
        return collectionFeature;
    }

    protected Object convert(Object value, Class<?> targetClass) {
        if (value == null) {
            return null;
        }
        Object converted = Converters.convert(value, targetClass);
        // custom array handling, unsure about adding a generally available converter
        // might have too many side effects as a globally available converter
        // we might revisit this decision later
        if (converted == null) {
            if (targetClass.isArray() && value instanceof List) {
                Class componentType = targetClass.getComponentType();
                List list = (List) value;
                converted = Array.newInstance(componentType, list.size());
                int i = 0;
                for (Object o : list) {
                    Object convertedItem = Converters.convert(o, componentType);
                    Array.set(converted, i++, convertedItem);
                }
            } else {
                throw new RestException(
                        value + " cannot be converted to a " + targetClass.getSimpleName(),
                        HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return converted;
    }

    protected Name toName(String sourceName, String defaultNamespace) {
        String[] split = sourceName.split(":");
        switch (split.length) {
            case 1:
                if ("geometry".equals(sourceName)) {
                    return new NameImpl(defaultNamespace, "footprint");
                } else {
                    return new NameImpl(defaultNamespace, sourceName);
                }
            case 2:
                String prefix = split[0];
                String localName = split[1];
                String namespaceURI = null;
                if ("eo".equals(prefix)) {
                    namespaceURI = OpenSearchAccess.EO_NAMESPACE;
                } else {
                    for (ProductClass pc : accessProvider.getProductClasses()) {
                        if (prefix.equals(pc.getPrefix())) {
                            namespaceURI = pc.getNamespace();
                        }
                    }
                }

                if (namespaceURI == null) {
                    throw new RestException(
                            "Unrecognized attribute prefix in property " + sourceName,
                            HttpStatus.BAD_REQUEST);
                }

                return new NameImpl(namespaceURI, localName);
            default:
                throw new RestException(
                        "Unrecognized attribute " + sourceName, HttpStatus.BAD_REQUEST);
        }
    }

    protected ListFeatureCollection beansToLinksCollection(OgcLinks links) throws IOException {
        SimpleFeatureType schema = getOpenSearchAccess().getOGCLinksSchema();
        ListFeatureCollection linksCollection = new ListFeatureCollection(schema);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(schema);
        for (OgcLink link : links.links) {
            fb.set("offering", link.offering);
            fb.set("method", link.method);
            fb.set("code", link.code);
            fb.set("type", link.type);
            fb.set("href", link.href);
            SimpleFeature sf = fb.buildFeature(null);
            linksCollection.add(sf);
        }
        return linksCollection;
    }

    protected <T extends ZipPart> Map<T, byte[]> parsePartsFromZip(InputStream body, T[] parts)
            throws IOException {
        // check the zip contents and map to the expected parts
        Map<T, byte[]> result = new HashMap<>();
        try {
            ZipInputStream zis = new ZipInputStream(body);
            ZipEntry entry = null;

            while ((entry = zis.getNextEntry()) != null) {
                String name = entry.getName();
                T part = null;
                for (T zp : parts) {
                    if (zp.matches(name)) {
                        part = zp;
                        break;
                    }
                }
                if (part != null) {
                    result.put(part, IOUtils.toByteArray(zis));
                } else {
                    LOGGER.warning("Ignoring un-recognized entry in zip file:" + name);
                }
            }
        } catch (ZipException e) {
            throw new RestException(e.getMessage(), HttpStatus.BAD_REQUEST);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    protected <T> T parseJSON(Class<T> clazz, byte[] rawData) throws IOException {
        T links =
                (T)
                        jsonConverter.read(
                                clazz,
                                new HttpInputMessage() {

                                    @Override
                                    public HttpHeaders getHeaders() {
                                        return new HttpHeaders();
                                    }

                                    @Override
                                    public InputStream getBody() throws IOException {
                                        return new ByteArrayInputStream(rawData);
                                    }
                                });
        return links;
    }

    protected SimpleFeature parseGeoJSONFeature(String fileReference, final byte[] payload) {
        try {
            SimpleFeature jsonFeature =
                    new FeatureJSON().readFeature(new ByteArrayInputStream(payload));
            return jsonFeature;
        } catch (IOException e) {
            throw new RestException(
                    fileReference + " contains invalid GeoJSON: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }

    protected SimpleFeatureCollection parseGeoJSONFeatureCollection(
            String fileReference, final byte[] payload) {
        try {
            SimpleFeatureCollection fc =
                    (SimpleFeatureCollection)
                            new FeatureJSON()
                                    .readFeatureCollection(new ByteArrayInputStream(payload));
            return fc;
        } catch (IOException e) {
            throw new RestException(
                    fileReference + " contains invalid GeoJSON: " + e.getMessage(),
                    HttpStatus.BAD_REQUEST,
                    e);
        }
    }
}
