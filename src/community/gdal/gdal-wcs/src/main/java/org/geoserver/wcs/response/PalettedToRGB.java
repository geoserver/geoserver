/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs.response;

import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.util.logging.Logger;
import org.geoserver.ogr.core.FormatAdapter;
import org.geotools.coverage.CoverageFactoryFinder;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Adapts a paletted coverage to RGB(A) for formats that cannot take paletted as an input
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PalettedToRGB implements FormatAdapter<GridCoverage2D> {

    static final Logger LOGGER = Logging.getLogger(PalettedToRGB.class);

    @Override
    public GridCoverage2D adapt(GridCoverage2D input) {
        RenderedImage image = input.getRenderedImage();
        if (image.getSampleModel().getNumBands() == 1
                && image.getColorModel() instanceof IndexColorModel) {
            LOGGER.fine("Expanding image from indexed to rgb(a)");

            ImageWorker iw = new ImageWorker(image);
            iw.forceComponentColorModel();
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
