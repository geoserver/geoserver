/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import java.util.HashMap;
import java.util.Map;

import org.geoserver.ows.AbstractDispatcherCallback;
import org.geoserver.ows.Dispatcher;
import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;

/**
 * Adapts plain incoming requests to be resolved to the GWC proxy service.
 * <p>
 * The GeoServer {@link Dispatcher} will call {@link #init(Request)} as the first step before
 * processing the request. This callback will set the {@link Request}'s service, version, and
 * request properties to the "fake" gwc service (service=gwc, version=1.0.0, request=dispatch), so
 * that when the {@link Dispatcher} looks up for the actual service bean to process the request it
 * finds out the {@link GwcServiceProxy} instance that's configured to handle such a service
 * request.
 * <p>
 * See the package documentation for more insights on how these all fit together.
 * 
 */
public class GwcServiceDispatcherCallback extends AbstractDispatcherCallback implements
        DispatcherCallback {

    @Override
    public Request init(Request request) {
        String context = request.getContext();
        if (context == null || !context.startsWith("gwc/service")) {
            return null;
        }

        Map<String, String> kvp = new HashMap<String, String>();
        kvp.put("service", "gwc");
        kvp.put("version", "1.0.0");
        kvp.put("request", "dispatch");
        request.setService("gwc");
        request.setVersion("1.0.0");
        request.setRequest("dispatch");
        request.setKvp(kvp);
        request.setRawKvp(kvp);

        return request;
    }

}
