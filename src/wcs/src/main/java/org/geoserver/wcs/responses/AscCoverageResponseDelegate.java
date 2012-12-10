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
import java.util.zip.GZIPOutputStream;

import org.apache.commons.io.IOUtils;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.arcgrid.ArcGridWriter;

/**
 * DOCUMENT ME!
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class AscCoverageResponseDelegate implements CoverageResponseDelegate {

    private static final List<String> FORMATS = Arrays.asList(
            "application/arcgrid", "application/arcgrid;gzipped=\"true\"");

    public AscCoverageResponseDelegate() {
    }

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && ("ArcGrid".equalsIgnoreCase(outputFormat)
                        || isOutputCompressed(outputFormat) || FORMATS
                        .contains(outputFormat.toLowerCase()));
    }

    public String getMimeFormatFor(String outputFormat) {
        if ("ArcGrid".equalsIgnoreCase(outputFormat))
            return "application/arcgrid";
        else if (isOutputCompressed(outputFormat))
            return "application/arcgrid;gzipped=\"true\"";
        else if (FORMATS.contains(outputFormat))
            return outputFormat;
        else
            return null;
    }

    public String getMimeType(String outputFormat) {
        return isOutputCompressed(outputFormat) ? "application/x-gzip" : "text/plain";
    }

    private boolean isOutputCompressed(String outputFormat) {
        return "ArcGrid-GZIP".equalsIgnoreCase(outputFormat) || "application/arcgrid;gzipped=\"true\"".equals(outputFormat);
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

    public String getFileExtension(String outputFormat) {
        return isOutputCompressed(outputFormat) ? "asc.gz" : "asc";
    }

    public void encode(GridCoverage2D sourceCoverage, String outputFormat,  Map<String,String> econdingParameters,OutputStream output) throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(new StringBuffer(
                    "It seems prepare() has not been called").append(" or has not succeed")
                    .toString());
        }

        GZIPOutputStream gzipOut = null;
        if (isOutputCompressed(outputFormat)) {
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
