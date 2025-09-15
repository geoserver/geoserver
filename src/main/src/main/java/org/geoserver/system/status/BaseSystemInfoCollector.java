/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.system.status;

import java.io.Serial;
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

    @Serial
    private static final long serialVersionUID = 5031022719592227250L;

    public static String DEFAULT_VALUE = "NOT AVAILABLE";

    Boolean statisticsEnabled = false;

    @Override
    public final Metrics retrieveAllSystemInfo() {
        Metrics metrics = new Metrics();
        for (MetricInfo sip : MetricInfo.values()) {
            metrics.addMetrics(retrieveSystemInfo(sip));
        }
        return metrics;
    }

    /**
     * Retrieve one or more metric values for a {@link MetricInfo} element.
     *
     * @param info the element to retrieve
     * @return a list of {@link MetricValue} for the {@link MetricInfo} element.
     */
    List<MetricValue> retrieveSystemInfo(MetricInfo info) {
        MetricValue mv = new MetricValue(info);
        mv.setAvailable(false);
        mv.setValue(DEFAULT_VALUE);
        return Collections.singletonList(mv);
    }

    @Override
    public void setStatisticsStatus(Boolean statistics) {
        this.statisticsEnabled = statistics;
    }

    @Override
    public Boolean getStatisticsStatus() {
        return Boolean.TRUE.equals(statisticsEnabled);
    }
}
