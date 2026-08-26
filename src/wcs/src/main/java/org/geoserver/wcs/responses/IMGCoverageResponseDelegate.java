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
import org.eclipse.imagen.media.range.NoDataContainer;
import org.geoserver.config.GeoServer;
import org.geoserver.platform.ServiceException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.util.CoverageUtilities;
import org.geotools.image.ImageWorker;

/**
 * Encodes coverages as plain PNG and JPEG images. The output carries no georeferencing: the client gets the pixels
 * only.
 *
 * @author $Author: Alessio Fabiani (alessio.fabiani@gmail.com) $ (last modification)
 * @author $Author: Simone Giannecchini (simboss1@gmail.com) $ (last modification)
 */
public class IMGCoverageResponseDelegate extends BaseCoverageResponseDelegate implements CoverageResponseDelegate {

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
    public void encode(
            GridCoverage2D sourceCoverage,
            String outputFormat,
            Map<String, String> encodingParameters,
            OutputStream output)
            throws ServiceException, IOException {
        if (sourceCoverage == null) {
            throw new IllegalStateException("It seems prepare() has not been called or has not succeed");
        }

        try {
            ImageWorker worker = new ImageWorker(sourceCoverage.getRenderedImage());
            worker.setROI(CoverageUtilities.getROIProperty(sourceCoverage));
            NoDataContainer noData = CoverageUtilities.getNoDataProperty(sourceCoverage);
            worker.setNoData(noData != null ? noData.getAsRange() : null);
            if ("jpeg".equals(getFileExtension(outputFormat))) {
                // 0.75 is the JPEG quality the JDK writer uses by default
                worker.writeJPEG(output, "JPEG", 0.75f);
            } else {
                // writePNG ignores the compression arguments, it always uses the writer defaults
                worker.writePNG(output, "FILTERED", 0.75f, false);
            }
        } finally {
            sourceCoverage.dispose(true);
        }
    }
}
