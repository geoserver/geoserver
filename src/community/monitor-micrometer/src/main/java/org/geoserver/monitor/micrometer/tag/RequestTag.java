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

public enum RequestTag implements MetricsTag {
    SERVICE("service") {
        @Override
        public String compute(RequestData rd) {
            return noneDefault(rd.getService());
        }
    },
    OPERATION("operation") {
        @Override
        public String compute(RequestData rd) {
            return noneDefault(rd.getOperation());
        }
    },
    RESPONSE_STATUS("responseStatus") {
        @Override
        public String compute(RequestData rd) {
            return emptyDefault(String.valueOf(rd.getResponseStatus()));
        }
    },
    STATUS("status") {
        @Override
        public String compute(RequestData rd) {
            return rd.getStatus().toString();
        }
    },
    ERROR_MESSAGE("errorMessage") {
        @Override
        public String compute(RequestData rd) {
            return emptyDefault(rd.getErrorMessage());
        }
    },
    HTTP_METHOD("httpMethod") {
        @Override
        public String compute(RequestData rd) {
            return rd.getHttpMethod();
        }
    },
    OWS_VERSION("owsVersion") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getOwsVersion());
        }
    },
    RESPONSE_CONTENT_TYPE("responseContentType") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getResponseContentType());
        }
    },
    RESOURCES_LIST("resources") {
        public String compute(RequestData rd) {
            return emptyDefault(rd.getResourcesList());
        }
    };

    private static final RequestTag[] VALUES = values();
    private final String name;

    RequestTag(String name) {
        this.name = name;
    }

    public static Tags computeMicrometerTags(RequestData rd) {
        return MetricsTag.toMicrometerTags(rd, VALUES);
    }

    @Override
    public String tagName() {
        return this.name;
    }
}
