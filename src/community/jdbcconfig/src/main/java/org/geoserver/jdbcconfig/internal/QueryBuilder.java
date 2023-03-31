/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.Info;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.geotools.filter.visitor.LiteralDemultiplyingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

class QueryBuilder<T extends Info> {

    private static final Logger LOGGER = Logging.getLogger(QueryBuilder.class);

    @SuppressWarnings("unused")
    private static final SortBy DEFAULT_ORDER =
            CommonFactoryFinder.getFilterFactory().sort("id", SortOrder.ASCENDING);

    private Integer offset;

    private Integer limit;

    private SortBy[] sortOrder;

    private final boolean isCountQuery;

    // yuck
    private final Dialect dialect;

    private Class<T> queryType;

    private FilterToCatalogSQL predicateBuilder;

    private DbMappings dbMappings;

    private Filter originalFilter;

    private Filter supportedFilter;

    private Filter unsupportedFilter;

    private boolean offsetLimitApplied = false;

    /** */
    private QueryBuilder(
            Dialect dialect,
            final Class<T> clazz,
            DbMappings dbMappings,
            final boolean isCountQuery) {
        this.dialect = dialect;
        this.queryType = clazz;
        this.dbMappings = dbMappings;
        this.isCountQuery = isCountQuery;
        this.originalFilter = this.supportedFilter = this.unsupportedFilter = Filter.INCLUDE;
    }

    public static <T extends Info> QueryBuilder<T> forCount(
            Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
        return new QueryBuilder<T>(dialect, clazz, dbMappings, true);
    }

    public static <T extends Info> QueryBuilder<T> forIds(
            Dialect dialect, final Class<T> clazz, DbMappings dbMappings) {
        return new QueryBuilder<T>(dialect, clazz, dbMappings, false);
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
        if (order == null) {
            this.sortOrder();
        } else {
            this.sortOrder(new SortBy[] {order});
        }
        return this;
    }

    public QueryBuilder<T> sortOrder(SortBy... order) {
        if (order == null || order.length == 0) {
            this.sortOrder = null;
        } else {
            this.sortOrder = order;
        }
        return this;
    }

    public QueryBuilder<T> filter(Filter filter) {
        this.originalFilter = filter;
        return this;
    }

    private void querySortBy(StringBuilder query, String whereClause, SortBy[] orders) {

        /*
         * Start with the oid and id from the object table selecting for type and the filter.
         *
         * Then left join on oid for each property to sort by to turn it into an attribute.
         *
         * The sort each of the created attribute.
         */

        // Need to put together the ORDER BY clause as we go and then add it at the end
        StringBuilder orderBy = new StringBuilder();
        orderBy.append("ORDER BY ");

        int i = 0;

        query.append("SELECT id FROM");
        dialect.appendIfDebug(query, "\n    ", " ");
        query.append("(SELECT oid, id FROM object WHERE type_id IN (:types)");
        dialect.appendComment(query, queryType.getName());
        if (whereClause != null) {
            dialect.appendIfDebug(query, "      ", " ");
            query.append("AND ").append(whereClause);
        }
        query.append(") object");

        for (SortBy order : orders) {
            final String sortProperty = order.getPropertyName().getPropertyName();
            final String subSelectName = "subSelect" + i;
            final String attributeName = "prop" + i;
            final String propertyParamName = "sortProperty" + i;

            final Set<Integer> sortPropertyTypeIds;
            sortPropertyTypeIds = dbMappings.getPropertyTypeIds(queryType, sortProperty);

            // Store the property type ID as a named parameter
            Map<String, Object> namedParameters = getNamedParameters();
            namedParameters.put(propertyParamName, sortPropertyTypeIds);

            dialect.appendIfDebug(query, "\n  ", " ");
            query.append("LEFT JOIN");
            dialect.appendIfDebug(query, "\n    ", " ");
            query.append("(SELECT oid, value ").append(attributeName).append(" FROM");
            dialect.appendIfDebug(query, "\n      ", " ");
            query.append("object_property WHERE property_type IN (:")
                    .append(propertyParamName)
                    .append(")) ")
                    .append(subSelectName);
            dialect.appendComment(
                    query, order.getPropertyName().getPropertyName(), " ", ascDesc(order));
            dialect.appendIfDebug(query, "  ", " ");
            query.append("ON object.oid = ").append(subSelectName).append(".oid");
            // Update the ORDER BY clause to be added later
            if (i > 0) orderBy.append(", ");
            orderBy.append(attributeName).append(" ").append(ascDesc(order));

            i++;
        }
        dialect.appendIfDebug(query, "\n  ", " ");
        query.append(orderBy);
    }

    private String buildWhereClause() {
        this.predicateBuilder =
                new FilterToCatalogSQL(this.dialect, this.queryType, this.dbMappings);
        if (Filter.INCLUDE.equals(this.originalFilter)) {
            return null;
        }
        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();
        Capabilities fcs = new Capabilities(FilterToCatalogSQL.CAPABILITIES);
        FeatureType parent = null;
        // use this to instruct the filter splitter which filters can be encoded depending on
        // whether a db mapping for a given property name exists
        ClientTransactionAccessor transactionAccessor =
                new ClientTransactionAccessor() {

                    @Override
                    public Filter getUpdateFilter(final String attributePath) {
                        Set<PropertyType> propertyTypes;
                        propertyTypes = dbMappings.getPropertyTypes(queryType, attributePath);

                        final boolean isMappedProp = !propertyTypes.isEmpty();

                        if (isMappedProp) {
                            // continue normally
                            return null;
                        } else if (LOGGER.isLoggable(Level.FINER)) {
                            LOGGER.finer("Unable to encode property: " + attributePath);
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
        Filter demultipliedFilter =
                (Filter) supported.accept(new LiteralDemultiplyingFilterVisitor(), null);
        this.supportedFilter = (Filter) demultipliedFilter.accept(filterSimplifier, null);
        this.unsupportedFilter = (Filter) unsupported.accept(filterSimplifier, null);
        if (Filter.INCLUDE.equals(this.supportedFilter)) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder();
        return this.supportedFilter.accept(predicateBuilder, whereClause).toString();
    }

    public String build() {

        String whereClause = buildWhereClause();

        StringBuilder query = new StringBuilder();
        if (isCountQuery) {
            query.append("SELECT COUNT(oid) FROM object WHERE type_id IN (:types)");
            dialect.appendComment(query, queryType.getName());
            if (whereClause != null) {
                dialect.appendIfDebug(query, "", " ");
                query.append("AND ").append(whereClause);
            }
        } else {
            if (sortOrder != null) {
                querySortBy(query, whereClause, sortOrder);
            } else {
                query.append("SELECT id FROM object WHERE type_id IN (:types)");
                dialect.appendComment(query, queryType.getName());
                dialect.appendIfDebug(query, "", " ");
                if (whereClause != null) {
                    query.append("AND ").append(whereClause);
                    dialect.appendIfDebug(query, whereClause.endsWith("\n") ? "" : " ", " ");
                }
                query.append("ORDER BY oid");
            }
            applyOffsetLimit(query);
        }

        return query.toString().trim();
    }

    /** When the query was built, were the offset and limit included. */
    public boolean isOffsetLimitApplied() {
        return offsetLimitApplied;
    }

    private static String ascDesc(SortBy order) {
        return SortOrder.ASCENDING.equals(order.getSortOrder()) ? "ASC" : "DESC";
    }

    protected void applyOffsetLimit(StringBuilder sql) {
        if (unsupportedFilter.equals(Filter.INCLUDE)) {
            dialect.applyOffsetLimit(sql, offset, limit);
            offsetLimitApplied = true;
        } else {
            offsetLimitApplied = false;
        }
    }
}
