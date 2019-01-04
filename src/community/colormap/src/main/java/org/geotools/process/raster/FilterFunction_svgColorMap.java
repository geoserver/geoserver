/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geotools.process.raster;

import static org.geotools.filter.capability.FunctionNameImpl.parameter;

import it.geosolutions.jaiext.classifier.LinearColorMap;
import it.geosolutions.jaiext.piecewise.TransformationException;
import java.awt.Color;
import java.awt.image.IndexColorModel;
import java.security.InvalidParameterException;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resource.Type;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.FunctionExpressionImpl;
import org.geotools.filter.capability.FunctionNameImpl;
import org.geotools.renderer.lite.gridcoverage2d.GradientColorMapGenerator;
import org.geotools.renderer.lite.gridcoverage2d.SLDColorMapBuilder;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.StyleFactory;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDTransformer;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.capability.FunctionName;

/**
 * Filter function to generate a {@link ColorMap} on top of an SVG file contained within the styles
 * data folder
 *
 * @author Daniele Romagnoli, GeoSolutions SAS
 */
public class FilterFunction_svgColorMap extends FunctionExpressionImpl {

    static final int LOG_SAMPLING_DEFAULT = 16;

    /**
     * Two colors are used for before and after palette, so this leaves a max of 254 colors to play
     * with
     */
    public static final int MAX_PALETTE_COLORS = 254;

    private static final Logger LOGGER = Logging.getLogger(FilterFunction_svgColorMap.class);

    public static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

    private static StyleFactory SF = CommonFactoryFinder.getStyleFactory();
    private static FilterFactory FF = CommonFactoryFinder.getFilterFactory();

    public static FunctionName NAME =
            new FunctionNameImpl(
                    "colormap",
                    parameter("colormap", ColorMap.class),
                    parameter("name", String.class),
                    parameter("min", Number.class),
                    parameter("max", Number.class),
                    parameter("beforeColor", String.class, 0, 1),
                    parameter("afterColor", String.class, 0, 1),
                    parameter("logarithmic", Boolean.class, 0, 1),
                    parameter("numcolors", Integer.class, 0, 1));

    public FilterFunction_svgColorMap() {
        super(NAME);
    }

    public Object evaluate(Object feature) {
        String colorMap = getParameters().get(0).evaluate(feature, String.class);
        double min = getParameters().get(1).evaluate(feature, Double.class).doubleValue();
        double max = getParameters().get(2).evaluate(feature, Double.class).doubleValue();
        String beforeColor = null;
        String afterColor = null;
        boolean logarithmic = false;
        int expressionCount = getParameters().size();
        int numColors = MAX_PALETTE_COLORS;
        if (expressionCount >= 4) {
            beforeColor = getParameters().get(3).evaluate(feature, String.class);
        }
        if (expressionCount >= 5) {
            afterColor = getParameters().get(4).evaluate(feature, String.class);
        }
        if (expressionCount >= 6) {
            Boolean log = getParameters().get(5).evaluate(feature, Boolean.class);
            if (log != null) {
                logarithmic = log;
            }
        }
        if (expressionCount >= 7) {
            Integer nc = getParameters().get(6).evaluate(feature, Integer.class);
            if (nc != null) {
                numColors = nc;
            }
        }
        return evaluate(colorMap, min, max, beforeColor, afterColor, logarithmic, numColors);
    }

    public Object evaluate(
            String colorMap,
            final double min,
            final double max,
            String beforeColor,
            String afterColor,
            boolean logarithmic,
            int numColors) {
        if (numColors < 1 || numColors > 254) {
            throw new InvalidParameterException(
                    "Number of colors must be comprised between 1 and 254");
        }
        GradientColorMapGenerator generator = null;
        Resource xmlFile = null;
        if (!colorMap.startsWith(GradientColorMapGenerator.RGB_INLINEVALUE_MARKER)
                && !colorMap.startsWith(GradientColorMapGenerator.RGBA_INLINEVALUE_MARKER)
                && !colorMap.startsWith(GradientColorMapGenerator.HEX_INLINEVALUE_MARKER)) {

            GeoServerResourceLoader loader =
                    GeoServerExtensions.bean(GeoServerResourceLoader.class);
            colorMap = colorMap.replace('\\', '/');

            String path = Paths.path("styles", "ramps", colorMap + ".svg");

            xmlFile = loader.get(path);
            if (xmlFile.getType() != Type.RESOURCE) {
                throw new IllegalArgumentException(
                        "The specified colorMap do not exist in the styles/ramps folder\n"
                                + "Check that "
                                + path
                                + " exists and is an .svg file");
            }
        }
        try {
            if (xmlFile != null) {
                generator = GradientColorMapGenerator.getColorMapGenerator(xmlFile.file());
            } else {
                generator = GradientColorMapGenerator.getColorMapGenerator(colorMap);
            }
            generator.setBeforeColor(beforeColor);
            generator.setAfterColor(afterColor);
            ColorMap cm;
            if (!logarithmic) {
                cm = generator.generateColorMap(min, max);
                if (numColors < MAX_PALETTE_COLORS) {
                    cm =
                            sampleColorMap(
                                    numColors,
                                    min,
                                    max,
                                    cm,
                                    Function.identity(),
                                    numColors < MAX_PALETTE_COLORS);
                }
            } else {
                if (min <= 0) {
                    throw new InvalidParameterException(
                            "Min range value must be positive in log scale mode");
                }
                double logMin = Math.log(min);
                double logMax = Math.log(max);
                ColorMap logcm = generator.generateColorMap(logMin, logMax);
                int colors = numColors < MAX_PALETTE_COLORS ? numColors : LOG_SAMPLING_DEFAULT;
                cm =
                        sampleColorMap(
                                colors,
                                logMin,
                                logMax,
                                logcm,
                                Math::exp,
                                numColors < MAX_PALETTE_COLORS);
            }
            if (LOGGER.isLoggable(Level.FINE)) {
                final SLDTransformer tx = new SLDTransformer();
                tx.setIndentation(2);
                String sld = tx.transform(cm);
                LOGGER.fine("Generated Colormap:\n " + sld);
            }
            return cm;
        } catch (Exception e) {
            // probably a type error
            throw new IllegalArgumentException("Filter Function problem for function colormap", e);
        }
    }

    private ColorMap sampleColorMap(
            int numColors,
            double min,
            double max,
            ColorMap sourceCM,
            Function<Double, Double> quantityMapper,
            boolean useIntervals)
            throws TransformationException {
        ColorMap cm;
        LinearColorMap lcm = toLinearColorMap(sourceCM);
        IndexColorModel icm = lcm.getColorModel();
        cm = SF.createColorMap();
        cm.addColorMapEntry(
                entryForValue(
                        min - Math.ulp(min), quantityMapper.apply(min), lcm, icm)); // before color
        double step = (max - min) / (numColors);
        // mind, the entry in interval mode defines the color up to that point
        for (int i = 0; i < (numColors - 1); i++) {
            double v = min + step * i;
            double mapValue = v;
            if (useIntervals) {
                mapValue = v + step;
            }
            cm.addColorMapEntry(entryForValue(v, quantityMapper.apply(mapValue), lcm, icm));
        }
        cm.addColorMapEntry(
                entryForValue(
                        max - Math.ulp(max), quantityMapper.apply(max), lcm, icm)); // last color
        if (useIntervals) {
            cm.setType(ColorMap.TYPE_INTERVALS);
            cm.addColorMapEntry(
                    entryForValue(max, Double.POSITIVE_INFINITY, lcm, icm)); // after color
        } else {
            cm.addColorMapEntry(
                    entryForValue(max, quantityMapper.apply(max), lcm, icm)); // after color
        }
        return cm;
    }

    private ColorMapEntry entryForValue(
            double value, double quantity, LinearColorMap lcm, IndexColorModel icm)
            throws TransformationException {
        ColorMapEntry entry = SF.createColorMapEntry();
        int position = (int) Math.round(lcm.transform(value));
        Color c = new Color(icm.getRed(position), icm.getGreen(position), icm.getBlue(position));
        entry.setColor(FF.literal(c));
        int alpha = icm.getAlpha(position);
        if (alpha < 255) {
            entry.setOpacity(FF.literal(alpha / 255.));
        }
        entry.setQuantity(FF.literal(quantity));
        return entry;
    }

    private LinearColorMap toLinearColorMap(ColorMap cm) {
        final SLDColorMapBuilder builder = new SLDColorMapBuilder();
        final ColorMapEntry[] entries = cm.getColorMapEntries();
        builder.setLinearColorMapType(ColorMap.TYPE_RAMP).setNumberColorMapEntries(entries.length);
        for (int i = 0; i < entries.length; i++) {
            builder.addColorMapEntry(entries[i]);
        }
        LinearColorMap lcm = builder.buildLinearColorMap();
        return lcm;
    }
}
