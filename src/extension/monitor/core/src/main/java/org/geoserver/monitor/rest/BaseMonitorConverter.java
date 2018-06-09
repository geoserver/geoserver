/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.util.Collections;
import java.util.List;
import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.MediaType;

/** Base class for monitor requests converters, handles visiting results. */
public abstract class BaseMonitorConverter extends BaseMessageConverter<MonitorQueryResults> {

    protected BaseMonitorConverter(MediaType... mediaType) {
        super(mediaType);
    }

    @Override
    protected boolean supports(Class<?> clazz) {
        return MonitorQueryResults.class.isAssignableFrom(clazz);
    }

    @Override
    protected boolean canRead(MediaType mediaType) {
        return false;
    }

    @SuppressWarnings("unchecked")
    static void handleRequests(Object object, RequestDataVisitor visitor, Monitor monitor) {
        if (object instanceof Query) {
            monitor.query((Query) object, visitor);
        } else {
            List<RequestData> requests;
            if (object instanceof List) {
                requests = (List<RequestData>) object;
            } else {
                requests = Collections.singletonList((RequestData) object);
            }
            for (RequestData data : requests) {
                visitor.visit(data, (Object[]) null);
            }
        }
    }
}
