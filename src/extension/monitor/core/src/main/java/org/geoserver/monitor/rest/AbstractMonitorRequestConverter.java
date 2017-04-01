/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.geoserver.monitor.Monitor;
import org.geoserver.monitor.Query;
import org.geoserver.monitor.RequestData;
import org.geoserver.monitor.RequestDataVisitor;
import org.geoserver.rest.converters.BaseMessageConverter;
import org.springframework.http.HttpInputMessage;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageNotReadableException;

/**
 * Base class for monitor requests converters
 */
public abstract class AbstractMonitorRequestConverter extends BaseMessageConverter {
    
    @Override
    public boolean canRead(Class clazz, MediaType mediaType) {
        return false;
    }

    @Override
    public boolean canWrite(Class clazz, MediaType mediaType) {
        return MonitorQueryResults.class.isAssignableFrom(clazz) && isSupportedMediaType(mediaType);
    }

    @Override
    public Object read(Class clazz, HttpInputMessage inputMessage)
            throws IOException, HttpMessageNotReadableException {
        throw new UnsupportedOperationException();
    }
    
    static void handleRequests(Object object, RequestDataVisitor visitor, Monitor monitor) {
        if (object instanceof Query) {
            monitor.query((Query) object, visitor);
        } else {
            List<RequestData> requests;
            if (object instanceof List) {
                requests = (List) object;
            } else {
                requests = Collections.singletonList((RequestData) object);
            }
            for (RequestData data : requests) {
                visitor.visit(data, null);
            }
        }
    }


}
