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
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridWriter;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;

/**
 * DOCUMENT ME!
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class AscCoverageResponseDelegate implements CoverageResponseDelegate {

    private static final Set<String> FORMATS = new HashSet<String>(Arrays.asList(
            "application/arcgrid", "application/arcgrid;zipped=\"true\""));

    /**
     * 
     * @uml.property name="sourceCoverage"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private GridCoverage2D sourceCoverage;

    private boolean compressOutput = false;

    public AscCoverageResponseDelegate() {
    }

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && ("ArcGrid".equalsIgnoreCase(outputFormat)
                        || "ArcGrid-GZIP".equalsIgnoreCase(outputFormat) || FORMATS
                        .contains(outputFormat.toLowerCase()));
    }

    public String getMimeFormatFor(String outputFormat) {
        if ("ArcGrid".equalsIgnoreCase(outputFormat))
            return "application/arcgrid";
        else if ("ArcGrid-GZIP".equalsIgnoreCase(outputFormat))
            return "application/arcgrid;zipped=\"true\"";
        else if (FORMATS.contains(outputFormat))
            return outputFormat;
        else
            return null;
    }

    public void prepare(String outputFormat, GridCoverage2D coverage) throws IOException {
        this.compressOutput = "ArcGrid-GZIP".equalsIgnoreCase(outputFormat);
        this.sourceCoverage = coverage;
    }

    public String getContentType() {
        // return gs.getMimeType();
        return compressOutput ? "application/x-gzip" : "text/plain";
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getContentEncoding() {
        // return compressOutput ? "gzip" : null;
        return null;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getContentDisposition() {
        return compressOutput ? ("attachment;filename=" + this.sourceCoverage.getName() + ".asc.gz")
                : null;
    }

    public String getFileExtension() {
        return compressOutput ? "asc.gz" : "asc";
    }

    public void encode(OutputStream output) throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(new StringBuffer(
                    "It seems prepare() has not been called").append(" or has not succeed")
                    .toString());
        }

        GZIPOutputStream gzipOut = null;
        if (compressOutput) {
            gzipOut = new GZIPOutputStream(output);
            output = gzipOut;
        }

        ArcGridWriter writer=null;
        try {
            writer = new ArcGridWriter(output);
            writer.write(sourceCoverage, null);

            if (gzipOut != null) {
                gzipOut.finish();
                gzipOut.flush();
            }


        }finally {
        	try{
        	if(writer!=null)
        		writer.dispose();
        	}catch (Throwable e) {
				// eating exception
			}
        	if(gzipOut!=null)
        		IOUtils.closeQuietly(gzipOut);
        	
            this.sourceCoverage.dispose(false);
            this.sourceCoverage = null;
		}
    }
}
