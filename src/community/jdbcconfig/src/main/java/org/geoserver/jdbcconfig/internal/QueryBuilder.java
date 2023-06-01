/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.jdbcconfig.internal;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.commons.lang3.tuple.Pair;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.function.IsInstanceOf;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.Capabilities;
import org.geotools.filter.visitor.CapabilitiesFilterSplitter;
import org.geotools.filter.visitor.ClientTransactionAccessor;
import org.geotools.filter.visitor.LiteralDemultiplyingFilterVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.And;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Function;
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

    public boolean isFullySupported() {
        return Filter.INCLUDE.equals(this.unsupportedFilter);
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
        Pair<Filter, Filter> split = splitFilter(this.originalFilter);
        this.supportedFilter = split.getLeft();
        this.unsupportedFilter = split.getRight();
        if (PublishedInfo.class.equals(this.queryType)) {
            splitPublishedInfoFilter();
        }
        if (Filter.INCLUDE.equals(this.supportedFilter)) {
            return null;
        }
        StringBuilder whereClause = new StringBuilder();
        return this.supportedFilter.accept(this.predicateBuilder, whereClause).toString();
    }

    private Pair<Filter, Filter> splitFilter(Filter filter) {
        Capabilities fcs = new Capabilities(FilterToCatalogSQL.CAPABILITIES);
        // use this to instruct the filter splitter which filters can be encoded depending on
        // whether a db mapping for a given property name exists
        ClientTransactionAccessor transactionAccessor =
                new ClientTransactionAccessor() {

                    @Override
                    public Filter getUpdateFilter(String attributePath) {
                        if (!dbMappings.getPropertyTypes(queryType, attributePath).isEmpty()) {
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
        CapabilitiesFilterSplitter filterSplitter =
                new CapabilitiesFilterSplitter(fcs, null, transactionAccessor);

        SimplifyingFilterVisitor filterSimplifier = new SimplifyingFilterVisitor();
        Filter simplified = (Filter) filter.accept(filterSimplifier, null);
        simplified.accept(filterSplitter, null);

        Filter supported = filterSplitter.getFilterPre();
        supported = (Filter) supported.accept(new LiteralDemultiplyingFilterVisitor(), null);
        supported = (Filter) supported.accept(filterSimplifier, null);
        Filter unsupported = filterSplitter.getFilterPost();
        unsupported = (Filter) unsupported.accept(filterSimplifier, null);
        return Pair.of(supported, unsupported);
    }

    /**
     * Checks if the unsupported filter contains an OR filter that matches the kind of published
     * info filter generated by the CSW extension. If it does, try to split that filter into two
     * separate OR filters, one that is supported by the JDBC Configuration database queries and one
     * that is not supported. This is a workaround for limitations handling OR queries in
     * org.geotools.filter.visitor.CapabilitiesFilterSplitter
     */
    private void splitPublishedInfoFilter() {
        // the published info filter may be AND'ed with other filters such as from
        // org.geoserver.security.SecureCatalogFacade
        List<Filter> children =
                this.unsupportedFilter instanceof And
                        ? ((And) this.unsupportedFilter).getChildren()
                        : Collections.singletonList(this.unsupportedFilter);
        Or or =
                children.stream()
                        .filter(Or.class::isInstance)
                        .map(Or.class::cast)
                        .filter(f -> f.getChildren().size() == 2)
                        .filter(f -> hasIsInstanceOf(f.getChildren().get(0)))
                        .filter(f -> hasIsInstanceOf(f.getChildren().get(1)))
                        .findFirst()
                        .orElse(null);
        if (or != null) {
            List<Filter> supported1 = new ArrayList<>();
            List<Filter> unsupported1 = new ArrayList<>();
            splitAndFilter(or.getChildren().get(0), supported1, unsupported1);
            List<Filter> supported2 = new ArrayList<>();
            List<Filter> unsupported2 = new ArrayList<>();
            splitAndFilter(or.getChildren().get(1), supported2, unsupported2);
            if (supported1.size() > 1 || supported2.size() > 1) {
                // update the filters if any part of the original filter could be split into
                // a new supported filter that is logically equivalent
                children = new ArrayList<>(children);
                children.remove(or);
                this.supportedFilter = appendFilter(this.supportedFilter, supported1, supported2);
                this.unsupportedFilter =
                        appendFilter(Predicates.and(children), unsupported1, unsupported2);
            }
        }
    }

    /**
     * Checks if the filter is an And filter with exactly one IsInstanceOf predicate
     *
     * @param filter the filter to check
     * @return whether the filter contains an IsInstanceOf filter
     */
    private static boolean hasIsInstanceOf(Filter filter) {
        if (filter instanceof And) {
            List<Filter> children = ((And) filter).getChildren();
            return children.stream().filter(QueryBuilder::isIsInstanceOf).count() == 1;
        }
        return false;
    }

    /**
     * Checks if the filter matches the filter created by calling
     * org.geoserver.catalog.Predicates.isInstanceOf(Class<?>)
     *
     * @param filter the filter to check
     * @return whether the filter is an IsInstanceOf filter
     */
    private static boolean isIsInstanceOf(Filter filter) {
        if (filter instanceof PropertyIsEqualTo) {
            PropertyIsEqualTo eq = (PropertyIsEqualTo) filter;
            return eq.getExpression1() instanceof Function
                    && IsInstanceOf.NAME.equals(((Function) eq.getExpression1()).getFunctionName());
        }
        return false;
    }

    /**
     * Splits and And filter into its supported components while adding the IsInstanceOf predicate
     * to both the supported and unsupported filters
     *
     * @param filter the filter to split
     * @param supported the list of supported filter children
     * @param unsupported the list of unsupported filter children
     */
    private void splitAndFilter(Filter filter, List<Filter> supported, List<Filter> unsupported) {
        for (Filter child : ((And) filter).getChildren()) {
            if (isIsInstanceOf(child)) {
                supported.add(child);
                unsupported.add(child);
            } else {
                Pair<Filter, Filter> split = splitFilter(child);
                if (!Filter.INCLUDE.equals(split.getLeft())) {
                    supported.add(split.getLeft());
                }
                if (!Filter.INCLUDE.equals(split.getRight())) {
                    unsupported.add(split.getRight());
                }
            }
        }
    }

    /**
     * Checks if either of the children lists contains more than the IsIstanceOf predicate and if it
     * does, then creates the filter OR(AND(children1), AND(children2). Returns this new filter if
     * the existing filter is an INCLUDE filter and otherwise returns the filter by AND'ing the
     * existing filter and the new filter.
     *
     * @param filter the filter to append to
     * @param children1 the children for the first And component
     * @param children2 the children for the second And component
     * @return the new filter
     */
    private static Filter appendFilter(
            Filter filter, List<Filter> children1, List<Filter> children2) {
        Filter newFilter = Predicates.or(Predicates.and(children1), Predicates.and(children2));
        return Filter.INCLUDE.equals(filter) ? newFilter : Predicates.and(newFilter, filter);
    }

    public String build() {

        String whereClause = buildWhereClause();
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Original filter: " + this.originalFilter);
            LOGGER.finer("Supported filter: " + this.supportedFilter);
            LOGGER.finer("Unsupported filter: " + this.unsupportedFilter);
        }

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
        if (isFullySupported()) {
            dialect.applyOffsetLimit(sql, offset, limit);
            offsetLimitApplied = true;
        } else {
            offsetLimitApplied = false;
        }
    }
}
