/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static java.util.Locale.ENGLISH;

import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Optional;
import javax.media.jai.Histogram;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.image.ImageWorker;
import org.geotools.process.classify.ClassificationMethod;
import org.geotools.process.raster.classify.Classification;
import org.geotools.processing.jai.ClassBreaksDescriptor;
import org.geotools.processing.jai.ClassBreaksRIF;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.StyleFactory;
import org.geotools.util.Converters;
import org.geotools.util.factory.GeoTools;
import org.opengis.filter.FilterFactory2;

public class RasterSymbolizerBuilder {

    private static FilterFactory2 FF =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private static StyleFactory SF =
            CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());

    public static final int USHORT_MAX_VALUE = 65535;
    static final int MAX_UNIQUE_VALUES =
            Integer.getInteger("org.geoserver.sldService.maxUniqueRange", USHORT_MAX_VALUE);

    /**
     * Builds a {@link ColorMap} of type "values" from the unique values in the raster
     *
     * @param image The source image
     * @param maxIntervals The maximum number of intervals that should be returned, above which an
     *     exception should be thrown
     */
    public ColorMap uniqueIntervalClassification(RenderedImage image, Integer maxIntervals) {
        // compute min and max, for the common case avoid doing an extrema a use a pre-defined range
        // instead
        int low, high;
        int dataType = image.getSampleModel().getDataType();
        ImageWorker iw = new ImageWorker(image);
        switch (dataType) {
            case DataBuffer.TYPE_BYTE:
                low = 0;
                high = 255;
                break;
            case DataBuffer.TYPE_USHORT:
                low = 0;
                high = 65535;
                break;
            case DataBuffer.TYPE_SHORT:
                low = Short.MIN_VALUE;
                high = Short.MAX_VALUE;
                break;
            case DataBuffer.TYPE_INT:
                low = (int) iw.getMinimums()[0];
                high = (int) iw.getMaximums()[0];
                break;
            default:
                throw new IllegalArgumentException(
                        "Cannot perform unique value classification over rasters of float type, only integer numbers are supported. Try a classification by intervals or quantiles instead");
        }
        if (high - low > MAX_UNIQUE_VALUES) {
            throw new IllegalArgumentException(
                    "Cannot perform unique value classification over rasters with a potential range of values greater than "
                            + MAX_UNIQUE_VALUES
                            + ", this raster could have up to "
                            + (high - low));
        }

        // compute the histogram
        Histogram histogram =
                iw.getHistogram(
                        new int[] {high - low + 1}, new double[] {low}, new double[] {high});
        int[] bins = histogram.getBins(0);

        // turn the histogram into a ColorMap (just values, no colors, those will be added later)
        ColorMap colorMap = SF.createColorMap();
        colorMap.setType(ColorMap.TYPE_VALUES);
        int entries = 0;
        for (int i = 0; i < bins.length; i++) {
            if (bins[i] > 0) {
                ColorMapEntry entry = SF.createColorMapEntry();
                int value = i + low;
                entry.setQuantity(FF.literal(value));
                entry.setLabel(String.valueOf(value));
                colorMap.addColorMapEntry(entry);
                entries++;
            }
        }

        if (maxIntervals != null && entries > maxIntervals && maxIntervals > 0) {
            throw new IllegalArgumentException(
                    "Found "
                            + entries
                            + " unique values, but a maximum of "
                            + maxIntervals
                            + " was requested");
        }

        return colorMap;
    }

    /**
     * Builds a {@link ColorMap} based on equal intervals between the min and max value found in the
     * raster
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param open
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap equalIntervalClassification(
            RenderedImage image, int intervals, boolean open, boolean continuous) {
        ImageWorker iw = new ImageWorker(image);
        double low = iw.getMinimums()[0];
        double high = iw.getMaximums()[0];

        Number[] breaks = new Number[continuous ? intervals : intervals + 1];
        double step = (high - low) / (continuous ? (intervals - 1) : intervals);
        for (int i = 0; i < breaks.length; i++) {
            double value = i * step + low;
            breaks[i] = value;
        }

        return getColorMapFromBreaks(breaks, open, continuous);
    }

    /**
     * Builds a {@link ColorMap} based on equal pixel count intervals
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param open
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap quantileClassification(
            RenderedImage image, Integer intervals, boolean open, boolean continuous) {
        Number[] breaks =
                getClassificationBreaks(
                        image,
                        continuous ? intervals - 1 : intervals,
                        ClassificationMethod.QUANTILE,
                        1,
                        1);
        return getColorMapFromBreaks(breaks, open, continuous);
    }

    /**
     * Builds a {@link ColorMap} based on equal pixel count intervals
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param open
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap jenksClassification(
            RenderedImage image, Integer intervals, boolean open, boolean continuous) {
        Number[] breaks =
                getClassificationBreaks(
                        image,
                        continuous ? intervals - 1 : intervals,
                        ClassificationMethod.NATURAL_BREAKS,
                        1,
                        1);
        return getColorMapFromBreaks(breaks, open, continuous);
    }

    private ColorMap getColorMapFromBreaks(Number[] breaks, boolean open, boolean continuous) {
        // turn the histogram into a ColorMap (just values, no colors, those will be added later)
        DecimalFormat format = new DecimalFormat("#.######", new DecimalFormatSymbols(ENGLISH));

        ColorMap colorMap = SF.createColorMap();
        if (continuous) {
            for (Number b : breaks) {
                ColorMapEntry entry = SF.createColorMapEntry();
                entry.setQuantity(FF.literal(b));
                entry.setLabel(format.format(b));
                colorMap.addColorMapEntry(entry);
            }
        } else {
            colorMap.setType(ColorMap.TYPE_INTERVALS);
            if (open) {
                double prev = breaks[0].doubleValue();
                for (int i = 1; i < breaks.length; i++) {
                    ColorMapEntry entry = SF.createColorMapEntry();
                    double value = breaks[i].doubleValue();
                    if (i == breaks.length - 1) {
                        long l = Double.doubleToLongBits(value);
                        double incremented = Double.longBitsToDouble(l + 1);
                        entry.setQuantity(FF.literal(incremented));
                    } else {
                        entry.setQuantity(FF.literal(value));
                    }
                    if (i == 1) {
                        entry.setLabel("< " + format.format(value));
                    } else if (i == breaks.length - 1) {
                        entry.setLabel(">= " + format.format(prev));
                    } else {
                        entry.setLabel(
                                ">= " + format.format(prev) + " AND < " + format.format(value));
                    }

                    prev = value;
                    colorMap.addColorMapEntry(entry);
                }
            } else {
                // build a transparenty entry as first
                double prev = breaks[0].doubleValue();
                ColorMapEntry transparentEntry = SF.createColorMapEntry();
                transparentEntry.setColor(FF.literal(new Color(0, 0, 0)));
                transparentEntry.setOpacity(FF.literal(0));
                transparentEntry.setQuantity(FF.literal(prev));
                colorMap.addColorMapEntry(transparentEntry);

                for (int i = 1; i < breaks.length; i++) {
                    ColorMapEntry entry = SF.createColorMapEntry();
                    double value = breaks[i].doubleValue();
                    if (i == breaks.length - 1) {
                        long l = Double.doubleToLongBits(value);
                        double incremented = Double.longBitsToDouble(l + 1);
                        entry.setQuantity(FF.literal(incremented));
                    } else {
                        entry.setQuantity(FF.literal(value));
                    }
                    if (i == breaks.length - 1) {
                        entry.setLabel(
                                ">= " + format.format(prev) + " AND <= " + format.format(value));
                    } else {
                        entry.setLabel(
                                ">= " + format.format(prev) + " AND < " + format.format(value));
                    }

                    prev = value;
                    colorMap.addColorMapEntry(entry);
                }
            }
        }
        return colorMap;
    }

    private Number[] getClassificationBreaks(
            RenderedImage image,
            Integer intervals,
            ClassificationMethod classificationMethod,
            int xPeriod,
            int yPeriod) {
        // used to extract some properties from the image
        ImageWorker iw = new ImageWorker(image);
        Double noData =
                Optional.ofNullable(iw.getNoData()).map(r -> r.getMin().doubleValue()).orElse(null);

        // setup the call to the operation and create it
        ParameterBlock pb = new ParameterBlock();
        pb.addSource(image);
        pb.set(intervals, 0);
        pb.set(classificationMethod, 1);
        pb.set(null, 2); /* extrema, no need to precompute for the methods we're using*/
        pb.set(iw.getROI(), 3);
        pb.set(new Integer[] {0}, 4); /* band, it was pre-selected */
        pb.set(xPeriod, 5); /* xPeriod */
        pb.set(yPeriod, 6); /* yPeriod */
        pb.set(noData, 7);
        // direct calls as there are some issues with the JAI op registration, at least in Tomcat
        RenderedImage op = new ClassBreaksRIF().create(pb, null);

        // actually extract the classification and its breaks
        Classification c =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        return c.getBreaks()[0];
    }

    /** Applies the given color ramp to the color map */
    public void applyColorRamp(
            ColorMap colorMap, ColorRamp colorRamp, boolean skipFirst, boolean reverse)
            throws Exception {
        int offset = skipFirst ? 1 : 0; // skip the transparent first entry in the closed case

        ColorMapEntry[] entries = colorMap.getColorMapEntries();
        colorRamp.setNumClasses(entries.length - offset);
        if (reverse) {
            colorRamp.revert();
        }
        List<Color> colors = colorRamp.getRamp();
        for (int i = 0; i < entries.length - offset; i++) {
            ColorMapEntry entry = entries[offset + i];
            Color color = colors.get(i);
            entry.setColor(FF.literal(color));
        }
    }

    /**
     * Builds a ColorMap based on a user specified set of values
     *
     * @param continous
     * @param classifier
     * @param b
     * @return
     */
    public ColorMap createCustomColorMap(
            RangedClassifier classifier, boolean open, boolean continuous) {
        Number[] breaks = new Number[classifier.getSize() + (continuous ? 0 : 1)];
        breaks[0] = Converters.convert(classifier.getMin(0), Double.class);
        for (int i = 0; i < breaks.length - 1; i++) {
            breaks[i + 1] = Converters.convert(classifier.getMax(i), Double.class);
        }

        return getColorMapFromBreaks(breaks, open, continuous);
    }
}
