/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.vfny.geoserver.wcs.responses.coverage;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.zip.ZipOutputStream;

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.gtopo30.GTopo30Writer;
import org.opengis.coverage.grid.GridCoverageWriter;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;

/**
 * Encoder for gtopo format
 * @author Simone Giannecchini, GeoSolutions SAS
 *
 */
@SuppressWarnings("deprecation")
public class GTopo30CoverageResponseDelegate implements CoverageResponseDelegate {

    private static final Set<String> FORMATS = new HashSet<String>(Arrays
            .asList("application/gtopo30"));

    /**
     * the grid coverage to be used in this repsonse
     * 
     * @uml.property name="sourceCoverage"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private GridCoverage2D sourceCoverage;

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

    public String getMimeFormatFor(String outputFormat) {
        if (canProduce(outputFormat))
            return "application/gtopo30";
        else
            return null;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#prepare(java.lang.String,
     * org.geotools.coverage.grid.GridCoverage2D)
     */
    public void prepare(String outputFormat, GridCoverage2D coverage) throws IOException {
        this.sourceCoverage = coverage;
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#getContentType(org.vfny.geoserver
     * .global.GeoServer)
     */
    public String getContentType() {
        return "application/x-zip";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#getContentEncoding()
     */
    public String getContentEncoding() {
        // return "zip";
        return null;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getContentDisposition() {
        return new StringBuffer("attachment;filename=").append(this.sourceCoverage.getName())
                .append(".zip").toString();
    }

    public String getFileExtension() {
        return "zip";
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.vfny.geoserver.wcs.responses.CoverageResponseDelegate#encode(java.io.OutputStream)
     */
	public void encode(OutputStream output) throws ServiceException, IOException {
        // creating a zip outputstream
        final ZipOutputStream outZ = new ZipOutputStream(output);
        output = outZ;

        // creating a writer
        final GridCoverageWriter writer = new GTopo30Writer(outZ);

        try{
	        // writing
	        writer.write(sourceCoverage, null);
        }finally{
        	try{
	            // freeing everything
	            writer.dispose();
        	}catch (Throwable e) {
				// TODO: handle exception
			}
            this.sourceCoverage.dispose(false);
            this.sourceCoverage = null;
        }


    }
}
