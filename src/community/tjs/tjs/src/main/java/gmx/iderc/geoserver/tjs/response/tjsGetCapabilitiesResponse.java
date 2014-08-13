/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.response;

import net.opengis.tjs10.GetCapabilitiesType;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geotools.xml.transform.TransformerBase;

import javax.xml.transform.TransformerException;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.List;


public class tjsGetCapabilitiesResponse extends Response {
    public tjsGetCapabilitiesResponse() {
        super(TransformerBase.class);
    }

    /**
     * Makes sure this triggers only
     * </p>
     */
    public boolean canHandle(Operation operation) {
        // is this a wfs capabilities request?
        return "GetCapabilities".equalsIgnoreCase(operation.getId()) &&
                       operation.getService().getId().equals("tjs");
    }

    public String getMimeType(Object value, Operation operation) {
        GetCapabilitiesType request = (GetCapabilitiesType) OwsUtils.parameter(operation
                                                                                       .getParameters(), GetCapabilitiesType.class);

        if ((request != null) && (request.getAcceptFormats() != null)) {
            //look for an accepted format
            List formats = request.getAcceptFormats().getOutputFormat();

            for (Iterator f = formats.iterator(); f.hasNext(); ) {
                String format = (String) f.next();

                if (format.endsWith("/xml")) {
                    return format;
                }
            }
        }

        //default
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
