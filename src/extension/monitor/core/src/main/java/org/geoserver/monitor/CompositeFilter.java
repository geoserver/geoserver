/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CompositeFilter extends Filter {
    List<Filter> filters;

    protected CompositeFilter(List<Filter> filters) {
        this.filters = filters;
    }

    protected CompositeFilter(Filter... filters) {
        this(new ArrayList(Arrays.asList(filters)));
    }

    public List<Filter> getFilters() {
        return filters;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("(");
        String type = getClass().getSimpleName().toUpperCase();
        for (Filter f : filters) {
            sb.append(f).append(" ").append(type).append(" ");
        }
        sb.setLength(sb.length() - 1 - type.length());
        sb.append(")");
        return sb.toString();
    }
}
