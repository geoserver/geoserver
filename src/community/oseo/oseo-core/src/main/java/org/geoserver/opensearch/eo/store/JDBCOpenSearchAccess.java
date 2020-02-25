/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import com.google.common.base.Objects;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.ProductClass;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.FeatureStore;
import org.geotools.data.Query;
import org.geotools.data.Repository;
import org.geotools.data.ServiceInfo;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.SoftValueHashMap;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;

/**
 * A data store building OpenSearch for EO records based on a wrapped data store providing all
 * expected tables in form of simple features (and leveraging joins to put them together into
 * complex features as needed).
 *
 * <p>The delegate store is fetched on demand to avoid being caught in a ResourcePool dispose
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccess implements OpenSearchAccess {

    protected static FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    public static final String COLLECTION = "collection";

    public static final String PRODUCT = "product";

    public static final String GRANULE = "granule";

    static final String EO_PREFIX = "eo";

    static final String SAR_PREFIX = "sar";

    static final String SOURCE_ATTRIBUTE = "sourceAttribute";

    static final String COLLECTION_NAME = "name";

    Repository repository;

    Name delegateStoreName;

    String namespaceURI;

    FeatureType collectionFeatureType;

    FeatureType productFeatureType;

    List<Name> typeNames;

    private GeoServer geoServer;

    private LowercasingDataStore delegateStoreCache;
    private SoftValueHashMap<Name, SimpleFeatureSource> featureSourceCache =
            new SoftValueHashMap<>();

    public JDBCOpenSearchAccess(
            Repository repository, Name delegateStoreName, String namespaceURI, GeoServer geoServer)
            throws IOException {
        this.repository = repository;
        this.delegateStoreName = delegateStoreName;
        this.namespaceURI = namespaceURI;
        this.geoServer = geoServer;

        // check the expected feature types are available
        DataStore delegate = getDelegateStore();
        List<String> missingTables = getMissingRequiredTables(delegate, COLLECTION, PRODUCT);
        if (!missingTables.isEmpty()) {
            throw new IOException("Missing required tables in the backing store " + missingTables);
        }

        collectionFeatureType = buildCollectionFeatureType(delegate);
        productFeatureType = buildProductFeatureType(delegate);
    }

    String getNamespaceURI() {
        return namespaceURI;
    }

    private FeatureType buildCollectionFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(COLLECTION);

        TypeBuilder typeBuilder = new OrderedTypeBuilder();

        // map the source attributes
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            AttributeTypeBuilder ab = new AttributeTypeBuilder();
            String name = ad.getLocalName();
            String namespaceURI = this.namespaceURI;
            if (name.startsWith(EO_PREFIX)) {
                name = name.substring(EO_PREFIX.length());
                char c[] = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                name = new String(c);
                namespaceURI = EO_NAMESPACE;
            }
            // get a more predictable name structure (will have to do something for oracle
            // like names too I guess)
            if (StringUtils.isAllUpperCase(name)) {
                name = name.toLowerCase();
            }
            // map into output type
            ab.init(ad);
            ab.setMinOccurs(0);
            ab.name(name).namespaceURI(namespaceURI).userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            AttributeDescriptor mappedDescriptor;
            if (ad instanceof GeometryDescriptor) {
                GeometryType at = ab.buildGeometryType();
                ab.setCRS(((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            } else {
                AttributeType at = ab.buildType();
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            }

            typeBuilder.add(mappedDescriptor);
        }

        // adding the metadata property
        AttributeDescriptor metadataDescriptor =
                buildSimpleDescriptor(METADATA_PROPERTY_NAME, String.class);
        typeBuilder.add(metadataDescriptor);

        // adding the layer publishing property
        Name layerPropertyName = new NameImpl(this.namespaceURI, OpenSearchAccess.LAYERS);
        AttributeDescriptor layerDescriptor =
                buildFeatureListDescriptor(
                        LAYERS_PROPERTY_NAME, delegate.getSchema("collection_ogclink"));
        typeBuilder.add(layerDescriptor);

        // map OGC links
        AttributeDescriptor linksDescriptor =
                buildFeatureListDescriptor(
                        OGC_LINKS_PROPERTY_NAME, delegate.getSchema("collection_ogclink"));
        typeBuilder.add(linksDescriptor);

        typeBuilder.setName(COLLECTION);
        typeBuilder.setNamespaceURI(namespaceURI);
        return typeBuilder.feature();
    }

    private AttributeDescriptor buildSimpleDescriptor(Name name, Class binding) {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        ab.name(name.getLocalPart()).namespaceURI(name.getNamespaceURI());
        ab.setBinding(binding);
        AttributeDescriptor descriptor = ab.buildDescriptor(name, ab.buildType());
        return descriptor;
    }

    private AttributeDescriptor buildFeatureListDescriptor(Name name, SimpleFeatureType schema) {
        return buildFeatureDescriptor(name, schema, 0, Integer.MAX_VALUE);
    }

    private AttributeDescriptor buildFeatureDescriptor(
            Name name, SimpleFeatureType schema, int minOccurs, int maxOccurs) {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        ab.name(name.getLocalPart()).namespaceURI(name.getNamespaceURI());
        ab.setMinOccurs(minOccurs);
        ab.setMaxOccurs(maxOccurs);
        AttributeDescriptor descriptor = ab.buildDescriptor(name, schema);
        return descriptor;
    }

    private FeatureType buildProductFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(PRODUCT);

        TypeBuilder typeBuilder = new OrderedTypeBuilder();

        // map the source attributes
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            String namespaceURI = this.namespaceURI;
            // hack to avoid changing the whole product attributes prefixes from eo to eop
            if (name.startsWith(EO_PREFIX)) {
                name = "eop" + name.substring(2);
            }
            for (ProductClass pc : ProductClass.getProductClasses(geoServer)) {
                String prefix = pc.getPrefix();
                if (name.startsWith(prefix)) {
                    name = name.substring(prefix.length());
                    char c[] = name.toCharArray();
                    c[0] = Character.toLowerCase(c[0]);
                    name = new String(c);
                    namespaceURI = pc.getNamespace();
                    break;
                }
            }

            // get a more predictable name structure (will have to do something for oracle
            // like names too I guess)
            if (StringUtils.isAllUpperCase(name)) {
                name = name.toLowerCase();
            }
            // map into output type
            ab.init(ad);
            ab.setMinOccurs(0);
            ab.name(name).namespaceURI(namespaceURI).userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            AttributeDescriptor mappedDescriptor;
            if (ad instanceof GeometryDescriptor) {
                GeometryType at = ab.buildGeometryType();
                ab.setCRS(((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            } else {
                AttributeType at = ab.buildType();
                mappedDescriptor = ab.buildDescriptor(new NameImpl(namespaceURI, name), at);
            }

            typeBuilder.add(mappedDescriptor);
        }
        // adding the metadata property
        AttributeDescriptor metadataDescriptor =
                buildSimpleDescriptor(METADATA_PROPERTY_NAME, String.class);
        typeBuilder.add(metadataDescriptor);

        // adding the quicklook property
        AttributeDescriptor quicklookDescriptor =
                buildSimpleDescriptor(QUICKLOOK_PROPERTY_NAME, byte[].class);
        typeBuilder.add(quicklookDescriptor);

        // map OGC links
        AttributeDescriptor linksDescriptor =
                buildFeatureListDescriptor(
                        OGC_LINKS_PROPERTY_NAME, delegate.getSchema("product_ogclink"));
        typeBuilder.add(linksDescriptor);

        typeBuilder.setName(PRODUCT);
        typeBuilder.setNamespaceURI(namespaceURI);
        return typeBuilder.feature();
    }

    private List<String> getMissingRequiredTables(DataStore delegate, String... tables)
            throws IOException {
        Set<String> availableNames = new HashSet<>(Arrays.asList(delegate.getTypeNames()));
        return Arrays.stream(tables)
                .map(String::toLowerCase)
                .filter(table -> !availableNames.contains(table))
                .collect(Collectors.toList());
    }

    /** Returns the store from the repository (which is based on GeoServer own resource pool) */
    DataStore getDelegateStore() throws IOException {
        DataStore store = getRawDelegateStore();
        if (delegateStoreCache != null && delegateStoreCache.wraps(store)) {
            return delegateStoreCache;
        }
        LowercasingDataStore result = new LowercasingDataStore(store);
        this.delegateStoreCache = result;
        return result;
    }

    JDBCDataStore getRawDelegateStore() {
        JDBCDataStore store = (JDBCDataStore) repository.dataStore(delegateStoreName);
        return store;
    }

    @Override
    public ServiceInfo getInfo() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public void createSchema(FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateSchema(Name typeName, FeatureType featureType) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void removeSchema(Name typeName) throws IOException {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Name> getNames() throws IOException {
        LinkedHashSet<Name> names = new LinkedHashSet<>();
        // add the well known ones
        names.add(collectionFeatureType.getName());
        names.add(productFeatureType.getName());
        // get all collections and their publishing setups

        getCollectionPublishingConfigurations()
                .forEach(
                        (name, layers) -> {
                            if (layers != null && !layers.isEmpty()) {
                                for (CollectionLayer layer : layers) {
                                    setupLayerFeatureTypes(names, name, layer);
                                }
                            } else {
                                // a single feature type per collection
                                names.add(new NameImpl(namespaceURI, name));
                            }
                        });
        return new ArrayList<>(names);
    }

    private void setupLayerFeatureTypes(
            LinkedHashSet<Name> names, String name, CollectionLayer layer) {
        if (layer != null
                && layer.isSeparateBands()
                && layer.getBands() != null
                && layer.getBands().length > 0) {
            // one feature type per band needed to setup a coverage view
            for (String band : layer.getBands()) {
                names.add(
                        new NameImpl(
                                namespaceURI, name + OpenSearchAccess.BAND_LAYER_SEPARATOR + band));
            }
        } else {
            names.add(new NameImpl(namespaceURI, name));
        }
    }

    private Map<String, List<CollectionLayer>> getCollectionPublishingConfigurations()
            throws IOException {
        FeatureSource<FeatureType, Feature> collectionSource = getCollectionSource();
        Query query = new Query(collectionSource.getName().getLocalPart());
        query.setPropertyNames(new String[] {COLLECTION_NAME, LAYERS});
        FeatureCollection<FeatureType, Feature> features = collectionSource.getFeatures(query);
        Map<String, List<CollectionLayer>> result = new LinkedHashMap<>();
        features.accepts(
                f -> {
                    Property p = f.getProperty(COLLECTION_NAME);
                    String name = (String) p.getValue();
                    List<CollectionLayer> configs = null;
                    try {
                        configs = CollectionLayer.buildCollectionLayersFromFeature(f);
                        result.put(name, configs);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                },
                null);
        return result;
    }

    @Override
    public FeatureType getSchema(Name name) throws IOException {
        // get the basic ones
        for (FeatureType ft : Arrays.asList(collectionFeatureType, productFeatureType)) {
            if (name.equals(ft.getName())) {
                return ft;
            }
        }
        // see if it's a collection case
        FeatureSource<FeatureType, Feature> source = getFeatureSource(name);
        if (source != null) {
            return source.getSchema();
        }
        return null;
    }

    @Override
    public FeatureSource<FeatureType, Feature> getFeatureSource(Name typeName) throws IOException {
        if (collectionFeatureType.getName().equals(typeName)) {
            return getCollectionSource();
        } else if (productFeatureType.getName().equals(typeName)) {
            return getProductSource();
        }
        if (Objects.equal(namespaceURI, typeName.getNamespaceURI())) {
            SimpleFeatureSource result = featureSourceCache.get(typeName);
            if (result == null && getNames().contains(typeName)) {
                result = getCollectionGranulesSource(typeName.getLocalPart());
                featureSourceCache.put(typeName, result);
            }
            return (FeatureSource) result;
        }

        throw new IOException("Schema '" + typeName + "' does not exist.");
    }

    public SimpleFeatureSource getCollectionGranulesSource(String typeName) throws IOException {
        int idx = typeName.lastIndexOf(OpenSearchAccess.BAND_LAYER_SEPARATOR);
        String collection, band;
        // the two parts must be non empty in order to have a valid combination
        if (idx > 1 && idx < (typeName.length() - 3)) {
            collection = typeName.substring(0, idx);
            band = typeName.substring(idx + OpenSearchAccess.BAND_LAYER_SEPARATOR.length());
        } else {
            collection = typeName;
            band = null;
        }

        // using joining for this one is hard because we need a flat representation
        // and be able to run filters on all attributes in whatever combination, the JOIN
        // support from GeoTools is too weak to do that. We'll setup a reusable virtual table
        // instead
        JDBCDataStore delegate = getRawDelegateStore();
        SQLDialect dialect = delegate.getSQLDialect();

        // a bit of craziness to avoid depending on the case of the table name
        String productTableName = null;
        String granuleTableName = null;
        String collectionTableName = null;
        for (String name : delegate.getTypeNames()) {
            if (JDBCOpenSearchAccess.PRODUCT.equalsIgnoreCase(name)) {
                productTableName = name;
            } else if (JDBCOpenSearchAccess.COLLECTION.equalsIgnoreCase(name)) {
                collectionTableName = name;
            } else if (JDBCOpenSearchAccess.GRANULE.equalsIgnoreCase(name)) {
                granuleTableName = name;
            }
        }
        checkName(productTableName, JDBCOpenSearchAccess.PRODUCT);
        checkName(collectionTableName, JDBCOpenSearchAccess.COLLECTION);
        checkName(granuleTableName, JDBCOpenSearchAccess.GRANULE);

        // get the product type, if any (might be a virtual collection)
        SimpleFeature collectionFeature =
                getCollectionFeature(collection, delegate, collectionTableName);
        String sensorType = (String) collectionFeature.getAttribute("eoSensorType");
        ProductClass productClass = null;
        if (sensorType != null) {
            productClass = ProductClass.getProductClassFromName(geoServer, sensorType);
        }

        final String dbSchema = delegate.getDatabaseSchema();
        // build the joining SQL
        StringJoiner attributes = new StringJoiner(", ");
        // collection attributes
        ContentFeatureSource collectionSource = delegate.getFeatureSource(collectionTableName);
        for (AttributeDescriptor ad : collectionSource.getSchema().getAttributeDescriptors()) {
            if (ad.getLocalName().startsWith(JDBCOpenSearchAccess.EO_PREFIX)) {
                String column = encodeColumn(dialect, "collection", ad.getLocalName());
                if (ad.getLocalName().equals("eoIdentifier")) {
                    attributes.add(column + " as \"collectionEoIdentifier\"");
                } else if (!"eoAcquisitionStation".equals(ad.getLocalName())) {
                    // add everything that's not duplicate
                    attributes.add(column);
                }
            }
        }
        // product attributes
        ContentFeatureSource productSource = delegate.getFeatureSource(productTableName);
        for (AttributeDescriptor ad : productSource.getSchema().getAttributeDescriptors()) {
            final String localName = ad.getLocalName();
            if (localName.startsWith(JDBCOpenSearchAccess.EO_PREFIX)
                    || "timeStart".equals(localName)
                    || "timeEnd".equals(localName)
                    || "crs".equals(localName)
                    || (productClass != null && localName.startsWith(productClass.getPrefix()))
                    || (productClass == null && matchesAnyProductClass(localName))) {
                String column = encodeColumn(dialect, "product", localName);
                attributes.add(column);
            }
        }
        // granule attributes
        SimpleFeatureType granuleSchema = delegate.getSchema(granuleTableName);
        String productIdColumn = null;
        String theGeomName = null;
        String gidName = null;
        for (AttributeDescriptor ad : granuleSchema.getAttributeDescriptors()) {
            String localName = ad.getLocalName();
            if ("id".equalsIgnoreCase(localName) || "band".equalsIgnoreCase(localName)) {
                // these two should not appear in the feature type
                continue;
            } else if ("product_id".equalsIgnoreCase(localName)) {
                productIdColumn = localName;
            } else {
                String column = encodeColumn(dialect, "granule", ad.getLocalName());
                attributes.add(column);
            }
            if ("the_geom".equalsIgnoreCase(localName)) {
                theGeomName = localName;
            } else if ("gid".equalsIgnoreCase(localName)) {
                gidName = localName;
            }
        }

        StringBuffer sb = new StringBuffer("SELECT ");
        sb.append(attributes.toString());
        sb.append("\n");
        sb.append(" FROM ");
        encodeTableName(dialect, dbSchema, granuleTableName, sb);
        sb.append(" as granule JOIN ");
        encodeTableName(dialect, dbSchema, productTableName, sb);
        sb.append(" as product ON ");
        sb.append("granule.\"").append(productIdColumn).append("\" = product.\"id\"");
        sb.append("\n");
        sb.append(" JOIN ");
        encodeTableName(dialect, dbSchema, collectionTableName, sb);
        sb.append(" as collection ON product.\"eoParentIdentifier\" = collection.\"eoIdentifier\"");
        // comparing with false on purpose, allows to default to true if primary is null or empty
        boolean primaryTable = !Boolean.FALSE.equals(collectionFeature.getAttribute("primary"));
        if (primaryTable || band != null) {
            sb.append(" WHERE ");
        }
        if (primaryTable) {
            sb.append(" collection.\"id\" = " + collectionFeature.getAttribute("id"));
        }
        if (band != null) {
            if (primaryTable) {
                sb.append("\n AND");
            }
            sb.append(" granule.\"band\" = '" + band + "'");
        }

        VirtualTable vt = new VirtualTable(typeName, sb.toString());
        vt.addGeometryMetadatata(theGeomName, Polygon.class, 4326);
        vt.setPrimaryKeyColumns(Arrays.asList(gidName));

        // now check if the virtual collection is already there
        Map<String, VirtualTable> existingVirtualTables = delegate.getVirtualTables();
        VirtualTable existing = existingVirtualTables.get(typeName);
        if (existing != null) {
            // was it updated in the meantime?
            if (!existing.equals(vt)) {
                delegate.dropVirtualTable(collectionTableName);
                existing = null;
            }
        }
        if (existing == null) {
            delegate.createVirtualTable(vt);
        }

        SimpleFeatureSource fs = delegate.getFeatureSource(typeName);

        // is it a virtual collection?
        if (!primaryTable) {
            String cqlFilter = (String) collectionFeature.getAttribute("productCqlFilter");
            if (cqlFilter != null) {
                try {
                    Filter filter = ECQL.toFilter(cqlFilter);
                    fs =
                            DataUtilities.createView(
                                    fs, new Query(fs.getSchema().getTypeName(), filter));
                } catch (CQLException | SchemaException e) {
                    throw new IOException(e);
                }
            }
        }

        return fs;
    }

    private void encodeTableName(
            SQLDialect dialect, String databaseSchema, String tableName, StringBuffer sql) {
        if (databaseSchema != null) {
            dialect.encodeSchemaName(databaseSchema, sql);
            sql.append(".");
        }
        dialect.encodeTableName(tableName, sql);
    }

    private String encodeColumn(SQLDialect dialect, String tableAliasName, String columnName) {
        StringBuffer sql = new StringBuffer();
        if (tableAliasName != null) {
            sql.append(tableAliasName).append(".");
        }
        dialect.encodeColumnName(null, columnName, sql);
        return sql.toString();
    }

    private boolean matchesAnyProductClass(String localName) {
        for (ProductClass pc : ProductClass.getProductClasses(geoServer)) {
            if (localName.startsWith(pc.getPrefix())) {
                return true;
            }
        }

        return false;
    }

    private SimpleFeature getCollectionFeature(
            String collectionName, JDBCDataStore delegate, String collectionTableName)
            throws IOException {
        final PropertyIsEqualTo collectionNameFilter =
                FF.equal(FF.property("name"), FF.literal(collectionName), true);
        final ContentFeatureCollection collections =
                delegate.getFeatureSource(collectionTableName).getFeatures(collectionNameFilter);
        SimpleFeature collectionFeature = DataUtilities.first(collections);
        return collectionFeature;
    }

    private void checkName(String tableName, String lookup) {
        if (tableName == null) {
            throw new IllegalStateException("Could not locate source table for " + lookup);
        }
    }

    public FeatureStore<FeatureType, Feature> getProductSource() throws IOException {
        return new JDBCProductFeatureStore(this, productFeatureType);
    }

    public FeatureStore<FeatureType, Feature> getCollectionSource() throws IOException {
        return new JDBCCollectionFeatureStore(this, collectionFeatureType);
    }

    @Override
    public void dispose() {
        // nothing to dispose, the delegate store is managed by the resource pool
    }

    @Override
    public SimpleFeatureSource getGranules(String collectionId, String productId)
            throws IOException {
        // a bit of craziness to avoid depending on the case of the table name
        String productTableName = null;
        String granuleTableName = null;
        JDBCDataStore delegate = getRawDelegateStore();
        for (String name : delegate.getTypeNames()) {
            if (JDBCOpenSearchAccess.PRODUCT.equalsIgnoreCase(name)) {
                productTableName = name;
            } else if (JDBCOpenSearchAccess.GRANULE.equalsIgnoreCase(name)) {
                granuleTableName = name;
            }
        }
        checkName(productTableName, JDBCOpenSearchAccess.PRODUCT);
        checkName(granuleTableName, JDBCOpenSearchAccess.GRANULE);

        // granule attributes
        SimpleFeatureType granuleSchema = delegate.getSchema(granuleTableName);
        final String productIdColumn =
                granuleSchema
                        .getAttributeDescriptors()
                        .stream()
                        .map(ad -> ad.getLocalName())
                        .filter(s -> "product_id".equalsIgnoreCase(s))
                        .findFirst()
                        .get();

        // grab the database product id
        ContentFeatureSource products = delegate.getFeatureSource(productTableName);
        final And productFilter =
                FF.and(
                        FF.equal(FF.property("eoParentIdentifier"), FF.literal(collectionId), true),
                        FF.equal(FF.property("eoIdentifier"), FF.literal(productId), true));
        SimpleFeature productFeature = DataUtilities.first(products.getFeatures(productFilter));

        if (productFeature == null) {
            throw new IOException(
                    "Could not find a product with id '"
                            + productId
                            + "' in collection '"
                            + collectionId
                            + "'");
        }

        Query granulesQuery = new Query();
        final Object dbProductId = productFeature.getAttribute("id");
        granulesQuery.setFilter(
                FF.equal(FF.property(productIdColumn), FF.literal(dbProductId), true));
        List<String> names =
                granuleSchema
                        .getAttributeDescriptors()
                        .stream()
                        .map(ad -> ad.getLocalName())
                        .filter(s -> !s.equals(productIdColumn))
                        .collect(Collectors.toList());
        granulesQuery.setPropertyNames(names);

        final SimpleFeatureStore granulesStore =
                (SimpleFeatureStore) delegate.getFeatureSource(granuleTableName);
        try {
            return new WritableDataView(granulesStore, granulesQuery) {
                public java.util.List<org.opengis.filter.identity.FeatureId> addFeatures(
                        org.geotools.feature.FeatureCollection<SimpleFeatureType, SimpleFeature>
                                featureCollection)
                        throws IOException {
                    ListFeatureCollection fc = new ListFeatureCollection(granulesStore.getSchema());
                    SimpleFeatureBuilder fb = new SimpleFeatureBuilder(granulesStore.getSchema());
                    try (SimpleFeatureIterator fi =
                            (SimpleFeatureIterator) featureCollection.features()) {
                        while (fi.hasNext()) {
                            SimpleFeature sf = fi.next();
                            fb.set("product_id", dbProductId);
                            fb.set("location", sf.getAttribute("location"));
                            fb.set("the_geom", sf.getDefaultGeometry());
                            fb.set("band", sf.getAttribute("band"));
                            SimpleFeature mapped = fb.buildFeature(null);
                            fc.add(mapped);
                        }
                    }
                    return delegate.addFeatures(fc);
                };
            };
        } catch (SchemaException e) {
            throw new IOException(e);
        }
    }

    @Override
    public SimpleFeatureType getCollectionLayerSchema() throws IOException {
        return new JDBCCollectionFeatureStore(this, collectionFeatureType)
                .getCollectionLayerSchema();
    }

    @Override
    public SimpleFeatureType getOGCLinksSchema() throws IOException {
        return new JDBCCollectionFeatureStore(this, collectionFeatureType).getOGCLinksSchema();
    }

    void clearFeatureSourceCaches() {
        featureSourceCache.clear();
    }
}
