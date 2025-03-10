/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.micrometer.tag;

import io.micrometer.core.instrument.Tags;
import org.geoserver.monitor.RequestData;

public enum HostMetricTag implements MetricsTag {
    REMOTE_ADDR("remoteAddr") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteAddr());
        }
    },
    REMOTE_HOST("remoteHost") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteHost());
        }
    },
    REMOTE_USER("remoteUser") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getRemoteUser());
        }
    };

    private static final HostMetricTag[] VALUES = values();
    private final String name;

    HostMetricTag(String name) {
        this.name = name;
    }

    public static Tags computeMicrometerTags(RequestData requestData) {
        return MetricsTag.toMicrometerTags(requestData, VALUES);
    }

    @Override
    public String tagName() {
        return this.name;
    }
}
