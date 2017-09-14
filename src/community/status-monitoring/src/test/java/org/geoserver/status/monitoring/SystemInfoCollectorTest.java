/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.status.monitoring;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertEquals;

import java.util.List;
import java.util.Map;

import org.geoserver.status.monitoring.collector.BaseSystemInfoCollector;
import org.geoserver.status.monitoring.collector.MetricValue;
import org.geoserver.status.monitoring.collector.Metrics;
import org.geoserver.status.monitoring.collector.SystemInfoCollector;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ErrorCollector;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class SystemInfoCollectorTest {

    @Rule
    public ErrorCollector collector = new ErrorCollector();

    private static ClassPathXmlApplicationContext context;

    @BeforeClass
    public static void beforeClass() {
        context = new ClassPathXmlApplicationContext("applicationContext.xml");
    }

    @AfterClass
    public static void afterClass() {
        context.close();
    }

    @Test
    public void testMetricCollector() throws Exception {
        Map<String, SystemInfoCollector> collectors = context
                .getBeansOfType(SystemInfoCollector.class);
        assertEquals(1, collectors.size());
        SystemInfoCollector systemInfoCollector = collectors.values().iterator().next();
        // SystemInfoCollector systemInfoCollector = context.getBean(SystemInfoCollector.class);
        Metrics collected = systemInfoCollector.retrieveAllSystemInfo();
        List<MetricValue> metrics = collected.getMetrics();
        for (MetricValue m : metrics) {
            if (m.getAvailable()) {
                System.out.println(
                        m.getName() + " IS available -> " + m.getValue() + " " + m.getUnit());
            } else {
                System.err.println(m.getName() + " IS NOT available");
            }
            collector.checkThat(
                    "Metric for " + m.getName() + " available but value is not retrived",
                    (m.getAvailable() && !m.getValue().equals(BaseSystemInfoCollector.DEFAULT_VALUE)
                            || (!m.getAvailable()
                                    && m.getValue().equals(BaseSystemInfoCollector.DEFAULT_VALUE))),
                    equalTo(true));
        }
    }

}
