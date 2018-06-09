/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;

/**
 * A data handler for the fake "geoserver/coverageDelegate" mime type. Uses a {@link
 * CoverageResponseDelegate} to determine the actual mime type and to encode the contents
 *
 * @author Andrea Aime - TOPP
 */
public class CoverageDelegateHandler implements DataContentHandler {

    public Object getContent(DataSource source) throws IOException {
        throw new UnsupportedOperationException(
                "This handler is not able to work on the parsing side");
    }

    public Object getTransferData(DataFlavor flavor, DataSource source)
            throws UnsupportedFlavorException, IOException {
        throw new UnsupportedOperationException(
                "This handler is not able to work on the parsing side");
    }

    public DataFlavor[] getTransferDataFlavors() {
        throw new UnsupportedOperationException(
                "This handler is not able to work on the parsing side");
    }

    public void writeTo(Object value, String mimeType, OutputStream os) throws IOException {
        CoverageEncoder encoder = (CoverageEncoder) value;
        encoder.encode(os);
        os.flush();
    }
}
