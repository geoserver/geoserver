/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.ParameterBlockImageN;
import org.eclipse.imagen.RenderedOp;
import org.eclipse.imagen.media.range.Range;
import org.eclipse.imagen.media.range.RangeFactory;
import org.eclipse.imagen.media.rlookup.RangeLookupTable;
import org.geotools.image.ImageWorker;
import org.geotools.util.logging.Logging;

/**
 * Utility class to generate a mask image from the UV bands, by using noData values (if provided) or NaN (if noData is
 * null) to identify invalid pixels. The resulting mask is a BYTE image where valid pixels are 255 and invalid
 * (noData/NaN) are 0.
 */
public final class PngWindMaskGenerator {
    private static final Logger LOGGER = Logging.getLogger(PngWindMaskGenerator.class);

    private static final Integer INVALID = 0;
    private static final Integer VALID = 255;

    private PngWindMaskGenerator() {}

    /**
     * Creates a mask image from the UV bands, using noData values (if provided) or NaN (if noData is null) to identify
     * invalid pixels.
     */
    public static RenderedImage createMask(RenderedImage uv, Double uNoData, Double vNoData) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Creating mask for UV bands with uNoData=" + uNoData + " and vNoData=" + vNoData);
        }
        if (uv == null) {
            throw new IllegalArgumentException("source must not be null");
        }
        if (uNoData == null && vNoData == null) {
            // Fast path: all pixels are valid
            if (LOGGER.isLoggable(Level.FINER)) {
                LOGGER.finer("Both uNoData and vNoData are null, creating a constant mask with all valid pixels");
            }
            return createConstantMask(uv);
        }

        // Build per-band masks: nodata/NaN -> 0, valid -> 255
        RenderedImage uMask = createValidMask(uv, 0, uNoData);
        RenderedImage vMask = createValidMask(uv, 1, vNoData);
        // if one mask is null, return the other, no need to combine
        if (uMask == null) {
            return vMask;
        }
        if (vMask == null) {
            return uMask;
        }

        // Combine: MIN is equivalent to logical AND for 0/255 masks
        return new ImageWorker().min(new RenderedImage[] {uMask, vMask}).getRenderedImage();
    }

    private static RenderedImage createValidMask(RenderedImage src, int band, Double noData) {
        if (noData == null) {
            // Fast path: no specific mask needed for this band, all valid
            return null;
        }
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Creating valid mask for band " + band + " with noData=" + noData);
        }
        ImageWorker iw = new ImageWorker(src);
        RenderedImage singleBand = iw.retainBands(new int[] {band}).getRenderedImage();
        Range noDataRange =
                singleValueRangeForType(noData, singleBand.getSampleModel().getDataType());

        // Create a RangeLookupTable mapping noDataRange to 0 and everything else to 255 (default)
        @SuppressWarnings({"rawtypes", "unchecked"})
        RangeLookupTable table =
                new RangeLookupTable.Builder().add(noDataRange, INVALID).build();
        ParameterBlockImageN pb = new ParameterBlockImageN("RLookup");
        pb.addSource(singleBand);
        pb.setParameter("table", table);
        pb.setParameter("default", VALID);
        RenderedOp mask = ImageN.create("RLookup", pb);

        // Convert to BYTE since the RLookup output will be
        // the same data type as the input (usually float/double) and we need a BYTE mask
        iw = new ImageWorker(mask);
        iw.format(DataBuffer.TYPE_BYTE);
        return iw.getRenderedImage();
    }

    /** Returns a BYTE image same size as src filled with 255. */
    private static RenderedImage createConstantMask(RenderedImage src) {
        ParameterBlockImageN pb = new ParameterBlockImageN("Constant");
        pb.setParameter("width", src.getWidth());
        pb.setParameter("height", src.getHeight());
        pb.setParameter("bandValues", new byte[] {VALID.byteValue()});
        return ImageN.create("Constant", pb);
    }

    private static Range singleValueRangeForType(Double noData, int dataType) {
        // Create valid mask invoke this method only if noData is not null, so we can safely assume noData is non-null
        // here
        Objects.requireNonNull(noData);
        switch (dataType) {
            case DataBuffer.TYPE_FLOAT: {
                if (Double.isNaN(noData)) {
                    return RangeFactory.create(Float.NaN, true, Float.NaN, true, true);
                } else {
                    float fNoData = (float) noData.doubleValue();
                    return RangeFactory.create(fNoData, true, fNoData, true, true);
                }
            }
            case DataBuffer.TYPE_DOUBLE: {
                return RangeFactory.create(noData, true, noData, true, true);
            }
            case DataBuffer.TYPE_BYTE: {
                byte v = (byte) Math.round(noData);
                return RangeFactory.create(v, true, v, true);
            }
            case DataBuffer.TYPE_USHORT: {
                int iv = (int) Math.round(noData);
                int clamped = Math.max(0, Math.min(65535, iv));
                short v = (short) (clamped & 0xFFFF);
                return RangeFactory.createU(v, true, v, true);
            }
            case DataBuffer.TYPE_SHORT: {
                int iv = (int) Math.round(noData);
                int clamped = Math.max(Short.MIN_VALUE, Math.min(Short.MAX_VALUE, iv));
                short v = (short) clamped;
                return RangeFactory.create(v, true, v, true);
            }
            case DataBuffer.TYPE_INT: {
                long lv = Math.round(noData);
                long clamped = Math.max(Integer.MIN_VALUE, Math.min(Integer.MAX_VALUE, lv));
                int v = (int) clamped;
                return RangeFactory.create(v, true, v, true);
            }
            default:
                throw new IllegalArgumentException("Unsupported dataType=" + dataType);
        }
    }
}
