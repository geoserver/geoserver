/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
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
import org.geotools.filter.visitor.LiteralDemultiplyingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

class QueryBuilder<T extends Info> {

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

    private void querySortBy(StringBuilder query, StringBuilder whereClause, SortBy[] orders) {

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

        query.append("SELECT id FROM ");

        query.append("\n    (SELECT oid, id FROM object WHERE ");
        if (queryType != null) {
            query.append("type_id in (:types) /* ")
                    .append(queryType.getCanonicalName())
                    .append(" */\n      AND ");
        }
        query.append(whereClause).append(") object");

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

            query.append("\n  LEFT JOIN");
            query.append("\n    (SELECT oid, value ")
                    .append(attributeName)
                    .append(" FROM \n      object_property WHERE property_type IN (:")
                    .append(propertyParamName)
                    .append(")) ")
                    .append(subSelectName);

            query.append("  /* ")
                    .append(order.getPropertyName().getPropertyName())
                    .append(" ")
                    .append(ascDesc(order))
                    .append(" */");

            query.append("\n  ON object.oid = ").append(subSelectName).append(".oid");
            // Update the ORDER BY clause to be added later
            if (i > 0) orderBy.append(", ");
            orderBy.append(attributeName).append(" ").append(ascDesc(order));

            i++;
        }

        query.append("\n  ").append(orderBy);
    }

    private StringBuilder buildWhereClause() {
        final SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();

        this.predicateBuilder = new FilterToCatalogSQL(this.queryType, this.dbMappings);
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

        StringBuilder whereClause = new StringBuilder();
        return (StringBuilder) this.supportedFilter.accept(predicateBuilder, whereClause);
    }

    public StringBuilder build() {

        StringBuilder whereClause = buildWhereClause();

        StringBuilder query = new StringBuilder();
        if (isCountQuery) {
            if (Filter.INCLUDE.equals(this.originalFilter)) {
                query.append("select count(oid) from object where type_id in (:types)");
            } else {
                query.append("select count(oid) from object where type_id in (:types) AND (\n");
                query.append(whereClause).append("\n)");
            }
        } else {
            SortBy[] orders = this.sortOrder;
            if (orders == null) {
                query.append("select id from object where type_id in (:types) AND (\n");
                query.append(whereClause).append(")\n");
                query.append(" ORDER BY oid");
            } else {
                querySortBy(query, whereClause, orders);
            }
            applyOffsetLimit(query);
        }

        return query;
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
