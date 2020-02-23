/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.dimension;

import java.util.ArrayList;
import java.util.List;
import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.geotools.util.Range;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;

/**
 * Helper class that builds dimension related filters against reference objects that can be point
 * ones (date, number, string) or range types (DateRange, NumberRange)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class DimensionFilterBuilder {

    Filter filter = null;

    FilterFactory ff;

    public DimensionFilterBuilder(FilterFactory ff) {
        this.ff = ff;
    }

    public void appendFilters(
            String startAttributeName, String endAttributeName, List<Object> ranges) {
        if (ranges == null || ranges.size() == 0) {
            return;
        }

        final List<Filter> timeFilters = new ArrayList<Filter>();
        final PropertyName attribute = ff.property(startAttributeName);
        final PropertyName endAttribute =
                endAttributeName == null ? null : ff.property(endAttributeName);

        for (Object datetime : ranges) {
            timeFilters.add(buildDimensionFilter(datetime, attribute, endAttribute));
        }
        final int size = timeFilters.size();
        Filter result;
        if (size > 1) {
            result = ff.or(timeFilters);
        } else {
            result = timeFilters.get(0);
        }

        if (filter == null) {
            filter = result;
        } else {
            filter = ff.and(filter, result);
        }
    }

    /**
     * Build a filter for a single value based on an attribute and optional endAttribute. The value
     * is either a Range or object that can be used as a literal (Date,Number).
     */
    Filter buildDimensionFilter(Object value, PropertyName attribute, PropertyName endAttribute) {
        Filter filter;
        if (value == null) {
            filter = Filter.INCLUDE;
        } else if (value instanceof Range) {
            Range range = (Range) value;
            if (endAttribute == null) {
                filter =
                        ff.between(
                                attribute,
                                ff.literal(range.getMinValue()),
                                ff.literal(range.getMaxValue()));
            } else {
                // Range intersects valid range of feature
                // @todo adding another option to dimensionInfo allows contains, versus intersects
                Literal qlower = ff.literal(range.getMinValue());
                Literal qupper = ff.literal(range.getMaxValue());
                Filter lower = ff.lessOrEqual(attribute, qupper);
                Filter upper = ff.greaterOrEqual(endAttribute, qlower);
                return ff.and(lower, upper);
            }
        } else {
            // Single element is equal to
            if (endAttribute == null) {
                filter = ff.equal(attribute, ff.literal(value), true);
            } else {
                // Single element is contained by valid range of feature
                Filter lower = ff.greaterOrEqual(ff.literal(value), attribute);
                Filter upper = ff.lessOrEqual(ff.literal(value), endAttribute);
                filter = ff.and(lower, upper);
            }
        }
        return filter;
    }

    public Filter getFilter() {
        if (filter == null) {
            return Filter.INCLUDE;
        }
        SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
        return (Filter) filter.accept(visitor, null);
    }
}
