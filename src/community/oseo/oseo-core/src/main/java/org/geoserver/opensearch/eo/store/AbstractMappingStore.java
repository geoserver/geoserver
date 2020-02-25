/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.FF;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYERS_PROPERTY_NAME;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.METADATA_PROPERTY_NAME;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.OGC_LINKS_PROPERTY_NAME;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geotools.data.DataAccess;
import org.geotools.data.DataSourceException;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultResourceInfo;
import org.geotools.data.FeatureListener;
import org.geotools.data.FeatureReader;
import org.geotools.data.FeatureStore;
import org.geotools.data.Join;
import org.geotools.data.Join.Type;
import org.geotools.data.Query;
import org.geotools.data.QueryCapabilities;
import org.geotools.data.ResourceInfo;
import org.geotools.data.Transaction;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.ComplexFeatureBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.opengis.feature.Attribute;
import org.opengis.feature.Feature;
import org.opengis.feature.FeatureFactory;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.Name;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Base class for the collection and product specific feature source wrappers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractMappingStore implements FeatureStore<FeatureType, Feature> {

    static final FeatureFactory FEATURE_FACTORY = CommonFactoryFinder.getFeatureFactory(null);

    static final Logger LOGGER = Logging.getLogger(AbstractMappingStore.class);

    /**
     * Like {@link BiFunction} but allowed to throw {@link IOException}
     *
     * @author Andrea Aime - GeoSolutions
     */
    @FunctionalInterface
    public interface IOBiFunction<T, U, R> {
        R apply(T t, U u) throws IOException;
    }

    protected JDBCOpenSearchAccess openSearchAccess;

    protected FeatureType schema;

    protected SourcePropertyMapper propertyMapper;

    protected SortBy[] defaultSort;

    private SimpleFeatureType linkFeatureType;

    private SimpleFeatureType collectionLayerSchema;

    private Transaction transaction;

    public AbstractMappingStore(
            JDBCOpenSearchAccess openSearchAccess, FeatureType collectionFeatureType)
            throws IOException {
        this.openSearchAccess = openSearchAccess;
        this.schema = collectionFeatureType;
        this.propertyMapper = new SourcePropertyMapper(schema);
        this.defaultSort = buildDefaultSort(schema);
        this.linkFeatureType = buildLinkFeatureType();
        this.collectionLayerSchema = buildCollectionLayerFeatureType();
    }

    protected SimpleFeatureType buildCollectionLayerFeatureType() throws IOException {
        SimpleFeatureType source =
                openSearchAccess.getDelegateStore().getSchema("collection_layer");
        try {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor ad : source.getAttributeDescriptors()) {
                if ("bands".equals(ad.getLocalName()) || "browseBands".equals(ad.getLocalName())) {
                    b.add(ad.getLocalName(), String[].class);
                } else {
                    b.add(ad);
                }
            }

            b.setName(LAYERS_PROPERTY_NAME);
            return b.buildFeatureType();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the renamed feature type.", e);
        }
    }

    protected SimpleFeatureType buildLinkFeatureType() throws IOException {
        SimpleFeatureType source = openSearchAccess.getDelegateStore().getSchema(getLinkTable());
        try {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.init(source);
            b.setName(OpenSearchAccess.OGC_LINKS_PROPERTY_NAME);
            return b.buildFeatureType();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the renamed feature type.", e);
        }
    }

    /** Builds the default sort for the underlying feature source query */
    protected SortBy[] buildDefaultSort(FeatureType schema) {
        String timeStart = propertyMapper.getSourceName("timeStart");
        String identifier = propertyMapper.getSourceName("identifier");
        return new SortBy[] {
            FF.sort(timeStart, SortOrder.DESCENDING), FF.sort(identifier, SortOrder.ASCENDING)
        };
    }

    @Override
    public Name getName() {
        return schema.getName();
    }

    @Override
    public ResourceInfo getInfo() {
        try {
            SimpleFeatureSource featureSource = getDelegateCollectionSource();
            ResourceInfo delegateInfo = featureSource.getInfo();
            DefaultResourceInfo result = new DefaultResourceInfo(delegateInfo);
            result.setSchema(new URI(schema.getName().getNamespaceURI()));
            return result;
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    /*
     * Returns the underlying delegate source
     */
    protected abstract SimpleFeatureSource getDelegateCollectionSource() throws IOException;

    protected SimpleFeatureStore getDelegateCollectionStore() throws IOException {
        SimpleFeatureStore fs = (SimpleFeatureStore) getDelegateCollectionSource();
        if (transaction != null) {
            fs.setTransaction(transaction);
        }
        return fs;
    }

    @Override
    public DataAccess<FeatureType, Feature> getDataStore() {
        return openSearchAccess;
    }

    @Override
    public QueryCapabilities getQueryCapabilities() {
        QueryCapabilities result =
                new QueryCapabilities() {
                    @Override
                    public boolean isOffsetSupported() {
                        return true;
                    }

                    @Override
                    public boolean isReliableFIDSupported() {
                        // the delegate store should have a primary key on collections
                        return true;
                    }
                };
        return result;
    }

    @Override
    public void addFeatureListener(FeatureListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeFeatureListener(FeatureListener listener) {
        throw new UnsupportedOperationException();
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Filter filter) throws IOException {
        return getFeatures(new Query(getSchema().getName().getLocalPart(), filter));
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures() throws IOException {
        return getFeatures(Query.ALL);
    }

    @Override
    public FeatureType getSchema() {
        return schema;
    }

    @Override
    public ReferencedEnvelope getBounds() throws IOException {
        return getDelegateCollectionSource().getBounds();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        Query mapped = mapToSimpleCollectionQuery(query, false);
        return getDelegateCollectionSource().getBounds(mapped);
    }

    @Override
    public Set<Key> getSupportedHints() {
        try {
            return getDelegateCollectionSource().getSupportedHints();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getCount(Query query) throws IOException {
        final Query mappedQuery = mapToSimpleCollectionQuery(query, false);
        return getDelegateCollectionSource().getCount(mappedQuery);
    }

    /** Maps query back the main underlying feature source */
    protected Query mapToSimpleCollectionQuery(Query query, boolean addJoins) throws IOException {
        Query result = new Query(getDelegateCollectionSource().getSchema().getTypeName());
        final Filter originalFilter = query.getFilter();
        if (originalFilter != null) {
            Filter mappedFilter = mapFilterToDelegateSchema(originalFilter);
            result.setFilter(mappedFilter);
        }
        if (query.getPropertyNames() != null && query.getPropertyNames().length > 0) {
            String[] mappedPropertyNames =
                    Arrays.stream(query.getPropertyNames())
                            .map(name -> propertyMapper.getSourceName(name))
                            .filter(name -> name != null)
                            .toArray(size -> new String[size]);
            if (mappedPropertyNames.length == 0) {
                result.setPropertyNames(Query.ALL_NAMES);
            } else {
                result.setPropertyNames(mappedPropertyNames);
            }
        }
        if (query.getSortBy() != null && query.getSortBy().length > 0) {
            SortBy[] mappedSortBy =
                    Arrays.stream(query.getSortBy())
                            .map(
                                    sb -> {
                                        if (sb == SortBy.NATURAL_ORDER
                                                || sb == SortBy.REVERSE_ORDER) {
                                            return sb;
                                        } else {
                                            String name = sb.getPropertyName().getPropertyName();
                                            String mappedName = propertyMapper.getSourceName(name);
                                            if (mappedName == null) {
                                                throw new IllegalArgumentException(
                                                        "Cannot sort on " + name);
                                            }
                                            return FF.sort(mappedName, sb.getSortOrder());
                                        }
                                    })
                            .toArray(size -> new SortBy[size]);
            result.setSortBy(mappedSortBy);
        } else {
            // get stable results for paging
            result.setSortBy(defaultSort);
        }

        if (addJoins) {
            // join to metadata table if necessary
            if (hasOutputProperty(query, METADATA_PROPERTY_NAME, false)) {
                Filter filter = FF.equal(FF.property("id"), FF.property("metadata.mid"), true);
                final String metadataTable = getMetadataTable();
                Join join = new Join(metadataTable, filter);
                join.setAlias("metadata");
                join.setType(Type.OUTER);
                result.getJoins().add(join);
            }

            // same for output layer, if necessary
            if (hasOutputProperty(query, LAYERS_PROPERTY_NAME, false)
                    || hasOutputProperty(query, LAYERS_PROPERTY_NAME, false)) {
                Filter filter = FF.equal(FF.property("id"), FF.property("layer.cid"), true);
                final String layerTable = getCollectionLayerTable();
                Join join = new Join(layerTable, filter);
                join.setAlias("layer");
                join.setType(Type.OUTER);
                result.getJoins().add(join);
            }

            // same goes for OGC links (they might be missing, so outer join is used)
            if (hasOutputProperty(query, OGC_LINKS_PROPERTY_NAME, true)) {
                final String linkTable = getLinkTable();
                final String linkForeignKey = getLinkForeignKey();
                Filter filter =
                        FF.equal(FF.property("id"), FF.property("link." + linkForeignKey), true);
                Join join = new Join(linkTable, filter);
                join.setAlias("link");
                join.setType(Type.OUTER);
                result.getJoins().add(join);
            }
        } else {
            // only non joined requests are pageable
            result.setStartIndex(query.getStartIndex());
            result.setMaxFeatures(query.getMaxFeatures());
        }

        return result;
    }

    private Filter mapFilterToDelegateSchema(final Filter filter) {
        MappingFilterVisitor visitor = new MappingFilterVisitor(propertyMapper);
        Filter mappedFilter = (Filter) filter.accept(visitor, null);
        return mappedFilter;
    }

    /**
     * Name of the table to join in case the {@link OpenSearchAccess#LAYERS} property is requested
     */
    protected String getCollectionLayerTable() {
        return "collection_layer";
    }

    /**
     * Name of the metadata table to join in case the {@link
     * OpenSearchAccess#METADATA_PROPERTY_NAME} property is requested
     */
    protected abstract String getMetadataTable();

    /**
     * Name of the link table to join in case the {@link OpenSearchAccess#OGC_LINKS_PROPERTY_NAME}
     * property is requested
     */
    protected abstract String getLinkTable();

    /**
     * Name of the field linking back to the main table in case the {@link
     * OpenSearchAccess#OGC_LINKS_PROPERTY_NAME} property is requested
     */
    protected abstract String getLinkForeignKey();

    /** Name of the thumbnail table */
    protected abstract String getThumbnailTable();

    /**
     * Searches for an optional property among the query attributes. Returns true only if the
     * property is explicitly listed
     */
    protected boolean hasOutputProperty(Query query, Name property, boolean includedByDefault) {
        if (query.getProperties() == null) {
            return includedByDefault;
        }

        final String localPart = property.getLocalPart();
        final String namespaceURI = property.getNamespaceURI();
        for (PropertyName pn : query.getProperties()) {
            if (localPart.equals(pn.getPropertyName())
                    && (pn.getNamespaceContext() == null
                            || namespaceURI.equals(pn.getNamespaceContext().getURI("")))) {
                return true;
            }
        }

        return false;
    }

    @Override
    public FeatureCollection<FeatureType, Feature> getFeatures(Query query) throws IOException {
        // first get the ids of the features we are going to return, no joins to support paging
        Query idsQuery = mapToSimpleCollectionQuery(query, false);
        // idsQuery.setProperties(Query.NO_PROPERTIES); (no can do, there are mandatory fields)
        SimpleFeatureCollection idFeatureCollection =
                getDelegateCollectionSource().getFeatures(idsQuery);

        Set<FeatureId> ids = new LinkedHashSet<>();
        idFeatureCollection.accepts(f -> ids.add(f.getIdentifier()), null);

        // if no features, return immediately
        SimpleFeatureCollection fc;
        if (ids.isEmpty()) {
            fc = new EmptyFeatureCollection(getDelegateCollectionSource().getSchema());
        } else {
            // the run a joined query with the specified ids
            Query dataQuery = mapToSimpleCollectionQuery(query, true);
            dataQuery.setFilter(FF.id(ids));
            fc = getDelegateCollectionSource().getFeatures(dataQuery);
        }

        return new MappingFeatureCollection(schema, fc, this::mapToComplexFeature);
    }

    /** Maps the underlying features (eventually joined) to the output complex feature */
    protected Feature mapToComplexFeature(PushbackFeatureIterator<SimpleFeature> it) {
        SimpleFeature fi = it.next();

        ComplexFeatureBuilder builder = new ComplexFeatureBuilder(schema);

        // allow subclasses to perform custom mappings while reusing the common ones
        mapPropertiesToComplex(builder, fi);

        // the OGC links can be more than one
        Set<SimpleFeature> links = new LinkedHashSet<>();
        Set<SimpleFeature> layers = new LinkedHashSet<>();
        for (; ; ) {
            Object link = fi.getAttribute("link");
            Object layer = fi.getAttribute("layer");

            // handle joined layer if any
            if (layer instanceof SimpleFeature) {
                layers.add((SimpleFeature) layer);
            }

            if (link instanceof SimpleFeature) {
                links.add((SimpleFeature) link);
            }

            if (it.hasNext()) {
                SimpleFeature next = it.next();
                if (!next.getID().equals(fi.getID())) {
                    // moved to the next feature, push it back,
                    // we're done for the current one
                    it.pushBack();
                    break;
                } else {
                    fi = next;
                }
            } else {
                break;
            }
        }

        for (SimpleFeature layerFeature : layers) {
            SimpleFeature retyped = retypeLayerFeature(layerFeature);
            builder.append(LAYERS_PROPERTY_NAME, retyped);
        }

        for (SimpleFeature link : links) {
            SimpleFeature linkFeature =
                    SimpleFeatureBuilder.retype((SimpleFeature) link, linkFeatureType);
            builder.append(OGC_LINKS_PROPERTY_NAME, linkFeature);
        }

        //
        Feature feature = builder.buildFeature(fi.getID());
        return feature;
    }

    /** Performs the common mappings, subclasses can override to add more */
    protected void mapPropertiesToComplex(ComplexFeatureBuilder builder, SimpleFeature fi) {
        AttributeBuilder ab = new AttributeBuilder(FEATURE_FACTORY);
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            if (!(pd instanceof AttributeDescriptor)) {
                continue;
            }
            String localName = (String) pd.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
            if (localName == null) {
                continue;
            }
            Object value = fi.getAttribute(localName);
            if (value == null) {
                continue;
            }
            ab.setDescriptor((AttributeDescriptor) pd);
            Attribute attribute = ab.buildSimple(null, value);
            builder.append(pd.getName(), attribute);
        }

        // handle joined metadata
        Object metadataValue = fi.getAttribute("metadata");
        if (metadataValue instanceof SimpleFeature) {
            SimpleFeature metadataFeature = (SimpleFeature) metadataValue;
            ab.setDescriptor((AttributeDescriptor) schema.getDescriptor(METADATA_PROPERTY_NAME));
            Attribute attribute = ab.buildSimple(null, metadataFeature.getAttribute("metadata"));
            builder.append(METADATA_PROPERTY_NAME, attribute);
        }
    }

    private SimpleFeature retypeLayerFeature(SimpleFeature layerFeature) {
        SimpleFeatureBuilder retypeBuilder = new SimpleFeatureBuilder(collectionLayerSchema);
        for (AttributeDescriptor att : layerFeature.getType().getAttributeDescriptors()) {
            final Name attName = att.getName();
            Object value = layerFeature.getAttribute(attName);
            final String localName = att.getLocalName();
            if (value != null && ("bands".equals(localName) || "browseBands".equals(localName))) {
                String[] split = ((String) value).split("\\s*,\\s*");
                retypeBuilder.set(attName, split);
            } else {
                retypeBuilder.set(attName, value);
            }
        }
        SimpleFeature retyped = retypeBuilder.buildFeature(layerFeature.getID());
        return retyped;
    }

    /** Maps a complex feature back to one or more simple features */
    protected SimpleFeature mapToMainSimpleFeature(Feature feature) throws IOException {
        // map the primary simple feature
        final SimpleFeatureType simpleSchema = getDelegateCollectionSource().getSchema();
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(simpleSchema);
        for (PropertyDescriptor pd : schema.getDescriptors()) {
            if (!(pd instanceof AttributeDescriptor)) {
                continue;
            }
            String localName = (String) pd.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
            if (localName == null) {
                continue;
            }
            Property property = feature.getProperty(pd.getName());
            if (pd instanceof GeometryDescriptor && property == null) {
                property = feature.getDefaultGeometryProperty();
            }
            if (property == null || property.getValue() == null) {
                continue;
            }
            fb.set(localName, property.getValue());
        }
        SimpleFeature sf = fb.buildFeature(null);

        return sf;
    }

    protected List<SimpleFeature> mapToSecondarySimpleFeatures(Feature feature) {
        // TODO: handle OGC links, but for the moment the code uses modifyFeatures to add those
        return Collections.emptyList();
    }

    @Override
    public List<FeatureId> addFeatures(FeatureCollection<FeatureType, Feature> featureCollection)
            throws IOException {
        // silly implementation assuming there will be only one insert at a time (which is
        // indeed the case for the current REST API), needs to be turned into a streaming
        // approach in case we want to handle larger data volumes
        final DataStore delegateStore = openSearchAccess.getDelegateStore();
        List<FeatureId> result = new ArrayList<>();
        try (FeatureIterator it = featureCollection.features()) {
            Feature feature = it.next();
            SimpleFeature simpleFeature = mapToMainSimpleFeature(feature);
            SimpleFeatureStore store = getDelegateCollectionStore();
            store.setTransaction(getTransaction());
            List<FeatureId> ids = store.addFeatures(DataUtilities.collection(simpleFeature));
            result.addAll(ids);

            List<SimpleFeature> simpleFeatures = mapToSecondarySimpleFeatures(feature);
            for (SimpleFeature sf : simpleFeatures) {
                SimpleFeatureStore fs =
                        (SimpleFeatureStore)
                                delegateStore.getFeatureSource(sf.getType().getTypeName());
                if (fs == null) {
                    throw new IOException(
                            "Could not find a delegate feature store for unmapped feature " + sf);
                }
                fs.setTransaction(getTransaction());
                fs.addFeatures(DataUtilities.collection(sf));
            }
        }

        featuresModified();

        return result;
    }

    @Override
    public void removeFeatures(Filter filter) throws IOException {
        Filter mappedFilter = mapFilterToDelegateSchema(filter);
        final List<String> collectionIdentifiers = getMainTypeDatabaseIdentifiers(mappedFilter);

        removeChildFeatures(collectionIdentifiers);

        // finally drop the collections themselves
        SimpleFeatureStore store = getDelegateCollectionStore();
        store.removeFeatures(mappedFilter);

        featuresModified();
    }

    /**
     * Removes the child features associated to a given main feature, the subclasses can override to
     * customize
     */
    protected void removeChildFeatures(final List<String> collectionIdentifiers)
            throws IOException {
        // remove all related metadata
        List<Filter> filters =
                collectionIdentifiers
                        .stream()
                        .map(id -> FF.equal(FF.property("mid"), FF.literal(id), false))
                        .collect(Collectors.toList());
        Filter metadataFilter = FF.or(filters);
        SimpleFeatureStore metadataStore = getFeatureStoreForTable(getMetadataTable());
        metadataStore.setTransaction(getTransaction());
        metadataStore.removeFeatures(metadataFilter);

        // remove all related OGC links
        filters =
                collectionIdentifiers
                        .stream()
                        .map(
                                id ->
                                        FF.equal(
                                                FF.property(getLinkForeignKey()),
                                                FF.literal(id),
                                                false))
                        .collect(Collectors.toList());
        Filter linksFilter = FF.or(filters);
        SimpleFeatureStore linkStore = getFeatureStoreForTable(getLinkTable());
        linkStore.setTransaction(getTransaction());
        linkStore.removeFeatures(linksFilter);
    }

    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter)
            throws IOException {
        modifyFeatures(new Name[] {attributeName}, new Object[] {attributeValue}, filter);
    }

    @Override
    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter)
            throws IOException {
        Filter mappedFilter = mapFilterToDelegateSchema(filter);

        // map names to local simple feature, store out the delegate ones
        List<String> localNames = new ArrayList<>();
        List<Object> localValues = new ArrayList<>();
        for (int i = 0; i < attributeNames.length; i++) {
            Name name = attributeNames[i];
            Object value = attributeValues[i];
            // sub-table related updates
            if (OpenSearchAccess.METADATA_PROPERTY_NAME.equals(name)) {
                final String tableName = getMetadataTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.id(FF.featureId(tableName + "." + id)),
                        (id, secondaryStore) -> {
                            SimpleFeatureBuilder fb =
                                    new SimpleFeatureBuilder(secondaryStore.getSchema());
                            fb.set("mid", id);
                            fb.set("metadata", value);
                            SimpleFeature metadataFeature = fb.buildFeature(tableName + "." + id);
                            metadataFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                            return DataUtilities.collection(metadataFeature);
                        });

                // this one has been handled
                continue;
            }
            if (OpenSearchAccess.QUICKLOOK_PROPERTY_NAME.equals(name)) {
                final String tableName = getThumbnailTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.id(FF.featureId(tableName + "." + id)),
                        (id, secondaryStore) -> {
                            SimpleFeatureBuilder fb =
                                    new SimpleFeatureBuilder(secondaryStore.getSchema());
                            fb.set("tid", id);
                            fb.set("thumb", value);
                            SimpleFeature thumbnailFeature = fb.buildFeature(tableName + "." + id);
                            thumbnailFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                            return DataUtilities.collection(thumbnailFeature);
                        });

                // this one done
                continue;
            }
            if (LAYERS_PROPERTY_NAME.equals(name)) {
                final String tableName = getCollectionLayerTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.equal(FF.property("cid"), FF.literal(id), false),
                        (id, layersStore) -> {
                            SimpleFeatureCollection layers = (SimpleFeatureCollection) value;
                            SimpleFeatureBuilder fb =
                                    new SimpleFeatureBuilder(layersStore.getSchema());

                            ListFeatureCollection mappedLayers =
                                    new ListFeatureCollection(layersStore.getSchema());
                            layers.accepts(
                                    f -> {
                                        SimpleFeature sf = (SimpleFeature) f;
                                        for (Property p : sf.getProperties()) {
                                            String attributeName = p.getName().getLocalPart();
                                            Object attributeValue = p.getValue();
                                            if (("bands".equals(attributeName)
                                                            || "browseBands".equals(attributeName))
                                                    && attributeValue instanceof String[]) {
                                                final String[] array = (String[]) attributeValue;
                                                attributeValue =
                                                        Arrays.stream(array)
                                                                .collect(Collectors.joining(","));
                                            }
                                            fb.set(attributeName, attributeValue);
                                        }
                                        fb.set("cid", id);
                                        SimpleFeature layerFeature =
                                                fb.buildFeature(tableName + "." + id);
                                        mappedLayers.add(layerFeature);
                                    },
                                    null);
                            return mappedLayers;
                        });

                // this one done
                continue;
            }
            if (OpenSearchAccess.OGC_LINKS_PROPERTY_NAME.equals(name)) {
                final String tableName = getLinkTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.equal(FF.property(getLinkForeignKey()), FF.literal(id), true),
                        (id, linksStore) -> {
                            SimpleFeatureCollection links = (SimpleFeatureCollection) value;
                            SimpleFeatureBuilder fb =
                                    new SimpleFeatureBuilder(linksStore.getSchema());
                            ListFeatureCollection mappedLinks =
                                    new ListFeatureCollection(linksStore.getSchema());
                            links.accepts(
                                    f -> {
                                        SimpleFeature sf = (SimpleFeature) f;
                                        for (AttributeDescriptor ad :
                                                linksStore.getSchema().getAttributeDescriptors()) {
                                            if (sf.getFeatureType().getDescriptor(ad.getLocalName())
                                                    != null) {
                                                fb.set(
                                                        ad.getLocalName(),
                                                        sf.getAttribute(ad.getLocalName()));
                                            }
                                        }
                                        fb.set(getLinkForeignKey(), id);
                                        SimpleFeature mappedLink = fb.buildFeature(null);
                                        mappedLinks.add(mappedLink);
                                    },
                                    null);
                            return mappedLinks;
                        });

                // this one has been handled
                continue;
            }
            if (modifySecondaryAttribute(name, value, mappedFilter)) {
                continue;
            }

            PropertyDescriptor descriptor = schema.getDescriptor(name);
            if (!(descriptor instanceof AttributeDescriptor)) {
                throw new IllegalArgumentException(
                        "Did not expect modification on attribute " + name);
            }
            String localName =
                    (String) descriptor.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
            if (localName == null) {
                throw new IllegalArgumentException(
                        "Did not expect modification on attribute " + name);
            }
            localNames.add(localName);
            localValues.add(value);
        }

        // update primary table
        if (localNames.size() > 0) {
            String[] nameArray = (String[]) localNames.toArray(new String[localNames.size()]);
            Object[] valueArray = (Object[]) localValues.toArray(new Object[localValues.size()]);
            getDelegateCollectionStore().modifyFeatures(nameArray, valueArray, mappedFilter);
        }

        featuresModified();
    }

    /**
     * Hooks for subclasses that need to track feature modification and deletion. By default it does
     * nothing.
     */
    protected void featuresModified() {}

    /** Allows subclasses to handle other attributes mapped in secondary tables */
    protected boolean modifySecondaryAttribute(Name name, Object value, Filter mappedFilter)
            throws IOException {
        return false;
    }

    /**
     * Modifies the contents of a secondary table by removing the old values completely and adding
     * the new mapped values as built by the feature build
     *
     * @param mainTypeFilter The filter to locate the main object
     * @param value The value to be mapped and replaced
     * @param tableName The secondary table name
     * @param secondaryTableFilterSupplier A supplier going from the the main filter to the
     *     secondary table one
     * @param featureBuilder Transforms the complex feature value in a feature collection for the
     *     secondary table, it will be inserted in place of the old values
     */
    protected void modifySecondaryTable(
            Filter mainTypeFilter,
            Object value,
            final String tableName,
            Function<String, Filter> secondaryTableFilterSupplier,
            IOBiFunction<String, SimpleFeatureStore, SimpleFeatureCollection> featureBuilder)
            throws IOException {
        SimpleFeatureStore secondaryStore = getFeatureStoreForTable(tableName);
        secondaryStore.setTransaction(getTransaction());
        for (String id : getMainTypeDatabaseIdentifiers(mainTypeFilter)) {
            Filter secondaryTableFilter = secondaryTableFilterSupplier.apply(id);
            // make it a delete and eventually insert case, easier to code and no more queries
            // than checking if the metadata was already there to perform an update
            secondaryStore.removeFeatures(secondaryTableFilter);
            if (value != null) {
                SimpleFeatureCollection collection = featureBuilder.apply(id, secondaryStore);
                secondaryStore.addFeatures(collection);
            }
        }
    }

    protected SimpleFeatureStore getFeatureStoreForTable(final String table) throws IOException {
        SimpleFeatureStore featureStore =
                (SimpleFeatureStore) openSearchAccess.getDelegateStore().getFeatureSource(table);
        return featureStore;
    }

    public List<String> getMainTypeDatabaseIdentifiers(Filter filter) throws IOException {
        SimpleFeatureSource fs = getDelegateCollectionSource();
        Transaction t = getTransaction();
        if (t != Transaction.AUTO_COMMIT && t != null) {
            ((SimpleFeatureStore) fs).setTransaction(transaction);
        }
        SimpleFeatureCollection idFeatureCollection = fs.getFeatures(filter);
        List<String> result = new ArrayList<>();
        try (SimpleFeatureIterator fi = idFeatureCollection.features()) {
            while (fi.hasNext()) {
                SimpleFeature f = fi.next();
                String progressive = f.getIdentifier().toString().split("\\.")[1];
                result.add(progressive);
            }
        }
        return result;
    }

    @Override
    public void setFeatures(FeatureReader<FeatureType, Feature> reader) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    @Override
    public Transaction getTransaction() {
        if (transaction == null) {
            return Transaction.AUTO_COMMIT;
        } else {
            return this.transaction;
        }
    }

    public SimpleFeatureType getCollectionLayerSchema() {
        return collectionLayerSchema;
    }

    public SimpleFeatureType getOGCLinksSchema() {
        return linkFeatureType;
    }
}
