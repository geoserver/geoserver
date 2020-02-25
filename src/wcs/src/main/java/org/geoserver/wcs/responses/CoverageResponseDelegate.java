/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * Classes implementing this interface can encode coverages in one or more output formats
 *
 * @author Alessio Fabiani, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public interface CoverageResponseDelegate {

    /** Returns true if the specified output format is supported, false otherwise */
    boolean canProduce(String outputFormat);

    /** Returns the content type for the specified output format */
    String getMimeType(String outputFormat);

    /**
     * Returns an appropriate file extension for the coverages encoded with this delegate (used
     * mainly when storing the coverage on disk for later retrieval). For example a GeoTiff encoding
     * delegate might return "tif" (no period, just extension).
     */
    String getFileExtension(String outputFormat);

    /** Encodes the coverage in the specified output format onto the output stream */
    void encode(
            GridCoverage2D coverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException;

    /** Returns the list of output formats managed by this delegate */
    List<String> getOutputFormats();

    /**
     * True if the encoder is available, false otherwise (possibly due to missing libraries and the
     * like)
     */
    boolean isAvailable();

    /** Returns the GML conformance class for this output format. */
    String getConformanceClass(String format);

    /** Allows the delegate to specify the output format given the object to encode and the */
    default String getFileName(GridCoverage2D value, String coverageId, String format) {
        return coverageId + "." + getFileExtension(format);
    }
}
