/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import ar.com.hjg.pngj.FilterType;
import it.geosolutions.imageio.plugins.png.PNGWriter;
import java.awt.image.RenderedImage;
import java.io.OutputStream;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.ImageWorker;
import org.geotools.map.Layer;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ColorMap;
import org.geotools.styling.Style;

/**
 * Encodes the image in PNG using the PNGJ library
 *
 * @author Andrea Aime - GeoSolutions
 */
public class PNGJWriter {

    public RenderedImage writePNG(
            RenderedImage image, OutputStream outStream, float quality, WMSMapContent mapContent) {
        // what kind of scaline filtering are we going to use?
        FilterType filterType = getFilterType(mapContent);
        // Creation of a new PNGWriter object
        PNGWriter writer = new PNGWriter();
        // Check if a Scanline is supported by the writer
        boolean isScanlineSupported = writer.isScanlineSupported(image);
        // If it is not supported, then the image is rescaled to bytes
        if (!isScanlineSupported) {
            image =
                    new ImageWorker(image)
                            .rescaleToBytes()
                            .forceComponentColorModel()
                            .getRenderedImage();
        }

        RenderedImage output = null;
        // Image writing
        try {
            output = writer.writePNG(image, outStream, quality, filterType);
        } catch (Exception e) {
            throw new ServiceException("Failed to encode the PNG", e);
        }

        return output;
    }

    /**
     * SUB filtering is useful for raster images with "high" variation, otherwise we go for NONE,
     * empirically it provides better compression at lower effort
     */
    private FilterType getFilterType(WMSMapContent mapContent) {
        RasterSymbolizerVisitor visitor = new RasterSymbolizerVisitor();
        for (Layer layer : mapContent.layers()) {
            // check if the style has a raster symbolizer, don't trust the layer type as
            // we don't know in advance if there is a rendering transformation
            // WMS cascading is a ugly case, we might be cascading a map that is vector, but
            // we don't get to know
            Style style = layer.getStyle();
            if (style != null) {
                style.accept(visitor);
                if (visitor.highChangeRasterSymbolizer) {
                    return FilterType.FILTER_SUB;
                }
            }
        }

        return FilterType.FILTER_NONE;
    }

    /**
     * Check if the style contains a "high change" raster symbolizer, that is, one that generates a
     * continuous set of values for which SUB filtering provides better results
     *
     * @author Andrea Aime - GeoSolutions
     */
    class RasterSymbolizerVisitor extends AbstractStyleVisitor {

        boolean highChangeRasterSymbolizer;

        public void visit(org.geotools.styling.RasterSymbolizer raster) {
            if (raster.getColorMap() == null) {
                highChangeRasterSymbolizer = true;
                return;
            }

            int cmType = raster.getColorMap().getType();
            if (cmType != ColorMap.TYPE_INTERVALS && cmType != ColorMap.TYPE_VALUES) {
                highChangeRasterSymbolizer = true;
            }
        }
    }
}
