/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2020, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geootols.dggs.clickhouse;

import static org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore.GEOMETRY;
import static org.geootols.dggs.clickhouse.ClickHouseDGGSDataStore.ZONE_ID;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;
import org.geotools.data.Query;
import org.geotools.dggs.DGGSFilterTransformer;
import org.geotools.dggs.DGGSFilterVisitor;
import org.geotools.dggs.DGGSInstance;
import org.geotools.dggs.gstore.DGGSResolutionCalculator;
import org.geotools.dggs.gstore.DGGSStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.PostPreProcessFilterSplittingVisitor;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.jdbc.BasicSQLDialect;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Function;
import org.opengis.filter.sort.SortBy;
import org.opengis.filter.sort.SortOrder;

/**
 * Splits a Query into a Query that cna be run against the delegate alphanumeric datastore, and a
 * post-filter that should be run against the resulting DGGS feature collection (containing the
 * spatial bits that could not be turned into zoneId filters).
 */
public class DGGSQuerySplitter {

    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();

    private final SimpleFeatureType schema;
    private DGGSInstance dggs;
    private DGGSResolutionCalculator resolutionCalculator;

    public static class PrePost {
        public Query pre;
        public Filter post;
    }

    public DGGSQuerySplitter(
            DGGSInstance dggs,
            DGGSResolutionCalculator resolutionCalculator,
            SimpleFeatureType schema) {
        this.dggs = dggs;
        this.resolutionCalculator = resolutionCalculator;
        this.schema = schema;
    }

    public PrePost split(Query query) {
        // adapt query to the database storage
        Query adapted = adaptQuery(query);

        // simplify filter
        Filter simplified = SimplifyingFilterVisitor.simplify(adapted.getFilter());

        // split using non spatial capabilities
        PostPreProcessFilterSplittingVisitor splitter =
                new PostPreProcessFilterSplittingVisitor(
                        BasicSQLDialect.BASE_DBMS_CAPABILITIES, schema, null) {
                    @Override
                    protected boolean supports(Object value) {
                        // delegate all functions to the underlying database, it will handle them
                        // its own native way, including eventually running them in memory
                        if (value instanceof Function) return true;
                        return super.supports(value);
                    }
                };
        simplified.accept(splitter, null);

        // pre-filter
        PrePost result = new PrePost();
        result.pre = new Query(adapted);
        result.pre.setFilter(splitter.getFilterPre());

        // the adapted filter can work on the target schema too, but it can be pretty inefficient,
        // in case there is a post filter, just run the original query instead
        result.post = splitter.getFilterPost();
        if (result.post != Filter.INCLUDE) {
            result.post = query.getFilter();
        }

        return result;
    }

    /**
     * Adapts the property names
     *
     * @param query
     * @return
     */
    private Query adaptQuery(Query query) {
        if (query == null) return Query.ALL;

        Query result = new Query(query);

        // remove the geometry property, delegate does not have it, replace it with
        // zoneId if necessary
        if (query.getPropertyNames() != null) {
            Set<String> requestedProperties =
                    new HashSet<>(Arrays.asList(query.getPropertyNames()));
            Stream<String> namesStream =
                    Arrays.stream(query.getPropertyNames()).filter(n -> !GEOMETRY.equals(n));
            if (requestedProperties.contains(GEOMETRY) && !requestedProperties.contains(ZONE_ID)) {
                namesStream = Stream.concat(namesStream, Stream.of(ZONE_ID));
            }
            String[] delegateProperties = namesStream.toArray(n -> new String[n]);

            result.setPropertyNames(delegateProperties);
        }

        // transform sortBy if necessary
        if (query.getSortBy() != null) {
            SortBy[] adaptedSort =
                    Arrays.stream(query.getSortBy())
                            .map(
                                    sb -> {
                                        if (sb == SortBy.NATURAL_ORDER)
                                            return FF.sort(ZONE_ID, SortOrder.ASCENDING);
                                        if (sb == SortBy.REVERSE_ORDER)
                                            return FF.sort(ZONE_ID, SortOrder.DESCENDING);
                                        return sb;
                                    })
                            .toArray(n -> new SortBy[n]);
            result.setSortBy(adaptedSort);
        } else if (query.getStartIndex() != null || query.getMaxFeatures() < Integer.MAX_VALUE) {
            // need a sort to do paging, the underlying store might not have a primary key
            result.setSortBy(new SortBy[] {FF.sort(ZONE_ID, SortOrder.ASCENDING)});
        }

        // reproject query spatial filters?

        Filter filter = query.getFilter();
        DGGSFilterVisitor dggsInjector = new DGGSFilterVisitor(dggs);
        filter = (Filter) filter.accept(dggsInjector, null);

        // extract resolution
        int resolution =
                resolutionCalculator.getTargetResolution(
                        query, DGGSFilterTransformer.RESOLUTION_NOT_SPECIFIED);

        // turn all spatial filters into checks against zoneId, if possible
        Filter adapted =
                DGGSFilterTransformer.adapt(filter, dggs, resolutionCalculator, resolution);
        if (resolution != DGGSFilterTransformer.RESOLUTION_NOT_SPECIFIED) {
            PropertyIsEqualTo resolutionFilter =
                    FF.equals(FF.property(DGGSStore.RESOLUTION), FF.literal(resolution));
            result.setFilter(FF.and(adapted, resolutionFilter));
        } else {
            result.setFilter(adapted);
        }

        return result;
    }
}
