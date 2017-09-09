/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.status.monitoring.collector;

import java.util.ArrayList;
import java.util.List;

/**
 * 
 * Container for {@link MetricValue} array
 * 
 * @author sandr
 *
 */
public class Metrics {

    private List<MetricValue> metrics = new ArrayList<>();

    public List<MetricValue> getMetrics() {
        return metrics;
    }

    public void setMetrics(List<MetricValue> metrics) {
        this.metrics = metrics;
    }

}
