/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.validator;

import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import org.geotools.coverage.grid.GridCoverage2D;

/**
 * Estimates the size of a raster as an uncompressed in memory grid
 *
 * @author Andrea Aime - GeoSolutions
 */
public class RasterSizeEstimator implements ObjectSizeEstimator {

    @Override
    public long getSizeOf(Object object) {
        if (object instanceof GridCoverage2D) {
            GridCoverage2D coverage = (GridCoverage2D) object;
            return estimateSize(coverage.getRenderedImage());
        } else if (object instanceof RenderedImage) {
            return estimateSize((RenderedImage) object);
        }

        return ObjectSizeEstimator.UNKNOWN_SIZE;
    }

    private long estimateSize(RenderedImage renderedImage) {
        SampleModel sm = renderedImage.getSampleModel();
        int bits = 0;
        for (int ss : sm.getSampleSize()) {
            bits += ss;
        }
        int pixelSizeByte = (int) Math.ceil(bits / 8d);
        return renderedImage.getWidth() * renderedImage.getHeight() * pixelSizeByte;
    }
}
