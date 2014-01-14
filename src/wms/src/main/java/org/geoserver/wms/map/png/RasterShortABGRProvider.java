/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.map.png;

import java.awt.image.ComponentSampleModel;
import java.awt.image.DataBufferUShort;
import java.awt.image.Raster;

/**
 * A scanline provider optimized for Raster objects containing a 16bit BGR or ABGR image
 * 
 * @author Andrea Aime - GeoSolutions
 */
public final class RasterShortABGRProvider extends AbstractScanlineProvider {

    final short[] shorts;

    final boolean bgrOrder;

    final boolean hasAlpha;

    public RasterShortABGRProvider(Raster raster, boolean hasAlpha) {
        super(raster, 16, (hasAlpha ? 8 : 6) * raster.getWidth());
        this.hasAlpha = hasAlpha;
        shorts = ((DataBufferUShort) raster.getDataBuffer()).getData();
        bgrOrder = ((ComponentSampleModel) raster.getSampleModel()).getBandOffsets()[0] != 0;
    }

    @Override
    public void next(final byte[] scanline, final int offset, final int length) {
        int shortsIdx = cursor.next();
        int i = offset;
        final int max = offset + length;
        if (hasAlpha) {
            if (bgrOrder) {
                while (i < max) {
                    final short a = shorts[shortsIdx++];
                    final short b = shorts[shortsIdx++];
                    final short g = shorts[shortsIdx++];
                    final short r = shorts[shortsIdx++];
                    scanline[i++] = (byte) ((r >> 8) & 0xFF);
                    scanline[i++] = (byte) (r & 0xFF);
                    scanline[i++] = (byte) ((g >> 8) & 0xFF);
                    scanline[i++] = (byte) (g & 0xFF);
                    scanline[i++] = (byte) ((b >> 8) & 0xFF);
                    scanline[i++] = (byte) (b & 0xFF);
                    scanline[i++] = (byte) ((a >> 8) & 0xFF);
                    scanline[i++] = (byte) (a & 0xFF);
                }
            } else {
                while (i < max) {
                    final short r = shorts[shortsIdx++];
                    final short g = shorts[shortsIdx++];
                    final short b = shorts[shortsIdx++];
                    final short a = shorts[shortsIdx++];
                    scanline[i++] = (byte) ((r >> 8) & 0xFF);
                    scanline[i++] = (byte) (r & 0xFF);
                    scanline[i++] = (byte) ((g >> 8) & 0xFF);
                    scanline[i++] = (byte) (g & 0xFF);
                    scanline[i++] = (byte) ((b >> 8) & 0xFF);
                    scanline[i++] = (byte) (b & 0xFF);
                    scanline[i++] = (byte) ((a >> 8) & 0xFF);
                    scanline[i++] = (byte) (a & 0xFF);
                }
            }
        } else {
            if(bgrOrder) {
                while (i < max) {
                    final short b = shorts[shortsIdx++];
                    final short g = shorts[shortsIdx++];
                    final short r = shorts[shortsIdx++];
                    scanline[i++] = (byte) ((r >> 8) & 0xFF);
                    scanline[i++] = (byte) (r & 0xFF);
                    scanline[i++] = (byte) ((g >> 8) & 0xFF);
                    scanline[i++] = (byte) (g & 0xFF);
                    scanline[i++] = (byte) ((b >> 8) & 0xFF);
                    scanline[i++] = (byte) (b & 0xFF);
                }
            } else {
                while (i < max) {
                    final short r = shorts[shortsIdx++];
                    final short g = shorts[shortsIdx++];
                    final short b = shorts[shortsIdx++];
                    scanline[i++] = (byte) ((r >> 8) & 0xFF);
                    scanline[i++] = (byte) (r & 0xFF);
                    scanline[i++] = (byte) ((g >> 8) & 0xFF);
                    scanline[i++] = (byte) (g & 0xFF);
                    scanline[i++] = (byte) ((b >> 8) & 0xFF);
                    scanline[i++] = (byte) (b & 0xFF);
                }
            }
        }
    }

}
