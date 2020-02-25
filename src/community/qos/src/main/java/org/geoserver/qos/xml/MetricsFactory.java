/*
 * (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.qos.xml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.apache.commons.lang.StringUtils;

public class MetricsFactory {

    protected static volatile MetricsFactory INSTANCE;

    public static MetricsFactory getInstance() {
        if (INSTANCE == null) {
            synchronized (MetricsFactory.class) {
                if (INSTANCE == null) INSTANCE = new MetricsFactory();
            }
        }
        return INSTANCE;
    }

    public MetricsFactory() {}

    public List<String> getCodes() {
        String[] codes =
                new String[] {
                    "ResponseTime",
                    "ServicePerformance",
                    "RequestResponsePerformance",
                    "InitialResponsePerformance",
                    "MinimumRequestResponsePerformance",
                    "ContinuousAvailability",
                    "AvailabilityMonthly",
                    "AvailabilityDaily",
                    "RequestCapacity",
                    "RequestCapacityPerSecond"
                };
        return asList(codes);
    }

    public List<String> getUom(String metricCode) {
        if (StringUtils.isEmpty(metricCode)) {
            return asList(new String[] {"s-1", "m-1", "H-1", "%", "s", "ms"});
        }
        // cases of percentage
        if (asList(
                        new String[] {
                            "AvailabilityMonthly", "AvailabilityDaily", "ContinuousAvailability"
                        })
                .stream()
                .anyMatch(x -> metricCode.equals(x))) {
            return asList(new String[] {"%"});
        }
        // ms
        if (asList(new String[] {"MinimumRequestResponsePerformance", "AvailabilityDaily"})
                .stream()
                .anyMatch(x -> metricCode.equals(x))) {
            return asList(new String[] {"%"});
        }
        // cases of ms, s
        if (asList(new String[] {"ResponseTime", "InitialResponsePerformance"})
                .stream()
                .anyMatch(x -> metricCode.equals(x))) {
            return asList(new String[] {"s", "ms"});
        }
        // s-1
        if (asList(new String[] {"RequestCapacityPerSecond"})
                .stream()
                .anyMatch(x -> metricCode.equals(x))) {
            return asList(new String[] {"s-1"});
        }
        // s-1, m-1, H-1
        if (asList(new String[] {"RequestCapacity"}).stream().anyMatch(x -> metricCode.equals(x))) {
            return asList(new String[] {"s-1", "m-1", "H-1"});
        }

        return asList(new String[] {"s-1", "m-1", "H-1", "%", "s", "ms"});
    }

    protected List<String> asList(String[] array) {
        return new ArrayList<>(Arrays.asList(array));
    }
}
