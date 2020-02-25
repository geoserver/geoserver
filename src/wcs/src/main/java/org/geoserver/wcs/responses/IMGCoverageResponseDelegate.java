/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.image.WorldImageWriter;
import org.opengis.coverage.grid.Format;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;

/**
 * Encodes coverages in "world image" formats, png, jpeg and gif.
 *
 * <p>Notice that depending on the underlying coverage structure this is not always possible.
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class IMGCoverageResponseDelegate extends BaseCoverageResponseDelegate
        implements CoverageResponseDelegate {

    @SuppressWarnings("serial")
    public IMGCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                Arrays.asList("png", "jpeg", "JPEG", "PNG"), // output formats
                new HashMap<String, String>() { // file extensions
                    {
                        put("png", "png");
                        put("jpeg", "jpeg");
                        put("JPEG", "jpeg");
                        put("PNG", "png");
                        put("image/png", "png");
                        put("image/jpeg", "jpeg");
                    }
                },
                new HashMap<String, String>() { // mime types
                    {
                        put("png", "image/png");
                        put("jpeg", "image/jpeg");
                        put("PNG", "image/png");
                        put("JPEG", "image/jpeg");
                    }
                });
    }

    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> econdingParameters,
            OutputStream output)
            throws ServiceException, IOException {
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
            writer.write(sourceCoverage, new GeneralParameterValue[] {format});
            output.flush();
        } finally {

            // freeing everything
            IOUtils.closeQuietly(output);

            try {
                writer.dispose();
            } catch (Throwable e) {
                // eat me
            }
            sourceCoverage.dispose(true);
        }
    }
}
