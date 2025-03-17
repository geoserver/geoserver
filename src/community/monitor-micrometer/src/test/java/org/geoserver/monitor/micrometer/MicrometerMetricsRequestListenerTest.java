/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.micrometer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusConfig;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.http.HttpStatus;

public class MicrometerMetricsRequestListenerTest {

    private static final Random RANDOM = new Random();

    @Rule
    public TemporaryFolder directory = new TemporaryFolder();

    private MonitorConfig config;
    private PrometheusMeterRegistry registry;
    private MicrometerMetricsRequestListener listener;

    @Before
    public void setUp() throws Exception {
        GeoServerResourceLoader loader = new GeoServerResourceLoader(this.directory.getRoot());

        config = new MonitorConfig(loader);
        config.getProperties().put("micrometer.enabled", "true");

        registry = new PrometheusMeterRegistry(PrometheusConfig.DEFAULT);
        listener = new MicrometerMetricsRequestListener(config, registry);

        GenericApplicationContext context = new GenericApplicationContext();
        context.getBeanFactory().registerSingleton("registry", registry);
        context.refresh();
    }

    @After
    public void tearDown() {
        System.clearProperty("micrometer.enabled");
        System.clearProperty("micrometer.metric.remote_host.enabled");
        System.clearProperty("micrometer.metric.reset_count");
        new GeoServerExtensions().setApplicationContext(null);
    }

    @Test
    public void testRequestsTotalTimeMetric() {

        RequestData requestDataA = composeRequestData();
        RequestData requestDataB = composeRequestData();

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        Timer requestsTotalTimer = registry.get("requests.total").timer();

        List<Tag> tags = requestsTotalTimer.getId().getTags();

        List<Tag> requestAExpectedTags = composeExpectedRequestTags(requestDataA);
        assertEquals(requestAExpectedTags, tags);

        double count = requestsTotalTimer.count();
        assertEquals(2, count, 0);

        long requestATotalTime = requestDataA.getTotalTime();
        long requestBTotalTime = requestDataB.getTotalTime();

        double max = requestsTotalTimer.max(TimeUnit.MILLISECONDS);
        assertEquals(Math.max(requestATotalTime, requestBTotalTime), max, 0d);

        double totalTime = requestsTotalTimer.totalTime(TimeUnit.MILLISECONDS);
        assertEquals(requestATotalTime + requestBTotalTime, totalTime, 0d);
    }

    @Test
    public void testRequestsProcessingTimeMetric() {

        RequestData requestDataA = composeRequestData();
        RequestData requestDataB = composeRequestData();

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        Timer requestsProcessingTimer = registry.get("requests.processing").timer();

        List<Tag> tags = requestsProcessingTimer.getId().getTags();

        List<Tag> requestAExpectedTags = composeExpectedRequestTags(requestDataA);
        List<Tag> requestBExpectedTags = composeExpectedRequestTags(requestDataB);
        assertEquals(requestAExpectedTags, requestBExpectedTags);
        assertEquals(requestAExpectedTags, tags);

        double count = requestsProcessingTimer.count();
        assertEquals(2, count, 0);

        Long requestAResourcesProcessingTime = requestDataA.getResourcesProcessingTime().stream()
                .reduce(Long::sum)
                .orElseThrow();
        Long requestBResourcesProcessingTime = requestDataB.getResourcesProcessingTime().stream()
                .reduce(Long::sum)
                .orElseThrow();

        double max = requestsProcessingTimer.max(TimeUnit.MILLISECONDS);
        assertEquals(Math.max(requestAResourcesProcessingTime, requestBResourcesProcessingTime), max, 0d);

        double totalTime = requestsProcessingTimer.totalTime(TimeUnit.MILLISECONDS);
        assertEquals(requestAResourcesProcessingTime + requestBResourcesProcessingTime, totalTime, 0d);
    }

    @Test
    public void testRequestsLabelingTimeMetric() {

        RequestData requestDataA = composeRequestData();
        RequestData requestDataB = composeRequestData();

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        Timer requestsProcessingTimer =
                registry.get("requests.labelling.processing").timer();

        List<Tag> tags = requestsProcessingTimer.getId().getTags();

        List<Tag> requestAExpectedTags = composeExpectedRequestTags(requestDataA);
        List<Tag> requestBExpectedTags = composeExpectedRequestTags(requestDataB);
        assertEquals(requestAExpectedTags, requestBExpectedTags);
        assertEquals(requestAExpectedTags, tags);

        double count = requestsProcessingTimer.count();
        assertEquals(2, count, 0);

        Long requestAResourcesLabelingProcessingTime = requestDataA.getLabellingProcessingTime();
        Long requestBResourcesLabelingProcessingTime = requestDataB.getLabellingProcessingTime();

        double max = requestsProcessingTimer.max(TimeUnit.MILLISECONDS);
        assertEquals(
                Math.max(requestAResourcesLabelingProcessingTime, requestBResourcesLabelingProcessingTime), max, 0d);

        double totalTime = requestsProcessingTimer.totalTime(TimeUnit.MILLISECONDS);
        assertEquals(requestAResourcesLabelingProcessingTime + requestBResourcesLabelingProcessingTime, totalTime, 0d);
    }

    @Test
    public void testRequestsResponseLengthMetric() {

        RequestData requestDataA = composeRequestData();
        RequestData requestDataB = composeRequestData();

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        DistributionSummary requestsResponseLengthSummary =
                registry.get("requests.response.length").summary();

        List<Tag> tags = requestsResponseLengthSummary.getId().getTags();

        List<Tag> requestAExpectedTags = composeExpectedRequestTags(requestDataA);
        List<Tag> requestBExpectedTags = composeExpectedRequestTags(requestDataB);
        assertEquals(requestAExpectedTags, requestBExpectedTags);
        assertEquals(requestAExpectedTags, tags);

        double count = requestsResponseLengthSummary.count();
        assertEquals(2, count, 0);

        long requestAResponseLength = requestDataA.getResponseLength();
        long requestBResponseLength = requestDataB.getResponseLength();

        double max = requestsResponseLengthSummary.max();
        assertEquals(Math.max(requestAResponseLength, requestBResponseLength), max, 0d);

        double totalAmount = requestsResponseLengthSummary.totalAmount();
        assertEquals(requestAResponseLength + requestBResponseLength, totalAmount, 0d);
    }

    @Test
    public void testRequestsHostMetric() {

        config.getProperties().put("micrometer.metric.remote_host.enabled", "true");

        RequestData requestDataA = composeRequestData();
        RequestData requestDataB = composeRequestData();

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        Counter requestHostCounter = registry.get("requests.host").counter();

        List<Tag> tags = requestHostCounter.getId().getTags();

        List<Tag> requestAExpectedTags = composeExpectedHostTags(requestDataA);
        List<Tag> requestBExpectedTags = composeExpectedHostTags(requestDataB);
        assertTrue(tags.containsAll(requestAExpectedTags));
        assertTrue(tags.containsAll(requestBExpectedTags));

        double count = requestHostCounter.count();
        assertEquals(2, count, 0);
    }

    @Test
    public void checkAbsenceOfHostMetricWhenNotEnabled() {
        Meter requestHostCounter = registry.find("requests.host").meter();
        assertNull(requestHostCounter);
    }

    @Test
    public void testMultipleSeries() {

        RequestData requestDataA = composeRequestData();
        requestDataA.setStatus(RequestData.Status.WAITING);

        RequestData requestDataB = composeRequestData();
        requestDataB.setResources(Collections.singletonList("world:map"));

        listener.requestPostProcessed(requestDataA);
        listener.requestPostProcessed(requestDataB);

        Timer requestATimer =
                registry.get("requests.total").tag("status", "WAITING").timer();
        Timer requestBTimer =
                registry.get("requests.total").tag("resources", "world:map").timer();

        List<Tag> requestATimerTags = requestATimer.getId().getTags();
        List<Tag> requestAExpectedTags = composeExpectedRequestTags(requestDataA);
        assertEquals(requestAExpectedTags, requestATimerTags);

        List<Tag> requestBTimerTags = requestBTimer.getId().getTags();
        List<Tag> requestBExpectedTags = composeExpectedRequestTags(requestDataB);
        assertEquals(requestBExpectedTags, requestBTimerTags);

        assertEquals(1, (double) requestATimer.count(), 0);
        assertEquals(1, (double) requestBTimer.count(), 0);

        long requestATotalTime = requestDataA.getTotalTime();
        assertEquals(requestATotalTime, requestATimer.totalTime(TimeUnit.MILLISECONDS), 0d);
        assertEquals(requestATotalTime, requestATimer.max(TimeUnit.MILLISECONDS), 0d);

        long requestBTotalTime = requestDataB.getTotalTime();
        assertEquals(requestBTotalTime, requestBTimer.totalTime(TimeUnit.MILLISECONDS), 0d);
        assertEquals(requestBTotalTime, requestBTimer.max(TimeUnit.MILLISECONDS), 0d);
    }

    @Test
    public void testResetMetrics() {

        config.getProperties().put("micrometer.metric.reset_count", "3");

        RequestData requestData = composeRequestData();

        listener.requestPostProcessed(requestData);
        listener.requestPostProcessed(requestData);
        listener.requestPostProcessed(requestData);

        Timer requestsTotalTimer = registry.get("requests.total").timer();
        assertEquals(3, (double) requestsTotalTimer.count(), 0);

        listener.requestPostProcessed(requestData);

        Timer requestsTotalTimerAfterReset = registry.get("requests.total").timer();
        assertEquals(1, (double) requestsTotalTimerAfterReset.count(), 0);
    }

    private RequestData composeRequestData() {
        RequestData data = new RequestData();

        data.setService("service");
        data.setOperation("operation");
        data.setStatus(RequestData.Status.FINISHED);
        data.setErrorMessage("error message");
        data.setHttpMethod("PATCH");
        data.setOwsVersion("0.0.1");
        data.setResources(List.of("resA", "resB", "resC"));

        data.setTotalTime(randomLong(5000));
        data.setResourcesProcessingTime(List.of(randomLong(1000), randomLong(2000), randomLong(3000)));
        data.setLabellingProcessingTime(randomLong(4000));

        data.setResponseStatus(HttpStatus.I_AM_A_TEAPOT.value());
        data.setResponseContentType("application/teapot");
        data.setResponseLength(randomLong(1_000_000));

        data.setRemoteAddr("192.168.1.1");
        data.setRemoteHost("REMOTE_HOST");
        data.setRemoteUser("USER");

        return data;
    }

    private long randomLong(long bound) {
        long nextLong = RANDOM.nextLong();
        return nextLong == Long.MIN_VALUE ? 0 : Math.abs(nextLong) % bound;
    }

    private List<Tag> composeExpectedRequestTags(RequestData requestData) {
        return Tags.of(
                        "service",
                        requestData.getService(),
                        "operation",
                        requestData.getOperation(),
                        "responseStatus",
                        String.valueOf(requestData.getResponseStatus()),
                        "status",
                        requestData.getStatus().toString(),
                        "errorMessage",
                        requestData.getErrorMessage(),
                        "httpMethod",
                        requestData.getHttpMethod(),
                        "owsVersion",
                        requestData.getOwsVersion(),
                        "responseContentType",
                        requestData.getResponseContentType(),
                        "resources",
                        requestData.getResourcesList())
                .stream()
                .collect(Collectors.toList());
    }

    private List<Tag> composeExpectedHostTags(RequestData requestData) {

        List<Tag> requestTags = composeExpectedRequestTags(requestData);
        List<Tag> hostTags = Tags.of(
                        "remoteAddr",
                        requestData.getRemoteAddr(),
                        "remoteHost",
                        requestData.getRemoteHost(),
                        "remoteUser",
                        requestData.getRemoteUser())
                .stream()
                .collect(Collectors.toList());

        requestTags.addAll(hostTags);

        return requestTags;
    }
}
