/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.spring.controller;

import org.locationtech.geogig.spring.controller.RequestExceptionHandler;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.bind.annotation.ControllerAdvice;

/** Pull in Geogig exception handling, but only apply it to geogig controllers. */
@ControllerAdvice(
    basePackages = {
        "org.locationtech.geogig.spring.controller",
        "org.geogig.geoserver.spring.controller"
    }
)
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GeogigExceptionHandler extends RequestExceptionHandler {}
