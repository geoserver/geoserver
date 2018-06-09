/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;
import javax.xml.transform.TransformerException;
import net.opengis.wcs11.GetCapabilitiesType;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

/**
 * Runs the transformer and outputs the capabilities
 *
 * @author Andrea Aime, TOPP TODO: This is a blatant copy of WFS GetCapabilities response. Find a
 *     way to share code.
 */
public class GetCapabilitiesResponse extends Response {
    public GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }

    /** Makes sure this triggers only */
    public boolean canHandle(Operation operation) {
        // is this a wcs 1.1.1 or 1.1.0 one?
        return "GetCapabilities".equalsIgnoreCase(operation.getId())
                && operation.getService().getId().equals("wcs")
                && (operation.getService().getVersion().toString().equals("1.1.0")
                        || operation.getService().getVersion().toString().equals("1.1.1"));
    }

    public String getMimeType(Object value, Operation operation) {
        GetCapabilitiesType request =
                OwsUtils.parameter(operation.getParameters(), GetCapabilitiesType.class);

        if ((request != null) && (request.getAcceptFormats() != null)) {
            // look for an accepted format
            List formats = request.getAcceptFormats().getOutputFormat();

            for (Iterator f = formats.iterator(); f.hasNext(); ) {
                String format = (String) f.next();

                if (format.endsWith("/xml")) {
                    return format;
                }
            }
        }

        // default
        return "text/xml";
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
