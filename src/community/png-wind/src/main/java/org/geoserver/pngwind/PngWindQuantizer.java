/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.pngwind;

import java.awt.RenderingHints;
import java.awt.Transparency;
import java.awt.color.ColorSpace;
import java.awt.image.ColorModel;
import java.awt.image.ComponentColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.SampleModel;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.eclipse.imagen.ImageLayout;
import org.eclipse.imagen.ImageN;
import org.eclipse.imagen.ParameterBlockImageN;
import org.geoserver.platform.ServiceException;
import org.geoserver.pngwind.config.PngWindConfig;
import org.geoserver.wms.GetMapRequest;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.image.ImageWorker;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.Envelope;

/**
 * Utility class to quantize wind data into a PNG image, and to generate the metadata map to be passed to the PNG
 * encoder.
 *
 * <p>The quantization process involves:
 *
 * <pre>
 *     1) Computing scale and offset for each band based on the provided min/max values.
 *     2) Quantizing each band to byte using the computed scale and offset.
 *     3) Generating a mask for nodata values.
 *     4) Merging the quantized bands and mask into a single RGB image (R=U, G=V, B=mask).
 *     5) Creating a metadata map containing the necessary information for decoding the quantized data from the PNG image.
 * </pre>
 */
public class PngWindQuantizer {

    private static final Logger LOGGER = Logging.getLogger(PngWindQuantizer.class);

    /** Container for the quantized image and its associated metadata. */
    public static class PngWindQuantizedImage {
        private final RenderedImage image;
        private final Map<String, String> metadata;

        public PngWindQuantizedImage(RenderedImage image, Map<String, String> metadata) {
            this.image = image;
            this.metadata = metadata;
        }

        public RenderedImage getImage() {
            return image;
        }

        public Map<String, String> getMetadata() {
            return metadata;
        }
    }

    private static final Double DELTA = 1E-18;

    private final PngWindConfig config;

    public PngWindQuantizer(PngWindConfig config) {
        this.config = config;
    }

    public PngWindQuantizedImage quantize(
            PngWindTransform.PngWindTransformResult windTransformResult, PngWindRequestContext ctx)
            throws ServiceException {

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Quantizing the image");
        }
        double uMin;
        double uMax;
        double vMin;
        double vMax;
        String b1Name = ctx.getBand1().getName();
        String b2Name = ctx.getBand2().getName();
        String b1Uom = ctx.getBand1().getUom();
        String b2Uom = ctx.getBand2().getUom();
        PngWindTransform.Kind kind = windTransformResult.getKind();

        PngWindRequestContext.BandInfo b1 = ctx.getBand1();
        PngWindRequestContext.BandInfo b2 = ctx.getBand2();

        if (kind == PngWindTransform.Kind.SPEED_DIR) {
            // For data coming from a speed/direction transformation we use the value from the speed band
            PngWindRequestContext.BandInfo speedBand =
                    config.getBandMatching().getSpeed().matches(PngWindTransform.normalize(b1Name)) ? b1 : b2;
            uMin = vMin = speedBand.getMin();
            uMax = vMax = speedBand.getMax();

            // since the original bands are not U and V, we set the names to U and V for clarity
            // and use the speed band uom for both
            b1Name = PngWindConstants.U;
            b2Name = PngWindConstants.V;
            b2Uom = b1Uom;
        } else {
            uMin = b1.getMin();
            uMax = b1.getMax();
            vMin = b2.getMin();
            vMax = b2.getMax();
        }

        // Compute scale/offset
        final double uScale = computeScale(uMin, uMax);
        final double vScale;
        RenderedImage rgb;
        RenderedImage sourceBands = windTransformResult.getUv();
        // Create the mask for nodata values (using the original bands to get the nodata values, which are not changed
        // by the UV transform)
        RenderedImage mask = PngWindMaskGenerator.createMask(sourceBands, b1.getNodata(), b2.getNodata());

        if ((Math.abs(uMin - vMin) <= DELTA && Math.abs(uMax - vMax) <= DELTA) && kind == PngWindTransform.Kind.UV) {
            // No need to compute separate scales when ranges are equal.
            // Optimize by quantizing both bands together and merging in one step.
            vScale = uScale;
            RenderedImage quantized = quantizeToByte(sourceBands, uMin, uScale);
            rgb = bandMerge(quantized, mask);

        } else {
            // Must compute separate scales and quantize separately:
            vScale = computeScale(vMin, vMax);
            int uBand = (kind != PngWindTransform.Kind.VU) ? 0 : 1;
            int vBand = (kind != PngWindTransform.Kind.VU) ? 1 : 0;

            RenderedImage u =
                    new ImageWorker(sourceBands).retainBands(new int[] {uBand}).getRenderedImage();
            RenderedImage v =
                    new ImageWorker(sourceBands).retainBands(new int[] {vBand}).getRenderedImage();

            // Quantize each band to byte:
            RenderedImage uByte = quantizeToByte(u, uMin, uScale);
            RenderedImage vByte = quantizeToByte(v, vMin, vScale);

            // Merge into RGB: R=uByte, G=vByte, B=mask
            rgb = bandMerge(uByte, vByte, mask);
        }

        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine("Preparing PNG Metadata");
        }
        // return Metadata map to pass to PNG encoder
        return setupMetadata(ctx, b1Name, uScale, uMin, b1Uom, b2Name, vScale, vMin, b2Uom, rgb);
    }

    private static PngWindQuantizedImage setupMetadata(
            PngWindRequestContext ctx,
            String b1Name,
            double b1Scale,
            double b1Offset,
            String b1Uom,
            String b2Name,
            double b2Scale,
            double b2Offset,
            String b2Uom,
            RenderedImage image) {
        Map<String, String> md = new LinkedHashMap<>();
        md.put("format", PngWindConstants.FORMAT);
        md.put("version", PngWindConstants.VERSION);

        md.put("wind_b1_name", b1Name);
        md.put("wind_b1_scale", Double.toString(b1Scale));
        md.put("wind_b1_offset", Double.toString(b1Offset));
        md.put("wind_b1_uom", b1Uom);

        md.put("wind_b2_name", b2Name);
        md.put("wind_b2_scale", Double.toString(b2Scale));
        md.put("wind_b2_offset", Double.toString(b2Offset));
        md.put("wind_b2_uom", b2Uom);

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
        md.put(
                "bbox",
                String.format(
                        Locale.ENGLISH,
                        "%f,%f,%f,%f",
                        envelope.getMinX(),
                        envelope.getMinY(),
                        envelope.getMaxX(),
                        envelope.getMaxY()));

        List<Object> times = request.getTime();
        List<Object> elevations = request.getElevation();
        if (times != null && !times.isEmpty()) {
            md.put("time", toCsv(times));
        }

        if (elevations != null && !elevations.isEmpty()) {
            md.put("elevation", toCsv(elevations));
        }
        return new PngWindQuantizedImage(image, md);
    }

    private static double computeScale(double min, double max) {
        double span = max - min;
        return (span == 0.0) ? 1.0 : (span / 255.0);
    }

    private static RenderedImage quantizeToByte(RenderedImage src, double offset, double scale) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Quantizing with offset=" + offset + " and scale=" + scale);
        }
        double factor = 1.0 / scale;
        double add = -offset / scale;

        ImageWorker w = new ImageWorker(src);
        w.rescale(new double[] {factor}, new double[] {add});
        w.format(DataBuffer.TYPE_BYTE);

        return w.getRenderedImage();
    }

    /** Merge Quantized bands and mask into a 3-band image (RGB-like). */
    private static RenderedImage bandMerge(RenderedImage... sources) {
        if (LOGGER.isLoggable(Level.FINER)) {
            LOGGER.finer("Composing final RGB image (BandMerge) with " + sources.length + " bands");
        }
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

    /**
     * Create RenderingHints with an ImageLayout that defines a byte-based RGB color model and sample model, compatible
     * with the source image dimensions and tiling.
     */
    private static RenderingHints getRGBHints(RenderedImage source) {
        final int w = source.getWidth();
        final int h = source.getHeight();
        ImageLayout layout = new ImageLayout();
        layout.setHeight(w);
        layout.setWidth(h);
        layout.setTileWidth(source.getTileWidth());
        layout.setTileHeight(source.getTileHeight());
        ColorModel cm = new ComponentColorModel(
                ColorSpace.getInstance(ColorSpace.CS_sRGB), false, false, Transparency.OPAQUE, DataBuffer.TYPE_BYTE);
        layout.setColorModel(cm);
        SampleModel sm = cm.createCompatibleSampleModel(w, h);
        layout.setSampleModel(sm);
        return new RenderingHints(ImageN.KEY_IMAGE_LAYOUT, layout);
    }

    private static String toCsv(List<?> values) {
        return values.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
