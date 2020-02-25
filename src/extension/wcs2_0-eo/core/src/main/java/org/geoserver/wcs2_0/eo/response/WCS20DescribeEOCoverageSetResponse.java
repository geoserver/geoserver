/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.response;

import java.io.IOException;
import java.io.OutputStream;
import javax.xml.transform.TransformerException;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

/**
 * Runs the transformer and outputs the describe coverage response.
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20DescribeEOCoverageSetResponse extends Response {
    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/xml";

    public WCS20DescribeEOCoverageSetResponse() {
        super(DescribeEOCoverageSetTransformer.class);
    }

    public String getMimeType(Object value, Operation operation) {
        return MIME_TYPE;
    }

    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        TransformerBase tx = (TransformerBase) value;

        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw new IOException(e);
        }
    }
}
