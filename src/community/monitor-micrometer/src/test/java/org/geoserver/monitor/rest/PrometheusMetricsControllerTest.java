/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.geoserver.rest.RestBaseController;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Before;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class PrometheusMetricsControllerTest extends GeoServerSystemTestSupport {
    private PrometheusMeterRegistry meterRegistry;

    @Before
    public void setup() throws ParseException {
        meterRegistry = applicationContext.getBean(PrometheusMeterRegistry.class);
    }

    @Test
    public void testMetricsEndpoint() throws Exception {

        Timer timerMetric = meterRegistry.timer("timer", "tagA", "A", "tagB", "B", "tagC", "C");
        timerMetric.record(5, TimeUnit.SECONDS);
        timerMetric.record(10, TimeUnit.SECONDS);

        Counter counterMetric = meterRegistry.counter("counter", "tagA", "A", "tagB", "B");
        counterMetric.increment(3.0);

        MockHttpServletResponse response =
                getAsServletResponse(RestBaseController.ROOT_PATH + "/monitor/requests/metrics");
        assertEquals(200, response.getStatus());

        String metricPageContent = response.getContentAsString();
        List<String> metricPageLines = Arrays.asList(metricPageContent.split("\n"));

        assertTrue(metricPageLines.containsAll(List.of(
                "timer_seconds_count{tagA=\"A\",tagB=\"B\",tagC=\"C\"} 2",
                "timer_seconds_sum{tagA=\"A\",tagB=\"B\",tagC=\"C\"} 15.0",
                "timer_seconds_max{tagA=\"A\",tagB=\"B\",tagC=\"C\"} 10.0",
                "counter_total{tagA=\"A\",tagB=\"B\"} 3.0")));
    }
}
