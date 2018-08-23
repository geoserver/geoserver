/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.io.Serializable;
import java.util.List;

public class OwsAllowedValues implements Serializable {
    private static final long serialVersionUID = 1L;
    private List<String> values;
    private List<OwsRange> ranges;

    public OwsAllowedValues() {}

    public List<String> getValues() {
        return values;
    }

    public void setValues(List<String> values) {
        this.values = values;
    }

    public List<OwsRange> getRanges() {
        return ranges;
    }

    public void setRanges(List<OwsRange> ranges) {
        this.ranges = ranges;
    }
}
