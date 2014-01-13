/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import java.awt.image.ColorModel;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.OutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.ServiceException;
import org.geoserver.wms.WMSMapContent;
import org.geotools.image.ImageWorker;
import org.geotools.map.Layer;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.styling.ColorMap;
import org.geotools.styling.Style;
import org.geotools.util.logging.Logging;

import ar.com.hjg.pngj.FilterType;
import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.PngWriter;
import ar.com.hjg.pngj.chunks.PngChunkPLTE;
import ar.com.hjg.pngj.chunks.PngChunkTRNS;

/**
 * Encodes the image in PNG using the PNGJ library
 * 
 * @author Andrea Aime - GeoSolutions
 */
public class PNGJWriter {

    private static final Logger LOGGER = Logging.getLogger(PNGJWriter.class);

    public RenderedImage writePNG(RenderedImage image, OutputStream outStream, float quality,
            WMSMapContent mapContent) {
        // compute the compression level similarly to what the Clib code does
        int level = Math.round(9 * (1f - quality));

        // what kind of scaline filtering are we going to use?
        FilterType filterType = getFilterType(mapContent);

        return writePNG(image, outStream, level, filterType);
    }

    RenderedImage writePNG(RenderedImage image, OutputStream outStream, int level,
            FilterType filterType) {
        // get the optimal scanline provider for this image
        RenderedImage original = image;
        ScanlineProvider scanlines = ScanlineProviderFactory.getProvider(image);
        if(scanlines == null) {
            image = new ImageWorker(image).rescaleToBytes().forceComponentColorModel().getRenderedImage();
            scanlines = ScanlineProviderFactory.getProvider(image);
        }
        if(scanlines == null) {
            throw new IllegalArgumentException("Could not find a scanline extractor for " + original);
        }

        // encode using the PNGJ library and the GeoServer own scanline providers
        ColorModel colorModel = image.getColorModel();
        boolean indexed = colorModel instanceof IndexColorModel;
        ImageInfo ii = getImageInfo(image, scanlines, colorModel, indexed);
        PngWriter pw = new PngWriter(outStream, ii);
        pw.setShouldCloseStream(false);
        try {
            pw.setCompLevel(level);
            pw.setFilterType(filterType);

            if (indexed) {
                IndexColorModel icm = (IndexColorModel) colorModel;
                PngChunkPLTE palette = pw.getMetadata().createPLTEChunk();
                int ncolors = icm.getMapSize();
                palette.setNentries(ncolors);
                for (int i = 0; i < ncolors; i++) {
                    final int red = icm.getRed(i);
                    final int green = icm.getGreen(i);
                    final int blue = icm.getBlue(i);
                    palette.setEntry(i, red, green, blue);
                }
                if (icm.hasAlpha()) {
                    PngChunkTRNS transparent = new PngChunkTRNS(ii);
                    int[] alpha = new int[ncolors];
                    for (int i = 0; i < ncolors; i++) {
                        final int a = icm.getAlpha(i);
                        alpha[i] = a;
                    }
                    transparent.setPalletteAlpha(alpha);
                    pw.getChunksList().queue(transparent);

                }
            }

            // write out the actual image lines
            for (int row = 0; row < image.getHeight(); row++) {
                pw.writeRow(scanlines);
            }
            pw.end();
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to encode the PNG", e);
            throw new ServiceException(e);
        } finally {
            pw.close();
        }
        
        return image;
    }

    private ImageInfo getImageInfo(RenderedImage image, ScanlineProvider scanlines,
            ColorModel colorModel, boolean indexed) {
        int numColorComponents = colorModel.getNumColorComponents();
        boolean grayscale = !indexed && numColorComponents < 3;
        byte bitDepth = scanlines.getBitDepth();
        boolean hasAlpha = !indexed && colorModel.hasAlpha();
        ImageInfo ii = new ImageInfo(image.getWidth(), image.getHeight(), bitDepth, hasAlpha,
                grayscale, indexed);
        return ii;
    }

    /**
     * SUB filtering is useful for raster images with "high" variation, otherwise we go for NONE,
     * empirically it provides better compression at lower effort
     * 
     * @param mapContent
     * @return
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
