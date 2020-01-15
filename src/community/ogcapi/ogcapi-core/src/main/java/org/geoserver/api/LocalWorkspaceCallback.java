/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.api;

import org.geoserver.catalog.impl.LocalWorkspaceCatalog;
import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Request;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Forces usage of local workspace de-qualification of names on {@link
 * org.geoserver.catalog.FeatureTypeInfo} too in {@link LocalWorkspaceCatalog}
 */
@Component
public class LocalWorkspaceCallback extends AbstractDispatcherCallback {

    @Override
    public Request init(Request request) {
        RequestAttributes requestAttributes = RequestContextHolder.getRequestAttributes();
        if (APIRequestInfo.get() != null && requestAttributes != null) {
            requestAttributes.setAttribute(
                    LocalWorkspaceCatalog.DEQUALIFY_ALL, true, RequestAttributes.SCOPE_REQUEST);
        }
        return super.init(request);
    }
}
