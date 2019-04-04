/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.xml.transform.TransformerException;
import net.opengis.wcs11.GetCoverageType;
import org.geoserver.catalog.CoverageInfo;

/**
 * A data handler for the fake "geoserver/coverage" mime type. In fact, it encodes the WCS 1.1
 * coverages document (an xml document)
 *
 * @author Andrea Aime - TOPP
 */
public class CoveragesHandler implements DataContentHandler {

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
        return null;
    }

    public void writeTo(Object value, String mimeType, OutputStream os) throws IOException {
        CoveragesData data = (CoveragesData) value;
        CoveragesTransformer ct = new CoveragesTransformer(data.request);
        try {
            ct.transform(data.info, os);
        } catch (TransformerException e) {
            IOException io = new IOException("Error occurred during wcs:coverage encoding");
            io.initCause(e);
            throw io;
        }
    }

    /**
     * Just a data holder to keep togheter the informations needed to encode the coverages response
     *
     * @author Andrea Aime - TOPP
     */
    static class CoveragesData {
        CoverageInfo info;

        GetCoverageType request;

        public CoveragesData(CoverageInfo info, GetCoverageType request) {
            super();
            this.info = info;
            this.request = request;
        }
    }
}
