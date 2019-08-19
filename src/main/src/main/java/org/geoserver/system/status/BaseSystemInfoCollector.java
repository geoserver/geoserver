/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

import java.io.Serializable;
import java.util.Collections;
import java.util.List;

/**
 * Create an empty system information metrics for all element defined in {@link MetricInfo}
 *
 * <p>As default all elements are initialized as not available
 *
 * @author sandr
 */
public class BaseSystemInfoCollector implements SystemInfoCollector, Serializable {

    private static final long serialVersionUID = 5031022719592227250L;

    public static String DEFAULT_VALUE = "NOT AVAILABLE";

    public final Metrics retrieveAllSystemInfo() {
        Metrics metrics = new Metrics();
        for (MetricInfo sip : MetricInfo.values()) {
            metrics.addMetrics(retrieveSystemInfo(sip));
        }
        return metrics;
    }

    /**
     * Retrieve one or more metric for each element defined in {@link MetricInfo}
     *
     * @param info the element to retrieve
     * @return a list of {@link MetricValue} for each {@link MetricInfo}
     */
    List<MetricValue> retrieveSystemInfo(MetricInfo info) {
        MetricValue mv = new MetricValue(info);
        mv.setAvailable(false);
        mv.setValue(DEFAULT_VALUE);
        return Collections.singletonList(mv);
    }
}
