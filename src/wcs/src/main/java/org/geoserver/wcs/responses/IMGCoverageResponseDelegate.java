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

import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.image.WorldImageWriter;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Encodes coverages in "world image" formats, png, jpeg and gif.
 * 
 * <p>
 * Notice that depending on the underlying coverage structure this is not always possible.      
 * 
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class IMGCoverageResponseDelegate implements CoverageResponseDelegate {

    private static final List<String> FORMATS = Arrays.asList("image/bmp",
            "image/gif","image/png", "image/jpeg");

    public IMGCoverageResponseDelegate() {
    }

    public boolean canProduce(String outputFormat) {
        return outputFormat != null
                && (FORMATS.contains(outputFormat.toLowerCase()) || FORMATS.contains("image/"
                        + outputFormat.toLowerCase()));
    }

    public String getMimeType(String outputFormat) {
        if (!canProduce(outputFormat))
            return null;

        if (FORMATS.contains(outputFormat.toLowerCase())) {
            return outputFormat;
        }
        String mime = "image/" + outputFormat.toLowerCase();
        if (FORMATS.contains(mime)) {
            return mime;
        }

        return null;
    }

    public String getFileExtension(String outputFormat) {
        String contentType = getMimeType(outputFormat);
        if(contentType == null) {
            return "img";
        } else {
            // extract the extension from the content type
            int idx = contentType.indexOf("/");
            return contentType.substring(idx + 1);
        }
    }

	public void encode(GridCoverage2D sourceCoverage, String outputFormat, Map<String,String> econdingParameters, OutputStream output) throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException(
                    "It seems prepare() has not been called or has not succeed");
        }

        final WorldImageWriter writer = new WorldImageWriter(output);

        // writing parameters for Image
        final Format writerParams = writer.getFormat();
        final ParameterValueGroup writeParameters = writerParams.getWriteParameters();
        final ParameterValue<?> format = writeParameters.parameter("Format");
        format.setValue(getFileExtension(outputFormat));
        
        try {
            // writing
            writer.write(sourceCoverage, new GeneralParameterValue[] { format });
            output.flush();
        } finally {

            // freeing everything
            org.apache.commons.io.IOUtils.closeQuietly(output);

            try {
                writer.dispose();
            } catch (Throwable e) {
                // eat me
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
