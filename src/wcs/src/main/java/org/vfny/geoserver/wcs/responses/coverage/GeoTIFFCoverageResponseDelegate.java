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

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.imageio.GeoToolsWriteParams;
import org.geotools.gce.geotiff.GeoTiffFormat;
import org.geotools.gce.geotiff.GeoTiffWriteParams;
import org.geotools.gce.geotiff.GeoTiffWriter;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.vfny.geoserver.wcs.responses.CoverageResponseDelegate;

/**
 * Coverage writer for the geotiff format.
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author Simone Giannecchini, GeoSolutions SAS
 */
public class GeoTIFFCoverageResponseDelegate implements CoverageResponseDelegate {

    private static final Set<String> FORMATS = new HashSet<String>(Arrays.asList("image/tiff","image/tiff;subtype=\"geotiff\"","image/geotiff"));

    /**
     * 
     * @uml.property name="sourceCoverage"
     * @uml.associationEnd multiplicity="(0 1)"
     */
    private GridCoverage2D sourceCoverage;

	private static final GeoTiffFormat format = new GeoTiffFormat();

	public static final String GEOTIFF_CONTENT_TYPE = "image/tiff;subtype=\"geotiff\"";

    public GeoTIFFCoverageResponseDelegate() {
    }

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && (outputFormat.equalsIgnoreCase("geotiff") || FORMATS.contains(outputFormat.toLowerCase()));
    }

    public void prepare(String outputFormat, GridCoverage2D coverage) throws IOException {
        this.sourceCoverage = coverage;
    }

    public String getMimeFormatFor(String outputFormat) {
        if (canProduce(outputFormat))
            return "image/tiff;subtype=\"geotiff\"";
        else
            return null;
    }

    public String getContentType() {
        return GEOTIFF_CONTENT_TYPE;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getContentEncoding() {
        return null;
    }

    /**
     * DOCUMENT ME!
     * 
     * @return DOCUMENT ME!
     */
    public String getContentDisposition() {
        return "attachment;filename=" + this.sourceCoverage.getName() + ".tif";
    }

    public String getFileExtension() {
        return "tif";
    }

    public void encode(OutputStream output) throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException("It seems prepare() has not been called"
                    + " or has not succeed");
        }

        // good for all params
        final GeoTiffWriteParams wp = new GeoTiffWriteParams();
        wp.setCompressionMode(GeoTiffWriteParams.MODE_EXPLICIT);
        wp.setCompressionType("LZW");
        wp.setCompressionQuality(0.75F);
        wp.setTilingMode(GeoToolsWriteParams.MODE_EXPLICIT);
        wp.setTiling(256, 256);
        final ParameterValueGroup writerParams = format.getWriteParameters();
        writerParams.parameter(AbstractGridFormat.GEOTOOLS_WRITE_PARAMS.getName().toString()).setValue(wp);

        // write down
        GeoTiffWriter writer = (GeoTiffWriter) format.getWriter(output);
        try{
        	if(writer!=null)
        		writer.write(sourceCoverage, (GeneralParameterValue[]) writerParams.values().toArray(new GeneralParameterValue[1]));
        }finally {
        	try{
        	if(writer!=null)
        		writer.dispose();
        	}catch (Throwable e) {
				// eating exception
			}
            this.sourceCoverage.dispose(false);
            this.sourceCoverage = null;
		}


    }
}
