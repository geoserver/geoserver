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
        int i = offset;
        final int max = offset + length;
        if (bitDepth == 4) {
            while (i < max) {
                final int low = bytes[pxIdx++];
                final int high = bytes[pxIdx++];
                row[i++] = (byte) (low | (high << 4));
            }
        } else if (bitDepth == 2) {
            while (i < max) {
                final int b1 = bytes[pxIdx++];
                final int b2 = bytes[pxIdx++];
                final int b3 = bytes[pxIdx++];
                final int b4 = bytes[pxIdx++];
                row[i++] = (byte) (b1 | (b2 << 2) | (b3 << 4) | (b4 << 6));
            }
        } else {
            while (i < max) {
                final int b1 = bytes[pxIdx++];
                final int b2 = bytes[pxIdx++];
                final int b3 = bytes[pxIdx++];
                final int b4 = bytes[pxIdx++];
                final int b5 = bytes[pxIdx++];
                final int b6 = bytes[pxIdx++];
                final int b7 = bytes[pxIdx++];
                final int b8 = bytes[pxIdx++];
                row[i++] = (byte) (b1 | (b2 << 1) | (b3 << 2) | (b4 << 3) | (b5 << 4) | (b6 << 5) | (b7 << 6) | (b8 << 7));
            }
        }

        currentRow++;
    }

}
