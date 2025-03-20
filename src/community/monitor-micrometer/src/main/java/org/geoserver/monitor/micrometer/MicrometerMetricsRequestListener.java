/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Meter;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.monitor.MonitorConfig;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataListener;
import org.geoserver.monitor.micrometer.tag.HostMetricTag;
import org.geoserver.monitor.micrometer.tag.RequestTag;
import org.geotools.util.logging.Logging;

public class MicrometerMetricsRequestListener implements RequestDataListener {

    private static final Logger LOGGER = Logging.getLogger(MicrometerMetricsRequestListener.class);
    private static final String MICROMETER = "micrometer";

    MonitorConfig config;
    PrometheusMeterRegistry registry;

    private final AtomicInteger requestsCounter = new AtomicInteger(0);

    /** Timer to record the total time of a request from {@link RequestData#getTotalTime()}. */
    private final Meter.MeterProvider<Timer> requestTimer;

    /** Meter to record the response length of a request from {@link RequestData#getResponseLength()}. */
    private final Meter.MeterProvider<DistributionSummary> responseLengthSummary;

    /**
     * Timer to record the processing time of a request summing the values from
     * {@link RequestData#getResourcesProcessingTime()}.
     */
    private final Meter.MeterProvider<Timer> requestProcessingTimer;

    /**
     * Timer to record the labeling time of a request from {@link RequestData#getLabellingProcessingTime()}.
     *
     * <p>Named "labelling" to maintain consistency with {@link RequestData#labellingProcessingTime} attribute.
     */
    private final Meter.MeterProvider<Timer> requestLabellingProcessingTimer;

    /**
     * Meter to record the remote address of the host that makes the request from {@link RequestData#getRemoteAddr()}.
     */
    private final Meter.MeterProvider<Counter> hostCounter;

    public MicrometerMetricsRequestListener(MonitorConfig config, PrometheusMeterRegistry registry) {
        this.config = config;
        this.registry = registry;

        requestTimer = Timer.builder("requests.total").withRegistry(registry);
        requestProcessingTimer = Timer.builder("requests.processing").withRegistry(registry);
        requestLabellingProcessingTimer =
                Timer.builder("requests.labelling.processing").withRegistry(registry);
        responseLengthSummary = DistributionSummary.builder("requests.response.length")
                .baseUnit("bytes")
                .withRegistry(registry);
        hostCounter = Counter.builder("requests.host").withRegistry(registry);
    }

    @Override
    public void requestPostProcessed(RequestData requestData) {
        try {
            if (!isMetricRequestEnabled()) {
                return;
            }

            if (requestData == null) {
                return;
            }

            resetMetricIfNeeded();

            Tags tags = composeTags(requestData);

            requestTimer.withTags(tags).record(requestData.getTotalTime(), TimeUnit.MILLISECONDS);
            responseLengthSummary.withTags(tags).record(requestData.getResponseLength());

            List<Long> resourcesProcessingTime = requestData.getResourcesProcessingTime();
            if (resourcesProcessingTime != null) {
                requestProcessingTimer
                        .withTags(tags)
                        .record(resourcesProcessingTime.stream().reduce(0L, Long::sum), TimeUnit.MILLISECONDS);
            }

            Long labellingProcessingTime = requestData.getLabellingProcessingTime();
            if (labellingProcessingTime != null) {
                requestLabellingProcessingTimer.withTags(tags).record(labellingProcessingTime, TimeUnit.MILLISECONDS);
            }

            if (isHostTrackingEnabled()) {
                Tags hostMetricTags = tags.and(HostMetricTag.computeMicrometerTags(requestData));
                hostCounter.withTags(hostMetricTags).increment();
            }

        } catch (Exception e) {
            throw new RuntimeException(
                    "Unexpected error occurred while trying to record the request into a Micrometer metric", e);
        }
    }

    private void resetMetricIfNeeded() {
        Integer resetCount = getProperty("metric.reset_count", Integer.class, 100);
        if (requestsCounter.incrementAndGet() > resetCount) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(Level.FINE, "reset count reached ({0}). Resetting metrics", resetCount);
            }
            requestsCounter.set(0);
            registry.forEachMeter(registry::remove);
        }
    }

    private boolean isMetricRequestEnabled() {
        return getProperty("enabled", Boolean.class, false);
    }

    private boolean isHostTrackingEnabled() {
        return getProperty("metric.remote_host.enabled", Boolean.class, false);
    }

    <T> T getProperty(String name, Class<T> target, T defaultValue) {
        T value = config.getProperty(MICROMETER, name, target);
        if (value == null) {
            return defaultValue;
        } else {
            return value;
        }
    }

    private Tags composeTags(RequestData rd) {
        return RequestTag.computeMicrometerTags(rd);
    }

    @Override
    public void requestStarted(RequestData rd) {
        /* no-op */
    }

    @Override
    public void requestUpdated(RequestData rd) {
        /* no-op */
    }

    @Override
    public void requestCompleted(RequestData rd) {
        /* no-op */
    }
}
