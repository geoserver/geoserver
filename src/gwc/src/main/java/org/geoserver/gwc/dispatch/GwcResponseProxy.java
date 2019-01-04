/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.dispatch;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.ServiceException;

/**
 * The configured GeoServer Dispatcher {@link Response} object that writes down the contents of the
 * {@link GwcOperationProxy} response produced by a {@link GwcServiceProxy}.
 *
 * <p>See the package documentation for more insights on how these all fit together.
 */
public class GwcResponseProxy extends Response {

    public GwcResponseProxy() {
        super(GwcOperationProxy.class);
    }

    @Override
    public String getMimeType(Object value, Operation operation) throws ServiceException {
        GwcOperationProxy op = (GwcOperationProxy) value;
        String mimeType = op.getMimeType();
        return mimeType;
    }

    @Override
    public String[][] getHeaders(Object value, Operation operation) throws ServiceException {
        GwcOperationProxy op = (GwcOperationProxy) value;
        Map<String, String> responseHeaders = op.getResponseHeaders();
        if (responseHeaders == null || responseHeaders.size() == 0) {
            return null;
        }
        String[][] headers = new String[responseHeaders.size()][2];
        int index = 0;
        for (java.util.Map.Entry<String, String> entry : responseHeaders.entrySet()) {
            headers[index][0] = entry.getKey();
            headers[index][1] = entry.getValue();
            index++;
        }
        return headers;
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation)
            throws IOException, ServiceException {

        GwcOperationProxy op = (GwcOperationProxy) value;
        byte[] contents = op.getContents();
        output.write(contents);
    }

    @Override
    public String getPreferredDisposition(Object value, Operation operation) {
        // do not override the content disposition set by GWC
        return null;
    }
}
