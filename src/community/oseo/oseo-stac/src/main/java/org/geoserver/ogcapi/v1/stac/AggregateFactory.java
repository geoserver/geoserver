/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ogcapi.v1.stac;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;
import org.geotools.api.data.FeatureSource;
import org.geotools.api.data.Query;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.sort.SortOrder;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.visitor.FeatureCalc;
import org.geotools.feature.visitor.MaxVisitor;
import org.geotools.feature.visitor.MinVisitor;
import org.geotools.feature.visitor.UniqueVisitor;
import org.geotools.geometry.jts.ReferencedEnvelope;

/** A factory for creating Aggregate objects. */
public class AggregateFactory {
    private static final String GEOMETRY_FIELD = "footprint";
    protected static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    /** Aggregate types */
    public enum AggregateType {
        MAX("max"),
        MIN("min"),
        DISTINCT("distinct"),
        BOUNDS("bounds");

        public final String label;

        AggregateType(String label) {
            this.label = label;
        }

        public static AggregateType fromString(String string) {
            for (AggregateType pt : values()) {
                if (pt.label.equals(string)) {
                    return pt;
                }
            }
            throw new NoSuchElementException(
                    "AggregateType with string " + string + " has not been found");
        }
    }

    private static Query getCollectionQuery(
            String collectionIdentifier, String sourceProperty, AggregateType aggregate) {
        Filter collectionFilter =
                STACService.getProductInCollectionFilter(
                        Collections.singletonList(collectionIdentifier));
        Query query = new Query();
        query.setFilter(collectionFilter);
        if (AggregateType.MAX.equals(aggregate) || AggregateType.MIN.equals(aggregate)) {
            query.setPropertyNames(sourceProperty);
        } else if (AggregateType.BOUNDS.equals(aggregate)) {
            query.setPropertyNames(GEOMETRY_FIELD);
        } else if (AggregateType.DISTINCT.equals(aggregate)) {
            query.setPropertyNames(sourceProperty);
            query.setSortBy(FF.sort(sourceProperty, SortOrder.ASCENDING));
        }
        return query;
    }

    private static Object wrapReturnValue(
            AggregateType aggregate, String property, Object visitorReturn) {
        if (AggregateType.MIN.equals(aggregate) || AggregateType.MAX.equals(aggregate)) {
            if (visitorReturn != null) {
                return visitorReturn;
            } else {
                return "";
            }
        } else if (AggregateType.BOUNDS.equals(aggregate)) {
            if (visitorReturn != null && visitorReturn instanceof ReferencedEnvelope) {
                ReferencedEnvelope envelope = (ReferencedEnvelope) visitorReturn;
                if ("x".equals(property)) {
                    return new EnvelopeWrapper(envelope).getX();
                } else if ("y".equals(property)) {
                    return new EnvelopeWrapper(envelope).getY();
                } else if ("xmin".equals(property)) {
                    return envelope.getMinX();
                } else if ("xmax".equals(property)) {
                    return envelope.getMaxX();
                } else if ("ymin".equals(property)) {
                    return envelope.getMinY();
                } else if ("ymax".equals(property)) {
                    return envelope.getMaxY();
                } else {
                    return new EnvelopeWrapper(envelope);
                }
            } else {
                return new EnvelopeWrapper(null);
            }
        } else if (AggregateType.DISTINCT.equals(aggregate)) {
            if (visitorReturn != null && visitorReturn instanceof Set) {
                List<Integer> distinct = new ArrayList<>((Set) visitorReturn);
                Collections.sort(distinct);
                return distinct;
            } else {
                return Collections.emptySet();
            }
        }
        return null;
    }

    /** Max aggregate stats */
    public static class MaxAggregate implements AggregateStats {

        @Override
        public Object getStat(
                FeatureSource productSource, String collectionIdentifier, String sourceProperty)
                throws IOException {
            FeatureCalc visitor = new MaxVisitor(sourceProperty);
            Query query =
                    getCollectionQuery(collectionIdentifier, sourceProperty, AggregateType.MAX);
            productSource.getFeatures(query).accepts(visitor, null);
            return wrapReturnValue(
                    AggregateType.MAX, sourceProperty, visitor.getResult().getValue());
        }
    }

    /** Min aggregate stats */
    public static class MinAggregate implements AggregateStats {

        @Override
        public Object getStat(
                FeatureSource productSource, String collectionIdentifier, String sourceProperty)
                throws IOException {
            FeatureCalc visitor = new MinVisitor(sourceProperty);
            Query query =
                    getCollectionQuery(collectionIdentifier, sourceProperty, AggregateType.MIN);
            productSource.getFeatures(query).accepts(visitor, null);
            return wrapReturnValue(
                    AggregateType.MIN, sourceProperty, visitor.getResult().getValue());
        }
    }

    /** Distinct aggregate stats */
    public static class DistinctAggregate implements AggregateStats {

        @Override
        public Object getStat(
                FeatureSource productSource, String collectionIdentifier, String sourceProperty)
                throws IOException {
            FeatureCalc visitor = new UniqueVisitor(sourceProperty);
            Query query =
                    getCollectionQuery(
                            collectionIdentifier, sourceProperty, AggregateType.DISTINCT);
            productSource.getFeatures(query).accepts(visitor, null);
            return wrapReturnValue(
                    AggregateType.DISTINCT, sourceProperty, visitor.getResult().getValue());
        }
    }

    /** Get Bounds Aggregate Statistics for a collection */
    public static class BoundsAggregate implements AggregateStats {

        @Override
        public Object getStat(
                FeatureSource productSource, String collectionIdentifier, String sourceProperty)
                throws IOException {
            Query query =
                    getCollectionQuery(collectionIdentifier, sourceProperty, AggregateType.BOUNDS);
            Object bounds = productSource.getFeatures(query).getBounds();
            return wrapReturnValue(AggregateType.BOUNDS, sourceProperty, bounds);
        }
    }

    /**
     * A wrapper class for a ReferencedEnvelope that exposes the minx, maxx, miny, and maxy values
     */
    static class EnvelopeWrapper {
        private List<Double> x;
        private List<Double> y;

        public EnvelopeWrapper(ReferencedEnvelope envelope) {
            if (envelope != null) {
                // Initialize the x and y fields with minx, maxx, miny, and maxy values.
                x = new ArrayList<>();
                x.add(envelope.getMinX());
                x.add(envelope.getMaxX());

                y = new ArrayList<>();
                y.add(envelope.getMinY());
                y.add(envelope.getMaxY());
            }
        }

        public List<Double> getX() {
            return x;
        }

        public List<Double> getY() {
            return y;
        }
    }

    /** Get the aggregate statistics wrapper */
    public static AggregateStats getAggregateStats(AggregateType aggregateType) {
        switch (aggregateType) {
            case MAX:
                return new MaxAggregate();
            case MIN:
                return new MinAggregate();
            case DISTINCT:
                return new DistinctAggregate();
            case BOUNDS:
                return new BoundsAggregate();
            default:
                // Should not happen.
                throw new IllegalArgumentException("Invalid aggregate type: " + aggregateType);
        }
    }
}
