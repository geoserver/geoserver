/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipOutputStream;

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.gtopo30.GTopo30Writer;
import org.opengis.coverage.grid.GridCoverageWriter;

/**
 * Encoder for gtopo format
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
public class GTopo30CoverageResponseDelegate implements CoverageResponseDelegate {

    private static final List<String> FORMATS = Arrays.asList("application/gtopo30");

    /*
     * (non-Javadoc)
     * 
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#canProduce(java.lang.String)
     */
    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && (outputFormat.equalsIgnoreCase("GTopo30") || FORMATS.contains(outputFormat
                        .toLowerCase()));
    }

    public String getMimeType(String outputFormat) {
        if (canProduce(outputFormat))
            return "application/gtopo30";
        else
            return null;
    }
  
    public String getFileExtension(String outputFormat) {
        return "zip";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#encode(java.io.OutputStream)
     */
	public void encode(GridCoverage2D sourceCoverage, String outputFormat, Map<String,String> econdingParameters, OutputStream output) throws ServiceException, IOException {
        // creating a zip outputstream
        final ZipOutputStream outZ = new ZipOutputStream(output);
        output = outZ;

        // creating a writer
        final GridCoverageWriter writer = new GTopo30Writer(outZ);

        try {
            // writing
            writer.write(sourceCoverage, null);
        } finally {
            try {
                // freeing everything
                writer.dispose();
            } catch (Throwable e) {
                // TODO: handle exception
            }
            sourceCoverage.dispose(false);
        }
    }
	
	@Override
	public List<String> getOutputFormats() {
	    return FORMATS;
	}
	
    @Override
    public boolean isAvailable() {
        return true;
    }

}
