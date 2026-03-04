/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.ParameterBlockImageN;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetMapRequest;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Envelope;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Utility class to quantize wind data into a PNG image, and to generate the metadata map to be passed to the PNG encoder.
 *
 * The quantization process involves:
 * 1) Computing scale and offset for each band based on the provided min/max values.
 * 2) Quantizing each band to byte using the computed scale and offset.
 * 3) Generating a mask for nodata values.
 * 4) Merging the quantized bands and mask into a single RGB image (R=band1, G=band2, B=mask).
 * 5) Creating a metadata map containing the necessary information for decoding the quantized data from the PNG image.
 */
public final class PngWindQuantizer {

    /** Container for the quantized image and its associated metadata. */
    static class PngWindQuantizedImage {
        private final RenderedImage image;
        private final Map<String, String> metadata;

        public PngWindQuantizedImage(RenderedImage image, Map<String, String> metadata) {
            this.image = image;
            this.metadata = metadata;
        }

        public RenderedImage getImage() { return image; }
        public Map<String, String> getMetadata() { return metadata; }
    }

    private static final String VERSION = "1.0";

    public static final String FORMAT = "PNG-WIND";

    private static final Double DELTA = 1E-18;


    public static PngWindQuantizedImage quantize(
            RenderedImage sourceBands,
            PngWindRequestContext ctx
    ) throws ServiceException {
        // 1) Compute scale/offset
        PngWindRequestContext.BandInfo b1 = ctx.band1();
        PngWindRequestContext.BandInfo b2 = ctx.band2();
        final double uMin = b1.getMin();
        final double uMax = b1.getMax();
        final double vMin = b2.getMin();
        final double vMax = b2.getMax();
        final double uScale = computeScale(uMin, uMax);
        final double vScale;
        RenderedImage rgb;
        RenderedImage mask = MaskGenerator.createMask(sourceBands, b1.getNodata(), b2.getNodata());
        if (Math.abs(uMin - vMin) <= DELTA && Math.abs(uMax - vMax) <= DELTA) {
            // No need to compute separate scales when ranges are equal.
            // Optimize by quantizing both bands together and merging in one step.
            vScale = uScale;
            RenderedImage quantized = quantizeToByte(sourceBands, uMin, uScale);
            rgb = bandMerge(quantized, mask);

        } else {
            // Must compute separate scales and quantize separately:
            vScale = computeScale(vMin, vMax);
            RenderedImage u = new ImageWorker(sourceBands).retainBands(new int[]{0}).getRenderedImage();
            RenderedImage v = new ImageWorker(sourceBands).retainBands(new int[]{1}).getRenderedImage();

            // Quantize each band to byte:
            RenderedImage uByte = quantizeToByte(u, uMin, uScale);
            RenderedImage vByte = quantizeToByte(v, vMin, vScale);

            // Merge into RGB: R=uByte, G=vByte, B=mask
            rgb = bandMerge(uByte, vByte, mask);
        }

        // 6) Metadata map to pass to PNG encoder
        Map<String, String> md = new LinkedHashMap<>();
        md.put("format", FORMAT);
        md.put("version", VERSION);

        md.put("wind_b1_name", b1.getName());
        md.put("wind_b1_scale", Double.toString(uScale));
        md.put("wind_b1_offset", Double.toString(uMin));
        md.put("wind_b1_uom", b1.getUom());

        md.put("wind_b2_name", b2.getName());
        md.put("wind_b2_scale", Double.toString(vScale));
        md.put("wind_b2_offset", Double.toString(vMin));
        md.put("wind_b2_uom", b2.getUom());

        GetMapRequest request = ctx.getRequest();
        Envelope envelope = ctx.getEnvelope();
        CoordinateReferenceSystem crs = request.getCrs();

        Integer code;
        try {
            code = CRS.lookupEpsgCode(crs, true);
        } catch (FactoryException e) {
            throw new ServiceException("Exception occurred while looking for EpsgCode for provided CRS:" + crs, e);
        }
        if (code != null) {
            md.put("CRS", "EPSG:" + code);
        }
        md.put("bbox", String.format("%f,%f,%f,%f", envelope.getMinX(), envelope.getMinY(), envelope.getMaxX(), envelope.getMaxY()));
        /*if (timeOrNull != null) md.put("time", timeOrNull);
        if (elevationOrNull != null) md.put("elevation", elevationOrNull);
        */
        return new PngWindQuantizedImage(rgb, md);
    }

    private static double computeScale(double min, double max) {
        double span = max - min;
        return (span == 0.0) ? 1.0 : (span / 255.0);
    }

    private static RenderedImage quantizeToByte(RenderedImage src, double offset, double scale) {
        double factor = 1.0 / scale;
        double add = -offset / scale;

        ImageWorker w = new ImageWorker(src);

        // rescale: dst = src * factor + add
        w.rescale(new double[]{factor}, new double[]{add});
        w.format(DataBuffer.TYPE_BYTE);

        return w.getRenderedImage();
    }

    /** Merge Quantized bands and mask into a 3-band image (RGB-like). */
    private static RenderedImage bandMerge(RenderedImage...sources) {
        if (sources == null || sources.length < 2) {
            throw new IllegalArgumentException("At least two images are required for bandMerge");
        }
        ParameterBlockImageN pb = new ParameterBlockImageN("BandMerge");
        for (RenderedImage src : sources) {
            pb.addSource(src);
        }

        // Setting up the layout for the output image to create a byte RGB image.
        RenderingHints hints = getRGBHints(sources[0]);
        return ImageN.create("BandMerge", pb, hints);
    }

    private static RenderingHints getRGBHints(RenderedImage source) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        ImageLayout layout = new ImageLayout();
        layout.setHeight(w);
        layout.setWidth(h);
        layout.setTileWidth(source.getTileWidth());
        layout.setTileHeight(source.getTileHeight());
        ColorModel cm = new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        layout.setColorModel(cm);
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        layout.setSampleModel(sm);
        return new RenderingHints(ImageN.KEY_IMAGE_LAYOUT, layout);
    }
}