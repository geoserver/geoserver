/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.kvp;

import org.geotools.filter.visitor.SimplifyingFilterVisitor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.PropertyName;

/**
 * Helper class that builds WPS Process Executions Status related filters
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetExecutionsKvpFilterBuilder {

    Filter filter = null;

    FilterFactory ff;

    public GetExecutionsKvpFilterBuilder(FilterFactory ff) {
        this.ff = ff;
    }

    public Filter getFilter() {
        if (filter == null) {
            return Filter.INCLUDE;
        }
        SimplifyingFilterVisitor visitor = new SimplifyingFilterVisitor();
        return (Filter) filter.accept(visitor, null);
    }

    protected void append(String propertyName, String propertyValue) {
        final PropertyName attribute = ff.property(propertyName);
        final PropertyIsEqualTo propertyFilter =
                ff.equal(attribute, ff.literal(propertyValue), true);
        if (filter == null) {
            filter = propertyFilter;
        } else {
            filter = ff.and(filter, propertyFilter);
        }
    }

    public void appendUserNameFilter(String userName) {
        append("userName", userName);
    }

    public void appendProcessNameFilter(String identifier) {
        append("processName", identifier);
    }

    public void appendStatusFilter(String status) {
        append("phase", status);
    }
}
