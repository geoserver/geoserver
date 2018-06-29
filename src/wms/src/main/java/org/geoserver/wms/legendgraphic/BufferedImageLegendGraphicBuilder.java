/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.IndexColorModel;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.data.DataUtilities;
import org.geotools.data.Parameter;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.GeometryDescriptorImpl;
import org.geotools.feature.type.GeometryTypeImpl;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.process.Processors;
import org.geotools.process.function.ProcessFunction;
import org.geotools.renderer.lite.MetaBufferEstimator;
import org.geotools.renderer.lite.RendererUtilities;
import org.geotools.renderer.lite.StyledShapePainter;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DpiRescaleStyleVisitor;
import org.geotools.styling.visitor.RescaleStyleVisitor;
import org.geotools.styling.visitor.UomRescaleStyleVisitor;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.LinearRing;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.Feature;
import org.opengis.feature.IllegalAttributeException;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.feature.type.GeometryType;
import org.opengis.feature.type.Name;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.style.GraphicLegend;
import org.springframework.util.StringUtils;

/**
 * Template {@linkPlain org.vfny.geoserver.responses.wms.GetLegendGraphicProducer} based on <a
 * href="http://svn.geotools.org/geotools/trunk/gt/module/main/src/org/geotools/renderer/lite/StyledShapePainter.java">
 * GeoTools StyledShapePainter</a> that produces a BufferedImage with the appropriate legend graphic
 * for a given GetLegendGraphic WMS request.
 *
 * <p>It should be enough for a subclass to implement {@linkPlain
 * org.vfny.geoserver.responses.wms.GetLegendGraphicProducer#writeTo(OutputStream)} and <code>
 * getContentType()</code> in order to encode the BufferedImage produced by this class to the
 * appropriate output format.
 *
 * <p>This class takes literally the fact that the arguments <code>WIDTH</code> and <code>HEIGHT
 * </code> are just <i>hints</i> about the desired dimensions of the produced graphic, and the need
 * to produce a legend graphic representative enough of the SLD style for which it is being
 * generated. Thus, if no <code>RULE</code> parameter was passed and the style has more than one
 * applicable Rule for the actual scale factor, there will be generated a legend graphic of the
 * specified width, but with as many stacked graphics as applicable rules were found, providing by
 * this way a representative enough legend.
 *
 * @author Gabriel Roldan
 * @author Simone Giannecchini, GeoSolutions SAS
 * @version $Id$
 */
public class BufferedImageLegendGraphicBuilder {
    Logger LOGGER = Logger.getLogger("org.geoserver.wms.legendgraphic");

    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;

    /**
     * Singleton shape painter to serve all legend requests. We can use a single shape painter
     * instance as long as it remains thread safe.
     */
    private static final StyledShapePainter shapePainter = new StyledShapePainter();

    /** used to create sample point shapes with LiteShape (not lines nor polygons) */
    private static final GeometryFactory geomFac = new GeometryFactory();

    /**
     * Just a holder to avoid creating many line shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;

    /**
     * Just a holder to avoid creating many point shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 samplePoint;

    /**
     * Default minimum size for symbols rendering. Can be overridden using LEGEND_OPTIONS
     * (minSymbolSize).
     */
    private final double MINIMUM_SYMBOL_SIZE = 3.0;

    /**
     * Default constructor. Subclasses may provide its own with a String parameter to establish its
     * desired output format, if they support more than one (e.g. a JAI based one)
     */
    public BufferedImageLegendGraphicBuilder() {
        super();
    }

    /**
     * Takes a GetLegendGraphicRequest and produces a BufferedImage that then can be used by a
     * subclass to encode it to the appropriate output format.
     *
     * @param request the "parsed" request, where "parsed" means that it's values are already
     *     validated so this method must not take care of verifying the requested layer exists and
     *     the like.
     * @throws ServiceException if there are problems creating a "sample" feature instance for the
     *     FeatureType <code>request</code> returns as the required layer (which should not occur).
     */
    public BufferedImage buildLegendGraphic(GetLegendGraphicRequest request)
            throws ServiceException {
        // list of images to be rendered for the layers (more than one if
        // a layer group is given)
        List<RenderedImage> layersImages = new ArrayList<RenderedImage>();

        List<LegendRequest> layers = request.getLegends();

        boolean forceLabelsOn = false;
        boolean forceLabelsOff = false;
        if (request.getLegendOptions().get("forceLabels") instanceof String) {
            String forceLabelsOpt = (String) request.getLegendOptions().get("forceLabels");
            if (forceLabelsOpt.equalsIgnoreCase("on")) {
                forceLabelsOn = true;
            } else if (forceLabelsOpt.equalsIgnoreCase("off")) {
                forceLabelsOff = true;
            }
        }

        boolean forceTitlesOff = false;
        if (request.getLegendOptions().get("forceTitles") instanceof String) {
            String forceTitlesOpt = (String) request.getLegendOptions().get("forceTitles");
            if (forceTitlesOpt.equalsIgnoreCase("off")) {
                forceTitlesOff = true;
            }
        }

        FeatureCountProcessor countProcessor = null;
        if (Boolean.TRUE.equals(
                request.getLegendOption(
                        GetLegendGraphicRequest.COUNT_MATCHED_KEY, Boolean.class))) {
            countProcessor = new FeatureCountProcessor(request);
        }

        for (LegendRequest legend : layers) {
            FeatureType layer = legend.getFeatureType();

            // style and rule to use for the current layer
            Style gt2Style = legend.getStyle();
            if (gt2Style == null) {
                throw new NullPointerException("request.getStyle()");
            }

            // get rule corresponding to the layer index
            // normalize to null for NO RULE
            String ruleName = legend.getRule(); // was null

            // width and height, we might have to rescale those in case of DPI usage
            int w = request.getWidth();
            int h = request.getHeight();

            // apply dpi rescale
            double dpi = RendererUtilities.getDpi(request.getLegendOptions());
            double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
            if (dpi != standardDpi) {
                double scaleFactor = dpi / standardDpi;
                w = (int) Math.round(w * scaleFactor);
                h = (int) Math.round(h * scaleFactor);
                DpiRescaleStyleVisitor dpiVisitor = new DpiRescaleStyleVisitor(scaleFactor);
                dpiVisitor.visit(gt2Style);
                gt2Style = (Style) dpiVisitor.getCopy();
            }
            // apply UOM rescaling if we have a scale
            if (request.getScale() > 0) {
                double pixelsPerMeters =
                        RendererUtilities.calculatePixelsPerMeterRatio(
                                request.getScale(), request.getLegendOptions());
                UomRescaleStyleVisitor rescaleVisitor = new UomRescaleStyleVisitor(pixelsPerMeters);
                rescaleVisitor.visit(gt2Style);
                gt2Style = (Style) rescaleVisitor.getCopy();
            }

            boolean strict = request.isStrict();

            final boolean transparent = request.isTransparent();
            RenderedImage titleImage = null;
            // if we have more than one layer, we put a title on top of each layer legend
            if (layers.size() > 1 && !forceTitlesOff) {
                titleImage = getLayerTitle(legend, w, h, transparent, request);
            }

            // Check for rendering transformation
            boolean hasVectorTransformation = false;
            boolean hasRasterTransformation = false;
            List<FeatureTypeStyle> ftsList = gt2Style.featureTypeStyles();
            for (int i = 0; i < ftsList.size(); i++) {
                FeatureTypeStyle fts = ftsList.get(i);
                Expression exp = fts.getTransformation();
                if (exp != null) {
                    ProcessFunction processFunction = (ProcessFunction) exp;
                    Name processName = processFunction.getProcessName();
                    Map<String, Parameter<?>> outputs = Processors.getResultInfo(processName, null);
                    if (outputs.isEmpty()) {
                        continue;
                    }
                    Parameter<?> output =
                            outputs.values()
                                    .iterator()
                                    .next(); // we assume there is only one output
                    if (SimpleFeatureCollection.class.isAssignableFrom(output.getType())) {
                        hasVectorTransformation = true;
                        break;
                    } else if (GridCoverage2D.class.isAssignableFrom(output.getType())) {
                        hasRasterTransformation = true;
                        break;
                    }
                }
            }

            final boolean buildRasterLegend =
                    (!strict && layer == null && LegendUtils.checkRasterSymbolizer(gt2Style))
                            || (LegendUtils.checkGridLayer(layer) && !hasVectorTransformation)
                            || hasRasterTransformation;

            // Just checks LegendInfo currently, should check gtStyle
            final boolean useProvidedLegend = layer != null && legend.getLayerInfo() != null;

            RenderedImage legendImage = null;
            if (useProvidedLegend) {
                legendImage = getLayerLegend(legend, w, h, transparent, request);
            }

            if (buildRasterLegend) {
                final RasterLayerLegendHelper rasterLegendHelper =
                        new RasterLayerLegendHelper(request, gt2Style, ruleName);
                final BufferedImage image = rasterLegendHelper.getLegend();
                if (image != null) {
                    if (titleImage != null) {
                        layersImages.add(titleImage);
                    }
                    layersImages.add(image);
                }
            } else if (useProvidedLegend && legendImage != null) {
                if (titleImage != null) {
                    layersImages.add(titleImage);
                }
                layersImages.add(legendImage);
            } else {
                final Feature sampleFeature;
                if (layer == null || hasVectorTransformation) {
                    sampleFeature = createSampleFeature();
                } else {
                    sampleFeature = createSampleFeature(layer);
                }
                final FeatureTypeStyle[] ftStyles =
                        gt2Style.featureTypeStyles().toArray(new FeatureTypeStyle[0]);
                final double scaleDenominator = request.getScale();

                Rule[] applicableRules;
                if (ruleName != null) {
                    Rule rule = LegendUtils.getRule(ftStyles, ruleName);
                    if (rule == null) {
                        throw new ServiceException(
                                "Specified style does not contains a rule named " + ruleName);
                    }
                    applicableRules = new Rule[] {rule};
                } else {
                    applicableRules = LegendUtils.getApplicableRules(ftStyles, scaleDenominator);
                }

                // do we have to alter the style to do context sensitive feature counts?
                if (countProcessor != null && !forceLabelsOff) {
                    applicableRules = updateRuleTitles(countProcessor, legend, applicableRules);
                }

                final NumberRange<Double> scaleRange =
                        NumberRange.create(scaleDenominator, scaleDenominator);
                final int ruleCount = applicableRules.length;

                /**
                 * A legend graphic is produced for each applicable rule. They're being held here
                 * until the process is done and then painted on a "stack" like legend.
                 */
                final List<RenderedImage> legendsStack = new ArrayList<RenderedImage>(ruleCount);

                final SLDStyleFactory styleFactory = new SLDStyleFactory();

                double minimumSymbolSize = MINIMUM_SYMBOL_SIZE;
                // get minSymbolSize from LEGEND_OPTIONS, if defined
                if (request.getLegendOptions().get("minSymbolSize") instanceof String) {
                    String minSymbolSizeOpt =
                            (String) request.getLegendOptions().get("minSymbolSize");
                    try {
                        minimumSymbolSize = Double.parseDouble(minSymbolSizeOpt);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                                "Invalid minSymbolSize value: should be a number");
                    }
                }
                // calculate the symbols rescaling factor necessary for them to be
                // drawn inside the icon box
                int defaultSize = Math.min(w, h);
                double[] minMax =
                        calcSymbolSize(
                                defaultSize,
                                minimumSymbolSize,
                                layer,
                                sampleFeature,
                                applicableRules);
                double actualMin = minMax[0];
                double actualMax = minMax[1];
                boolean rescalingRequired =
                        actualMin < minimumSymbolSize || actualMax > defaultSize;
                java.util.function.Function<Double, Double> rescaler = null;
                if (actualMax == actualMin
                        || ((actualMin / actualMax) * defaultSize) > minimumSymbolSize) {
                    rescaler = size -> (size / actualMax) * defaultSize;
                } else {
                    double finalMinimumSymbolSize = minimumSymbolSize;
                    rescaler =
                            size ->
                                    (size - actualMin)
                                                    / (actualMax - actualMin)
                                                    * (defaultSize - finalMinimumSymbolSize)
                                            + finalMinimumSymbolSize;
                }

                MetaBufferEstimator estimator = new MetaBufferEstimator(sampleFeature);
                for (int i = 0; i < ruleCount; i++) {

                    final RenderedImage image =
                            ImageUtils.createImage(w, h, (IndexColorModel) null, transparent);
                    final Map<RenderingHints.Key, Object> hintsMap =
                            new HashMap<RenderingHints.Key, Object>();
                    final Graphics2D graphics =
                            ImageUtils.prepareTransparency(
                                    transparent,
                                    LegendUtils.getBackgroundColor(request),
                                    image,
                                    hintsMap);
                    graphics.setRenderingHint(
                            RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                    Feature sample =
                            getSampleFeatureForRule(layer, sampleFeature, applicableRules[i]);

                    final Symbolizer[] symbolizers = applicableRules[i].getSymbolizers();
                    final GraphicLegend graphic = applicableRules[i].getLegend();

                    // If this rule has a legend graphic defined in the SLD, use it
                    if (graphic != null) {
                        if (this.samplePoint == null) {
                            Coordinate coord = new Coordinate(w / 2, h / 2);

                            try {
                                this.samplePoint =
                                        new LiteShape2(
                                                geomFac.createPoint(coord), null, null, false);
                            } catch (Exception e) {
                                this.samplePoint = null;
                            }
                        }
                        shapePainter.paint(
                                graphics, this.samplePoint, graphic, scaleDenominator, false);

                    } else {
                        for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                            Symbolizer symbolizer = symbolizers[sIdx];

                            if (symbolizer instanceof RasterSymbolizer) {
                                // skip it
                            } else {
                                // rescale symbols if needed
                                LiteShape2 shape = getSampleShape(symbolizer, w, h, w, h);
                                if (rescalingRequired
                                        && (symbolizer instanceof PointSymbolizer
                                                || symbolizer instanceof LineSymbolizer)) {
                                    double size =
                                            getSymbolizerSize(
                                                    estimator, symbolizer, Math.min(w, h) - 4);
                                    double newSize = rescaler.apply(size);
                                    symbolizer = rescaleSymbolizer(symbolizer, size, newSize);
                                } else if (symbolizer instanceof PolygonSymbolizer) {
                                    // need to make room for the stroke in the symbol, thus, a
                                    // smaller rect
                                    double symbolizerSize =
                                            getSymbolizerSize(estimator, symbolizer, 0);
                                    int rescaledWidth =
                                            (int)
                                                    Math.ceil(
                                                            Math.max(
                                                                    minimumSymbolSize,
                                                                    w - symbolizerSize));
                                    int rescaledHeight =
                                            (int)
                                                    Math.ceil(
                                                            Math.max(
                                                                    minimumSymbolSize,
                                                                    h - symbolizerSize));
                                    shape =
                                            getSampleShape(
                                                    symbolizer,
                                                    rescaledWidth,
                                                    rescaledHeight,
                                                    w,
                                                    h);

                                    symbolizer =
                                            rescaleSymbolizer(
                                                    symbolizer, w, (double) rescaledWidth);
                                }

                                Style2D style2d =
                                        styleFactory.createStyle(sample, symbolizer, scaleRange);
                                if (style2d != null) {
                                    shapePainter.paint(graphics, shape, style2d, scaleDenominator);
                                }
                            }
                        }
                    }
                    if (image != null && titleImage != null) {
                        layersImages.add(titleImage);
                        titleImage = null;
                    }
                    legendsStack.add(image);
                    graphics.dispose();
                }
                int labelMargin = 3;
                if (!StringUtils.isEmpty(request.getLegendOptions().get("labelMargin"))) {
                    labelMargin =
                            Integer.parseInt(
                                    request.getLegendOptions().get("labelMargin").toString());
                }
                LegendMerger.MergeOptions options =
                        LegendMerger.MergeOptions.createFromRequest(
                                legendsStack,
                                0,
                                0,
                                0,
                                labelMargin,
                                request,
                                forceLabelsOn,
                                forceLabelsOff,
                                forceTitlesOff);
                if (ruleCount > 0) {
                    BufferedImage image =
                            LegendMerger.mergeLegends(applicableRules, request, options);

                    if (image != null) {
                        layersImages.add(image);
                    }
                }
            }
        }
        // all legend graphics are merged if we have a layer group
        BufferedImage finalLegend =
                mergeGroups(
                        layersImages, null, request, forceLabelsOn, forceLabelsOff, forceTitlesOff);
        if (finalLegend == null) {
            throw new IllegalArgumentException("no legend passed");
        }
        return finalLegend;
    }

    public Symbolizer rescaleSymbolizer(Symbolizer symbolizer, double size, double newSize) {
        // perform a unit-less rescale
        double scaleFactor = newSize / size;
        RescaleStyleVisitor rescaleVisitor =
                new RescaleStyleVisitor(scaleFactor) {
                    @Override
                    protected Expression rescale(Expression expr) {
                        if (expr == null) {
                            return null;
                        } else if (expr instanceof Literal) {
                            Double value = expr.evaluate(null, Double.class);
                            return ff.literal(value * scaleFactor);
                        } else {
                            return ff.multiply(expr, ff.literal(scaleFactor));
                        }
                    }
                };
        symbolizer.accept(rescaleVisitor);
        symbolizer = (Symbolizer) rescaleVisitor.getCopy();
        return symbolizer;
    }

    protected Rule[] updateRuleTitles(
            FeatureCountProcessor processor, LegendRequest legend, Rule[] applicableRules) {
        return processor.preProcessRules(legend, applicableRules);
    }

    /**
     * Calculates a global rescaling factor for all the symbols to be drawn in the given rules. This
     * is to be sure all symbols are drawn inside the given w x h box.
     *
     * @param width horizontal constraint
     * @param height vertical constraint
     * @param featureType FeatureType to be used for size extraction in expressions (used to create
     *     a sample if feature is null)
     * @param feature Feature to be used for size extraction in expressions (if null a sample
     *     Feature will be created from featureType)
     * @param rules set of rules to scan for symbols
     * @param minimumSymbolSize lower constraint for the symbols size
     */
    private double[] calcSymbolSize(
            double defaultMaxSize,
            double defaultMinSize,
            FeatureType featureType,
            Feature feature,
            final Rule[] rules) {
        // check for max and min size in rendered symbols
        double minSize = defaultMaxSize;
        double maxSize = defaultMinSize;

        final int ruleCount = rules.length;
        for (int i = 0; i < ruleCount; i++) {
            Feature sample = getSampleFeatureForRule(featureType, feature, rules[i]);
            MetaBufferEstimator estimator = new MetaBufferEstimator(sample);
            final Symbolizer[] symbolizers = rules[i].getSymbolizers();
            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];
                if (symbolizer instanceof PointSymbolizer || symbolizer instanceof LineSymbolizer) {
                    double size = getSymbolizerSize(estimator, symbolizer, defaultMaxSize);
                    // a line symbolizer is depicted as a line of the requested size, so don't go
                    // below min
                    if (size < minSize && !(symbolizer instanceof LineSymbolizer)) {
                        minSize = size;
                    }
                    if (size > maxSize) {
                        maxSize = size;
                    }
                }
            }
        }
        return new double[] {minSize, maxSize};
    }

    /**
     * Gets a numeric value for the given PointSymbolizer
     *
     * @param feature sample to be used for evals
     * @param symbolizer symbolizer
     * @param defaultSize size to use is none can be taken from the symbolizer
     */
    private double getSymbolizerSize(
            MetaBufferEstimator estimator, Symbolizer symbolizer, double defaultSize) {
        estimator.reset();
        symbolizer.accept(estimator);
        int buffer = estimator.getBuffer();
        if (buffer > 0) {
            return buffer;
        } else {
            return defaultSize;
        }
    }

    /**
     * Returns a sample feature for the given rule, with the following criteria: - if a sample is
     * given in input is returned in output - if a sample is not given in input, scan the rule
     * symbolizers to find the one with the max dimensionality, and return a sample for that
     * dimensionality.
     *
     * @param featureType featureType used to create a sample, if none is given as input
     * @param sample feature sample to be returned as is in output, if defined
     * @param rule rule containing symbolizers to scan for max dimensionality
     */
    private Feature getSampleFeatureForRule(
            FeatureType featureType, Feature sample, final Rule rule) {
        Symbolizer[] symbolizers = rule.getSymbolizers();
        // if we don't have a sample as input, we need to create a sampleFeature
        // looking at the requested symbolizers (we chose the one with the max
        // dimensionality and create a congruent sample)
        if (sample == null) {
            int dimensionality = 1;
            for (int sIdx = 0; sIdx < symbolizers.length; sIdx++) {
                final Symbolizer symbolizer = symbolizers[sIdx];
                if (LineSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 2;
                }
                if (PolygonSymbolizer.class.isAssignableFrom(symbolizer.getClass())) {
                    dimensionality = 3;
                }
            }
            return createSampleFeature(featureType, dimensionality);
        } else {
            return sample;
        }
    }

    /**
     * Renders a title for a layer (to be put on top of the layer legend).
     *
     * @param legend FeatureType representing the layer
     * @param w width for the image (hint)
     * @param h height for the image (hint)
     * @param transparent (should the image be transparent)
     * @param request GetLegendGraphicRequest being built
     * @return image with the title
     */
    private RenderedImage getLayerTitle(
            LegendRequest legend,
            int w,
            int h,
            boolean transparent,
            GetLegendGraphicRequest request) {
        String title = legend.getTitle();
        final BufferedImage image =
                ImageUtils.createImage(w, h, (IndexColorModel) null, transparent);
        return LegendMerger.getRenderedLabel(image, title, request);
    }

    /**
     * Extracts legend for layer based on LayerInfo configuration or style LegendGraphics.
     *
     * @param published FeatureType representing the layer
     * @param w width for the image (hint)
     * @param h height for the image (hint)
     * @param transparent (should the image be transparent)
     * @param request GetLegendGraphicRequest being built
     * @return image with the title
     */
    private RenderedImage getLayerLegend(
            LegendRequest legend,
            int w,
            int h,
            boolean transparent,
            GetLegendGraphicRequest request) {

        LegendInfo legendInfo = legend.getLegendInfo();
        if (legendInfo == null) {
            return null; // nothing provided will need to dynamically generate
        }
        String onlineResource = legendInfo.getOnlineResource();
        if (onlineResource == null || onlineResource.isEmpty()) {
            return null; // nothing provided will need to dynamically generate
        }
        URL url = null;
        try {
            url = new URL(onlineResource);
        } catch (MalformedURLException invalid) {
            LOGGER.fine("Unable to obtain " + onlineResource);
            return null; // should log this!
        }
        try {
            BufferedImage image = ImageIO.read(url);

            if (image.getWidth() == w && image.getHeight() == h) {
                return image;
            }
            final BufferedImage rescale =
                    ImageUtils.createImage(w, h, (IndexColorModel) null, true);

            Graphics2D g = (Graphics2D) rescale.getGraphics();
            g.setColor(new Color(255, 255, 255, 0));
            g.fillRect(0, 0, w, h);

            double aspect = ((double) h) / ((double) image.getHeight());
            int legendWidth = (int) (aspect * ((double) image.getWidth()));

            g.drawImage(image, 0, 0, legendWidth, h, null);
            g.dispose();

            return rescale;
        } catch (IOException notFound) {
            LOGGER.log(Level.FINE, "Unable to legend graphic:" + url, notFound);
            return null; // unable to access image
        }
    }

    /**
     * Receives a list of <code>BufferedImages</code> and produces a new one which holds all the
     * images in <code>imageStack</code> one above the other, handling labels.
     *
     * @param imageStack the list of BufferedImages, one for each applicable Rule
     * @param rules The applicable rules, one for each image in the stack (if not null it's used to
     *     compute labels)
     * @param request The request.
     * @param forceLabelsOn true for force labels on also with a single image.
     * @param forceLabelsOff true for force labels off also with more than one rule.
     * @return the stack image with all the images on the argument list.
     * @throws IllegalArgumentException if the list is empty
     */
    private BufferedImage mergeGroups(
            List<RenderedImage> imageStack,
            Rule[] rules,
            GetLegendGraphicRequest req,
            boolean forceLabelsOn,
            boolean forceLabelsOff,
            boolean forceTitlesOff) {
        LegendMerger.MergeOptions options =
                LegendMerger.MergeOptions.createFromRequest(
                        imageStack, 0, 0, 0, 0, req, forceLabelsOn, forceLabelsOff, forceTitlesOff);
        options.setLayout(LegendUtils.getGroupLayout(req));
        return LegendMerger.mergeGroups(rules, options);
    }

    /**
     * Returns a <code>java.awt.Shape</code> appropiate to render a legend graphic given the
     * symbolizer type and the legend dimensions.
     *
     * @param symbolizer the Symbolizer for whose type a sample shape will be created
     * @param legendWidth the requested width, in output units, of the legend graphic
     * @param legendHeight the requested height, in output units, of the legend graphic
     * @return an appropiate Line2D, Rectangle2D or LiteShape(Point) for the symbolizer, wether it
     *     is a LineSymbolizer, a PolygonSymbolizer, or a Point ot Text Symbolizer
     * @throws IllegalArgumentException if an unknown symbolizer impl was passed in.
     */
    private LiteShape2 getSampleShape(
            Symbolizer symbolizer,
            int legendWidth,
            int legendHeight,
            int areaWidth,
            int areaHeight) {
        LiteShape2 sampleShape;
        final float hpad =
                (areaWidth * LegendUtils.hpaddingFactor) + (areaWidth - legendWidth) / 2f;
        final float vpad =
                (areaHeight * LegendUtils.vpaddingFactor) + (areaHeight - legendHeight) / 2f;
        ;

        if (symbolizer instanceof LineSymbolizer) {
            if (this.sampleLine == null) {
                Coordinate[] coords = {
                    new Coordinate(hpad, legendHeight - vpad - 1),
                    new Coordinate(legendWidth - hpad - 1, vpad)
                };
                LineString geom = geomFac.createLineString(coords);

                try {
                    this.sampleLine = new LiteShape2(geom, null, null, false);
                } catch (Exception e) {
                    this.sampleLine = null;
                }
            }

            sampleShape = this.sampleLine;
        } else if ((symbolizer instanceof PolygonSymbolizer)
                || (symbolizer instanceof RasterSymbolizer)) {
            final float w = areaWidth - (2 * hpad) - 1;
            final float h = areaHeight - (2 * vpad) - 1;

            Coordinate[] coords = {
                new Coordinate(hpad, vpad),
                new Coordinate(hpad, vpad + h),
                new Coordinate(hpad + w, vpad + h),
                new Coordinate(hpad + w, vpad),
                new Coordinate(hpad, vpad)
            };
            LinearRing shell = geomFac.createLinearRing(coords);
            Polygon geom = geomFac.createPolygon(shell, null);

            try {
                return new LiteShape2(geom, null, null, false);
            } catch (Exception e) {
                return null;
            }
        } else if (symbolizer instanceof PointSymbolizer || symbolizer instanceof TextSymbolizer) {
            if (this.samplePoint == null) {
                Coordinate coord = new Coordinate(legendWidth / 2d, legendHeight / 2d);

                try {
                    this.samplePoint =
                            new LiteShape2(geomFac.createPoint(coord), null, null, false);
                } catch (Exception e) {
                    this.samplePoint = null;
                }
            }

            sampleShape = this.samplePoint;
        } else {
            throw new IllegalArgumentException("Unknown symbolizer: " + symbolizer);
        }

        return sampleShape;
    }

    private SimpleFeature createSampleFeature() {
        SimpleFeatureType type;
        try {
            type = DataUtilities.createType("Sample", "the_geom:Geometry");
        } catch (SchemaException e) {
            throw new RuntimeException(e);
        }
        return SimpleFeatureBuilder.template(type, null);
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the rendering of the
     * legend graphic, using the given dimensionality for the geometry attribute.
     *
     * @param schema the schema for which to create a sample Feature instance
     * @param dimensionality the geometry dimensionality required (ovverides the one defined in the
     *     schema) 1= points, 2= lines, 3= polygons
     * @throws ServiceException
     */
    private Feature createSampleFeature(FeatureType schema, int dimensionality)
            throws ServiceException {
        if (schema instanceof SimpleFeatureType) {
            schema = cloneWithDimensionality(schema, dimensionality);
        }

        return createSampleFeature(schema);
    }

    /**
     * Clones the given schema, changing the geometry attribute to match the given dimensionality.
     *
     * @param schema schema to clone
     * @param dimensionality dimensionality for the geometry 1= points, 2= lines, 3= polygons
     */
    private FeatureType cloneWithDimensionality(FeatureType schema, int dimensionality) {
        SimpleFeatureType simpleFt = (SimpleFeatureType) schema;
        SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(schema.getName());
        builder.setCRS(schema.getCoordinateReferenceSystem());
        for (AttributeDescriptor desc : simpleFt.getAttributeDescriptors()) {
            if (isMixedGeometry(desc)) {
                GeometryDescriptor geomDescriptor = (GeometryDescriptor) desc;
                GeometryType geomType = geomDescriptor.getType();

                Class<?> geometryClass = getGeometryForDimensionality(dimensionality);

                GeometryType gt =
                        new GeometryTypeImpl(
                                geomType.getName(),
                                geometryClass,
                                geomType.getCoordinateReferenceSystem(),
                                geomType.isIdentified(),
                                geomType.isAbstract(),
                                geomType.getRestrictions(),
                                geomType.getSuper(),
                                geomType.getDescription());

                builder.add(
                        new GeometryDescriptorImpl(
                                gt,
                                geomDescriptor.getName(),
                                geomDescriptor.getMinOccurs(),
                                geomDescriptor.getMaxOccurs(),
                                geomDescriptor.isNillable(),
                                geomDescriptor.getDefaultValue()));
            } else {
                builder.add(desc);
            }
        }
        schema = builder.buildFeatureType();
        return schema;
    }

    /**
     * Creates a Geometry class for the given dimensionality.
     *
     * @param dimensionality
     */
    private Class<?> getGeometryForDimensionality(int dimensionality) {
        if (dimensionality == 1) {
            return Point.class;
        }
        if (dimensionality == 2) {
            return LineString.class;
        }
        return Polygon.class;
    }

    /**
     * Creates a sample Feature instance in the hope that it can be used in the rendering of the
     * legend graphic.
     *
     * @param schema the schema for which to create a sample Feature instance
     * @throws ServiceException
     */
    private Feature createSampleFeature(FeatureType schema) throws ServiceException {
        Feature sampleFeature;
        try {
            if (schema instanceof SimpleFeatureType) {
                if (hasMixedGeometry((SimpleFeatureType) schema)) {
                    // we can't create a sample for a generic Geometry type
                    sampleFeature = null;
                } else {
                    sampleFeature = SimpleFeatureBuilder.template((SimpleFeatureType) schema, null);
                }
            } else {
                sampleFeature = DataUtilities.templateFeature(schema);
            }
        } catch (IllegalAttributeException e) {
            throw new ServiceException(e);
        }
        return sampleFeature;
    }

    /**
     * Checks if the given schema contains a GeometryDescriptor that has a generic Geometry type.
     *
     * @param schema
     */
    private boolean hasMixedGeometry(SimpleFeatureType schema) {
        for (AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if (isMixedGeometry(attDesc)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if the given AttributeDescriptor describes a generic Geometry.
     *
     * @param attDesc
     */
    private boolean isMixedGeometry(AttributeDescriptor attDesc) {
        if (attDesc instanceof GeometryDescriptor
                && attDesc.getType().getBinding() == Geometry.class) {
            return true;
        }
        return false;
    }
}
