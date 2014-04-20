/* Copyright (c) 2014 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

/**
 * A scanline provider optimized for Raster objects containing a 8bit gray and alpha bands
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterByteGrayAlphaProvider extends AbstractScanlineProvider {

    final byte[] bytes;

    boolean alphaFirst;

    public RasterByteGrayAlphaProvider(Raster raster) {
        super(raster, 8, raster.getWidth() * 2);
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
        int[] bandOffsets = ((ComponentSampleModel) raster.getSampleModel()).getBandOffsets();
        this.alphaFirst = bandOffsets[0] != 0;
    }

    
    public void next(final byte[] row, final int offset, final int length) {
        int bytesIdx = cursor.next();
        if (alphaFirst) {
            int i = offset;
            final int max = offset + length;
            while (i < max) {
                final byte a = bytes[bytesIdx++];
                final byte g = bytes[bytesIdx++];
                row[i++] = g;
                row[i++] = a;
            }
        } else {
            System.arraycopy(bytes, bytesIdx, row, offset, length);
        }
    }

}
