/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.gs.download.vertical.op;

import it.geosolutions.jaiext.range.Range;
import java.awt.*;
import java.awt.image.*;
import java.util.Map;
import java.util.Vector;
import java.util.logging.Logger;
import javax.media.jai.*;
import org.geoserver.wps.WPSException;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.TransformException;

/**
 * A PointOpImage applying vertical transform to each pixel. Consider moving it down to geotools
 * process-raster or similar packages when ready.
 */
public class VerticalTransformOpImage extends PointOpImage {

    private static final double DELTA = 1E-6;

    private static final Logger LOGGER =
            Logger.getLogger(VerticalTransformOpImage.class.toString());

    // The 2 transforms have different dimension: 2D vs 3D
    // we might find some way to concatenate them
    private final MathTransform coordinatesTransform;
    private final MathTransform verticalTransform;
    private final boolean hasNoData;
    private Range noData;
    private double noDataDouble = Double.NaN;

    public VerticalTransformOpImage(
            Map config,
            ImageLayout layout,
            MathTransform coordinatesTransform,
            MathTransform verticalTransform,
            Range noData,
            RenderedImage... sources) {
        super(vectorize(sources), layout, config, true);

        // Check the number of sources
        if (sources.length > 1) {
            LOGGER.warning("Multiple sources found, only the first one will be used");
        }

        this.coordinatesTransform = coordinatesTransform;
        this.verticalTransform = verticalTransform;

        if (noData != null) {
            hasNoData = true;
            this.noData = noData;
            this.noDataDouble = noData.getMin().doubleValue();

        } else {
            hasNoData = false;
        }

        // Set flag to permit in-place operation.
        permitInPlaceOperation();
    }

    /**
     * Computes the final pixel from N source images within a specified rectangle.
     *
     * @param sources Cobbled sources, guaranteed to provide all the source data necessary for
     *     computing the rectangle.
     * @param dest The tile containing the rectangle to be computed.
     * @param destRect The rectangle within the tile to be computed.
     */
    protected void computeRect(Raster[] sources, WritableRaster dest, Rectangle destRect) {

        // Retrieve format tags.
        RasterFormatTag[] formatTags = getFormatTags();

        RasterAccessor rasterArray =
                new RasterAccessor(
                        sources[0], destRect, formatTags[0], getSourceImage(0).getColorModel());

        RasterAccessor d = new RasterAccessor(dest, destRect, formatTags[1], getColorModel());

        try {
            switch (d.getDataType()) {
                case DataBuffer.TYPE_BYTE:
                    byteLoop(rasterArray, d);
                    break;
                case DataBuffer.TYPE_USHORT:
                    ushortLoop(rasterArray, d);
                    break;
                case DataBuffer.TYPE_SHORT:
                    shortLoop(rasterArray, d);
                    break;
                case DataBuffer.TYPE_INT:
                    intLoop(rasterArray, d);
                    break;
                case DataBuffer.TYPE_FLOAT:
                    floatLoop(rasterArray, d);
                    break;
                case DataBuffer.TYPE_DOUBLE:
                    doubleLoop(rasterArray, d);
                    break;
            }
        } catch (TransformException e) {
            throw new WPSException("Exception occurred while interpolating the vertical data", e);
        }

        if (d.needsClamping()) {
            d.clampDataArrays();
        }
        d.copyDataToRaster();
    }

    private void byteLoop(RasterAccessor rasterArray, RasterAccessor dst)
            throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        byte[][] dData = dst.getByteDataArrays();
        byte[] srcData;
        byte[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getByteDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];
        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = (byte) destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void shortLoop(RasterAccessor rasterArray, RasterAccessor dst)
            throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        short[][] dData = dst.getShortDataArrays();
        short[] srcData;
        short[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getShortDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = (short) destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void ushortLoop(RasterAccessor rasterArray, RasterAccessor dst)
            throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        short[][] dData = dst.getShortDataArrays();
        short[] srcData;
        short[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getShortDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = (short) destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void intLoop(RasterAccessor rasterArray, RasterAccessor dst) throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        int[][] dData = dst.getIntDataArrays();
        int[] srcData;
        int[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getIntDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = (int) destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void floatLoop(RasterAccessor rasterArray, RasterAccessor dst)
            throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        float[][] dData = dst.getFloatDataArrays();
        float[] srcData;
        float[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getFloatDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = (float) destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void doubleLoop(RasterAccessor rasterArray, RasterAccessor dst)
            throws TransformException {

        int srcLineStride = rasterArray.getScanlineStride();
        int srcPixelStride = rasterArray.getPixelStride();
        int[] srcBandOffsets = rasterArray.getBandOffsets();
        int dWidth = dst.getWidth();
        int dHeight = dst.getHeight();
        int dLineStride = dst.getScanlineStride();
        int dPixelStride = dst.getPixelStride();
        int[] dBandOffsets = dst.getBandOffsets();

        double[][] dData = dst.getDoubleDataArrays();
        double[] srcData;
        double[] d;

        int srcX = rasterArray.getX();
        int srcY = rasterArray.getY();

        int srcLineOffset;
        int srcPixelOffset;

        srcData = rasterArray.getDoubleDataArray(0);
        srcLineOffset = srcBandOffsets[0];

        d = dData[0];
        double[] srcPoints = new double[3];
        double[] destPoints = new double[3];
        int dLineOffset = dBandOffsets[0];

        for (int h = 0; h < dHeight; h++) {
            srcPixelOffset = srcLineOffset;
            srcLineOffset += srcLineStride;
            int dPixelOffset = dLineOffset;
            dLineOffset += dLineStride;

            for (int w = 0; w < dWidth; w++) {
                srcPoints[0] = srcX + w;
                srcPoints[1] = srcY + h;
                srcPoints[2] = srcData[srcPixelOffset];
                transform(srcPoints, destPoints);
                d[dPixelOffset] = destPoints[2];
                srcPixelOffset += srcPixelStride;
                dPixelOffset += dPixelStride;
            }
        }
    }

    private void transform(double[] srcPoints, double[] destPoints) throws TransformException {
        if (Double.isNaN(srcPoints[2])
                || (hasNoData && Math.abs(this.noDataDouble - srcPoints[2]) < DELTA)) {
            destPoints[2] = srcPoints[2];
        } else {
            // Transform the pixel coordinate to the coordinate in the vertical grid crs
            coordinatesTransform.transform(srcPoints, 0, srcPoints, 0, 1);

            // Transform the vertical value of the current position
            verticalTransform.transform(srcPoints, 0, destPoints, 0, 1);
            if (Double.isNaN(destPoints[2])) {
                destPoints[2] = this.noDataDouble;
            }
        }
    }

    private static Vector<RenderedImage> vectorize(RenderedImage[] sources) {

        Vector<RenderedImage> vec = new Vector<RenderedImage>(sources.length);

        for (RenderedImage image : sources) {
            if (image != null) {
                vec.add(image);
            }
        }

        if (vec.isEmpty()) {
            return null;
        }

        return vec;
    }
}
