/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo.store;

import com.google.common.base.Objects;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringJoiner;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.lang.WordUtils;
import org.apache.commons.lang3.StringUtils;
import org.geoserver.config.GeoServer;
import org.geoserver.opensearch.eo.ProductClass;
import org.geotools.api.data.DataSourceException;
import org.geotools.api.data.DataStore;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.FeatureStore;
import org.geotools.api.data.Query;
import org.geotools.api.data.Repository;
import org.geotools.api.data.ServiceInfo;
import org.geotools.api.data.SimpleFeatureSource;
import org.geotools.api.data.SimpleFeatureStore;
import org.geotools.api.data.Transaction;
import org.geotools.api.feature.Feature;
import org.geotools.api.feature.Property;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.feature.type.AttributeDescriptor;
import org.geotools.api.feature.type.AttributeType;
import org.geotools.api.feature.type.FeatureType;
import org.geotools.api.feature.type.GeometryDescriptor;
import org.geotools.api.feature.type.GeometryType;
import org.geotools.api.feature.type.Name;
import org.geotools.api.filter.And;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.PropertyIsEqualTo;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.filter.expression.Function;
import org.geotools.api.filter.expression.Literal;
import org.geotools.api.filter.expression.PropertyName;
import org.geotools.data.DataUtilities;
import org.geotools.data.collection.ListFeatureCollection;
import org.geotools.data.postgis.PostGISDialect;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.store.ContentFeatureCollection;
import org.geotools.data.store.ContentFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.AttributeTypeBuilder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.SchemaException;
import org.geotools.feature.TypeBuilder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.AttributeExpressionImpl;
import org.geotools.filter.function.JsonPointerFunction;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.geotools.jdbc.JDBCDataStore;
import org.geotools.jdbc.SQLDialect;
import org.geotools.jdbc.VirtualTable;
import org.geotools.util.SoftValueHashMap;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Polygon;

/**
 * A data store building OpenSearch for EO records based on a wrapped data store providing all
 * expected tables in form of simple features (and leveraging joins to put them together into
 * complex features as needed).
 *
 * <p>The delegate store is fetched on demand to avoid being caught in a ResourcePool dispose
 *
 * @author Andrea Aime - GeoSolutions
 */
public class JDBCOpenSearchAccess implements org.geoserver.opensearch.eo.store.OpenSearchAccess {

    static final Logger LOGGER = Logging.getLogger(JDBCOpenSearchAccess.class);

    protected static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public static final String COLLECTION = "collection";

    public static final String PRODUCT = "product";

    public static final String GRANULE = "granule";

    static final String EO_PREFIX = "eo";

    static final String EOP_PREFIX = "eop";

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

    private SourcePropertyMapper propertyMapper;

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

        collectionFeatureType = buildCollectionFeatureType(delegate, this.namespaceURI);
        productFeatureType = buildProductFeatureType(delegate);
        this.propertyMapper = new SourcePropertyMapper(productFeatureType);
    }

    String getNamespaceURI() {
        return namespaceURI;
    }

    private FeatureType buildCollectionFeatureType(DataStore delegate, String namespaceURI)
            throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(COLLECTION);

        TypeBuilder typeBuilder = new OrderedTypeBuilder();

        // map the source attributes
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            AttributeTypeBuilder ab = new AttributeTypeBuilder();
            String name = ad.getLocalName();
            String prefix = "";
            String attributeNamespace = namespaceURI;
            if (name.startsWith(EO_PREFIX)) {
                name = name.substring(EO_PREFIX.length());
                char[] c = name.toCharArray();
                c[0] = Character.toLowerCase(c[0]);
                name = new String(c);
                attributeNamespace = EO_NAMESPACE;
                prefix = EO_PREFIX;
            }
            // get a more predictable name structure (will have to do something for oracle
            // like names too I guess)
            if (StringUtils.isAllUpperCase(name)) {
                name = name.toLowerCase();
            }
            // map into output type
            ab.init(ad);
            ab.setMinOccurs(0);
            ab.name(name)
                    .namespaceURI(attributeNamespace)
                    .userData(SOURCE_ATTRIBUTE, ad.getLocalName());
            ab.userData(PREFIX, prefix);
            AttributeDescriptor mappedDescriptor;
            if (ad instanceof GeometryDescriptor) {
                GeometryType at = ab.buildGeometryType();
                ab.setCRS(((GeometryDescriptor) ad).getCoordinateReferenceSystem());
                mappedDescriptor = ab.buildDescriptor(new NameImpl(attributeNamespace, name), at);
            } else {
                AttributeType at = ab.buildType();
                mappedDescriptor = ab.buildDescriptor(new NameImpl(attributeNamespace, name), at);
            }

            typeBuilder.add(mappedDescriptor);
        }

        // adding the layer publishing property
        AttributeDescriptor layerDescriptor =
                buildFeatureListDescriptor(
                        LAYERS_PROPERTY_NAME, EO_PREFIX, delegate.getSchema("collection_ogclink"));
        typeBuilder.add(layerDescriptor);

        // map OGC links
        AttributeDescriptor linksDescriptor =
                buildFeatureListDescriptor(
                        OGC_LINKS_PROPERTY_NAME,
                        EO_PREFIX,
                        delegate.getSchema("collection_ogclink"));
        typeBuilder.add(linksDescriptor);

        typeBuilder.setName(COLLECTION);
        typeBuilder.setNamespaceURI(namespaceURI);
        return typeBuilder.feature();
    }

    private AttributeDescriptor buildSimpleDescriptor(Name name, String prefix, Class<?> binding) {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        ab.name(name.getLocalPart()).namespaceURI(name.getNamespaceURI());
        ab.setBinding(binding);
        ab.userData(PREFIX, prefix);
        AttributeDescriptor descriptor = ab.buildDescriptor(name, ab.buildType());
        return descriptor;
    }

    private AttributeDescriptor buildFeatureListDescriptor(
            Name name, String prefix, SimpleFeatureType schema) {
        return buildFeatureDescriptor(name, prefix, schema, 0, Integer.MAX_VALUE);
    }

    private AttributeDescriptor buildFeatureDescriptor(
            Name name, String prefix, SimpleFeatureType schema, int minOccurs, int maxOccurs) {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        String ns = name.getNamespaceURI();
        ab.name(name.getLocalPart()).namespaceURI(ns);
        ab.setMinOccurs(minOccurs);
        ab.setMaxOccurs(maxOccurs);
        ab.userData(PREFIX, prefix);
        AttributeDescriptor descriptor = ab.buildDescriptor(name, applyNamespace(ns, schema));
        return descriptor;
    }

    private AttributeDescriptor buildFeatureDescriptor(
            Name name, String prefix, FeatureType schema, int minOccurs, int maxOccurs) {
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        String ns = name.getNamespaceURI();
        ab.name(name.getLocalPart()).namespaceURI(ns);
        ab.setMinOccurs(minOccurs);
        ab.setMaxOccurs(maxOccurs);
        ab.userData(PREFIX, prefix);
        AttributeDescriptor descriptor = ab.buildDescriptor(name, schema);
        return descriptor;
    }

    private FeatureType applyNamespace(String namespaceURI, SimpleFeatureType schema) {
        TypeBuilder tb = new OrderedTypeBuilder();
        tb.setName(schema.getTypeName());
        tb.setNamespaceURI(namespaceURI);
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : schema.getAttributeDescriptors()) {
            ab.init(ad);
            ab.setNamespaceURI(namespaceURI);
            NameImpl name = new NameImpl(namespaceURI, ad.getLocalName());
            AttributeDescriptor newDescriptor = ab.buildDescriptor(name, ab.buildType());
            tb.add(newDescriptor);
        }
        return tb.feature();
    }

    private FeatureType buildProductFeatureType(DataStore delegate) throws IOException {
        SimpleFeatureType flatSchema = delegate.getSchema(PRODUCT);

        TypeBuilder typeBuilder = new OrderedTypeBuilder();

        // map the source attributes
        AttributeTypeBuilder ab = new AttributeTypeBuilder();
        for (AttributeDescriptor ad : flatSchema.getAttributeDescriptors()) {
            String name = ad.getLocalName();
            String namespaceURI = this.namespaceURI;
            String prefix = "";
            // hack to avoid changing the whole product attributes prefixes from eo to eop
            if (name.startsWith(EO_PREFIX)) {
                name = "eop" + name.substring(2);
                prefix = EOP_PREFIX;
            }
            for (ProductClass pc : ProductClass.getProductClasses(geoServer)) {
                String pcPrefix = pc.getPrefix();
                if (name.startsWith(pcPrefix)) {
                    name = name.substring(pcPrefix.length());
                    char[] c = name.toCharArray();
                    c[0] = Character.toLowerCase(c[0]);
                    name = new String(c);
                    namespaceURI = pc.getNamespace();
                    prefix = pcPrefix;
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
            ab.userData(PREFIX, prefix);
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

        // adding the quicklook property
        AttributeDescriptor quicklookDescriptor =
                buildSimpleDescriptor(QUICKLOOK_PROPERTY_NAME, EO_PREFIX, byte[].class);
        typeBuilder.add(quicklookDescriptor);

        // map OGC links
        AttributeDescriptor linksDescriptor =
                buildFeatureListDescriptor(
                        OGC_LINKS_PROPERTY_NAME, EO_PREFIX, delegate.getSchema("product_ogclink"));
        typeBuilder.add(linksDescriptor);

        // the product collection itself
        FeatureType collectionType = buildCollectionFeatureType(delegate, EO_NAMESPACE);
        AttributeDescriptor collectionDescriptor =
                buildFeatureDescriptor(COLLECTION_PROPERTY_NAME, EO_PREFIX, collectionType, 0, 1);
        typeBuilder.add(collectionDescriptor);

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
        query.setPropertyNames(COLLECTION_NAME, LAYERS);
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

    @SuppressWarnings("unchecked")
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

    @Override
    public void updateIndexes(String collection, List<Indexable> indexables) throws IOException {
        try (Connection cx = getRawDelegateStore().getConnection(Transaction.AUTO_COMMIT)) {
            List<JDBCIndex> existing = getIndexes(cx, collection);

            // loop and find indexes that need to be created, and build a map from queryables to
            // expressions as recorded in the DB, for later match and index disposal
            Map<String, String> expressions = new HashMap<>();
            for (Indexable indexable : indexables) {
                try {
                    String queryable = indexable.getQueryable();
                    String expression =
                            getIndexExpression(indexable.getExpression(), indexable.getFieldType());
                    expressions.put(queryable, expression);
                    boolean exists =
                            existing.stream().anyMatch(idx -> idx.matches(queryable, expression));
                    logIndexHandling("already exists", collection, queryable, expression, exists);
                    if (!exists) createIndex(cx, collection, indexable);
                } catch (IOException e) {
                    // thrown only by getIndexExpression
                    LOGGER.log(Level.WARNING, "Failed to index expression", e);
                }
            }

            // find indexes that are no longer needed and remove
            for (JDBCIndex index : existing) {
                String queryable = index.getQueryable();
                String expression = expressions.get(queryable);
                boolean exists = Objects.equal(expression, index.getExpression());
                logIndexHandling("still needed", collection, queryable, expression, exists);
                if (!exists) {
                    dropIndex(cx, collection, queryable, index.getExpression());
                }
            }

        } catch (SQLException e) {
            throw new DataSourceException(e);
        }
    }

    private void logIndexHandling(
            String action, String collection, String queryable, String expression, boolean exists) {
        LOGGER.info(
                () ->
                        "Checking if index is "
                                + action
                                + " for "
                                + collection
                                + "/"
                                + queryable
                                + " over "
                                + expression
                                + ": "
                                + exists);
    }

    private List<JDBCIndex> getIndexes(Connection cx, String collection) throws SQLException {
        List<JDBCIndex> indexes = new ArrayList<>();
        try (PreparedStatement ps =
                cx.prepareStatement("SELECT * FROM queryable_idx_tracker WHERE collection = ?")) {
            ps.setString(1, collection);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    JDBCIndex index = new JDBCIndex();
                    index.setQueryable(rs.getString("queryable"));
                    index.setName(rs.getString("index_name"));
                    index.setCollection(rs.getString("collection"));
                    index.setExpression(rs.getString("expression"));
                    indexes.add(index);
                }
            }
        }
        return indexes;
    }

    private String getIndexExpression(Expression expression, Indexable.FieldType fieldType)
            throws IOException {
        switch (fieldType) {
            case Geometry:
            case Array:
            case Other:
                return getIndexField(expression);
            default:
                return encodeJsonPointer((Function) expression, fieldType);
        }
    }

    /** Create indices on Product fields */
    private void createIndex(Connection cx, String collectionName, Indexable idx) {
        JDBCDataStore delegate = getRawDelegateStore();
        SQLDialect dialect = delegate.getSQLDialect();
        if (!(dialect instanceof PostGISDialect)) {
            throw new IllegalArgumentException(
                    "Index creation is only current supported with PostGIS");
        }
        try {
            String indexTitle = getIndexTitle(collectionName, idx.getQueryable());
            Indexable.FieldType fieldType = idx.getFieldType();
            String indexExpression = getIndexExpression(idx.getExpression(), fieldType);

            if (!isFieldOrPointerIndexed(cx, indexExpression)) {
                LOGGER.info(
                        "Creating missing index on " + collectionName + "/" + idx.getQueryable());
                StringBuilder sql = new StringBuilder("CREATE INDEX IF NOT EXISTS ");
                sql.append(indexTitle).append(" ON product ");
                if (fieldType == Indexable.FieldType.Geometry) {
                    sql.append(" USING GIST ");
                } else if (fieldType == Indexable.FieldType.Array) {
                    sql.append(" USING GIN ");
                }
                sql.append("(");
                if (fieldType == Indexable.FieldType.Other
                        || fieldType == Indexable.FieldType.Geometry) {
                    sql.append(indexExpression);
                } else {
                    sql.append("(").append(indexExpression).append(")");
                }
                sql.append(")");
                LOGGER.log(Level.FINE, "Creating index " + sql);
                try (PreparedStatement preparedStatement = cx.prepareStatement(sql.toString())) {
                    preparedStatement.execute();
                }
            } else {
                LOGGER.info(
                        "Index for indexExpression already exists, tracking new queryable: "
                                + collectionName
                                + "/"
                                + idx.getQueryable());
                indexTitle = getExistingIndexTitle(cx, indexExpression);
            }
            recordIndex(cx, collectionName, idx.getQueryable(), indexExpression, indexTitle);
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.WARNING, "Error when creating index on " + idx.getQueryable(), e);
        }
    }

    private String getExistingIndexTitle(Connection cx, String fieldOrPointer) throws SQLException {
        try (PreparedStatement ps =
                cx.prepareStatement(
                        "SELECT index_name FROM queryable_idx_tracker WHERE expression = ? LIMIT 1")) {
            ps.setString(1, fieldOrPointer);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    return rs.getString(1);
                }
            }
        }

        return null;
    }

    private boolean isFieldOrPointerIndexed(Connection cx, String fieldOrPointer)
            throws SQLException {
        boolean out = false;
        try (PreparedStatement preparedStatement =
                cx.prepareStatement(
                        "SELECT count(*)>0 FROM queryable_idx_tracker WHERE expression = ?")) {
            preparedStatement.setString(1, fieldOrPointer);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    out = rs.getBoolean(1);
                }
            }
        }
        return out;
    }

    private void recordIndex(
            Connection cx,
            String collectionName,
            String queryable,
            String fieldOrPointer,
            String indexTitle)
            throws SQLException {
        try (PreparedStatement preparedStatement =
                cx.prepareStatement(
                        "insert into queryable_idx_tracker (index_name, collection, queryable, expression) "
                                + "values (?,?,?,?)")) {
            preparedStatement.setString(1, indexTitle);
            preparedStatement.setString(2, collectionName);
            preparedStatement.setString(3, queryable);
            preparedStatement.setString(4, fieldOrPointer);
            preparedStatement.execute();
        }
    }

    /** Drop product table index */
    private void dropIndex(
            Connection cx, String collectionName, String queryable, String indexExpression) {
        JDBCDataStore delegate = getRawDelegateStore();
        SQLDialect dialect = delegate.getSQLDialect();
        if (!(dialect instanceof PostGISDialect)) {
            throw new IllegalArgumentException(
                    "Index deletion is only current supported with PostGIS");
        }
        try {
            int indexWithExpressionCount = getIndexExpressionCount(cx, indexExpression);
            LOGGER.info("Removing tracking of : " + collectionName + "/" + queryable);
            String indexName =
                    deleteIndexExpressionRecord(cx, collectionName, queryable, indexExpression);
            // if there is an index, and this is the last field using it, drop
            if (indexWithExpressionCount <= 1 && indexName != null) {
                LOGGER.info("Dropping index " + indexName);
                String sql = "DROP INDEX IF EXISTS " + indexName;
                try (PreparedStatement ps = cx.prepareStatement(sql)) {
                    ps.execute();
                }
            }
        } catch (SQLException e) {
            LOGGER.log(Level.WARNING, "Error when deleting index on " + indexExpression, e);
        }
    }

    private String deleteIndexExpressionRecord(
            Connection cx, String collection, String queryable, String expression)
            throws SQLException {
        String out = null;
        // two queryables might be pointing to the same field, just remove one
        try (PreparedStatement preparedStatement =
                cx.prepareStatement(
                        "DELETE FROM queryable_idx_tracker "
                                + " WHERE collection = ? AND queryable = ? AND expression = ? "
                                + "RETURNING index_name")) {
            preparedStatement.setString(1, collection);
            preparedStatement.setString(2, queryable);
            preparedStatement.setString(3, expression);
            preparedStatement.execute();
            try (ResultSet rs = preparedStatement.getResultSet()) {
                while (rs.next()) {
                    out = rs.getString(1);
                }
            }
        }
        return out;
    }

    private int getIndexExpressionCount(Connection cx, String indexExpression) throws SQLException {
        int out = 0;
        try (PreparedStatement preparedStatement =
                cx.prepareStatement(
                        "SELECT count(*) FROM queryable_idx_tracker WHERE expression = ?")) {
            preparedStatement.setString(1, indexExpression);
            try (ResultSet rs = preparedStatement.executeQuery()) {
                while (rs.next()) {
                    out = rs.getInt(1);
                }
            }
        }
        return out;
    }

    @Override
    public List<String> getIndexNames(String tableName) throws IOException {
        // check we're running against PostGIS
        JDBCDataStore delegate = getRawDelegateStore();
        SQLDialect dialect = delegate.getSQLDialect();
        if (!(dialect instanceof PostGISDialect)) {
            throw new IOException("Index creation is only current supported with PostGIS");
        }

        List<String> out = new ArrayList<>();
        try (Connection cx = delegate.getConnection(Transaction.AUTO_COMMIT);
                PreparedStatement ps =
                        cx.prepareStatement(
                                "SELECT indexname FROM pg_indexes WHERE tablename = ?")) {
            ps.setString(1, tableName);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    out.add(rs.getString(1));
                }
            }
        } catch (IOException | SQLException e) {
            LOGGER.log(Level.WARNING, "Error when getting index  list for " + tableName, e);
        }

        return out;
    }

    private String getIndexField(Expression expression) throws IOException {
        String indexField;
        if (expression instanceof AttributeExpressionImpl) {
            AttributeExpressionImpl aei = (AttributeExpressionImpl) expression;
            indexField = propertyMapper.getSourceName(aei.getPropertyName());
        } else if (expression instanceof JsonPointerFunction) {
            Function function = (Function) expression;
            Expression p0 = function.getParameters().get(0);
            if (p0 instanceof PropertyName) {
                indexField = propertyMapper.getSourceName(((PropertyName) p0).getPropertyName());
            } else {
                throw new IOException(
                        "The first argument for the function "
                                + function
                                + " arg: "
                                + p0
                                + " is not "
                                + " property name and cannot be converted into a field name");
            }
        } else {
            throw new IOException(
                    "Expression "
                            + expression
                            + " is neither a JSON pointer nor a "
                            + "attribute and can not be converted into a field name, cannot index it.");
        }

        if (indexField == null)
            throw new IOException("Could not map " + expression + " to a source field");

        return "\"" + indexField + "\"";
    }

    private String getIndexTitle(String collectionName, String fieldName) {
        return collectionName.replaceAll(":", "_") + "_" + fieldName.replaceAll(":", "_") + "_idx";
    }

    private String encodeJsonPointer(Function jsonPointer, Indexable.FieldType type)
            throws IOException {
        StringBuilder out = new StringBuilder();
        Expression json = getParameter(jsonPointer, 0, true);
        Expression pointer = getParameter(jsonPointer, 1, true);
        if (json instanceof PropertyName && pointer instanceof Literal) {
            // if not a string need to cast the json attribute
            boolean needCast = !type.equals(Indexable.FieldType.JsonString);
            if (needCast) out.append('(');
            out.append("\"");
            out.append(((PropertyName) json).getPropertyName());
            out.append("\"");
            String strPointer = ((Literal) pointer).getValue().toString();
            List<String> pointerEl =
                    Stream.of(strPointer.split("/"))
                            .filter(p -> !p.equals(""))
                            .collect(Collectors.toList());
            for (int i = 0; i < pointerEl.size(); i++) {
                String p = pointerEl.get(i);
                if (i != pointerEl.size() - 1) out.append(" -> ");
                // using for last element the ->> operator
                // to have a text instead of a json returned
                else out.append(" ->> ");
                out.append("'");
                out.append(p);
                out.append("'");
            }
            if (needCast) {
                // cast from text to needed type
                out.append(')');
                out.append(cast("", type));
            }
        } else {
            throw new IOException(
                    "The first argument of the JSONPointer has to be a property name "
                            + "and the second has to be a literal");
        }
        return out.toString();
    }

    private String cast(String property, Indexable.FieldType type) {
        if (String.class.getSimpleName().equals(type.name())) {
            return property + "::text";
        } else if (Short.class.getSimpleName().equals(type.name())
                || Byte.class.equals(type.name())) {
            return property + "::smallint";
        } else if (Integer.class.getSimpleName().equals(type.name())) {
            return property + "::integer";
        } else if (Long.class.getSimpleName().equals(type.name())) {
            return property + "::bigint";
        } else if (Float.class.getSimpleName().equals(type.name())) {
            return property + "::real";
        } else if (Double.class.getSimpleName().equals(type.name())) {
            return property + "::float8";
        } else if (BigInteger.class.getSimpleName().equals(type.name())) {
            return property + "::numeric";
        } else if (BigDecimal.class.getSimpleName().equals(type.name())) {
            return property + "::decimal";
        } else if (Double.class.getSimpleName().equals(type.name())) {
            return property + "::float8";
        } else if (Time.class.getSimpleName().equals(type.name())) {
            return property + "::time";
        } else if (Timestamp.class.getSimpleName().equals(type.name())) {
            return property + "::timestamp";
        } else if (Date.class.getSimpleName().equals(type.name())) {
            return property + "::date";
        } else if (java.util.Date.class.getSimpleName().equals(type.name())) {
            return property + "::timestamp";
        } else {
            // dunno how to cast, leave as is
            return property;
        }
    }

    Expression getParameter(Function function, int idx, boolean mandatory) {
        final List<Expression> params = function.getParameters();
        if (params == null || params.size() <= idx) {
            if (mandatory) {
                throw new IllegalArgumentException(
                        "Missing parameter number "
                                + (idx + 1)
                                + "for function "
                                + function.getName()
                                + ", cannot encode in SQL");
            }
        }
        return params.get(idx);
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
        if (collectionFeature == null)
            throw new IOException("Collection " + collection + " not found");

        String sensorType = (String) collectionFeature.getAttribute("eoSensorType");
        ProductClass productClass = null;
        if (sensorType != null) {
            productClass = ProductClass.getProductClassFromName(geoServer, sensorType);
        }

        final String dbSchema = delegate.getDatabaseSchema();
        // build the joining SQL
        StringJoiner attributes = new StringJoiner(", ");
        Set<String> names = new HashSet<>();

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
                String column = encodeColumn(dialect, "granule", localName);
                attributes.add(column);
                names.add(localName);
            }
            if ("the_geom".equalsIgnoreCase(localName)) {
                theGeomName = localName;
            } else if ("gid".equalsIgnoreCase(localName)) {
                gidName = localName;
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
                names.add(localName);
            }
        }

        // collection attributes
        ContentFeatureSource collectionSource = delegate.getFeatureSource(collectionTableName);
        for (AttributeDescriptor ad : collectionSource.getSchema().getAttributeDescriptors()) {
            String localName = ad.getLocalName();
            if (localName.startsWith(JDBCOpenSearchAccess.EO_PREFIX)) {
                String column = encodeColumn(dialect, "collection", localName);
                if (names.contains(localName)) {
                    int counter = 1;
                    String base = "collection" + WordUtils.capitalize(localName);
                    String alias = base;
                    while (names.contains(alias)) {
                        alias = base + counter++;
                    }
                    attributes.add(column + " as \"" + alias + "\"");
                    names.add(alias);
                } else {
                    attributes.add(column);
                }
            }
        }

        StringBuffer sb = new StringBuffer("SELECT ");
        sb.append(attributes);
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

    @Override
    public FeatureStore<FeatureType, Feature> getProductSource() throws IOException {
        return new JDBCProductFeatureStore(this, productFeatureType);
    }

    @Override
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
                granuleSchema.getAttributeDescriptors().stream()
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
                granuleSchema.getAttributeDescriptors().stream()
                        .map(ad -> ad.getLocalName())
                        .filter(s -> !s.equals(productIdColumn))
                        .collect(Collectors.toList());
        granulesQuery.setPropertyNames(names);

        final SimpleFeatureStore granulesStore =
                (SimpleFeatureStore) delegate.getFeatureSource(granuleTableName);
        try {
            return new WritableDataView(granulesStore, granulesQuery) {
                @Override
                public java.util.List<org.geotools.api.filter.identity.FeatureId> addFeatures(
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
