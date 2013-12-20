/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.TreeSet;

import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.util.ReaderDimensionsAccessor;
import org.geoserver.platform.ServiceException;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.FeatureAttributeVisitor;
import org.geotools.util.DateRange;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;

/**
 * This strategy implements the current time selection based on finding the timestamp that's nearest to the current system time.
 * 
 * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
 * 
 */
public class NearestCurrentTimeSelectionStrategy implements CurrentTimeSelectionStrategy {

    /**
     * Default constructor.
     */
    public NearestCurrentTimeSelectionStrategy() {
    }

    @Override
    public Date getCurrentTime(FeatureTypeInfo typeInfo, FeatureCollection<?, ?> dimensionCollection)
            throws IOException {
        // check the time metadata
        DimensionInfo time = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + typeInfo.prefixedName()
                    + " does not have time support enabled");
        }
        if (dimensionCollection == null) {
            throw new ServiceException(
                    "No dimension collection given, cannot select 'current' value for time dimension");
        }

        final NearestVisitor<Date> nearest = new NearestVisitor<Date>(time.getAttribute(),
                new Date());
        dimensionCollection.accepts(nearest, null);
        if (nearest.getNearestMatch() != null) {
            return nearest.getNearestMatch();
        } else {
            return null;
        }
    }

    @Override
    public Date getCurrentTime(CoverageInfo coverage, ReaderDimensionsAccessor dimensions)
            throws IOException {
        Date candidate = null;

        DimensionInfo time = coverage.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        String name = coverage.prefixedName();
        if (time == null || !time.isEnabled()) {
            throw new ServiceException("Layer " + name + " does not have time support enabled");
        }

        TreeSet<Object> timeDomain = dimensions.getTimeDomain();
        Date now = new Date();
        long shortestDistance = Long.MAX_VALUE;
        long currentDistance = 0;
        for (Object dateOrRange : timeDomain) {
            if (dateOrRange instanceof Date) {
                Date d = (Date) dateOrRange;
                if (d.before(now)) {
                    currentDistance = now.getTime() - d.getTime();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d;
                    }
                } else if (d.after(now)) {
                    currentDistance = d.getTime() - now.getTime();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d;
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    // break;
                } else if (d.equals(now)) {
                    candidate = d;
                    break;
                }
            } else if (dateOrRange instanceof DateRange) {
                DateRange d = (DateRange) dateOrRange;
                if (d.getMaxValue().before(now)) {
                    currentDistance = now.getTime() - d.getMaxValue().getTime();
                    if (currentDistance < shortestDistance) {
                        shortestDistance = currentDistance;
                        candidate = d.getMaxValue();
                    }
                } else if (d.getMinValue().after(now)) {
                    currentDistance = d.getMinValue().getTime() - now.getTime();
                    if (currentDistance < shortestDistance) {
                        candidate = d.getMinValue();
                        shortestDistance = currentDistance;
                    }
                    // the distance can only grow after this
                    // assuming the times are in ascending order,
                    // so stop iterating at this point for efficiency:
                    // break;
                } else {
                    // we are within this range, "now" will do:
                    candidate = now;
                    break;
                }
            }
        }
        return candidate;
    }

    /**
     * Feature visitor for finding the property value nearest to the given comparison value. Knows how to operate with Numbers, Dates and Strings.
     * 
     * @author Ilkka Rinne <ilkka.rinne@spatineo.com>
     * 
     * @param <T> type of the matched value
     */
    static class NearestVisitor<T> implements FeatureAttributeVisitor {
        private Expression expr;

        private T nearestValueFound;

        private T matchedValue;

        double shortestDistance = Double.MAX_VALUE;

        public NearestVisitor(String attributeTypeName, T valueToMatch) {
            FilterFactory factory = CommonFactoryFinder.getFilterFactory(null);
            expr = factory.property(attributeTypeName);
            this.matchedValue = valueToMatch;
        }

        @Override
        public List<Expression> getExpressions() {
            return Arrays.asList(expr);
        }

        /**
         * Visitor function, which looks at each feature and finds the value of the attribute given attribute nearest to the given comparison value.
         * 
         * @param feature the feature to be visited
         */
        public void visit(org.opengis.feature.Feature feature) {
            Object attribValue = expr.evaluate(feature);

            if (attribValue == null) {
                return;
            }

            if (attribValue instanceof Number) {
                double doubleVal = ((Number) attribValue).doubleValue();

                if (Double.isNaN(doubleVal) || Double.isInfinite(doubleVal)) {
                    return;
                }
                if (this.matchedValue instanceof Number) {
                    double toMatch = ((Number) this.matchedValue).doubleValue();
                    if (Math.abs(toMatch - doubleVal) < this.shortestDistance) {
                        this.shortestDistance = Math.abs(toMatch - doubleVal);
                        this.nearestValueFound = (T) attribValue;
                    }
                } else {
                    return;
                }
            } else if ((attribValue instanceof Date) && (this.matchedValue instanceof Date)) {
                long time = ((Date) attribValue).getTime();
                if (Math.abs(time - ((Date) this.matchedValue).getTime()) < this.shortestDistance) {
                    this.shortestDistance = Math.abs(time - ((Date) this.matchedValue).getTime());
                    this.nearestValueFound = (T) attribValue;
                }

            } else if ((attribValue instanceof String) && (this.matchedValue instanceof String)) {
                String s = (String) attribValue;
                if (Math.abs(s.compareTo((String) this.matchedValue)) < this.shortestDistance) {
                    this.shortestDistance = Math.abs(s.compareTo((String) this.matchedValue));
                    this.nearestValueFound = (T) attribValue;
                }
            }
        }

        public T getNearestMatch() throws IllegalStateException {
            return this.nearestValueFound;
        }
    }
}
