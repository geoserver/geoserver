/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.rest.catalog;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.geoserver.config.util.patch.PatchContext;
import org.geoserver.rest.DispatcherCallbackAdapter;
import org.springframework.stereotype.Component;

/**
 * REST Dispatcher callback to manage the patch context for REST requests. The patch context is started for PUT
 * requests, and stopped at the end of the request. this allows to track the properties that have been explicitly set to
 * null in the XML/JSON payload, as opposed to properties that are missing from the payload and should be ignored. For a
 * complete picture of the problem see the package-info of org.geoserver.config.util.patch, in the main module.
 */
@Component
public class PatchRequestCallback extends DispatcherCallbackAdapter {

    @Override
    public void init(HttpServletRequest request, HttpServletResponse response) {
        if ("PUT".equals(request.getMethod())) PatchContext.start();
    }

    @Override
    public void finished(HttpServletRequest request, HttpServletResponse response) {
        if (PatchContext.isActive()) PatchContext.stop();
    }
}
