/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static java.util.Locale.ENGLISH;

import it.geosolutions.jaiext.JAIExt;
import it.geosolutions.jaiext.classbreaks.ClassBreaksDescriptor;
import it.geosolutions.jaiext.classbreaks.ClassBreaksRIF;
import it.geosolutions.jaiext.classbreaks.Classification;
import it.geosolutions.jaiext.classbreaks.ClassificationMethod;
import it.geosolutions.jaiext.stats.Statistics;
import it.geosolutions.jaiext.stats.Statistics.StatsType;
import java.awt.*;
import java.awt.image.DataBuffer;
import java.awt.image.RenderedImage;
import java.awt.image.renderable.ParameterBlock;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.List;
import java.util.Optional;
import java.util.stream.DoubleStream;
import java.util.stream.IntStream;
import javax.media.jai.*;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.image.ImageWorker;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.StyleFactory;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.GeoTools;
import org.opengis.filter.FilterFactory2;

public class RasterSymbolizerBuilder {

    private static FilterFactory2 FF =
            CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    private static StyleFactory SF =
            CommonFactoryFinder.getStyleFactory(GeoTools.getDefaultHints());

    /** Number of histogram bins, if not specified via system variable */
    private static final int NUM_HISTOGRAM_BINS =
            Integer.getInteger("org.geoserver.sldService.histogramBins", 256);

    /**
     * Maximum number of values collected by the unique value classifier, if not specified via
     * system variable
     */
    private static final int MAX_UNIQUE_VALUES =
            Integer.getInteger("org.geoserver.sldService.maxUniqueRange", 1024);

    /**
     * Maximum number of pixels read, operations will use subsampling to stay below it. The default
     * value is the pixels of a 2048*2048 image
     */
    public static final int DEFAULT_MAX_PIXELS =
            Integer.getInteger("org.geoserver.sldService.maxPixels", 4194304);

    private long maxPixels;
    private Double standardDeviations;
    private boolean outputPercentages;
    private Integer percentagesScale;

    /**
     * Builds the {@link RasterSymbolizerBuilder} with a given pixel reading threshold before
     * starting to recur to subsampling
     */
    public RasterSymbolizerBuilder(int maxPixels) {
        if (maxPixels <= 0) {
            throw new IllegalArgumentException(
                    "The maximum number of pixels to be read should be a positive number");
        }
        this.maxPixels = maxPixels;
    }

    /** Default constructor */
    public RasterSymbolizerBuilder() {
        this.maxPixels = DEFAULT_MAX_PIXELS;
    }

    public RasterSymbolizerBuilder(boolean outputPercentages, Integer percentagesScale) {
        this.maxPixels = DEFAULT_MAX_PIXELS;
        this.outputPercentages = outputPercentages;
        this.percentagesScale = percentagesScale;
    }

    /**
     * Builds a {@link ColorMap} of type "values" from the unique values in the raster
     *
     * @param image The source image
     * @param maxIntervals The maximum number of intervals that should be returned, above which an
     *     exception should be thrown
     */
    public ColorMap uniqueIntervalClassification(RenderedImage image, Integer maxIntervals) {
        int low, high;
        int dataType = image.getSampleModel().getDataType();
        ImageWorker iw = getImageWorker(image);

        // compute min and max, for the common case avoid doing an extrema or use a pre-defined
        // range instead
        if (dataType == DataBuffer.TYPE_BYTE && standardDeviations == null) {
            low = 0;
            high = 255;
        } else if (dataType == DataBuffer.TYPE_DOUBLE || dataType == DataBuffer.TYPE_FLOAT) {
            throw new IllegalArgumentException(
                    "Cannot perform unique value classification over rasters of float type, only integer numbers are supported. Try a classification by intervals or quantiles instead");
        } else {
            final NumberRange range = getOperationRange(iw);
            low = (int) range.getMinimum();
            high = (int) range.getMaximum();
        }

        // The histogram can be very expensive memory wise as it's backed by a
        // AtomicDouble[], check how many are they going to be
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
        PercentagesRoundHandler roundHandler = new PercentagesRoundHandler(percentagesScale);
        for (int i = 0; i < bins.length; i++) {
            if (bins[i] > 0) {
                ColorMapEntry entry = SF.createColorMapEntry();
                int value = i + low;
                entry.setQuantity(FF.literal(value));
                entry.setLabel(String.valueOf(value));
                colorMap.addColorMapEntry(entry);
                if (outputPercentages) {
                    double total = IntStream.of(bins).sum();
                    double classMembers = bins[i];
                    double percentage = roundHandler.roundDouble((classMembers / total) * 100);
                    StringBuilder sb =
                            new StringBuilder(entry.getLabel())
                                    .append(" (")
                                    .append(percentage)
                                    .append("%)");
                    entry.setLabel(sb.toString());
                }
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
     * Builds a ImageWorker with subsampling factors suitable to respect the configured max pixels
     */
    ImageWorker getImageWorker(RenderedImage image) {
        ImageWorker iw = new ImageWorker(image);

        // check if subsampling is needed
        long pixels = image.getWidth() * (long) image.getHeight();
        if (pixels > maxPixels) {
            // try to get as many pixels as possible, don't jump from 1M to 250k. Prefer skipping
            // rows rather than cols (for the not so uncommon striped raster to be classified)
            int yPeriod = (int) Math.round(Math.sqrt(pixels / (double) maxPixels));
            int xPeriod = (int) Math.ceil(pixels / (yPeriod * (double) maxPixels));
            iw.setXPeriod(xPeriod).setYPeriod(yPeriod);
        }

        return iw;
    }

    /**
     * Builds a {@link ColorMap} based on equal intervals between the min and max value found in the
     * raster
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap equalIntervalClassification(
            RenderedImage image, int intervals, boolean open, boolean continuous) {
        ImageWorker iw = getImageWorker(image);
        final NumberRange range = getOperationRange(iw);
        double low = (int) range.getMinimum();
        double high = (int) range.getMaximum();
        Number[] breaks = new Number[continuous ? intervals : intervals + 1];
        double step = (high - low) / (continuous ? (intervals - 1) : intervals);
        for (int i = 0; i < breaks.length; i++) {
            double value = i * step + low;
            breaks[i] = value;
        }
        double[] percentages = null;
        if (outputPercentages) {
            percentages = computePercentagesFromHistogram(iw, intervals, low, high);
            percentages =
                    new PercentagesRoundHandler(percentagesScale).roundPercentages(percentages);
        }
        return getColorMapFromBreaks(breaks, open, continuous, percentages);
    }

    /**
     * Builds a {@link ColorMap} based on equal pixel count intervals
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap quantileClassification(
            RenderedImage image, Integer intervals, boolean open, boolean continuous) {
        Classification c =
                getClassificationBreaks(
                        image,
                        continuous ? intervals - 1 : intervals,
                        ClassificationMethod.QUANTILE,
                        NUM_HISTOGRAM_BINS);
        double[] percentages = c.getPercentages();
        if (outputPercentages)
            percentages =
                    new PercentagesRoundHandler(percentagesScale).roundPercentages(percentages);
        return getColorMapFromBreaks(c.getBreaks()[0], open, continuous, percentages);
    }

    /**
     * Builds a {@link ColorMap} based on equal pixel count intervals
     *
     * @param image The source image
     * @param intervals Number of resulting intervals
     * @param continuous If the resulting ColorMap should be of type interval (discrete) or of type
     */
    public ColorMap jenksClassification(
            RenderedImage image, Integer intervals, boolean open, boolean continuous) {
        Classification c =
                getClassificationBreaks(
                        image,
                        continuous ? intervals - 1 : intervals,
                        ClassificationMethod.NATURAL_BREAKS,
                        NUM_HISTOGRAM_BINS);
        Number[] breaks = c.getBreaks()[0];
        double[] percentages = c.getPercentages();
        if (outputPercentages)
            percentages =
                    new PercentagesRoundHandler(percentagesScale).roundPercentages(percentages);
        return getColorMapFromBreaks(breaks, open, continuous, percentages);
    }

    private ColorMap getColorMapFromBreaks(
            Number[] breaks, boolean open, boolean continuous, double[] percentages) {
        // turn the histogram into a ColorMap (just values, no colors, those will be added later)
        DecimalFormat format = new DecimalFormat("#.######", new DecimalFormatSymbols(ENGLISH));

        ColorMap colorMap = SF.createColorMap();
        if (continuous) {
            for (int i = 0; i < breaks.length; i++) {
                Number b = breaks[i];
                ColorMapEntry entry = SF.createColorMapEntry();
                entry.setQuantity(FF.literal(b));
                entry.setLabel(format.format(b) + getPercentagesLabelPortion(percentages, i));
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
                        entry.setQuantity(FF.literal(Double.MAX_VALUE));
                    } else {
                        entry.setQuantity(FF.literal(value));
                    }
                    if (i == 1) {
                        entry.setLabel(
                                "< "
                                        + format.format(value)
                                        + getPercentagesLabelPortion(percentages, i - 1));
                    } else if (i == breaks.length - 1) {
                        entry.setLabel(
                                ">= "
                                        + format.format(prev)
                                        + getPercentagesLabelPortion(percentages, i - 1));
                    } else {
                        entry.setLabel(
                                ">= "
                                        + format.format(prev)
                                        + " AND < "
                                        + format.format(value)
                                        + getPercentagesLabelPortion(percentages, i - 1));
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
                                ">= "
                                        + format.format(prev)
                                        + " AND <= "
                                        + format.format(value)
                                        + getPercentagesLabelPortion(percentages, i - 1));
                    } else {
                        entry.setLabel(
                                ">= "
                                        + format.format(prev)
                                        + " AND < "
                                        + format.format(value)
                                        + getPercentagesLabelPortion(percentages, i - 1));
                    }

                    prev = value;
                    colorMap.addColorMapEntry(entry);
                }
            }
        }
        return colorMap;
    }

    private Classification getClassificationBreaks(
            RenderedImage image,
            Integer intervals,
            ClassificationMethod classificationMethod,
            int numHistogramBins) {
        // used to extract some properties from the image
        ImageWorker iw = getImageWorker(image);
        Double noData =
                Optional.ofNullable(iw.getNoData()).map(r -> r.getMin().doubleValue()).orElse(null);

        // setup the call to the operation and create it
        ParameterBlock pb = new ParameterBlockJAI("ClassBreaks");
        pb.addSource(image);
        pb.set(intervals, 0);
        pb.set(classificationMethod, 1);
        pb.set(iw.getROI(), 3);
        pb.set(new Integer[] {0}, 4); /* band, it was pre-selected */
        pb.set(iw.getXPeriod(), 5);
        pb.set(iw.getYPeriod(), 6);
        pb.set(noData, 7);
        if (numHistogramBins > 0) {
            final NumberRange range = getOperationRange(iw);
            Double[][] extrema = new Double[2][1];
            extrema[0][0] = range.getMinimum();
            extrema[1][0] = range.getMaximum();
            pb.set(extrema, 2);
            pb.set(true, 8);
            pb.set(numHistogramBins, 9);
        } else {
            pb.set(null, 2); /* extrema, no need to precompute for the methods we're using*/
        }
        pb.set(outputPercentages, 10);
        // direct calls as there are some issues with the JAI op registration, at least in Tomcat
        RenderedImage op = new ClassBreaksRIF().create(pb, null);
        // actually extract the classification and its breaks
        Classification c =
                (Classification) op.getProperty(ClassBreaksDescriptor.CLASSIFICATION_PROPERTY);
        return c;
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

    /** Builds a ColorMap based on a user specified set of values */
    public ColorMap createCustomColorMap(
            RenderedImage image, RangedClassifier classifier, boolean open, boolean continuous) {
        Number[] breaks = new Number[classifier.getSize() + (continuous ? 0 : 1)];
        breaks[0] = Converters.convert(classifier.getMin(0), Double.class);
        for (int i = 0; i < breaks.length - 1; i++) {
            breaks[i + 1] = Converters.convert(classifier.getMax(i), Double.class);
        }
        double[] percentages = null;
        if (outputPercentages) {
            percentages = getCustomClassifierPercentages(image, breaks);
            percentages =
                    new PercentagesRoundHandler(percentagesScale).roundPercentages(percentages);
        }
        return getColorMapFromBreaks(breaks, open, continuous, percentages);
    }

    public void setStandardDeviations(Double standardDeviations) {
        this.standardDeviations = standardDeviations;
    }

    private NumberRange getOperationRange(ImageWorker iw) {
        if (standardDeviations == null) {
            double min = iw.getMinimums()[0];
            double max = iw.getMaximums()[0];
            return new NumberRange(Double.class, min, max);
        } else {
            // Create the parameterBlock
            ParameterBlock pb = new ParameterBlock();
            pb.setSource(iw.getRenderedImage(), 0);
            if (JAIExt.isJAIExtOperation("Stats")) {
                StatsType[] stats =
                        new StatsType[] {StatsType.MEAN, StatsType.DEV_STD, StatsType.EXTREMA};

                // Image parameters
                pb.set(iw.getXPeriod(), 0); // xPeriod
                pb.set(iw.getYPeriod(), 1); // yPeriod
                pb.set(iw.getROI(), 2); // ROI
                pb.set(iw.getNoData(), 3); // NoData
                pb.set(stats, 6); // statistic operation
                final RenderedOp statsImage = JAI.create("Stats", pb, iw.getRenderingHints());
                // Retrieving the statistics
                Statistics[][] results =
                        (Statistics[][]) statsImage.getProperty(Statistics.STATS_PROPERTY);
                double mean = (double) results[0][0].getResult();
                double stddev = (double) results[0][1].getResult();
                double[] extrema = (double[]) results[0][2].getResult();
                double min = extrema[0];
                double max = extrema[1];
                // return a range centered in the mean with the desired number of standard
                // deviations, but make sure it does not exceed the data minimim and maximums
                return new NumberRange(
                        Double.class,
                        Math.max(mean - stddev * standardDeviations, min),
                        Math.min(mean + stddev * standardDeviations, max));
            } else {
                // the op should be unique to jai-ext but best be careful
                throw new IllegalArgumentException(
                        "Stats image operation is not backed by JAIExt, please enable JAIExt");
            }
        }
    }

    private String getPercentagesLabelPortion(double[] percentages, int i) {
        if (percentages == null) return "";
        else return " (" + percentages[i] + "%)";
    }

    private double[] getCustomClassifierPercentages(RenderedImage image, Number[] breaks) {
        ImageWorker iw = new ImageWorker(image);
        int classNum = breaks.length - 1;
        double classMembersAr[] = new double[classNum];
        for (int i = 0; i < classNum; i++) {
            double[] low = new double[] {(double) breaks[i]};
            double dHigh =
                    i != classNum - 1
                            ? Math.nextDown((double) breaks[i + 1])
                            : (double) breaks[i + 1];
            double[] high = new double[] {dHigh};
            Histogram hist = iw.getHistogram(new int[] {1}, low, high);
            classMembersAr[i] = hist.getBins(0)[0];
        }
        double total = DoubleStream.of(classMembersAr).sum();
        double[] percentages = new double[classNum];
        for (int i = 0; i < classNum; i++) {
            double classMembers = classMembersAr[i];
            if (classMembers != 0d && total != 0d) percentages[i] = (classMembers / total) * 100;
            else percentages[i] = 0d;
        }
        return percentages;
    }

    private double[] computePercentagesFromHistogram(
            ImageWorker iw, int intervals, double low, double high) {
        Histogram hist =
                iw.getHistogram(new int[] {intervals}, new double[] {low}, new double[] {high});
        int[] bins = hist.getBins(0);
        double[] percentages = new double[intervals];
        int total = IntStream.of(bins).sum();
        for (int i = 0; i < intervals; i++) {
            double classMembers = bins[i];
            if (classMembers != 0d && total != 0d) percentages[i] = (classMembers / total) * 100;
            else percentages[i] = 0d;
        }
        return percentages;
    }
}
