/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.png.providers;

import java.awt.image.DataBufferUShort;
import java.awt.image.PixelInterleavedSampleModel;
import java.awt.image.Raster;

/**
 * A scanline provider optimized for a Raster with 16 bit gray + 16 bits alpha
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterShortGrayAlphaProvider extends AbstractScanlineProvider {

    final short[] shorts;

    final boolean alphaFirst;

    public RasterShortGrayAlphaProvider(Raster raster) {
        super(raster, 16, raster.getWidth() * 4);
        this.shorts = ((DataBufferUShort) raster.getDataBuffer()).getData();
        int[] bandOffsets = ((PixelInterleavedSampleModel) raster.getSampleModel()).getBandOffsets();
        this.alphaFirst = bandOffsets[0] != 0;
    }

    @Override
    public void next(final byte[] scanline, final int offset, final int length) {
        int shortsIdx = cursor.next();
        int i = offset;
        final int max = offset + length;
        if(alphaFirst) {
            while (i < max) {
                final short alpha = shorts[shortsIdx++];
                final short gray = shorts[shortsIdx++];
                scanline[i++] = (byte) ((gray >> 8) & 0xFF);
                scanline[i++] = (byte) (gray & 0xFF);
                scanline[i++] = (byte) ((alpha >> 8) & 0xFF);
                scanline[i++] = (byte) (alpha & 0xFF);
            } 
        } else {
            while (i < max) {
                final short gray = shorts[shortsIdx++];
                final short alpha = shorts[shortsIdx++];
                scanline[i++] = (byte) ((gray >> 8) & 0xFF);
                scanline[i++] = (byte) (gray & 0xFF);
                scanline[i++] = (byte) ((alpha >> 8) & 0xFF);
                scanline[i++] = (byte) (alpha & 0xFF);
            }
        }
    }

}
