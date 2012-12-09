/* Copyright (c) 2012 - TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Collections;

import org.geoserver.platform.ServiceException;
import org.geoserver.wcs.responses.CoverageResponseDelegate;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * Utility class used to encode a grid coverage onto an output stream, used for the Mail MIME
 * encoding
 * 
 * @author Andrea Aime - GeoSolutions
 */
class CoverageEncoder {

    CoverageResponseDelegate delegate;

    GridCoverage2D coverage;

    String outputFormat;

    public CoverageEncoder(CoverageResponseDelegate delegate, GridCoverage2D coverage,
            String outputFormat) {
        this.delegate = delegate;
        this.coverage = coverage;
        this.outputFormat = outputFormat;
    }
    
    public void encode(OutputStream output) throws ServiceException, IOException {
        this.delegate.encode(coverage, outputFormat, Collections.EMPTY_MAP,output);
    }

}
