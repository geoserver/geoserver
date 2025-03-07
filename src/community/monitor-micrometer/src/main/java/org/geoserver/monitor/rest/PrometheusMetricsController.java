/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 *
 * Micrometer is licensed under the Apache License, Version 2.0.
 * See https://github.com/micrometer-metrics/micrometer/blob/main/LICENSE for details.
 */

package org.geoserver.monitor.rest;

import io.micrometer.prometheusmetrics.PrometheusMeterRegistry;
import org.geoserver.rest.RestBaseController;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PrometheusMetricsController {
    PrometheusMeterRegistry registry;

    public PrometheusMetricsController(PrometheusMeterRegistry registry) {
        this.registry = registry;
    }

    @GetMapping
    @RequestMapping(
            value = RestBaseController.ROOT_PATH + "/monitor/requests/metrics",
            produces = MediaType.TEXT_PLAIN_VALUE)
    protected String scrape() {
        return registry.scrape();
    }
}
