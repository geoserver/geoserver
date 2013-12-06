/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.png.providers;

import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

/**
 * A scanline provider that can copy 1-1 data from the buffered image into the scanline without
 * performing any kind of transformation
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterByteSingleBandProvider extends AbstractScanlineProvider {

    final byte[] bytes;

    public RasterByteSingleBandProvider(Raster raster, int bitDepth, int scanlineLength) {
        super(raster, bitDepth, scanlineLength);
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
    }

    public RasterByteSingleBandProvider(Raster raster, int bitDepth, int scanlineLength,
            IndexColorModel palette) {
        super(raster, bitDepth, scanlineLength, palette);
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
    }

    @Override
    public void next(final byte[] scanline, final int offset, final int length) {
        if (this.currentRow == height) {
            throw new IllegalStateException("All scanlines have been read already");
        }

        final int next = cursor.next();
        System.arraycopy(bytes, next, scanline, offset, length);
        currentRow++;
    }

}
