/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.wmts.dimensions;

import org.geotools.geometry.jts.ReferencedEnvelope;


/**
 * Simple containers of statistics about a domain
 */
class DomainSummary {
    
    private ReferencedEnvelope envelope;
    private Object min;
    private Object max;
    private long count = -1;

    public DomainSummary(ReferencedEnvelope envelope, Object min, Object max) {
        this.envelope = envelope;
        this.min = min;
        this.max = max;
    }

    public DomainSummary(ReferencedEnvelope envelope, Object min, Object max, long count) {
        this.envelope = envelope;
        this.min = min;
        this.max = max;
        this.count = count;
    }

    public ReferencedEnvelope getEnvelope() {
        return envelope;
    }

    public Object getMin() {
        return min;
    }

    public Object getMax() {
        return max;
    }

    public long getCount() {
        return count;
    }
}
