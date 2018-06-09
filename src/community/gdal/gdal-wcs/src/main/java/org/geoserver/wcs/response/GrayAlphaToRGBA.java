/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.awt.image.RenderedImage;
import java.util.logging.Logger;
import org.geoserver.ogr.core.FormatAdapter;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Adapts a gray/alpha coverage to RGBA for formats that cannot take gray/alpha as an input
 *
 * @author Andrea Aime - GeoSolutions
 */
public class GrayAlphaToRGBA implements FormatAdapter<GridCoverage2D> {

    static final Logger LOGGER = Logging.getLogger(GrayAlphaToRGBA.class);

    @Override
    public GridCoverage2D adapt(GridCoverage2D input) {
        RenderedImage image = input.getRenderedImage();
        if (image.getSampleModel().getNumBands() == 2 && image.getColorModel().hasAlpha()) {
            LOGGER.fine("Expanding image from gray/alpha to rgba");
            ImageWorker iw = new ImageWorker(image);
            iw.retainBands(1).forceColorSpaceRGB();
            RenderedImage alphaBand = new ImageWorker(image).retainLastBand().getRenderedImage();
            iw.addBand(alphaBand, false);
            RenderedImage converted = iw.getRenderedImage();
            GridCoverage2D adapted =
                    CoverageFactoryFinder.getGridCoverageFactory(null)
                            .create(
                                    input.getName(),
                                    converted,
                                    input.getGridGeometry(),
                                    null,
                                    new GridCoverage2D[] {input},
                                    input.getProperties());
            return adapted;
        }

        return input;
    }
}
