/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
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
 * Runs the transformer and outputs the describe coverage response
 * @author Andrea Aime, TOPP
 * TODO: This is a blatant copy of WFS GetCapabilities response. Find a way to share code.
 */
public class DescribeCoverageResponse extends Response {
    public DescribeCoverageResponse() {
        super(DescribeCoverageTransformer.class);
    }

    public String getMimeType(Object value, Operation operation) {
        return "application/xml";
    }

    public void write(Object value, OutputStream output, Operation operation)
        throws IOException {
        TransformerBase tx = (TransformerBase) value;

        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
