/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * Utility class used to encode a grid coverage onto an output stream, used for the Mail MIME
 * encoding
 *
 * @author Andrea Aime - GeoSolutions
 */
public class CoverageEncoder {

    CoverageResponseDelegate delegate;

    GridCoverage2D coverage;

    String outputFormat;

    Map<String, String> encodingParameters;

    public CoverageEncoder(
            CoverageResponseDelegate delegate,
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> encodingParameters) {
        this.delegate = delegate;
        this.coverage = coverage;
        this.outputFormat = outputFormat;
        this.encodingParameters = encodingParameters;
    }

    public void encode(OutputStream output) throws ServiceException, IOException {
        this.delegate.encode(coverage, outputFormat, encodingParameters, output);
    }
}
