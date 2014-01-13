/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.SinglePixelPackedSampleModel;

/**
 * A scanline provider optimized for rasters with int packed RGB or RGBA pixels
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterIntABGRProvider extends AbstractScanlineProvider {

    final int[] pixels;

    final boolean bgrOrder;
    
    final boolean hasAlpha;

    public RasterIntABGRProvider(Raster raster, boolean hasAlpha) {
        super(raster, 8, raster.getWidth() * (hasAlpha ? 4 : 3));
        this.pixels = ((DataBufferInt) raster.getDataBuffer()).getData();
        this.hasAlpha = hasAlpha;
        if (hasAlpha) {
            bgrOrder = false;
        } else {
            int[] offsets = ((SinglePixelPackedSampleModel) raster.getSampleModel()).getBitOffsets();
            bgrOrder = offsets[0] != 0;
        }
    }

    @Override
    public void next(final byte[] row, final int offset, final int length) {
        int pxIdx = cursor.next();
        int i = offset;
        final int max = offset + length;
        if (hasAlpha) {
            while (i < max) {
                final int color = pixels[pxIdx++];

                row[i++] = (byte) ((color >> 16) & 0xff);
                row[i++] = (byte) ((color >> 8) & 0xff);
                row[i++] = (byte) ((color) & 0xff);
                row[i++] = (byte) ((color >> 24) & 0xff);
            }
        } else if (bgrOrder) {
            while (i < max) {
                final int color = pixels[pxIdx++];

                row[i++] = (byte) ((color >> 16) & 0xff);
                row[i++] = (byte) ((color >> 8) & 0xff);
                row[i++] = (byte) ((color) & 0xff);
            }
        } else {
            while (i < max) {
                final int color = pixels[pxIdx++];

                row[i++] = (byte) ((color) & 0xff);
                row[i++] = (byte) ((color >> 8) & 0xff);
                row[i++] = (byte) ((color >> 16) & 0xff);
            }
        }
    }

}
