/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.responses;

import static java.util.Map.entry;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geoserver.util.IOUtils;
import org.geotools.api.coverage.grid.Format;
import org.geotools.api.parameter.GeneralParameterValue;
import org.geotools.api.parameter.ParameterValue;
import org.geotools.api.parameter.ParameterValueGroup;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.gce.image.WorldImageWriter;

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

    public IMGCoverageResponseDelegate(GeoServer geoserver) {
        super(
                geoserver,
                List.of("png", "jpeg", "JPEG", "PNG", "image/png", "image/jpeg"), // output formats
                Map.ofEntries( // file extensions
                        entry("png", "png"),
                        entry("jpeg", "jpeg"),
                        entry("JPEG", "jpeg"),
                        entry("PNG", "png"),
                        entry("image/png", "png"),
                        entry("image/jpeg", "jpeg")),
                Map.ofEntries( // mime types
                        entry("png", "image/png"),
                        entry("jpeg", "image/jpeg"),
                        entry("PNG", "image/png"),
                        entry("JPEG", "image/jpeg"),
                        entry("image/png", "image/png"),
                        entry("image/jpeg", "image/jpeg")));
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
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
