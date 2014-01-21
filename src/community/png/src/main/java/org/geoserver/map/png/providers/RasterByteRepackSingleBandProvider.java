/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.map.png.providers;

import java.awt.image.DataBufferByte;
import java.awt.image.IndexColorModel;
import java.awt.image.Raster;

/**
 * A scanline provider that packs more than one pixel per output byte
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterByteRepackSingleBandProvider extends AbstractScanlineProvider {

    final byte[] bytes;

    public RasterByteRepackSingleBandProvider(Raster raster, int bitDepth, int scanlineLength) {
        super(raster, bitDepth, scanlineLength);
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
    }

    public RasterByteRepackSingleBandProvider(Raster raster, int bitDepth, int scanlineLength,
            IndexColorModel palette) {
        super(raster, bitDepth, scanlineLength, palette);
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
    }

    @Override
    public void next(final byte[] row, final int offset, final int length) {
        if (this.currentRow == height) {
            throw new IllegalStateException("All scanlines have been read already");
        }

        int pxIdx = cursor.next();
        final int pxLimit = pxIdx + width;
        int i = offset;
        final int max = offset + length;
        if (bitDepth == 4) {
            while (i < max) {
                final int low = bytes[pxIdx++];
                final int high = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                row[i++] = (byte) ((low << 4) | high);
            }
        } else if (bitDepth == 2) {
            while (i < max) {
                final int b1 = bytes[pxIdx++];
                final int b2 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b3 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b4 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                row[i++] = (byte) (b4 | (b3 << 2) | (b2 << 4) | (b1 << 6));
            }
        } else {
            while (i < max) {
                final int b1 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b2 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b3 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b4 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b5 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b6 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b7 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                final int b8 = pxIdx < pxLimit ? bytes[pxIdx++] : 0;
                row[i++] = (byte) (b8 | (b7 << 1) | (b6 << 2) | (b5 << 3) | (b4 << 4) | (b3 << 5)
                        | (b2 << 6) | (b1 << 7));
            }
        }

        currentRow++;
    }

}
