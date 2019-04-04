/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wps.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

/** @author Lucas Reed, Refractions Research Inc */
public class GetCapabilitiesResponse extends Response {
    public GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }

    @Override
    public boolean canHandle(Operation operation) {
        // is this a wps capabilities request?
        return "GetCapabilities".equalsIgnoreCase(operation.getId())
                && operation.getService().getId().equals("wps");
    }

    public String getMimeType(Object value, Operation operation) {
        return "application/xml";
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        TransformerBase tx = (TransformerBase) value;

        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
