/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geowebcache.controller.GeoWebCacheDispatcherController;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Modified top-level dispatcher controller for use by GeoServer. Same as {@link
 * GeoWebCacheDispatcherController}, except the "/service/**" endpoint is excluded. This is handled
 * seperately by the GeoServer Dispatcher.
 */
@Component
@RestController
@RequestMapping(path = "${gwc.context.suffix:}")
public class GeoServerGWCDispatcherController extends GeoWebCacheDispatcherController {

    // Let the GeoServer dispatcher handle "/service/**"
    @Override
    @RequestMapping(path = {"", "/home", "/demo/**", "/proxy/**"})
    public void handleRestApiRequest(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        super.handleRestApiRequest(request, response);
    }
}
