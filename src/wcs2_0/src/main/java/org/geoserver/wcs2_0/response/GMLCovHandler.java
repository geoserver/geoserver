/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.io.OutputStream;
import javax.activation.DataContentHandler;
import javax.activation.DataSource;
import javax.xml.transform.TransformerException;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.vfny.geoserver.wcs.WcsException;

/**
 * A data handler for the fake "geoserver/coverage20" mime type. In fact, it encodes WCS 2.0 GMLCov
 * document (an xml document)
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GMLCovHandler implements DataContentHandler {

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
        CoverageData data = (CoverageData) value;

        final GMLTransformer transformer = new GMLTransformer(data.envelopeDimensionsMapper);
        transformer.setIndentation(4);
        transformer.setFileReference(data.fileReference);
        try {
            transformer.transform(data.coverage, os);
        } catch (TransformerException e) {
            throw new WcsException(e);
        }
    }

    /**
     * Just a data holder to keep togheter the informations needed to encode the GMLCOV response
     *
     * @author Andrea Aime - GeoSolutions
     */
    static class CoverageData {
        GridCoverage2D coverage;

        FileReference fileReference;

        EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

        public CoverageData(
                GridCoverage2D coverage,
                FileReference fileReference,
                EnvelopeAxesLabelsMapper envelopeDimensionsMapper) {
            this.coverage = coverage;
            this.fileReference = fileReference;
            this.envelopeDimensionsMapper = envelopeDimensionsMapper;
        }
    }
}
