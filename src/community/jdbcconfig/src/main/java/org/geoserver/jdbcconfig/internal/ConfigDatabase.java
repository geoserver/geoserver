/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Throwables.throwIfUnchecked;
import static org.geoserver.catalog.CatalogFacade.ANY_WORKSPACE;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;
import static org.geoserver.catalog.Predicates.isNull;
import static org.geoserver.jdbcconfig.internal.DbUtils.logStatement;
import static org.geoserver.jdbcconfig.internal.DbUtils.params;

import com.google.common.base.Function;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.base.Stopwatch;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheLoader;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.google.common.collect.Iterators;
import com.google.common.collect.Lists;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.lang.reflect.Proxy;
import java.nio.charset.StandardCharsets;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.sql.DataSource;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections.Predicate;
import org.apache.wicket.util.string.Strings;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.WMSLayerInfo;
import org.geoserver.catalog.WMSStoreInfo;
import org.geoserver.catalog.WMTSLayerInfo;
import org.geoserver.catalog.WMTSStoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.catalog.impl.ClassMappings;
import org.geoserver.catalog.impl.ModificationProxy;
import org.geoserver.catalog.impl.ResourceInfoImpl;
import org.geoserver.catalog.impl.StoreInfoImpl;
import org.geoserver.catalog.impl.StyleInfoImpl;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.config.ConfigurationListenerAdapter;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.SettingsInfo;
import org.geoserver.config.impl.CoverageAccessInfoImpl;
import org.geoserver.config.impl.GeoServerInfoImpl;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.resource.Resource;
import org.geoserver.util.CacheProvider;
import org.geoserver.util.DefaultCacheProvider;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.Converters;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.filter.sort.SortBy;
import org.springframework.dao.EmptyResultDataAccessException;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcOperations;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/** */
public class ConfigDatabase {

    public static final Logger LOGGER = Logging.getLogger(ConfigDatabase.class);

    private static final int LOCK_TIMEOUT_SECONDS = 60;

    private Dialect dialect;

    private DataSource dataSource;

    private DbMappings dbMappings;

    private CatalogImpl catalog;

    private GeoServer geoServer;

    private NamedParameterJdbcOperations template;

    private XStreamInfoSerialBinding binding;

    private Cache<String, Info> cache;

    private Cache<InfoIdentity, String> identityCache;

    private Cache<ServiceIdentity, ServiceInfo> serviceCache;

    private InfoRowMapper<CatalogInfo> catalogRowMapper;

    private InfoRowMapper<Info> configRowMapper;

    private ConfigClearingListener configListener;

    private ConcurrentMap<String, Semaphore> locks;

    /** Protected default constructor needed by spring-jdbc instrumentation */
    protected ConfigDatabase() {
        //
    }

    public ConfigDatabase(final DataSource dataSource, final XStreamInfoSerialBinding binding) {
        this(dataSource, binding, null);
    }

    public ConfigDatabase(
            final DataSource dataSource,
            final XStreamInfoSerialBinding binding,
            CacheProvider cacheProvider) {

        this.binding = binding;
        this.template = new NamedParameterJdbcTemplate(dataSource);
        // cannot use dataSource at this point due to spring context config hack
        // in place to support tx during testing
        this.dataSource = dataSource;

        this.catalogRowMapper = new InfoRowMapper<CatalogInfo>(CatalogInfo.class, binding);
        this.configRowMapper = new InfoRowMapper<Info>(Info.class, binding);

        if (cacheProvider == null) {
            cacheProvider = DefaultCacheProvider.findProvider();
        }
        cache = cacheProvider.getCache("catalog");
        identityCache = cacheProvider.getCache("catalogNames");
        serviceCache = cacheProvider.getCache("services");
        locks = new ConcurrentHashMap<>();
    }

    private Dialect dialect() {
        if (dialect == null) {
            this.dialect = Dialect.detect(dataSource);
        }
        return dialect;
    }

    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public void initDb(@Nullable Resource resource) throws IOException {
        this.dbMappings = new DbMappings(dialect());
        if (resource != null) {
            runInitScript(resource);
        }
        dbMappings.initDb(template);
    }

    private void runInitScript(Resource resource) throws IOException {

        LOGGER.info(
                "------------- Running catalog database init script "
                        + resource.path()
                        + " ------------");

        try (InputStream in = resource.in()) {
            Util.runScript(in, template.getJdbcOperations(), LOGGER);
        }

        LOGGER.info("Initialization SQL script run sucessfully");
    }

    public DbMappings getDbMappings() {
        return dbMappings;
    }

    public void setCatalog(CatalogImpl catalog) {
        this.catalog = catalog;
        this.binding.setCatalog(catalog);

        catalog.removeListeners(CatalogClearingListener.class);
        catalog.addListener(new CatalogClearingListener());
    }

    public CatalogImpl getCatalog() {
        return this.catalog;
    }

    public void setGeoServer(GeoServer geoServer) {
        this.geoServer = geoServer;

        if (configListener != null) geoServer.removeListener(configListener);
        configListener = new ConfigClearingListener();
        geoServer.addListener(configListener);
    }

    public GeoServer getGeoServer() {
        return geoServer;
    }

    public <T extends CatalogInfo> int count(final Class<T> of, final Filter filter) {

        QueryBuilder<T> sqlBuilder = QueryBuilder.forCount(dialect, of, dbMappings).filter(filter);

        final StringBuilder sql = sqlBuilder.build();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        final int count;
        if (fullySupported) {
            final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
            logStatement(sql, namedParameters);

            count = template.queryForObject(sql.toString(), namedParameters, Integer.class);
        } else {
            LOGGER.fine(
                    "Filter is not fully supported, doing scan of supported part to return the number of matches");
            // going the expensive route, filtering as much as possible
            CloseableIterator<T> iterator = query(of, filter, null, null, (SortBy) null);
            try {
                return Iterators.size(iterator);
            } finally {
                iterator.close();
            }
        }
        return count;
    }

    public <T extends Info> CloseableIterator<T> query(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer limit,
            @Nullable SortBy sortOrder) {
        if (sortOrder == null) {
            return query(of, filter, offset, limit, new SortBy[] {});
        } else {
            return query(of, filter, offset, limit, new SortBy[] {sortOrder});
        }
    }

    public <T extends Info> CloseableIterator<T> query(
            final Class<T> of,
            final Filter filter,
            @Nullable Integer offset,
            @Nullable Integer limit,
            @Nullable SortBy... sortOrder) {

        checkNotNull(of);
        checkNotNull(filter);
        checkArgument(offset == null || offset.intValue() >= 0);
        checkArgument(limit == null || limit.intValue() >= 0);

        QueryBuilder<T> sqlBuilder =
                QueryBuilder.forIds(dialect, of, dbMappings)
                        .filter(filter)
                        .offset(offset)
                        .limit(limit)
                        .sortOrder(sortOrder);
        final StringBuilder sql = sqlBuilder.build();

        List<String> ids = null;

        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();
        final Filter simplifiedFilter =
                (Filter) sqlBuilder.getSupportedFilter().accept(filterSimplifier, null);
        if (simplifiedFilter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo isEqualTo = (PropertyIsEqualTo) simplifiedFilter;
            if (isEqualTo.getExpression1() instanceof PropertyName
                    && isEqualTo.getExpression2() instanceof Literal
                    && ((PropertyName) isEqualTo.getExpression1()).getPropertyName().equals("id")) {
                ids =
                        Collections.singletonList(
                                ((Literal) isEqualTo.getExpression2()).getValue().toString());
            }
            if (isEqualTo.getExpression2() instanceof PropertyName
                    && isEqualTo.getExpression1() instanceof Literal
                    && ((PropertyName) isEqualTo.getExpression2()).getPropertyName().equals("id")) {
                ids =
                        Collections.singletonList(
                                ((Literal) isEqualTo.getExpression1()).getValue().toString());
            }
        }

        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        if (ids == null) {
            final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();

            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Original filter: " + filter);
                LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
                LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
            }
            logStatement(sql, namedParameters);

            Stopwatch sw = Stopwatch.createStarted();
            // the oracle offset/limit implementation returns a two column result set
            // with rownum in the 2nd - queryForList will throw an exception
            ids =
                    template.query(
                            sql.toString(),
                            namedParameters,
                            new RowMapper<String>() {
                                @Override
                                public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                                    return rs.getString(1);
                                }
                            });
            sw.stop();
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.fine(
                        Joiner.on("")
                                .join(
                                        "query returned ",
                                        ids.size(),
                                        " records in ",
                                        sw.toString()));
            }
        }

        List<T> lazyTransformed =
                Lists.transform(
                        ids,
                        new Function<String, T>() {
                            @Nullable
                            @Override
                            public T apply(String id) {
                                return getById(id, of);
                            }
                        });

        CloseableIterator<T> result;
        Iterator<T> iterator =
                Iterators.filter(
                        lazyTransformed.iterator(), com.google.common.base.Predicates.notNull());

        if (fullySupported) {
            result = new CloseableIteratorAdapter<T>(iterator);
        } else {
            // Apply the filter
            result = CloseableIteratorAdapter.filter(iterator, filter);
            // The offset and limit should not have been applied as part of the query
            assert (!sqlBuilder.isOffsetLimitApplied());
            // Apply offset and limits after filtering
            result = applyOffsetLimit(result, offset, limit);
        }

        return result;
    }

    public <T extends Info> CloseableIterator<String> queryIds(
            final Class<T> of, final Filter filter) {

        checkNotNull(of);
        checkNotNull(filter);

        QueryBuilder<T> sqlBuilder = QueryBuilder.forIds(dialect, of, dbMappings).filter(filter);

        final StringBuilder sql = sqlBuilder.build();
        final Map<String, Object> namedParameters = sqlBuilder.getNamedParameters();
        final Filter unsupportedFilter = sqlBuilder.getUnsupportedFilter();
        final boolean fullySupported = Filter.INCLUDE.equals(unsupportedFilter);

        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + filter);
            LOGGER.finer("Supported filter: " + sqlBuilder.getSupportedFilter());
            LOGGER.finer("Unsupported filter: " + sqlBuilder.getUnsupportedFilter());
        }
        logStatement(sql, namedParameters);

        Stopwatch sw = Stopwatch.createStarted();
        // the oracle offset/limit implementation returns a two column result set
        // with rownum in the 2nd - queryForList will throw an exception
        List<String> ids =
                template.query(
                        sql.toString(),
                        namedParameters,
                        new RowMapper<String>() {
                            @Override
                            public String mapRow(ResultSet rs, int rowNum) throws SQLException {
                                return rs.getString(1);
                            }
                        });
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("query returned " + ids.size() + " records in " + sw);
        }

        CloseableIterator<String> result;
        Iterator<String> iterator =
                Iterators.filter(ids.iterator(), com.google.common.base.Predicates.notNull());

        if (fullySupported) {
            result = new CloseableIteratorAdapter<String>(iterator);
        } else {
            // Apply the filter
            result = CloseableIteratorAdapter.filter(iterator, filter);
            // The offset and limit should not have been applied as part of the query
            assert (!sqlBuilder.isOffsetLimitApplied());
        }

        return result;
    }

    private <T extends Info> CloseableIterator<T> applyOffsetLimit(
            CloseableIterator<T> iterator, Integer offset, Integer limit) {
        if (offset != null) {
            Iterators.advance(iterator, offset.intValue());
        }
        if (limit != null) {
            iterator = CloseableIteratorAdapter.limit(iterator, limit.intValue());
        }
        return iterator;
    }

    public <T extends Info> List<T> queryAsList(
            final Class<T> of,
            final Filter filter,
            Integer offset,
            Integer count,
            SortBy sortOrder) {

        CloseableIterator<T> iterator = query(of, filter, offset, count, sortOrder);
        List<T> list;
        try {
            list = ImmutableList.copyOf(iterator);
        } finally {
            iterator.close();
        }
        return list;
    }

    public <T extends CatalogInfo> T getDefault(final String key, Class<T> type) {
        String sql = "SELECT ID FROM DEFAULT_OBJECT WHERE DEF_KEY = :key";

        String defaultObjectId;
        try {
            ImmutableMap<String, String> params = ImmutableMap.of("key", key);
            logStatement(sql, params);
            defaultObjectId = template.queryForObject(sql, params, String.class);
        } catch (EmptyResultDataAccessException notFound) {
            return null;
        }
        return getById(defaultObjectId, type);
    }

    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public <T extends Info> T add(final T info) {
        checkNotNull(info);
        checkNotNull(info.getId(), "Object has no id");
        checkArgument(!(info instanceof Proxy), "Added object shall not be a dynamic proxy");

        final String id = info.getId();

        byte[] value = binding.objectToEntry(info);
        final String blob = new String(value, StandardCharsets.UTF_8);
        final Class<T> interf = ClassMappings.fromImpl(info.getClass()).getInterface();
        final Integer typeId = dbMappings.getTypeId(interf);

        Map<String, ?> params = params("type_id", typeId, "id", id, "blob", blob);
        final String statement =
                String.format(
                        "insert into object (oid, type_id, id, blob) values (%s, :type_id, :id, :blob)",
                        dialect.nextVal("seq_OBJECT"));
        logStatement(statement, params);
        KeyHolder keyHolder = new GeneratedKeyHolder();
        int updateCount =
                template.update(
                        statement,
                        new MapSqlParameterSource(params),
                        keyHolder,
                        new String[] {"oid"});
        checkState(updateCount == 1, "Insert statement failed");
        // looks like some db's return the pk different than others, so lets try both ways
        Number key = (Number) keyHolder.getKeys().get("oid");
        if (key == null) {
            key = keyHolder.getKey();
        }
        addAttributes(info, key);

        return getById(id, interf);
    }

    public <T extends Info> void addNames(String id, String... names) {}

    private void addAttributes(final Info info, final Number infoPk) {
        final String id = info.getId();
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Storing properties of " + id + " with pk " + infoPk);
        }

        final Iterable<Property> properties = dbMappings.properties(info);

        for (Property prop : properties) {
            if (LOGGER.isLoggable(Level.FINEST)) {
                LOGGER.finest(
                        "Adding property " + prop.getPropertyName() + "='" + prop.getValue() + "'");
            }

            final List<?> values = valueList(prop);

            Object propValue;
            Integer colIndex;

            for (int index = 0; index < values.size(); index++) {
                colIndex = prop.isCollectionProperty() ? (index + 1) : 0;
                propValue = values.get(index);
                final String storedValue = marshalValue(propValue);

                addAttribute(info, infoPk, prop, colIndex, storedValue);
            }
        }
    }

    private void addAttribute(
            final Info info,
            final Number infoPk,
            Property prop,
            Integer colIndex,
            final String storedValue) {
        Map<String, ?> params = params("value", storedValue);

        final String insertPropertySQL =
                "insert into object_property " //
                        + "(oid, property_type, related_oid, related_property_type, colindex, value, id) " //
                        + "values (:object_id, :property_type, :related_oid, :related_property_type, :colindex, :value, :id)";

        final boolean isRelationShip = prop.isRelationship();

        Integer relatedObjectId = null;
        final Integer concreteTargetPropertyOid;

        if (isRelationShip) {
            Info relatedObject = lookUpRelatedObject(info, prop, colIndex);
            if (relatedObject == null) {
                concreteTargetPropertyOid = null;
            } else {
                // the related property may refer to an abstract type (e.g.
                // LayerInfo.resource.name), so we need to find out the actual property type id (for
                // example, whether it belongs to FeatureTypeInfo or CoverageInfo)
                relatedObject = ModificationProxy.unwrap(relatedObject);
                relatedObjectId = this.findObjectId(relatedObject);

                Integer targetPropertyOid = prop.getPropertyType().getTargetPropertyOid();
                PropertyType targetProperty;
                String targetPropertyName;

                Class<?> targetQueryType;
                ClassMappings classMappings = ClassMappings.fromImpl(relatedObject.getClass());
                targetQueryType = classMappings.getInterface();
                targetProperty = dbMappings.getPropertyType(targetPropertyOid);
                targetPropertyName = targetProperty.getPropertyName();

                Set<Integer> propertyTypeIds;
                propertyTypeIds =
                        dbMappings.getPropertyTypeIds(targetQueryType, targetPropertyName);
                checkState(propertyTypeIds.size() == 1);
                concreteTargetPropertyOid = propertyTypeIds.iterator().next();
            }
        } else {
            concreteTargetPropertyOid = null;
        }

        final Number propertyType = prop.getPropertyType().getOid();
        final String id = info.getId();

        params =
                params(
                        "object_id",
                        infoPk, //
                        "property_type",
                        propertyType, //
                        "id",
                        id, //
                        "related_oid",
                        relatedObjectId, //
                        "related_property_type",
                        concreteTargetPropertyOid, //
                        "colindex",
                        colIndex, //
                        "value",
                        storedValue);

        logStatement(insertPropertySQL, params);
        template.update(insertPropertySQL, params);
    }

    /** */
    private Info lookUpRelatedObject(
            final Info info, final Property prop, @Nullable Integer collectionIndex) {

        checkArgument(collectionIndex == 0 || prop.isCollectionProperty());

        final FilterFactory ff = CommonFactoryFinder.getFilterFactory();

        final Integer targetPropertyTypeId = prop.getPropertyType().getTargetPropertyOid();
        checkArgument(targetPropertyTypeId != null);

        final PropertyType targetPropertyType = dbMappings.getPropertyType(targetPropertyTypeId);
        checkState(targetPropertyType != null);

        final Class<?> targetType = dbMappings.getType(targetPropertyType.getObjectTypeOid());
        checkState(targetType != null);

        final String localPropertyName = prop.getPropertyName();
        String[] steps = localPropertyName.split("\\.");
        // Step back through ancestor property references If starting at a.b.c.d, then look at
        // a.b.c, then a.b, then a
        for (int i = steps.length - 1; i >= 0; i--) {
            String backPropName = Strings.join(".", Arrays.copyOfRange(steps, 0, i));
            Object backProp = ff.property(backPropName).evaluate(info);
            if (backProp != null) {
                if (prop.isCollectionProperty()
                        && (backProp instanceof Set || backProp instanceof List)) {
                    List<?> list;
                    if (backProp instanceof Set) {
                        list = asValueList(backProp);
                        if (list.size() > 0
                                && list.get(0) != null
                                && targetType.isAssignableFrom(list.get(0).getClass())) {
                            String targetPropertyName = targetPropertyType.getPropertyName();
                            final PropertyName expr = ff.property(targetPropertyName);
                            Collections.sort(
                                    list,
                                    new Comparator<Object>() {
                                        @Override
                                        public int compare(Object o1, Object o2) {
                                            Object v1 = expr.evaluate(o1);
                                            Object v2 = expr.evaluate(o2);
                                            String m1 = marshalValue(v1);
                                            String m2 = marshalValue(v2);
                                            return m1 == null
                                                    ? (m2 == null ? 0 : -1)
                                                    : (m2 == null ? 1 : m1.compareTo(m2));
                                        }
                                    });
                        }
                    } else {
                        list = (List<?>) backProp;
                    }
                    if (collectionIndex <= list.size()) {
                        backProp = list.get(collectionIndex - 1);
                    }
                }
                if (targetType.isAssignableFrom(backProp.getClass())) {
                    return (Info) backProp;
                }
            }
        }
        // throw new IllegalArgumentException("Found no related object of type "
        // + targetType.getName() + " for property " + localPropertyName + " of " + info);
        return null;
    }

    private List<?> valueList(Property prop) {
        final Object value = prop.getValue();
        return asValueList(value);
    }

    private List<?> asValueList(final Object value) {
        final List<?> values;
        if (value instanceof List) {
            values = (List<?>) value;
        } else if (value instanceof Collection) {
            values = Lists.newArrayList((Collection<?>) value);
        } else {
            values = Lists.newArrayList(value);
        }
        return values;
    }

    /** @return the stored representation of a scalar property value */
    private String marshalValue(Object propValue) {
        // TODO pad numeric values
        String marshalled = Converters.convert(propValue, String.class);
        return marshalled;
    }

    /** @param info */
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public void remove(Info info) {
        Integer oid;
        try {
            oid = findObjectId(info);
        } catch (EmptyResultDataAccessException notFound) {
            return;
        }

        String deleteObject = "delete from object where id = :id";
        String deleteRelatedProperties = "delete from object_property where related_oid = :oid";

        Map<String, ?> params = ImmutableMap.of("id", info.getId());
        logStatement(deleteObject, params);
        int updateCount = template.update(deleteObject, params);
        if (updateCount != 1) {
            LOGGER.warning(
                    "Requested to delete "
                            + info
                            + " ("
                            + info.getId()
                            + ") but nothing happened on the database.");
        }
        params = params("oid", oid);
        logStatement(deleteRelatedProperties, params);
        final int relatedPropCount = template.update(deleteRelatedProperties, params);
        LOGGER.fine("Removed " + relatedPropCount + " related properties of " + info.getId());
    }

    /** @param info */
    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public <T extends Info> T save(T info) {
        checkNotNull(info);

        final String id = info.getId();

        checkNotNull(id, "Can't modify an object with no id");

        final ModificationProxy modificationProxy = ModificationProxy.handler(info);
        Preconditions.checkNotNull(modificationProxy, "Not a modification proxy: ", info);

        final Info oldObject = (Info) modificationProxy.getProxyObject();

        // get changed properties before h.commit()s
        final Iterable<Property> changedProperties = dbMappings.changedProperties(oldObject, info);

        // see HACK block bellow
        final boolean updateResouceLayersName =
                info instanceof ResourceInfo
                        && modificationProxy.getPropertyNames().contains("name");
        final boolean updateResouceLayersAdvertised =
                info instanceof ResourceInfo
                        && modificationProxy.getPropertyNames().contains("advertised");
        final boolean updateResourceLayersEnabled =
                info instanceof ResourceInfo
                        && modificationProxy.getPropertyNames().contains("enabled");
        final boolean updateResourceLayersKeywords =
                CollectionUtils.exists(
                        modificationProxy.getPropertyNames(),
                        new Predicate() {
                            @Override
                            public boolean evaluate(Object input) {
                                return ((String) input).contains("keyword");
                            }
                        });

        modificationProxy.commit();

        Map<String, ?> params;

        // get the object's internal id
        final Integer objectId = findObjectId(info);
        byte[] value = binding.objectToEntry(info);
        final String blob = new String(value, StandardCharsets.UTF_8);
        String updateStatement = "update object set blob = :blob where oid = :oid";
        params = params("blob", blob, "oid", objectId);
        logStatement(updateStatement, params);
        template.update(updateStatement, params);

        updateQueryableProperties(oldObject, objectId, changedProperties);

        Class<T> clazz = ClassMappings.fromImpl(oldObject.getClass()).getInterface();

        // / <HACK>
        // we're explicitly changing the resourceinfo's layer name property here because
        // LayerInfo.getName() is a derived property. This can be removed once LayerInfo.name become
        // a regular JavaBean property
        if (info instanceof ResourceInfo) {
            if (updateResouceLayersName) {
                updateResourceLayerProperty(
                        (ResourceInfo) info, "name", ((ResourceInfo) info).getName());
            }
            if (updateResouceLayersAdvertised) {
                updateResourceLayerProperty(
                        (ResourceInfo) info, "advertised", ((ResourceInfo) info).isAdvertised());
            }
            if (updateResourceLayersEnabled) {
                updateResourceLayerProperty(
                        (ResourceInfo) info, "enabled", ((ResourceInfo) info).isEnabled());
            }
            if (updateResourceLayersKeywords) {
                updateResourceLayerProperty(
                        (ResourceInfo) info,
                        "resource.keywords.value",
                        ((ResourceInfo) info).getKeywords());
            }
        }
        // / </HACK>

        return getById(id, clazz);
    }

    private <T> void updateResourceLayerProperty(
            ResourceInfo info, String propertyPath, Object newValue) {
        Filter filter = Predicates.equal("resource.id", info.getId());
        List<LayerInfo> resourceLayers;
        resourceLayers = this.queryAsList(LayerInfo.class, filter, null, null, null);
        for (LayerInfo layer : resourceLayers) {
            Set<PropertyType> propertyTypes =
                    dbMappings.getPropertyTypes(LayerInfo.class, propertyPath);
            PropertyType propertyType = propertyTypes.iterator().next();
            Property changedProperty = new Property(propertyType, newValue);
            Integer layerOid = findObjectId(layer);
            updateQueryableProperties(layer, layerOid, ImmutableSet.of(changedProperty));
        }
    }

    private Integer findObjectId(final Info info) {
        final String id = info.getId();
        final String oidQuery = "select oid from object where id = :id";
        Map<String, ?> params = params("id", id);
        logStatement(oidQuery, params);
        final Integer objectId = template.queryForObject(oidQuery, params, Integer.class);
        Preconditions.checkState(objectId != null, "Object not found: " + id);
        return objectId;
    }

    private void updateQueryableProperties(
            final Info info, final Integer objectId, Iterable<Property> changedProperties) {

        Map<String, ?> params;

        final Integer oid = objectId;
        Integer propertyType;
        Integer relatedOid;
        Integer relatedPropertyType;
        Integer colIndex;
        String storedValue;

        for (Property changedProp : changedProperties) {
            LOGGER.finer("Updating property " + changedProp);

            final boolean isRelationship = changedProp.isRelationship();
            propertyType = changedProp.getPropertyType().getOid();

            final List<?> values = valueList(changedProp);

            for (int i = 0; i < values.size(); i++) {
                final Object rawValue = values.get(i);
                storedValue = marshalValue(rawValue);
                checkArgument(
                        changedProp.isCollectionProperty() || values.size() == 1,
                        "Got a multivalued value for a non collection property "
                                + changedProp.getPropertyName()
                                + "="
                                + values);

                colIndex = changedProp.isCollectionProperty() ? (i + 1) : 0;

                if (isRelationship) {
                    final Info relatedObject = lookUpRelatedObject(info, changedProp, colIndex);
                    relatedOid = relatedObject == null ? null : findObjectId(relatedObject);
                    relatedPropertyType = changedProp.getPropertyType().getTargetPropertyOid();
                } else {
                    // it's a self property, lets update the value on the property table
                    relatedOid = null;
                    relatedPropertyType = null;
                }
                String sql =
                        "update object_property set " //
                                + "related_oid = :related_oid, " //
                                + "related_property_type = :related_property_type, " //
                                + "value = :value " //
                                + "where oid = :oid and property_type = :property_type and colindex = :colindex";
                params =
                        params(
                                "related_oid",
                                relatedOid,
                                "related_property_type",
                                relatedPropertyType,
                                "value",
                                storedValue,
                                "oid",
                                oid,
                                "property_type",
                                propertyType,
                                "colindex",
                                colIndex);

                logStatement(sql, params);
                final int updateCnt = template.update(sql, params);

                if (updateCnt == 0) {
                    addAttribute(info, oid, changedProp, colIndex, storedValue);
                } else {
                    // prop existed already, lets update any related property that points to its old
                    // value
                    String updateRelated =
                            "update object_property set value = :value "
                                    + "where related_oid = :oid and related_property_type = :property_type and colindex = :colindex";
                    params =
                            params(
                                    "oid",
                                    oid,
                                    "property_type",
                                    propertyType,
                                    "colindex",
                                    colIndex,
                                    "value",
                                    storedValue);
                    logStatement(updateRelated, params);
                    int relatedUpdateCnt = template.update(updateRelated, params);
                    if (LOGGER.isLoggable(Level.FINER)) {
                        LOGGER.finer(
                                "Updated "
                                        + relatedUpdateCnt
                                        + " back pointer properties to "
                                        + changedProp.getPropertyName()
                                        + " of "
                                        + info.getClass().getSimpleName()
                                        + "["
                                        + info.getId()
                                        + "]");
                    }
                }
            }
            if (changedProp.isCollectionProperty()) {
                // delete any remaining collection value that's no longer in the value list
                String sql =
                        "delete from object_property where oid=:oid and property_type=:property_type "
                                + "and colindex > :maxIndex";
                Integer maxIndex = Integer.valueOf(values.size());
                params = params("oid", oid, "property_type", propertyType, "maxIndex", maxIndex);
                logStatement(sql, params);
                template.update(sql, params);
            }
        }
    }

    @Nullable
    public <T extends Info> T getById(final String id, final Class<T> type) {
        Assert.notNull(id, "id");

        Info info = null;
        try {
            final Callable<? extends Info> valueLoader;
            if (CatalogInfo.class.isAssignableFrom(type)) {
                valueLoader = new CatalogLoader(id);
            } else {
                valueLoader = new ConfigLoader(id);
            }

            Semaphore lock = locks.computeIfAbsent(id, x -> new Semaphore(1));

            info = cache.getIfPresent(id);
            if (info == null) {
                // we try the write lock
                if (lock.tryAcquire()) {
                    try {
                        info = cache.get(id, valueLoader);
                    } finally {
                        lock.release();
                    }
                }
            }

            if (info == null) {
                // if the write lock was locked, we fall back
                // to a read-only method
                try {
                    info = valueLoader.call();
                } catch (Exception e) {
                    throw new ExecutionException(e);
                }
            }

        } catch (CacheLoader.InvalidCacheLoadException notFound) {
            return null;
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        }

        if (info == null) {
            return null;
        }
        if (info instanceof CatalogInfo) {
            info = resolveCatalog((CatalogInfo) info);
        } else if (info instanceof ServiceInfo) {
            resolveTransient((ServiceInfo) info);
        }

        if (type.isAssignableFrom(info.getClass())) {
            // use ModificationProxy only in this case as returned object is cached. saveInternal
            // follows suite checking whether the object being saved is a mod proxy, but that's not
            // mandatory in this implementation and should only be the case when the object was
            // obtained by id
            return ModificationProxy.create(type.cast(info), type);
        }

        return null;
    }

    @Nullable
    public <T extends Info> String getIdByIdentity(
            final Class<T> type, final String... identityMappings) {
        Assert.notNull(identityMappings, "id");
        int length = identityMappings.length / 2;
        String[] descriptor = new String[length];
        String[] values = new String[length];
        for (int i = 0; i < length; i++) {
            descriptor[i] = identityMappings[i * 2];
            values[i] = identityMappings[i * 2 + 1];
        }
        InfoIdentity infoIdentity = new InfoIdentity(InfoIdentities.root(type), descriptor, values);

        String id = null;
        try {
            id = identityCache.get(infoIdentity, new IdentityLoader(infoIdentity));

        } catch (CacheLoader.InvalidCacheLoadException notFound) {
            return null;
        } catch (ExecutionException e) {
            throwIfUnchecked(e.getCause());
            throw new RuntimeException(e.getCause());
        }

        return id;
    }

    @Nullable
    public ServiceInfo getService(
            final WorkspaceInfo ws, final Class<? extends ServiceInfo> clazz) {
        Assert.notNull(clazz, "clazz");

        ServiceInfo info = null;
        try {
            ServiceIdentity id = new ServiceIdentity(clazz, ws);
            info = serviceCache.get(id, new ServiceLoader(id));

        } catch (CacheLoader.InvalidCacheLoadException notFound) {
            return null;
        } catch (ExecutionException e) {
            Throwable throwable = e.getCause();
            throwIfUnchecked(throwable);
            throw new RuntimeException(throwable);
        }

        if (info == null) {
            return null;
        }
        resolveTransient(info);

        return info;
    }

    @Nullable
    public <T extends Info> T getByIdentity(final Class<T> type, final String... identityMappings) {
        String id = getIdByIdentity(type, identityMappings);

        if (id == null) {
            return null;
        } else {
            return getById(id, type);
        }
    }

    private <T extends CatalogInfo> T resolveCatalog(final T real) {
        if (real == null) {
            return null;
        }
        CatalogImpl catalog = getCatalog();
        catalog.resolve(real);
        // may the cached value have been serialized and hence lost transient fields? (that's why I
        // don't like having transient fields foreign to the domain model in the catalog config
        // objects)
        resolveTransient(real);

        // if this came from the cache, force update references
        real.accept(new CatalogReferenceUpdater());

        return real;
    }

    private <T extends CatalogInfo> void resolveTransient(T real) {
        if (null == real) {
            return;
        }
        real = ModificationProxy.unwrap(real);
        if (real instanceof StyleInfoImpl
                || real instanceof StoreInfoImpl
                || real instanceof ResourceInfoImpl) {
            OwsUtils.set(real, "catalog", catalog);
        }
        if (real instanceof ResourceInfoImpl) {
            resolveTransient(((ResourceInfoImpl) real).getStore());
        } else if (real instanceof LayerInfo) {
            LayerInfo layer = (LayerInfo) real;
            resolveTransient(layer.getDefaultStyle());
            if (!layer.getStyles().isEmpty()) {
                for (StyleInfo s : layer.getStyles()) {
                    resolveTransient(s);
                }
            }
            resolveTransient(layer.getResource());
        } else if (real instanceof LayerGroupInfo) {
            for (PublishedInfo p : ((LayerGroupInfo) real).getLayers()) {
                resolveTransient(p);
            }
            for (StyleInfo s : ((LayerGroupInfo) real).getStyles()) {
                resolveTransient(s);
            }
        }
    }

    private <T extends ServiceInfo> void resolveTransient(T real) {
        real = ModificationProxy.unwrap(real);
        OwsUtils.resolveCollections(real);
        real.setGeoServer(getGeoServer());
    }

    /** @return immutable list of results */
    public <T extends Info> List<T> getAll(final Class<T> clazz) {

        Map<String, ?> params = params("types", typesParam(clazz));

        final String sql = "select id from object where type_id in ( :types ) order by id";

        logStatement(sql, params);
        Stopwatch sw = Stopwatch.createStarted();
        List<String> ids = template.queryForList(sql, params, String.class);
        sw.stop();
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("query returned " + ids.size() + " records in " + sw);
        }

        List<T> transformed =
                Lists.transform(
                        ids,
                        new Function<String, T>() {
                            @Nullable
                            @Override
                            public T apply(String input) {
                                return getById(input, clazz);
                            }
                        });
        Iterable<T> filtered =
                Iterables.filter(transformed, com.google.common.base.Predicates.notNull());
        return ImmutableList.copyOf(filtered);
    }

    private <T extends Info> List<Integer> typesParam(final Class<T> clazz) {

        final Class<?>[] actualTypes;

        actualTypes = ClassMappings.fromInterface(clazz).concreteInterfaces();

        List<Integer> inValues = new ArrayList<Integer>(actualTypes.length);
        for (Class<?> type : actualTypes) {
            inValues.add(this.dbMappings.getTypeId(type));
        }

        return inValues;
    }

    @Transactional(
        transactionManager = "jdbcConfigTransactionManager",
        propagation = Propagation.REQUIRED,
        rollbackFor = Exception.class
    )
    public void setDefault(final String key, @Nullable final String id) {
        String sql = "DELETE FROM DEFAULT_OBJECT WHERE DEF_KEY = :key";
        Map<String, ?> params = params("key", key);
        logStatement(sql, params);
        template.update(sql, params);
        if (id != null) {
            sql = "INSERT INTO DEFAULT_OBJECT (DEF_KEY, ID) VALUES(:key, :id)";
            params = params("key", key, "id", id);
            logStatement(sql, params);
            template.update(sql, params);
        }
    }

    public void dispose() {
        cache.invalidateAll();
        cache.cleanUp();
        identityCache.invalidateAll();
        identityCache.cleanUp();
        disposeServiceCache();
    }

    private void disposeServiceCache() {
        serviceCache.invalidateAll();
        serviceCache.cleanUp();
    }

    private final class CatalogLoader implements Callable<CatalogInfo> {

        private final String id;

        public CatalogLoader(final String id) {
            this.id = id;
        }

        @Override
        public CatalogInfo call() throws Exception {
            CatalogInfo info;
            try {
                String sql = "select blob from object where id = :id";
                Map<String, String> params = ImmutableMap.of("id", id);
                logStatement(sql, params);
                info = template.queryForObject(sql, params, catalogRowMapper);
            } catch (EmptyResultDataAccessException noSuchObject) {
                return null;
            }
            return info;
        }
    }

    private final class IdentityLoader implements Callable<String> {

        private final InfoIdentity identity;

        public IdentityLoader(final InfoIdentity identity) {
            this.identity = identity;
        }

        @Override
        public String call() throws Exception {
            Filter filter = Filter.INCLUDE;
            for (int i = 0; i < identity.getDescriptor().length; i++) {
                filter =
                        and(
                                filter,
                                identity.getValues()[i] == null
                                        ? isNull(identity.getDescriptor()[i])
                                        : equal(
                                                identity.getDescriptor()[i],
                                                identity.getValues()[i]));
            }

            try {
                return getId(identity.getClazz(), filter);
            } catch (IllegalArgumentException multipleResults) {
                return null;
            }
        }
    }

    private static final class ServiceIdentity implements Serializable {
        private static final long serialVersionUID = 4054478633697271203L;

        private Class<? extends ServiceInfo> clazz;
        private WorkspaceInfo workspace;

        public ServiceIdentity(Class<? extends ServiceInfo> clazz, WorkspaceInfo workspace) {
            this.clazz = clazz;
            this.workspace = workspace;
        }

        public Class<? extends ServiceInfo> getClazz() {
            return clazz;
        }

        public WorkspaceInfo getWorkspace() {
            return workspace;
        }

        @Override
        public int hashCode() {
            final int prime = 31;
            int result = 1;
            result = prime * result + ((clazz == null) ? 0 : clazz.hashCode());
            result = prime * result + ((workspace == null) ? 0 : workspace.hashCode());
            return result;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;
            ServiceIdentity other = (ServiceIdentity) obj;
            if (clazz == null) {
                if (other.clazz != null) return false;
            } else if (!clazz.equals(other.clazz)) return false;
            if (workspace == null) {
                if (other.workspace != null) return false;
            } else if (!workspace.equals(other.workspace)) return false;
            return true;
        }
    }

    @SuppressWarnings("unchecked")
    private <T extends ServiceInfo> CloseableIterator<T> filterService(
            final Class<T> clazz, CloseableIterator<ServiceInfo> it) {
        return (CloseableIterator<T>)
                CloseableIteratorAdapter.filter(
                        it,
                        new com.google.common.base.Predicate<ServiceInfo>() {

                            @Override
                            public boolean apply(@Nullable ServiceInfo input) {
                                return clazz.isAssignableFrom(input.getClass());
                            }
                        });
    }

    private final class ServiceLoader implements Callable<ServiceInfo> {

        private final ServiceIdentity id;

        public ServiceLoader(final ServiceIdentity id) {
            this.id = id;
        }

        @Override
        public ServiceInfo call() throws Exception {
            Filter filter;
            if (id.getWorkspace() != null && id.getWorkspace() != ANY_WORKSPACE) {
                filter = equal("workspace.id", id.getWorkspace().getId());
            } else {
                filter = isNull("workspace.id");
            }

            // In order to handle new service types, get all services, deserialize them, and then
            // filter
            // by checking if the implement the given interface.  Since there shouldn't be too many
            // per
            // workspace, this shouldn't be a significant performance problem.
            CloseableIterator<? extends ServiceInfo> it =
                    filterService(
                            id.getClazz(),
                            query(ServiceInfo.class, filter, null, null, (SortBy) null));

            ServiceInfo service;
            if (it.hasNext()) {
                service = it.next();
            } else {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(
                            Level.FINE,
                            "Could not find service of type "
                                    + id.getClazz()
                                    + " in "
                                    + id.getWorkspace());
                return null;
            }

            if (it.hasNext()) {
                LOGGER.log(
                        Level.WARNING,
                        "Found multiple services of type "
                                + id.getClass()
                                + " in "
                                + id.getWorkspace());
                return null;
            }
            return service;
        }
    }

    private final class ConfigLoader implements Callable<Info> {

        private final String id;

        public ConfigLoader(final String id) {
            this.id = id;
        }

        @Override
        public Info call() throws Exception {
            Info info;
            try {
                String sql = "select blob from object where id = :id";
                Map<String, String> params = ImmutableMap.of("id", id);
                logStatement(sql, params);
                info = template.queryForObject(sql, params, configRowMapper);
            } catch (EmptyResultDataAccessException noSuchObject) {
                return null;
            }
            OwsUtils.resolveCollections(info);
            if (info instanceof GeoServerInfo) {

                GeoServerInfoImpl global = (GeoServerInfoImpl) info;
                if (global.getMetadata() == null) {
                    global.setMetadata(new MetadataMap());
                }
                if (global.getClientProperties() == null) {
                    global.setClientProperties(new HashMap<Object, Object>());
                }
                if (global.getCoverageAccess() == null) {
                    global.setCoverageAccess(new CoverageAccessInfoImpl());
                }
                if (global.getJAI() == null) {
                    global.setJAI(new JAIInfoImpl());
                }
            }
            if (info instanceof ServiceInfo) {
                ((ServiceInfo) info).setGeoServer(geoServer);
            }

            return info;
        }
    }

    /**
     * @return whether there exists a property named {@code propertyName} for the given type of
     *     object, and hence native sorting can be done over it.
     */
    public boolean canSort(Class<? extends CatalogInfo> type, String propertyName) {
        Set<PropertyType> propertyTypes = dbMappings.getPropertyTypes(type, propertyName);
        return !propertyTypes.isEmpty();
    }

    public void clearCache(Info info) {
        if (info instanceof ServiceInfo) {
            // need to figure out how to remove only the relevant cache
            // entries for the service info, like with InfoIdenties below,
            // that will be able to handle new service types.
            disposeServiceCache();
        }
        identityCache.invalidateAll(InfoIdentities.get().getIdentities(info));
        cache.invalidate(info.getId());
    }

    public void clearCacheIfPresent(String id) {
        Info info = cache.getIfPresent(id);
        if (info != null) {
            clearCache(info);
        }
    }

    void updateCache(Info info) {
        info = ModificationProxy.unwrap(info);
        cache.put(info.getId(), info);
        List<InfoIdentity> identities = InfoIdentities.get().getIdentities(info);
        for (InfoIdentity identity : identities) {
            if (identityCache.getIfPresent(identity) == null) {
                identityCache.put(identity, info.getId());
            } else {
                // not a unique identity
                identityCache.invalidate(identity);
            }
        }
    }

    public <T extends Info> T get(Class<T> type, Filter filter) throws IllegalArgumentException {

        CloseableIterator<T> it =
                query(type, filter, null, 2, (org.opengis.filter.sort.SortBy) null);
        T result = null;
        try {
            if (it.hasNext()) {
                result = it.next();
                if (it.hasNext()) {
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
                }
            }
        } finally {
            it.close();
        }
        return result;
    }

    public <T extends Info> String getId(Class<T> type, Filter filter)
            throws IllegalArgumentException {

        CloseableIterator<String> it = queryIds(type, filter);
        String result = null;
        try {
            if (it.hasNext()) {
                result = it.next();
                if (it.hasNext()) {
                    throw new IllegalArgumentException(
                            "Specified query predicate resulted in more than one object");
                }
            }
        } finally {
            it.close();
        }
        return result;
    }

    private void acquireWriteLock(String id) {
        Semaphore lock = locks.computeIfAbsent(id, x -> new Semaphore(1));
        try {
            if (!lock.tryAcquire(LOCK_TIMEOUT_SECONDS, TimeUnit.SECONDS)) {
                LOGGER.severe(
                        "Time-out waiting for lock on "
                                + id
                                + ", assuming it was abandoned and moving on. This shouldn't happen!");
            }
        } catch (InterruptedException e) {

        }
    }

    private void releaseWriteLock(String id) {
        locks.get(id).release();
    }

    /** Listens to catalog events clearing cache entires when resources are modified. */
    // Copied from org.geoserver.catalog.ResourcePool
    public class CatalogClearingListener implements CatalogListener {

        public void handleAddEvent(CatalogAddEvent event) {
            updateCache(event.getSource());
        }

        public void handleModifyEvent(CatalogModifyEvent event) {
            // make sure that cache is not refilled before commit
            if (event.getSource() instanceof ResourceInfo) {
                String liId =
                        getIdByIdentity(LayerInfo.class, "resource.id", event.getSource().getId());
                acquireWriteLock(liId);
                clearCacheIfPresent(liId);
            }
            acquireWriteLock(event.getSource().getId());
            clearCache(event.getSource());
        }

        public void handlePostModifyEvent(CatalogPostModifyEvent event) {
            updateCache(event.getSource());
            releaseWriteLock(event.getSource().getId());
            if (event.getSource() instanceof ResourceInfo) {
                String liId =
                        getIdByIdentity(LayerInfo.class, "resource.id", event.getSource().getId());
                releaseWriteLock(liId);
            }
        }

        public void handleRemoveEvent(CatalogRemoveEvent event) {
            clearCache(event.getSource());
        }

        public void reloaded() {}
    }
    /** Listens to configuration events clearing cache entires when resources are modified. */
    public class ConfigClearingListener extends ConfigurationListenerAdapter {

        @Override
        public void handleSettingsRemoved(SettingsInfo settings) {
            clearCache(settings);
        }

        @Override
        public void handleServiceRemove(ServiceInfo service) {
            clearCache(service);
        }

        @Override
        public void handleGlobalChange(
                GeoServerInfo global,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // make sure that cache is not refilled before commit
            acquireWriteLock(global.getId());
            clearCache(global);
        }

        @Override
        public void handlePostGlobalChange(GeoServerInfo global) {
            updateCache(global);
            releaseWriteLock(global.getId());
        }

        @Override
        public void handleSettingsModified(
                SettingsInfo settings,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // make sure that cache is not refilled before commit
            acquireWriteLock(settings.getId());
            clearCache(settings);
        }

        @Override
        public void handleSettingsPostModified(SettingsInfo settings) {
            updateCache(settings);
            releaseWriteLock(settings.getId());
        }

        @Override
        public void handleLoggingChange(
                LoggingInfo logging,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // make sure that cache is not refilled before commit
            acquireWriteLock(logging.getId());
            clearCache(logging);
        }

        @Override
        public void handlePostLoggingChange(LoggingInfo logging) {
            updateCache(logging);
            releaseWriteLock(logging.getId());
        }

        @Override
        public void handleServiceChange(
                ServiceInfo service,
                List<String> propertyNames,
                List<Object> oldValues,
                List<Object> newValues) {
            // make sure that cache is not refilled before commit
            acquireWriteLock(service.getId());
            clearCache(service);
        }

        @Override
        public void handlePostServiceChange(ServiceInfo service) {
            updateCache(service);
            releaseWriteLock(service.getId());
        }

        @Override
        public void handleSettingsAdded(SettingsInfo settings) {
            updateCache(settings);
        }
    }

    public class CatalogReferenceUpdater implements CatalogVisitor {

        private CatalogReferenceUpdater() {}

        @Override
        public void visit(Catalog catalog) {}

        @Override
        public void visit(WorkspaceInfo workspace) {}

        @Override
        public void visit(NamespaceInfo workspace) {}

        public void visitStore(StoreInfo store) {
            if (store.getWorkspace() != null) {
                store.setWorkspace(getById(store.getWorkspace().getId(), WorkspaceInfo.class));
            }
        }

        @Override
        public void visit(DataStoreInfo dataStore) {
            visitStore(dataStore);
        }

        @Override
        public void visit(CoverageStoreInfo coverageStore) {
            visitStore(coverageStore);
        }

        @Override
        public void visit(WMSStoreInfo wmsStore) {
            visitStore(wmsStore);
        }

        @Override
        public void visit(WMTSStoreInfo wmsStore) {
            visitStore(wmsStore);
        }

        public void visitResource(ResourceInfo resourceInfo) {
            if (resourceInfo.getNamespace() != null) {
                resourceInfo.setNamespace(
                        getById(resourceInfo.getNamespace().getId(), NamespaceInfo.class));
            }
            resourceInfo.setStore(getById(resourceInfo.getStore().getId(), StoreInfo.class));
        }

        @Override
        public void visit(FeatureTypeInfo featureType) {
            visitResource(featureType);
        }

        @Override
        public void visit(CoverageInfo coverage) {
            visitResource(coverage);
        }

        @Override
        public void visit(WMSLayerInfo wmsLayer) {
            visitResource(wmsLayer);
        }

        @Override
        public void visit(WMTSLayerInfo wmtsLayer) {
            visitResource(wmtsLayer);
        }

        @Override
        public void visit(LayerInfo layer) {
            if (layer.getDefaultStyle() != null) {
                layer.setDefaultStyle(getById(layer.getDefaultStyle().getId(), StyleInfo.class));
            }
            Set<StyleInfo> newStyles = new HashSet<>();
            for (StyleInfo style : layer.getStyles()) {
                if (style != null) {
                    newStyles.add(getById(style.getId(), StyleInfo.class));
                }
            }
            layer.getStyles().clear();
            layer.getStyles().addAll(newStyles);
        }

        @Override
        public void visit(StyleInfo style) {
            if (style.getWorkspace() != null) {
                style.setWorkspace(getById(style.getWorkspace().getId(), WorkspaceInfo.class));
            }
        }

        @Override
        public void visit(LayerGroupInfo layerGroup) {
            if (layerGroup.getWorkspace() != null) {
                layerGroup.setWorkspace(
                        getById(layerGroup.getWorkspace().getId(), WorkspaceInfo.class));
            }
            for (int i = 0; i < layerGroup.getLayers().size(); i++) {
                if (layerGroup.getLayers().get(i) != null) {
                    layerGroup
                            .getLayers()
                            .set(
                                    i,
                                    getById(
                                            layerGroup.getLayers().get(i).getId(),
                                            PublishedInfo.class));
                }
            }
            if (layerGroup.getRootLayer() != null) {
                layerGroup.setRootLayer(
                        getById(layerGroup.getRootLayer().getId(), LayerInfo.class));
            }
            if (layerGroup.getRootLayerStyle() != null) {
                layerGroup.setRootLayerStyle(
                        getById(layerGroup.getRootLayerStyle().getId(), StyleInfo.class));
            }
            for (int i = 0; i < layerGroup.getStyles().size(); i++) {
                if (layerGroup.getStyles().get(i) != null) {
                    layerGroup
                            .getStyles()
                            .set(
                                    i,
                                    getById(
                                            layerGroup.getStyles().get(i).getId(),
                                            StyleInfo.class));
                }
            }
        }
    }
}
