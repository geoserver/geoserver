/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

/**
 * Runs the transformer and outputs the capabilities
 *
 * @author Andrea Aime, TOPP TODO: This is a blatant copy of WFS GetCapabilities response. Find a
 *     way to share code.
 */
public class Wcs10GetCapabilitiesResponse extends Response {
    public Wcs10GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }

    /** Makes sure this triggers only */
    public boolean canHandle(Operation operation) {
        return "GetCapabilities".equalsIgnoreCase(operation.getId())
                && operation.getService().getId().equals("wcs")
                && operation.getService().getVersion().toString().equals("1.0.0");
    }

    public String getMimeType(Object value, Operation operation) {
        // default
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
