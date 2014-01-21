/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.png.providers;

import java.awt.image.DataBufferUShort;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

/**
 * A scanline provider optimized for a Raster with 16 bit gray pixels
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterShortSingleBandProvider extends AbstractScanlineProvider {

    final short[] shorts;

    public RasterShortSingleBandProvider(Raster raster) {
        super(raster, 16, raster.getWidth() * 2);
        this.shorts = ((DataBufferUShort) raster.getDataBuffer()).getData();
    }
    
    public RasterShortSingleBandProvider(Raster raster, int bidDepth, int scanlineLength, IndexColorModel palette) {
        super(raster, bidDepth, scanlineLength, palette);
        this.shorts = ((DataBufferUShort) raster.getDataBuffer()).getData();
    }

    @Override
    public void next(final byte[] scanline, final int offset, final int length) {
        int shortsIdx = cursor.next();
        int i = offset;
        int max = offset + length;
        while (i < max) {
            short gray = shorts[shortsIdx++];
            scanline[i++] = (byte) ((gray >> 8) & 0xFF);
            if(i < max) {
                scanline[i++] = (byte) (gray & 0xFF);
            }
        }
    }

}
