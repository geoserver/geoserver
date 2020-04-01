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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.imageio.ImageIO;
import org.geoserver.catalog.LegendInfo;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.CascadedLegendRequest;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
import org.geoserver.wms.map.ImageUtils;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.renderer.lite.MetaBufferEstimator;
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
import org.geotools.styling.visitor.RescaleStyleVisitor;
import org.geotools.util.NumberRange;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
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
public class BufferedImageLegendGraphicBuilder extends LegendGraphicBuilder {
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
        setup(request);
        List<RenderedImage> layersImages = new ArrayList<RenderedImage>();
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

            boolean strict = request.isStrict();

            gt2Style = resizeForDPI(request, gt2Style);

            final boolean transparent = request.isTransparent();
            RenderedImage titleImage = null;
            // if we have more than one layer, we put a title on top of each layer legend
            if (layers.size() > 1 && !forceTitlesOff) {
                titleImage = getLayerTitle(legend, w, h, transparent, request);
            }

            checkForRenderingTransformations(gt2Style);

            final boolean buildRasterLegend =
                    (!strict && layer == null && LegendUtils.checkRasterSymbolizer(gt2Style))
                            || (LegendUtils.checkGridLayer(layer) && !hasVectorTransformation)
                            || hasRasterTransformation;

            // Just checks LegendInfo currently, should check gtStyle
            final boolean useProvidedLegend = layer != null && legend.getLayerInfo() != null;

            RenderedImage legendImage = null;
            if (useProvidedLegend || legend instanceof CascadedLegendRequest) {
                boolean forceResize = !(legend instanceof CascadedLegendRequest);
                legendImage =
                        getLayerLegend(legend, w, h, transparent, forceResize, request, titleImage);
            }

            if (useProvidedLegend && legendImage != null) {
                if (titleImage != null) {
                    layersImages.add(titleImage);
                }
                layersImages.add(legendImage);
            } else if (buildRasterLegend) {
                final RasterLayerLegendHelper rasterLegendHelper =
                        new RasterLayerLegendHelper(request, gt2Style, ruleName);
                final BufferedImage image = rasterLegendHelper.getLegend();
                if (image != null) {
                    if (titleImage != null) {
                        layersImages.add(titleImage);
                    }
                    layersImages.add(image);
                }
            } else if (legend instanceof CascadedLegendRequest) {
                // coming from cascading wms service
                if (titleImage != null) layersImages.add(titleImage);
                if (legendImage != null) layersImages.add(legendImage);
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

                renderRules(
                        request,
                        layersImages,
                        forceLabelsOn,
                        forceLabelsOff,
                        forceTitlesOff,
                        layer,
                        transparent,
                        titleImage,
                        sampleFeature,
                        scaleDenominator,
                        applicableRules,
                        scaleRange,
                        ruleCount,
                        legendsStack,
                        styleFactory,
                        minimumSymbolSize,
                        rescalingRequired,
                        rescaler);
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

    /** */
    private void renderRules(
            GetLegendGraphicRequest request,
            List<RenderedImage> layersImages,
            boolean forceLabelsOn,
            boolean forceLabelsOff,
            boolean forceTitlesOff,
            FeatureType layer,
            final boolean transparent,
            RenderedImage titleImage,
            final Feature sampleFeature,
            final double scaleDenominator,
            Rule[] applicableRules,
            final NumberRange<Double> scaleRange,
            final int ruleCount,
            final List<RenderedImage> legendsStack,
            final SLDStyleFactory styleFactory,
            double minimumSymbolSize,
            boolean rescalingRequired,
            java.util.function.Function<Double, Double> rescaler) {
        MetaBufferEstimator estimator = new MetaBufferEstimator(sampleFeature);
        for (int i = 0; i < ruleCount; i++) {

            final RenderedImage image =
                    ImageUtils.createImage(w, h, (IndexColorModel) null, transparent);
            final Map<RenderingHints.Key, Object> hintsMap =
                    new HashMap<RenderingHints.Key, Object>();
            final Graphics2D graphics =
                    ImageUtils.prepareTransparency(
                            transparent, LegendUtils.getBackgroundColor(request), image, hintsMap);
            graphics.setRenderingHint(
                    RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            Feature sample = getSampleFeatureForRule(layer, sampleFeature, applicableRules[i]);

            final List<Symbolizer> symbolizers = applicableRules[i].symbolizers();
            final GraphicLegend graphic = applicableRules[i].getLegend();

            // If this rule has a legend graphic defined in the SLD, use it
            if (graphic != null) {
                if (this.samplePoint == null) {
                    Coordinate coord = new Coordinate(w / 2, h / 2);

                    try {
                        this.samplePoint =
                                new LiteShape2(geomFac.createPoint(coord), null, null, false);
                    } catch (Exception e) {
                        this.samplePoint = null;
                    }
                }
                shapePainter.paint(graphics, this.samplePoint, graphic, scaleDenominator, false);

            } else {
                for (Symbolizer symbolizer : symbolizers) {
                    // skip raster symbolizers
                    if (!(symbolizer instanceof RasterSymbolizer)) {
                        // rescale symbols if needed
                        LiteShape2 shape = getSampleShape(symbolizer, w, h, w, h);
                        if (rescalingRequired
                                && (symbolizer instanceof PointSymbolizer
                                        || symbolizer instanceof LineSymbolizer)) {
                            double size =
                                    getSymbolizerSize(estimator, symbolizer, Math.min(w, h) - 4);
                            double newSize = rescaler.apply(size);
                            symbolizer = rescaleSymbolizer(symbolizer, size, newSize);
                        } else if (symbolizer instanceof PolygonSymbolizer) {
                            // need to make room for the stroke in the symbol, thus, a
                            // smaller rect
                            double symbolizerSize = getSymbolizerSize(estimator, symbolizer, 0);
                            int rescaledWidth =
                                    (int)
                                            Math.ceil(
                                                    Math.max(
                                                            minimumSymbolSize, w - symbolizerSize));
                            int rescaledHeight =
                                    (int)
                                            Math.ceil(
                                                    Math.max(
                                                            minimumSymbolSize, h - symbolizerSize));
                            shape = getSampleShape(symbolizer, rescaledWidth, rescaledHeight, w, h);

                            symbolizer = rescaleSymbolizer(symbolizer, w, (double) rescaledWidth);
                        }

                        Style2D style2d = styleFactory.createStyle(sample, symbolizer, scaleRange);
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
                    Integer.parseInt(request.getLegendOptions().get("labelMargin").toString());
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
            BufferedImage image = LegendMerger.mergeLegends(applicableRules, request, options);

            if (image != null) {
                layersImages.add(image);
            }
        }
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
     * @param w width for the image (hint)
     * @param h height for the image (hint)
     * @param transparent (should the image be transparent)
     * @param forceDimensions (should the image be resized if response does not match w,h)
     * @param request GetLegendGraphicRequest being built
     */
    private RenderedImage getLayerLegend(
            LegendRequest legend,
            int w,
            int h,
            boolean transparent,
            boolean forceDimensions,
            GetLegendGraphicRequest request,
            RenderedImage titleImage) {

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

            if ((image.getWidth() == w && image.getHeight() == h) || !forceDimensions) {
                return image;
            }

            image =
                    rescaleBufferedImage(
                            image,
                            titleImage != null
                                    ? titleImage
                                    : getLayerTitle(legend, w, h, transparent, request));
            final BufferedImage rescale =
                    ImageUtils.createImage(
                            image.getWidth(), image.getHeight(), (IndexColorModel) null, true);

            Graphics2D g = (Graphics2D) rescale.getGraphics();
            g.setColor(new Color(255, 255, 255, 0));
            g.fillRect(0, 0, w, h);
            g.drawImage(image, 0, 0, null);
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
     * @param req The request.
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

    protected Rule[] updateRuleTitles(
            FeatureCountProcessor processor, LegendRequest legend, Rule[] applicableRules) {
        return processor.preProcessRules(legend, applicableRules);
    }

    protected BufferedImage rescaleBufferedImage(BufferedImage image, RenderedImage titleImage) {
        int titleHeight = titleImage != null ? titleImage.getHeight() : 0;
        int originalHeight = image.getHeight();
        int originalWidth = image.getWidth();
        double scaleFactor = getScale(originalHeight, h);
        double scaleFactorW = getScale(originalWidth, w);
        int scaleWidth = originalWidth >= w ? w : (int) Math.round(originalWidth * scaleFactorW);
        int scaleHeight = (int) Math.round(originalHeight * scaleFactor);
        scaleHeight -= titleHeight;
        int delta = (scaleHeight + titleHeight) - h;
        boolean stillTooBig = Math.signum(delta) >= 0;
        if (stillTooBig) {
            delta += titleHeight / 2;
            scaleHeight -= delta;
        }
        Image result = image.getScaledInstance(scaleWidth, scaleHeight, Image.SCALE_DEFAULT);
        if (result instanceof BufferedImage) return (BufferedImage) result;
        else {
            BufferedImage bufResult =
                    new BufferedImage(
                            image.getWidth(), image.getHeight(), BufferedImage.TYPE_INT_ARGB);
            Graphics g = bufResult.getGraphics();
            g.drawImage(result, 0, 0, null);
            g.dispose();
            return bufResult;
        }
    }

    private double getScale(int original, int target) {
        return (double) target / (double) original;
    }
}
