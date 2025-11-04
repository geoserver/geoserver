/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import jakarta.activation.ActivationDataFlavor;
import jakarta.activation.DataContentHandler;
import jakarta.activation.DataSource;
import java.io.IOException;
import java.io.OutputStream;

/**
 * A data handler for the fake "geoserver/coverageDelegate" mime type. Uses a {@link CoverageResponseDelegate} to
 * determine the actual mime type and to encode the contents
 *
 * @author Andrea Aime - TOPP
 */
public class CoverageDelegateHandler implements DataContentHandler {

    @Override
    public Object getContent(DataSource source) throws IOException {
        throw new UnsupportedOperationException("This handler is not able to work on the parsing side");
    }

    @Override
    public ActivationDataFlavor[] getTransferDataFlavors() {
        throw new UnsupportedOperationException("This handler is not able to work on the parsing side");
    }

    @Override
    public Object getTransferData(ActivationDataFlavor activationDataFlavor, DataSource dataSource) throws IOException {
        throw new UnsupportedOperationException("This handler is not able to work on the parsing side");
    }

    @Override
    public void writeTo(Object value, String mimeType, OutputStream os) throws IOException {
        CoverageEncoder encoder = (CoverageEncoder) value;
        encoder.encode(os);
        os.flush();
    }
}
