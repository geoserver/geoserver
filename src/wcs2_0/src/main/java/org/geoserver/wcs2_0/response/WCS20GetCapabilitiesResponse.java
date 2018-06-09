/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import javax.xml.transform.TransformerException;
import net.opengis.wcs11.GetCapabilitiesType;
import org.geoserver.ows.Response;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.platform.Operation;
import org.geoserver.wcs2_0.WCS20Const;
import org.geotools.xml.transform.TransformerBase;

/**
 * Runs the transformer and outputs the capabilities
 *
 * @author Emanuele Tajariol (etj) - GeoSolutions
 */
public class WCS20GetCapabilitiesResponse extends Response {
    public WCS20GetCapabilitiesResponse() {
        super(TransformerBase.class);
    }

    /** Makes sure this triggers only */
    @Override
    public boolean canHandle(Operation operation) {

        return "GetCapabilities".equalsIgnoreCase(operation.getId())
                && operation.getService().getId().equals(WCS20Const.SERVICE_NAME)
                && (operation.getService().getVersion().toString().equals(WCS20Const.V201)
                        || operation.getService().getVersion().toString().equals(WCS20Const.V20));
    }

    @Override
    public String getMimeType(Object value, Operation operation) {
        GetCapabilitiesType request =
                (GetCapabilitiesType)
                        OwsUtils.parameter(operation.getParameters(), GetCapabilitiesType.class);

        if ((request != null) && (request.getAcceptFormats() != null)) {
            // look for an accepted format
            List<String> formats = request.getAcceptFormats().getOutputFormat();
            for (String format : formats) {
                if (format.endsWith("/xml")) {
                    return format;
                }
            }
        }

        // default
        return "application/xml";
    }

    @Override
    public void write(Object value, OutputStream output, Operation operation) throws IOException {
        TransformerBase tx = (TransformerBase) value;

        try {
            tx.transform(operation.getParameters()[0], output);
        } catch (TransformerException e) {
            throw (IOException) new IOException().initCause(e);
        }
    }
}
