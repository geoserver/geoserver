/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;

import org.geoserver.catalog.Info;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

class QueryBuilder<T extends Info> {

    private static final SortBy DEFAULT_ORDER = CommonFactoryFinder.getFilterFactory().sort("id",
            SortOrder.ASCENDING);

    private Integer offset;

    private Integer limit;

    private SortBy sortOrder;

    private final boolean isCountQuery;

    private Dialect dialect = new Dialect();

    private Class<T> queryType;

    private FilterToCatalogSQL predicateBuilder;

    private DbMappings dbMappings;

    private Filter originalFilter;

    private Filter supportedFilter;

    private Filter unsupportedFilter;

    /**
     * @param clazz
     * @param
     * 
     * 
     */
    private QueryBuilder(final Class<T> clazz, DbMappings dbMappings, final boolean isCountQuery) {
        this.queryType = clazz;
        this.dbMappings = dbMappings;
        this.isCountQuery = isCountQuery;
        this.originalFilter = this.supportedFilter = this.unsupportedFilter = Filter.INCLUDE;
    }

    public static <T extends Info> QueryBuilder<T> forCount(final Class<T> clazz,
            DbMappings dbMappings) {
        return new QueryBuilder<T>(clazz, dbMappings, true);
    }

    public static <T extends Info> QueryBuilder<T> forIds(final Class<T> clazz,
            DbMappings dbMappings) {
        return new QueryBuilder<T>(clazz, dbMappings, false);
    }

    public Filter getUnsupportedFilter() {
        return unsupportedFilter;
    }

    public Filter getSupportedFilter() {
        return supportedFilter;
    }

    public Map<String, Object> getNamedParameters() {
        Map<String, Object> params = Collections.emptyMap();
        if (predicateBuilder != null) {
            params = predicateBuilder.getNamedParameters();
        }
        return params;
    }

    public QueryBuilder<T> offset(Integer offset) {
        this.offset = offset;
        return this;
    }

    public QueryBuilder<T> limit(Integer limit) {
        this.limit = limit;
        return this;
    }

    public QueryBuilder<T> sortOrder(SortBy order) {
        this.sortOrder = order;
        return this;
    }

    public QueryBuilder<T> filter(Filter filter) {
        this.originalFilter = filter;
        return this;
    }

    public StringBuilder build() {
        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();
        
        this.predicateBuilder = new FilterToCatalogSQL(this.queryType, this.dbMappings);
        {
            Capabilities fcs = new Capabilities(FilterToCatalogSQL.CAPABILITIES);
            FeatureType parent = null;
            // use this to instruct the filter splitter which filters can be encoded depending on
            // whether a db mapping for a given property name exists
            ClientTransactionAccessor transactionAccessor = new ClientTransactionAccessor() {

                @Override
                public Filter getUpdateFilter(final String attributePath) {
                    Set<PropertyType> propertyTypes;
                    propertyTypes = dbMappings.getPropertyTypes(queryType, attributePath);

                    final boolean isMappedProp = !propertyTypes.isEmpty();

                    if (isMappedProp) {
                        // continue normally
                        return null;
                    }
                    // tell the caps filter splitter this property name is not encodable
                    return Filter.EXCLUDE;
                }

                @Override
                public Filter getDeleteFilter() {
                    return null;
                }
            };
            

            CapabilitiesFilterSplitter filterSplitter;
            filterSplitter = new CapabilitiesFilterSplitter(fcs, parent, transactionAccessor);

            final Filter filter = (Filter) this.originalFilter.accept(filterSimplifier, null);
            filter.accept(filterSplitter, null);

            Filter supported = filterSplitter.getFilterPre();
            Filter unsupported = filterSplitter.getFilterPost();
            this.supportedFilter = (Filter) supported.accept(filterSimplifier, null);
            this.unsupportedFilter = (Filter) unsupported.accept(filterSimplifier, null);
        }

        StringBuilder whereClause = new StringBuilder();
        whereClause = (StringBuilder) this.supportedFilter.accept(predicateBuilder, whereClause);

        StringBuilder query = new StringBuilder();
        if (isCountQuery) {
            if (Filter.INCLUDE.equals(this.originalFilter)) {
                query.append("select count(oid) from object where type_id in (:types)");
            } else {
                query.append("select count(oid) from object where type_id in (:types) AND (\n");
                query.append(whereClause).append("\n)");
            }
        } else {
            final SortBy order = this.sortOrder;
            if (order == null) {
                query.append("select id from object where type_id in (:types) AND (\n");
                query.append(whereClause).append(")\n");
                query.append(" ORDER BY oid");
            } else {
                final String sortProperty = order.getPropertyName().getPropertyName();
                final Set<Integer> sortPropertyTypeIds;
                sortPropertyTypeIds = dbMappings.getPropertyTypeIds(queryType, sortProperty);

                Map<String, Object> namedParameters = getNamedParameters();
                namedParameters.put("sortProperty", sortPropertyTypeIds);

                query.append("select id from object_property where property_type in (:sortProperty) AND (\n");

                query.append(whereClause).append("\n)\n");
                query.append(" ORDER BY value ").append(
                        SortOrder.ASCENDING.equals(order.getSortOrder()) ? "ASC" : "DESC");
            }
            applyOffsetLimit(query);

        }

        return query;
    }

    protected void applyOffsetLimit(StringBuilder sql) {
        dialect.applyOffsetLimit(sql, offset, limit);
    }

}
