/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferByte;
import java.awt.image.Raster;

/**
 * A scanline provider optimized for Raster objects containig a 8bit BGR or ABGR image
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterByteABGRProvider extends AbstractScanlineProvider {

    final byte[] bytes;
    final boolean bgrOrder;
    final boolean hasAlpha;

    public RasterByteABGRProvider(Raster raster, boolean hasAlpha) {
        super(raster, 8, raster.getWidth() * (hasAlpha ? 4 : 3));
        this.hasAlpha = hasAlpha;
        this.bytes = ((DataBufferByte) raster.getDataBuffer()).getData();
        ComponentSampleModel sm = (ComponentSampleModel) raster.getSampleModel();
        this.bgrOrder = sm.getBandOffsets()[0] != 0;
    }

    @Override
    public void next(final byte[] row, final int offset, final int length) {
        int bytesIdx = cursor.next();
        int i = offset;
        final int max = offset + length;
        if (!bgrOrder) {
            System.arraycopy(bytes, bytesIdx, row, offset, length);
        } else {
            if (hasAlpha) {
                while (i < max) {
                    final byte a = bytes[bytesIdx++];
                    final byte b = bytes[bytesIdx++];
                    final byte g = bytes[bytesIdx++];
                    final byte r = bytes[bytesIdx++];
                    row[i++] = r;
                    row[i++] = g;
                    row[i++] = b;
                    row[i++] = a;
                }
            } else {
                while (i < max) {
                    final byte b = bytes[bytesIdx++];
                    final byte g = bytes[bytesIdx++];
                    final byte r = bytes[bytesIdx++];
                    row[i++] = r;
                    row[i++] = g;
                    row[i++] = b;
                }
            }
        }
    }

}
