/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 *           (c) 2008 Open Source Geospatial Foundation (LGPL)
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.quantize;

import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.awt.image.WritableRaster;

import javax.media.jai.ImageLayout;
import javax.media.jai.PointOpImage;

/**
 * {@link PointOpImage} to perform a color inversion given a certain {@link ColorIndexer}. Derived and
 * improved from GeoTools one
 * 
 * @author Andrea Aime, GeoSolutions
 * 
 * @source $URL$
 */
@SuppressWarnings("unchecked")
public class ColorIndexerOpImage extends PointOpImage {
    private ColorIndexer palette;

    private IndexColorModel icm;

    public ColorIndexerOpImage(RenderedImage image, ColorIndexer palette, RenderingHints hints) {
        super(image, buildLayout(image, palette.toIndexColorModel()), hints, false);
        this.icm = palette.toIndexColorModel();

        this.setSource(image, 0);
        this.palette = palette;
    }

    /**
     * Builds an {@code ImageLayout} for this image. The {@code width} and {@code height} arguments
     * are requested tile dimensions which will only be used if they are smaller than this
     * operator's default tile dimension.
     * 
     * @param minX origin X ordinate
     * @param minY origin Y ordinate
     * @param width requested tile width
     * @param height requested tile height
     * @param sm sample model
     * 
     * @return the {@code ImageLayout} object
     */
    static ImageLayout buildLayout(RenderedImage image, IndexColorModel icm) {
        // build a sample model for the single tile
        ImageLayout il = new ImageLayout();
        il.setMinX(image.getMinX());
        il.setMinY(image.getMinY());
        il.setWidth(image.getWidth());
        il.setHeight(image.getHeight());
        il.setColorModel(icm);

        SampleModel sm = icm.createCompatibleSampleModel(image.getWidth(), image.getHeight());
        il.setSampleModel(sm);
        
        if(!(image instanceof BufferedImage)){

            il.setTileWidth(image.getTileWidth());
            il.setTileHeight(image.getTileHeight());
            il.setTileGridXOffset(image.getTileGridXOffset());
            il.setTileGridYOffset(image.getTileGridYOffset());        	
        } else {
        	// untiled in case the input image is untiled
        	// this could be optimized further by _not_
        	// simply forwarding getTile calls but converting coords.
            il.setTileWidth(image.getWidth());
            il.setTileHeight(image.getHeight());
            il.setTileGridXOffset(0);
            il.setTileGridYOffset(0);          	
        }

        return il;
    }

    @Override
    public Raster computeTile(int tx, int ty) {
        final RenderedImage sourceImage = getSourceImage(0);
        final Raster src = sourceImage.getTile(tx, ty);
        if (src == null) {
            return null;
        }
        final WritableRaster dest = icm.createCompatibleWritableRaster(src.getWidth(),
                src.getHeight()).createWritableTranslatedChild(src.getMinX(), src.getMinY());
        final int w = dest.getWidth();
        final int h = dest.getHeight();
        final int srcMinX = Math.max(sourceImage.getMinX(), src.getMinX());
        final int srcMinY = Math.max(sourceImage.getMinY(), src.getMinY());
        final int srcMaxX = Math.min(sourceImage.getMinX() + sourceImage.getWidth(), src.getMinX()
                + w);
        final int srcMaxY = Math.min(sourceImage.getMinY() + sourceImage.getHeight(), src.getMinY()
                + h);
        final int dstMinX = Math.max(src.getMinX(), sourceImage.getMinX());
        final int dstMinY = Math.max(src.getMinY(), sourceImage.getMinY());
        int srcBands = src.getNumBands();
        final int[] pixel = new int[srcBands];
        final byte[] bytes = new byte[srcBands];
        for (int y = srcMinY, y_ = dstMinY; y < srcMaxY; y++, y_++) {
            for (int x = srcMinX, x_ = dstMinX; x < srcMaxX; x++, x_++) {
                src.getPixel(x, y, pixel);
                for (int i = 0; i < srcBands; i++) {
                    bytes[i] = (byte) (pixel[i] & 0xFF);
                }

                int r, g, b, a;

                if(srcBands == 1 || srcBands == 2) {
                    r = g = b = pixel[0] & 0xFF;
                    a = srcBands == 2 ? pixel[1] & 0xFF : 255;
                } else  {
                    r = pixel[0] & 0xFF;
                    g = pixel[1] & 0xFF;
                    b = pixel[2] & 0xFF;
                    a = srcBands == 4 ? pixel[3] & 0xFF : 255;
                }

                int idx = palette.getClosestIndex(r, g, b, a);
                dest.setSample(x_, y_, 0, (byte) (idx & 0xff));
            }
        }

        return dest;
    }

}
