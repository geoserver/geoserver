/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows.adapters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;
import org.vfny.geoserver.Request;
import org.vfny.geoserver.Response;

/**
 * Wraps an old style {@link Response} in a new {@link org.geoserver.ows.Response}.
 *
 * <p>The class binding (see {@link #getBinding()} ), is the implementation of {@link Response}
 * which will delegated to.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 */
public class ResponseAdapter extends org.geoserver.ows.Response {
    GeoServer gs;

    public ResponseAdapter(Class delegateClass, GeoServer gs) {
        super(delegateClass);

        this.gs = gs;
    }

    public String getMimeType(Object value, Operation operation) throws ServiceException {
        // get the delegate
        Response delegate = (Response) value;

        // get the requst object from the operation
        Request request = (Request) OwsUtils.parameter(operation.getParameters(), Request.class);

        // the old contract specifies that execute must be called before
        // get content type
        delegate.execute(request);

        // return the content type
        return delegate.getContentType(gs);
    }

    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {
        // get the delegate
        Response delegate = (Response) value;

        // write the response
        delegate.writeTo(output);
    }

    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        Response delegate = (Response) value;
        HashMap map = new HashMap();
        if (delegate.getContentDisposition() != null) {
            map.put("Content-Disposition", delegate.getContentDisposition());
        }

        HashMap m = delegate.getResponseHeaders();
        if (m != null && !m.isEmpty()) {
            map.putAll(m);
        }

        if (map == null || map.isEmpty()) return null;

        String[][] headers = new String[map.size()][2];
        List keys = new ArrayList(map.keySet());
        for (int i = 0; i < headers.length; i++) {
            headers[i][0] = (String) keys.get(i);
            headers[i][1] = (String) map.get(keys.get(i));
        }
        return headers;
    }

    /**
     * Backwards compatibility for adapter - dispatcher will ignore.
     *
     * @param value
     * @param operation
     * @return null
     */
    @Override
    public String getAttachmentFileName(Object value, Operation operation) {
        return null;
    }
}
