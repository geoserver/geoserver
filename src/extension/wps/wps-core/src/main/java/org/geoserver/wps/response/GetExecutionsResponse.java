/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.wps.GetExecutionsTransformer;

/**
 * Builds the GetExecutionsResponse XML document
 *
 * @author Alessio Fabiani - GeoSolutions
 */
public class GetExecutionsResponse extends Response {

    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/xml";

    public GetExecutionsResponse() {
        super(GetExecutionsTransformer.class);
    }

    @Override
    public boolean canHandle(Operation operation) {
        // is this a wps capabilities request?
        return "GetExecutions".equalsIgnoreCase(operation.getId())
                && operation.getService().getId().equals("wps");
    }

    public String getMimeType(Object value, Operation operation) {
        return MIME_TYPE;
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        GetExecutionsTransformer tx = (GetExecutionsTransformer) value;

        try {
            tx.transform(null, output);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }
}
