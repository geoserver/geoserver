/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.micrometer.tag;

import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Collectors;
import org.geoserver.monitor.RequestData;

interface MetricsTag {

    String DEFAULT_VALUE = "None";

    default String noneDefault(String value) {
        return Objects.requireNonNullElse(value, DEFAULT_VALUE);
    }

    default String emptyDefault(String value) {
        return Objects.requireNonNullElse(value, "");
    }

    static Tags toMicrometerTags(RequestData requestData, MetricsTag... metricsTags) {
        return Tags.of(Arrays.stream(metricsTags)
                .map(t -> Tag.of(t.tagName(), t.compute(requestData)))
                .collect(Collectors.toList()));
    }

    String tagName();

    String compute(RequestData requestData);
}
