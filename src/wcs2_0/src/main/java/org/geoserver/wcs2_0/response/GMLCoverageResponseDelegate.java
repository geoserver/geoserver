/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.response;

import static java.util.Map.entry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import javax.xml.transform.TransformerException;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.BaseCoverageResponseDelegate;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geoserver.wcs2_0.util.EnvelopeAxesLabelsMapper;
import org.geotools.coverage.grid.GridCoverage2D;
import org.vfny.geoserver.wcs.WcsException;

/**
 * Encoding a {@link GridCoverage2D} as per WCS 2.0 GML format.
 *
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GMLCoverageResponseDelegate extends BaseCoverageResponseDelegate implements CoverageResponseDelegate {

    /** FILE_EXTENSION */
    private static final String FILE_EXTENSION = "gml";

    /** MIME_TYPE */
    private static final String MIME_TYPE = "application/gml+xml";

    /** Can be used to map dimensions name to indexes */
    private EnvelopeAxesLabelsMapper envelopeDimensionsMapper;

    public GMLCoverageResponseDelegate(EnvelopeAxesLabelsMapper envelopeDimensionsMapper, GeoServer geoserver) {
        super(
                geoserver,
                List.of(FILE_EXTENSION, MIME_TYPE), // output formats
                Map.ofEntries( // file extensions
                        entry(MIME_TYPE, FILE_EXTENSION), //
                        entry(FILE_EXTENSION, FILE_EXTENSION)),
                Map.ofEntries( // mime types
                        entry(MIME_TYPE, MIME_TYPE), //
                        entry(FILE_EXTENSION, MIME_TYPE)));

        this.envelopeDimensionsMapper = envelopeDimensionsMapper;
    }

    @Override
    public void encode(
            GridCoverage2D coverage, String outputFormat, Map<String, String> econdingParameters, OutputStream output)
            throws ServiceException, IOException {
        final GMLTransformer transformer = new GMLTransformer(envelopeDimensionsMapper);
        transformer.setIndentation(4);
        try {
            transformer.transform(coverage, output);
        } catch (TransformerException e) {
            throw new WcsException(e);
        }
    }
}
