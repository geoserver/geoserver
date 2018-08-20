/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.util.Set;

/** Simple containers of statistics about a domain */
class DomainSummary {

    private Object min;
    private Object max;
    private Set<Object> uniqueValues;
    private int count = -1;

    public DomainSummary(Set<Object> uniqueValues) {
        this.count = uniqueValues.size();
        this.uniqueValues = uniqueValues;
    }

    public DomainSummary(Object min, Object max, Number count) {
        this.min = min;
        this.max = max;
        this.count = count == null ? 0 : count.intValue();
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }

    public int getCount() {
        return count;
    }

    public Set<Object> getUniqueValues() {
        return uniqueValues;
    }
}
