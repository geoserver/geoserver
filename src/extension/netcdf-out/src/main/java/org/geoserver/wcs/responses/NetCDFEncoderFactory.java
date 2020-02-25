/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Map;
import org.geoserver.wcs2_0.response.GranuleStack;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * Builds a NetCDF encoder given the specified parameters. The {@link
 * NetCDFCoverageResponseDelegate} will look for factories registered in the application context
 * that can handle the specified parameters, if none is found, a {@link DefaultNetCDFEncoder} will
 * be used instead
 */
public interface NetCDFEncoderFactory {

    /**
     * Attempts to build an encoder for the given parameters (see {@link
     * CoverageResponseDelegate#encode(GridCoverage2D, String, Map, OutputStream)}
     *
     * @return A {@link NetCDFEncoder}, or null if this factory could not generate one
     */
    NetCDFEncoder getEncoderFor(
            GranuleStack granuleStack,
            File file,
            Map<String, String> encodingParameters,
            String outputFormat)
            throws IOException;

    /** Builds a file name from the object to be encoded, or returns null */
    default String getOutputFileName(GranuleStack granuleStack, String coverageId, String format) {
        return null;
    }
}
