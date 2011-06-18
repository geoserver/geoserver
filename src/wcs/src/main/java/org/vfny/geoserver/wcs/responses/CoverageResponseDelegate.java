/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * DOCUMENT ME!
 * 
 * @author Alessio Fabiani, GeoSolutions SAS
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public interface CoverageResponseDelegate {
    boolean canProduce(String outputFormat);

    void prepare(String outputFormat, GridCoverage2D coverage) throws IOException;

    String getContentType();

    /**
     * 
     * @uml.property name="contentEncoding" multiplicity="(0 1)"
     */
    String getContentEncoding();

    /**
     * Returns an appropriate file extension for the coverages encoded with this delegate (used
     * mainly when storing the coverage on disk for later retrieval). For example a GeoTiff encoding
     * delegate might return "tif" (no period, just extension).
     * 
     * @return
     */
    String getFileExtension();

    /**
     * 
     * @uml.property name="contentDisposition" multiplicity="(0 1)"
     */
    String getContentDisposition();

    void encode(OutputStream output) throws ServiceException, IOException;

    /**
     * Returns the MIME type matching the specified format, or null if the specified format is not
     * supported
     * 
     * @return
     */
    public String getMimeFormatFor(String format);
}
