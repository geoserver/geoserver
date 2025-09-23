/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.EO_PREFIX;
import static org.geoserver.opensearch.eo.store.JDBCOpenSearchAccess.FF;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYERS;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYER_DESCRIPTION;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.LAYER_TITLE;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.OGC_LINKS_PROPERTY_NAME;
import static org.geoserver.opensearch.eo.store.OpenSearchAccess.STYLES;

import java.awt.RenderingHints.Key;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.config.ServiceInfo;
import org.geotools.api.data.DataAccess;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureListener;
import org.geotools.api.data.FeatureReader;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Join;
import org.geotools.api.data.Join.Type;
import org.geotools.api.data.Query;
import org.geotools.api.data.QueryCapabilities;
import org.geotools.api.data.ResourceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Attribute;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.FeatureFactory;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.Name;
import org.geotools.api.feature.type.PropertyDescriptor;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.api.filter.identity.FeatureId;
import org.geotools.api.filter.sort.SortBy;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.api.style.Style;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultResourceInfo;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.EmptyFeatureCollection;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeBuilder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;

/**
 * Base class for the collection and product specific feature source wrappers
 *
 * @author Andrea Aime - GeoSolutions
 */
public abstract class AbstractMappingStore implements FeatureStore<FeatureType, Feature> {

    static final FeatureFactory FEATURE_FACTORY = CommonFactoryFinder.getFeatureFactory(null);

    /**
     * List of well known service names, used to build the services feature type (only services for raster layers listed
     * here)
     */
    static final Set<String> SERVICE_NAMES = Set.of("wms", "maps", "wcs", "coverages", "wmts", "tiles");

    static final Logger LOGGER = Logging.getLogger(AbstractMappingStore.class);
    private final FeatureType servicesType;

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

    private FeatureType collectionLayerComplexSchema;

    private SimpleFeatureType styleType;

    private Transaction transaction;

    public AbstractMappingStore(JDBCOpenSearchAccess openSearchAccess, FeatureType schema) throws IOException {
        this.openSearchAccess = openSearchAccess;
        this.schema = schema;
        this.propertyMapper = new SourcePropertyMapper(this.schema);
        this.defaultSort = buildDefaultSort(this.schema);
        this.linkFeatureType = buildLinkFeatureType();
        this.styleType = buildStyleType(openSearchAccess);
        this.collectionLayerSchema = buildCollectionLayerFeatureType(openSearchAccess);
        this.collectionLayerComplexSchema = buildComplexLayerType(collectionLayerSchema, styleType, openSearchAccess);
        this.servicesType = buildServicesType(openSearchAccess);
    }

    static SimpleFeatureType buildStyleType(JDBCOpenSearchAccess openSearchAccess) throws IOException {
        try {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(STYLES);
            b.setNamespaceURI(openSearchAccess.getNamespaceURI());
            b.add("name", String.class);
            b.add("title", String.class);
            return b.buildFeatureType();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the styles feature type.", e);
        }
    }

    private static SimpleFeatureType buildServiceType(String service, JDBCOpenSearchAccess openSearchAccess)
            throws IOException {
        try {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            b.setName(service);
            b.setNamespaceURI(openSearchAccess.getNamespaceURI());
            b.add("enabled", Boolean.class);
            b.maxOccurs(Integer.MAX_VALUE);
            b.add("formats", String.class);
            return b.buildFeatureType();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the styles feature type.", e);
        }
    }

    private static FeatureType buildServicesType(JDBCOpenSearchAccess openSearchAccess) throws IOException {
        try {
            OrderedTypeBuilder b = new OrderedTypeBuilder();
            b.setNamespaceURI(openSearchAccess.getNamespaceURI());
            b.setName("services");
            List<String> serviceNames = getServiceNames(openSearchAccess);
            for (String service : serviceNames) {
                SimpleFeatureType simpleServiceType = buildServiceType(service, openSearchAccess);
                FeatureType serviceType =
                        JDBCOpenSearchAccess.applyNamespace(openSearchAccess.getNamespaceURI(), simpleServiceType);
                b.setMinOccurs(0);
                b.addAttribute(service, serviceType);
            }
            return b.feature();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the styles feature type.", e);
        }
    }

    private static List<String> getServiceNames(JDBCOpenSearchAccess openSearchAccess) {
        Collection<? extends ServiceInfo> services =
                openSearchAccess.getGeoServer().getServices();
        List<String> names =
                services.stream().map(s -> s.getName().toLowerCase()).collect(Collectors.toList());
        names.retainAll(SERVICE_NAMES);
        return names;
    }

    static SimpleFeatureType buildCollectionLayerFeatureType(JDBCOpenSearchAccess openSearchAccess) throws IOException {
        SimpleFeatureType source = openSearchAccess.getDelegateStore().getSchema("collection_layer");
        try {
            SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
            for (AttributeDescriptor ad : source.getAttributeDescriptors()) {
                if ("bands".equals(ad.getLocalName()) || "browseBands".equals(ad.getLocalName())) {
                    b.add(ad.getLocalName(), String[].class);
                } else {
                    b.add(ad);
                }
            }

            b.setName(openSearchAccess.getName(LAYERS));
            return b.buildFeatureType();
        } catch (Exception e) {
            throw new DataSourceException("Could not build the renamed feature type.", e);
        }
    }

    static FeatureType buildComplexLayerType(
            SimpleFeatureType collectionLayerSchema, SimpleFeatureType styleType, JDBCOpenSearchAccess openSearchAccess)
            throws IOException {
        try {
            OrderedTypeBuilder b = new OrderedTypeBuilder();
            b.setNamespaceURI(openSearchAccess.getNamespaceURI());
            b.setName(LAYERS);
            AttributeTypeBuilder ab = new AttributeTypeBuilder();
            String defaultNamespace = openSearchAccess.getNamespaceURI();
            for (AttributeDescriptor ad : collectionLayerSchema.getAttributeDescriptors()) {
                ab.init(ad);
                ab.setNamespaceURI(defaultNamespace);
                NameImpl name = new NameImpl(defaultNamespace, ad.getLocalName());
                AttributeDescriptor newDescriptor = ab.buildDescriptor(name, ab.buildType());
                b.add(newDescriptor);
            }

            // title and description
            b.setMinOccurs(0);
            b.addAttribute(defaultNamespace, LAYER_TITLE, String.class);
            b.setMinOccurs(0);
            b.addAttribute(defaultNamespace, LAYER_DESCRIPTION, String.class);

            // list of styles
            Name stylesName = new NameImpl(openSearchAccess.namespaceURI, STYLES);
            AttributeDescriptor stylesDescriptor =
                    JDBCOpenSearchAccess.buildFeatureDescriptor(stylesName, EO_PREFIX, styleType, 1, Integer.MAX_VALUE);
            b.add(stylesDescriptor);

            // services, keyed by services name
            b.setMinOccurs(0);
            FeatureType servicesType = buildServicesType(openSearchAccess);
            b.addAttribute(servicesType.getName(), servicesType);

            FeatureType feature = b.feature();

            return feature;
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
        return new SortBy[] {FF.sort(timeStart, SortOrder.DESCENDING), FF.sort(identifier, SortOrder.ASCENDING)};
    }

    @Override
    public Name getName() {
        return schema.getName();
    }

    @Override
    public ResourceInfo getInfo() {
        try {
            SimpleFeatureSource featureSource = getDelegateSource();
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
    public abstract SimpleFeatureSource getDelegateSource() throws IOException;

    protected SimpleFeatureStore getDelegateCollectionStore() throws IOException {
        SimpleFeatureSource simpleFeatureSource = getDelegateSource();
        if (simpleFeatureSource instanceof WorkspaceFeatureSource source) {
            simpleFeatureSource = source.getDelegate();
        }
        SimpleFeatureStore fs = (SimpleFeatureStore) simpleFeatureSource;
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
        QueryCapabilities result = new QueryCapabilities() {
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
        return getDelegateSource().getBounds();
    }

    @Override
    public ReferencedEnvelope getBounds(Query query) throws IOException {
        Query mapped = mapToSimpleCollectionQuery(query, false);
        return getDelegateSource().getBounds(mapped);
    }

    @Override
    public Set<Key> getSupportedHints() {
        try {
            return getDelegateSource().getSupportedHints();
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public int getCount(Query query) throws IOException {
        final Query mappedQuery = mapToSimpleCollectionQuery(query, false);
        return getDelegateSource().getCount(mappedQuery);
    }

    /**
     * Maps query back the main underlying feature source. When updating this method for joins, update also #needsJoins
     */
    protected Query mapToSimpleCollectionQuery(Query query, boolean addJoins) throws IOException {
        Query result = new Query(getDelegateSource().getSchema().getTypeName());
        final Filter originalFilter = query.getFilter();
        if (originalFilter != null) {
            Filter mappedFilter = mapFilterToDelegateSchema(originalFilter);
            result.setFilter(mappedFilter);
        }
        if (query.getPropertyNames() != null && query.getPropertyNames().length > 0) {
            String[] mappedPropertyNames = Arrays.stream(query.getPropertyNames())
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
            SortBy[] mappedSortBy = Arrays.stream(query.getSortBy())
                    .map(sb -> {
                        if (sb == SortBy.NATURAL_ORDER || sb == SortBy.REVERSE_ORDER) {
                            return sb;
                        } else {
                            String name = sb.getPropertyName().getPropertyName();
                            String mappedName = propertyMapper.getSourceName(name);
                            if (mappedName == null) {
                                throw new IllegalArgumentException("Cannot sort on " + name);
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

        if (addJoins && needsJoins(query)) {
            // join output layer, if necessary
            if (hasOutputProperty(query, openSearchAccess.getName(LAYERS), true)) {
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
                Filter filter = FF.equal(FF.property("id"), FF.property("link." + linkForeignKey), true);
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

    /**
     * Checks if the query at hand needs to join with other tables, or not. Subclasses should override if they override
     * {@link #mapToSimpleCollectionQuery(Query, boolean)} and add extra joins
     */
    protected boolean needsJoins(Query query) {
        return hasOutputProperty(query, openSearchAccess.getName(LAYERS), true)
                || hasOutputProperty(query, OGC_LINKS_PROPERTY_NAME, true);
    }

    private Filter mapFilterToDelegateSchema(final Filter filter) {
        MappingFilterVisitor visitor = new MappingFilterVisitor(propertyMapper);
        Filter mappedFilter = (Filter) filter.accept(visitor, null);
        return mappedFilter;
    }

    /** Name of the table to join in case the {@link OpenSearchAccess#LAYERS} property is requested */
    protected String getCollectionLayerTable() {
        return "collection_layer";
    }

    /**
     * Name of the link table to join in case the {@link OpenSearchAccess#OGC_LINKS_PROPERTY_NAME} property is requested
     */
    protected abstract String getLinkTable();

    /**
     * Name of the field linking back to the main table in case the {@link OpenSearchAccess#OGC_LINKS_PROPERTY_NAME}
     * property is requested
     */
    protected abstract String getLinkForeignKey();

    /** Name of the thumbnail table */
    protected abstract String getThumbnailTable();

    /**
     * Searches for an optional property among the query attributes. Returns true only if the property is explicitly
     * listed
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
        // fast path for query with no paging or with no joins
        if (!needsJoins(query) || (query.getStartIndex() == null && query.getMaxFeatures() == Integer.MAX_VALUE)) {
            Query mappedQuery = mapToSimpleCollectionQuery(query, true);
            SimpleFeatureCollection fc = getDelegateSource().getFeatures(mappedQuery);
            HashMap<String, Object> mapperState = new HashMap<>();
            return new MappingFeatureCollection(schema, fc, it -> mapToComplexFeature(it, mapperState));
        }

        // Paging is active, and joins cause extra records to be returned, so we need to
        // first collect the collection/product ids for the current page, and then use a
        // paging-less query

        // first get the ids of the features we are going to return, no joins to support paging
        Query idsQuery = mapToSimpleCollectionQuery(query, false);
        // uncommenting causes a ClassCastException, need to figure out why
        idsQuery.setPropertyNames("eoIdentifier");
        SimpleFeatureCollection idFeatureCollection = getDelegateSource().getFeatures(idsQuery);

        Set<FeatureId> ids = new LinkedHashSet<>();
        idFeatureCollection.accepts(f -> ids.add(f.getIdentifier()), null);

        // if no features, return immediately
        SimpleFeatureCollection fc;
        if (ids.isEmpty()) {
            fc = new EmptyFeatureCollection(getDelegateSource().getSchema());
        } else {
            // the run a joined query with the specified ids
            Query dataQuery = mapToSimpleCollectionQuery(query, true);
            dataQuery.setFilter(FF.id(ids));
            fc = getDelegateSource().getFeatures(dataQuery);
        }

        // the mapper state allows the simple to complex map funcion to retain state across
        // feature mappings (e.g. for caching)
        HashMap<String, Object> mapperState = new HashMap<>();
        return new MappingFeatureCollection(schema, fc, it -> mapToComplexFeature(it, mapperState));
    }

    /** Maps the underlying features (eventually joined) to the output complex feature */
    protected Feature mapToComplexFeature(PushbackFeatureIterator<SimpleFeature> it, Map<String, Object> mapperState) {
        SimpleFeature fi = it.next();

        ComplexFeatureBuilder builder = new ComplexFeatureBuilder(schema, FEATURE_FACTORY);

        // allow subclasses to perform custom mappings while reusing the common ones
        mapPropertiesToComplex(builder, fi, mapperState);

        // the OGC links can be more than one
        Set<SimpleFeature> links = new LinkedHashSet<>();
        Set<SimpleFeature> layers = new LinkedHashSet<>();
        for (; ; ) {
            Object link = fi.getAttribute("link");
            Object layer = fi.getAttribute("layer");

            // handle joined layer if any
            if (layer instanceof SimpleFeature feature) {
                layers.add(feature);
            }

            if (link instanceof SimpleFeature feature) {
                links.add(feature);
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
            Feature retyped = retypeLayerFeature(layerFeature);
            builder.append(openSearchAccess.getName(LAYERS), retyped);
        }

        for (SimpleFeature link : links) {
            SimpleFeature linkFeature = SimpleFeatureBuilder.retype((SimpleFeature) link, linkFeatureType);
            builder.append(OGC_LINKS_PROPERTY_NAME, linkFeature);
        }

        //
        Feature feature = builder.buildFeature(fi.getID());
        return feature;
    }

    /** Performs the common mappings, subclasses can override to add more */
    protected void mapPropertiesToComplex(
            ComplexFeatureBuilder builder, SimpleFeature fi, Map<String, Object> mapperState) {
        AttributeBuilder ab = new AttributeBuilder(FEATURE_FACTORY);
        FeatureType schema = builder.getFeatureType();
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
    }

    private Feature retypeLayerFeature(SimpleFeature layerFeature) {
        ComplexFeatureBuilder layerBuilder = new ComplexFeatureBuilder(collectionLayerComplexSchema, FEATURE_FACTORY);
        for (Property p : layerFeature.getProperties()) {
            final Name attName = p.getName();
            Object value = layerFeature.getAttribute(attName);
            final String localName = attName.getLocalPart();
            if (value != null && ("bands".equals(localName) || "browseBands".equals(localName))) {
                value = ((String) value).split("\\s*,\\s*");
            }
            layerBuilder.append(localName, value);
        }

        Catalog catalog = openSearchAccess.getCatalog();
        String workspace = (String) layerFeature.getAttribute("workspace");
        String name = (String) layerFeature.getAttribute("layer");
        LayerInfo li = catalog.getLayerByName(workspace + ":" + name);
        if (li != null) {
            // start with title and descriptions
            org.geoserver.catalog.ResourceInfo ri = li.getResource();
            layerBuilder.append(LAYER_TITLE, ri.getTitle());
            layerBuilder.append(LAYER_DESCRIPTION, ri.getDescription());

            // go build the style features
            LinkedHashSet<StyleInfo> styles = new LinkedHashSet<>();
            styles.add(li.getDefaultStyle());
            styles.addAll(li.getStyles());
            Name stylesName = new NameImpl(openSearchAccess.namespaceURI, STYLES);
            if (!styles.isEmpty()) {
                SimpleFeatureBuilder styleBuilder = new SimpleFeatureBuilder(styleType);
                for (StyleInfo style : styles) {
                    styleBuilder.set("name", style.getName());
                    styleBuilder.set("title", getStyleTitle(style));
                    layerBuilder.append(stylesName, styleBuilder.buildFeature(null));
                }
            }

            // go build the service features
            ComplexFeatureBuilder servicesBuilder = new ComplexFeatureBuilder(servicesType, FEATURE_FACTORY);
            Set<String> disabledServices =
                    ri.getDisabledServices().stream().map(s -> s.toLowerCase()).collect(Collectors.toSet());
            for (ServiceInfo service : openSearchAccess.getGeoServer().getServices()) {
                String serviceName = service.getName().toLowerCase();
                PropertyDescriptor serviceDescriptor = servicesType.getDescriptor(serviceName);
                if (serviceDescriptor == null) continue;
                ComplexFeatureBuilder serviceBuilder =
                        new ComplexFeatureBuilder((FeatureType) serviceDescriptor.getType(), FEATURE_FACTORY);
                boolean enabled = service.isEnabled() && !disabledServices.contains(serviceName);
                serviceBuilder.append("enabled", enabled);
                OutputFormatProvider.getFormatNames(serviceName, li).forEach(f -> {
                    serviceBuilder.append("formats", f);
                });
                servicesBuilder.append(serviceDescriptor.getName(), serviceBuilder.buildFeature(null));
            }
            layerBuilder.append(servicesType.getName(), servicesBuilder.buildFeature(null));
        }

        return layerBuilder.buildFeature(layerFeature.getID());
    }

    private String getStyleTitle(StyleInfo si) {
        try {
            Style style = si.getStyle();
            return Optional.ofNullable(style.getDescription())
                    .map(d -> d.getTitle())
                    .map(t -> t.toString())
                    .orElse(style.getName());
        } catch (Exception e) {
            LOGGER.log(Level.WARNING, "Could not get title for style " + si.getName(), e);
            return null;
        }
    }

    /** Maps a complex feature back to one or more simple features */
    protected SimpleFeature mapToMainSimpleFeature(Feature feature) throws IOException {
        // map the primary simple feature
        final SimpleFeatureType simpleSchema = getDelegateSource().getSchema();
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
    public List<FeatureId> addFeatures(FeatureCollection<FeatureType, Feature> featureCollection) throws IOException {
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
                SimpleFeatureStore fs = (SimpleFeatureStore)
                        delegateStore.getFeatureSource(sf.getType().getTypeName());
                if (fs == null) {
                    throw new IOException("Could not find a delegate feature store for unmapped feature " + sf);
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

    /** Removes the child features associated to a given main feature, the subclasses can override to customize */
    protected void removeChildFeatures(final List<String> collectionIdentifiers) throws IOException {
        // remove all related OGC links
        List<Filter> filters = collectionIdentifiers.stream()
                .map(id -> FF.equal(FF.property(getLinkForeignKey()), FF.literal(id), false))
                .collect(Collectors.toList());
        Filter linksFilter = FF.or(filters);
        SimpleFeatureStore linkStore = getFeatureStoreForTable(getLinkTable());
        linkStore.setTransaction(getTransaction());
        linkStore.removeFeatures(linksFilter);
    }

    @Override
    public void modifyFeatures(Name attributeName, Object attributeValue, Filter filter) throws IOException {
        modifyFeatures(new Name[] {attributeName}, new Object[] {attributeValue}, filter);
    }

    @Override
    public void modifyFeatures(Name[] attributeNames, Object[] attributeValues, Filter filter) throws IOException {
        Filter mappedFilter = mapFilterToDelegateSchema(filter);

        // map names to local simple feature, store out the delegate ones
        List<String> localNames = new ArrayList<>();
        List<Object> localValues = new ArrayList<>();
        Name layersName = openSearchAccess.getName(LAYERS);
        for (int i = 0; i < attributeNames.length; i++) {
            Name name = attributeNames[i];
            Object value = attributeValues[i];
            // sub-table related updates
            if (OpenSearchAccess.QUICKLOOK_PROPERTY_NAME.equals(name)) {
                final String tableName = getThumbnailTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.id(FF.featureId(tableName + "." + id)),
                        (id, secondaryStore) -> {
                            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(secondaryStore.getSchema());
                            fb.set("tid", id);
                            fb.set("thumb", value);
                            SimpleFeature thumbnailFeature = fb.buildFeature(tableName + "." + id);
                            thumbnailFeature.getUserData().put(Hints.USE_PROVIDED_FID, true);
                            return DataUtilities.collection(thumbnailFeature);
                        });

                // this one done
                continue;
            }
            if (layersName.equals(name)) {
                final String tableName = getCollectionLayerTable();
                modifySecondaryTable(
                        mappedFilter,
                        value,
                        tableName,
                        id -> FF.equal(FF.property("cid"), FF.literal(id), false),
                        (id, layersStore) -> {
                            SimpleFeatureCollection layers = (SimpleFeatureCollection) value;
                            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(layersStore.getSchema());

                            ListFeatureCollection mappedLayers = new ListFeatureCollection(layersStore.getSchema());
                            layers.accepts(
                                    f -> mapCollectionLayer(id, (SimpleFeature) f, fb, tableName, mappedLayers), null);
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
                            SimpleFeatureBuilder fb = new SimpleFeatureBuilder(linksStore.getSchema());
                            ListFeatureCollection mappedLinks = new ListFeatureCollection(linksStore.getSchema());
                            links.accepts(
                                    f -> {
                                        SimpleFeature sf = (SimpleFeature) f;
                                        for (AttributeDescriptor ad :
                                                linksStore.getSchema().getAttributeDescriptors()) {
                                            if (sf.getFeatureType().getDescriptor(ad.getLocalName()) != null) {
                                                fb.set(ad.getLocalName(), sf.getAttribute(ad.getLocalName()));
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
                throw new IllegalArgumentException("Did not expect modification on attribute " + name);
            }
            String localName = (String) descriptor.getUserData().get(JDBCOpenSearchAccess.SOURCE_ATTRIBUTE);
            if (localName == null) {
                throw new IllegalArgumentException("Did not expect modification on attribute " + name);
            }
            localNames.add(localName);
            localValues.add(value);
        }

        // update primary table
        if (!localNames.isEmpty()) {
            String[] nameArray = localNames.toArray(new String[localNames.size()]);
            Object[] valueArray = localValues.toArray(new Object[localValues.size()]);
            getDelegateCollectionStore().modifyFeatures(nameArray, valueArray, mappedFilter);
        }

        featuresModified();
    }

    private static void mapCollectionLayer(
            String id, SimpleFeature f, SimpleFeatureBuilder fb, String tableName, ListFeatureCollection mappedLayers) {
        SimpleFeatureType ft = f.getFeatureType();
        for (AttributeDescriptor at : ft.getAttributeDescriptors()) {
            String attributeName = at.getLocalName();
            Object attributeValue = f.getAttribute(attributeName);
            if (("bands".equals(attributeName) || "browseBands".equals(attributeName))
                    && attributeValue instanceof String[] array) {
                attributeValue = Arrays.stream(array).collect(Collectors.joining(","));
            }
            if (!isSynthentic(at)) {
                fb.set(attributeName, attributeValue);
            }
        }
        fb.set("cid", id);
        SimpleFeature layerFeature = fb.buildFeature(tableName + "." + id);
        mappedLayers.add(layerFeature);
    }

    private static boolean isSynthentic(AttributeDescriptor at) {
        return Optional.ofNullable(at.getUserData())
                        .map(ud -> ud.get(JDBCOpenSearchAccess.SYNTHETIC))
                        .orElse(false)
                == Boolean.TRUE;
    }

    /** Hooks for subclasses that need to track feature modification and deletion. By default it does nothing. */
    protected void featuresModified() {}

    /** Allows subclasses to handle other attributes mapped in secondary tables */
    protected boolean modifySecondaryAttribute(Name name, Object value, Filter mappedFilter) throws IOException {
        return false;
    }

    /**
     * Modifies the contents of a secondary table by removing the old values completely and adding the new mapped values
     * as built by the feature build
     *
     * @param mainTypeFilter The filter to locate the main object
     * @param value The value to be mapped and replaced
     * @param tableName The secondary table name
     * @param secondaryTableFilterSupplier A supplier going from the main filter to the secondary table one
     * @param featureBuilder Transforms the complex feature value in a feature collection for the secondary table, it
     *     will be inserted in place of the old values
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
        SimpleFeatureSource fs = getDelegateSource();
        Transaction t = getTransaction();
        if (t != Transaction.AUTO_COMMIT && t != null) {
            if (fs instanceof WorkspaceFeatureSource source) {
                fs = source.getDelegate();
            }
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
