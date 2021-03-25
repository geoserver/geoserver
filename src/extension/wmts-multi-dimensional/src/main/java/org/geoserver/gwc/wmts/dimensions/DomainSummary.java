/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import java.util.Set;

/** Simple containers of statistics about a domain */
class DomainSummary {

    private Comparable min;
    private Comparable max;
    private Set<Comparable> uniqueValues;
    private int count = -1;

    public DomainSummary(Set<Comparable> uniqueValues) {
        this.count = uniqueValues.size();
        this.uniqueValues = uniqueValues;
    }

    public DomainSummary(Comparable min, Comparable max, Number count) {
        this.min = min;
        this.max = max;
        this.count = count == null ? 0 : count.intValue();
    }

    public Comparable getMin() {
        return min;
    }

    public Comparable getMax() {
        return max;
    }

    public int getCount() {
        return count;
    }

    public Set<Comparable> getUniqueValues() {
        return uniqueValues;
    }
}
