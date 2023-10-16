/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac.functions;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import java.io.IOException;
import java.util.*;
import org.geoserver.ogcapi.v1.stac.AggregatesCache;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.capability.FunctionName;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionImpl;
import org.geotools.filter.capability.FunctionNameImpl;

/**
 * A function that returns the min, max, bounds, or distinct values of a property in a collection.
 */
public class EoSummaries extends FunctionImpl {
    public static FunctionName NAME =
            new FunctionNameImpl(
                    "eoSummaries",
                    Object.class,
                    parameter("aggregate", String.class),
                    parameter("collectionIdentifier", String.class),
                    parameter("property", String.class));
    static FilterFactory FF = CommonFactoryFinder.getFilterFactory();
    static final String DELEGATE_PARENT_ID_FIELD = "eoParentIdentifier";
    private static final String DELEGATE_GEOMETRY_FIELD = "footprint";
    public static final String MIN = "min";
    public static final String MAX = "max";
    public static final String DISTINCT = "distinct";
    public static final String BOUNDS = "bounds";

    AggregatesCache aggregatesCache;

    /** Creates a new instance of EoSummaries */
    public EoSummaries() {
        this.functionName = NAME;
    }

    @Override
    public Object evaluate(Object feature) {
        String aggregate = getParameters().get(0).evaluate(feature, String.class);
        String collectionIdentifier = getParameters().get(1).evaluate(feature, String.class);
        String property = getParameters().get(2).evaluate(feature, String.class);
        checkArguments(aggregate, collectionIdentifier, property);
        try {
            return getFromCache(aggregate, collectionIdentifier, property);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public Object evaluate(String aggregate, String collectionIdentifier, String property) {
        checkArguments(aggregate, collectionIdentifier, property);
        try {
            return getFromCache(aggregate, collectionIdentifier, property);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    protected Object getFromCache(String aggregate, String collectionIdentifier, String property)
            throws IOException {
        AggregatesCache aggregatesCacheInit = getAggregatesCache();
        AggregatesCache.AggregateCacheKey key = getKey(aggregate, collectionIdentifier, property);
        return aggregatesCacheInit.getWrappedAggregate(key);
    }

    protected AggregatesCache.AggregateCacheKey getKey(
            String aggregate, String collectionIdentifier, String property) {
        return new AggregatesCache.AggregateCacheKey(aggregate, collectionIdentifier, property);
    }

    private void checkArguments(String aggregate, String collectionIdentifier, String property) {
        if (!(MIN.equals(aggregate)
                || MAX.equals(aggregate)
                || DISTINCT.equals(aggregate)
                || BOUNDS.equals(aggregate))) {
            throw new IllegalArgumentException(
                    "Invalid aggregate function, valid values are: "
                            + MIN
                            + ", "
                            + MAX
                            + ", "
                            + DISTINCT
                            + ", "
                            + BOUNDS);
        }
        if (collectionIdentifier == null) {
            throw new IllegalArgumentException("Collection identifier cannot be null");
        }
        if (property == null) {
            throw new IllegalArgumentException("Property cannot be null");
        }
        if (BOUNDS.equals(aggregate)
                && !"x".equals(property)
                && !"y".equals(property)
                && !"xmin".equals(property)
                && !"xmax".equals(property)
                && !"ymin".equals(property)
                && !"ymax".equals(property)) {
            throw new IllegalArgumentException(
                    "Property must be 'x' or 'y' or 'xmin' or 'xmax' or 'ymin' or 'ymax' when aggregate is 'bounds'");
        }
    }

    protected AggregatesCache getAggregatesCache() {
        if (aggregatesCache == null) {
            // the meta tile cache is a singleton, so no need to keep it as a static member
            aggregatesCache = (AggregatesCache) GeoServerExtensions.bean("aggregatesCache");
        }
        return aggregatesCache;
    }
}
