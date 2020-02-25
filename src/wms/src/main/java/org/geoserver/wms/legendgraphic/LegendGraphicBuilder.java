/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.legendgraphic;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.geoserver.platform.ServiceException;
import org.geoserver.wms.GetLegendGraphicRequest;
import org.geoserver.wms.GetLegendGraphicRequest.LegendRequest;
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

/** @author ian */
public abstract class LegendGraphicBuilder {

    Logger LOGGER = Logger.getLogger("org.geoserver.wms.legendgraphic");
    /** Tolerance used to compare doubles for equality */
    public static final double TOLERANCE = 1e-6;
    /** used to create sample point shapes with LiteShape (not lines nor polygons) */
    protected static final GeometryFactory geomFac = new GeometryFactory();
    /**
     * Just a holder to avoid creating many line shapes from inside <code>getSampleShape()</code>
     */
    private LiteShape2 sampleLine;
    /**
     * Just a holder to avoid creating many point shapes from inside <code>getSampleShape()</code>
     */
    protected LiteShape2 samplePoint;
    /**
     * Default minimum size for symbols rendering. Can be overridden using LEGEND_OPTIONS
     * (minSymbolSize).
     */
    protected final double MINIMUM_SYMBOL_SIZE = 3.0;

    protected int w;
    protected int h;
    boolean forceLabelsOn = false;
    boolean forceLabelsOff = false;
    boolean forceTitlesOff = false;
    FeatureCountProcessor countProcessor = null;
    List<LegendRequest> layers;
    boolean hasVectorTransformation = false;
    boolean hasRasterTransformation = false;
    /** */
    public LegendGraphicBuilder() {
        super();
    }

    public void setup(GetLegendGraphicRequest request) {
        // width and height, we might have to rescale those in case of DPI usage
        w = request.getWidth();
        h = request.getHeight();

        if (request.getLegendOptions().get("forceLabels") instanceof String) {
            String forceLabelsOpt = (String) request.getLegendOptions().get("forceLabels");
            if (forceLabelsOpt.equalsIgnoreCase("on")) {
                forceLabelsOn = true;
            } else if (forceLabelsOpt.equalsIgnoreCase("off")) {
                forceLabelsOff = true;
            }
        }

        if (request.getLegendOptions().get("forceTitles") instanceof String) {
            String forceTitlesOpt = (String) request.getLegendOptions().get("forceTitles");
            if (forceTitlesOpt.equalsIgnoreCase("off")) {
                forceTitlesOff = true;
            }
        }

        if (Boolean.TRUE.equals(
                        request.getLegendOption(
                                GetLegendGraphicRequest.COUNT_MATCHED_KEY, Boolean.class))
                || Boolean.TRUE.equals(
                        request.getLegendOption(
                                GetLegendGraphicRequest.HIDE_EMPTY_RULES, Boolean.class))) {
            countProcessor = new FeatureCountProcessor(request);
        }
        layers = request.getLegends();
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

    protected Style resizeForDPI(GetLegendGraphicRequest request, Style gt2Style) {

        // apply dpi rescale
        double dpi = RendererUtilities.getDpi(request.getLegendOptions());
        double standardDpi = RendererUtilities.getDpi(Collections.emptyMap());
        if (dpi != standardDpi) {
            double scaleFactor = dpi / standardDpi;
            w = ((int) Math.round(w * scaleFactor));
            h = ((int) Math.round(h * scaleFactor));
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
        return gt2Style;
    }
    /**
     * Calculates a global rescaling factor for all the symbols to be drawn in the given rules. This
     * is to be sure all symbols are drawn inside the given w x h box.
     *
     * @param featureType FeatureType to be used for size extraction in expressions (used to create
     *     a sample if feature is null)
     * @param feature Feature to be used for size extraction in expressions (if null a sample
     *     Feature will be created from featureType)
     * @param rules set of rules to scan for symbols
     */
    protected double[] calcSymbolSize(
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
            for (Symbolizer symbolizer : rules[i].symbolizers()) {
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
     * @param symbolizer symbolizer
     * @param defaultSize size to use is none can be taken from the symbolizer
     */
    protected double getSymbolizerSize(
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
    protected Feature getSampleFeatureForRule(
            FeatureType featureType, Feature sample, final Rule rule) {
        // if we don't have a sample as input, we need to create a sampleFeature
        // looking at the requested symbolizers (we chose the one with the max
        // dimensionality and create a congruent sample)
        if (sample == null) {
            int dimensionality = 1;
            for (Symbolizer symbolizer : rule.symbolizers()) {
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
    protected LiteShape2 getSampleShape(
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

    protected SimpleFeature createSampleFeature() {
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

    /** Creates a Geometry class for the given dimensionality. */
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
     */
    protected Feature createSampleFeature(FeatureType schema) throws ServiceException {
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
     */
    private boolean hasMixedGeometry(SimpleFeatureType schema) {
        for (AttributeDescriptor attDesc : schema.getAttributeDescriptors()) {
            if (isMixedGeometry(attDesc)) {
                return true;
            }
        }
        return false;
    }

    /** Checks if the given AttributeDescriptor describes a generic Geometry. */
    private boolean isMixedGeometry(AttributeDescriptor attDesc) {
        if (attDesc instanceof GeometryDescriptor
                && attDesc.getType().getBinding() == Geometry.class) {
            return true;
        }
        return false;
    }

    /** @return the w */
    public int getW() {
        return w;
    }

    /** @param w the w to set */
    public void setW(int w) {
        this.w = w;
    }

    /** */
    public abstract Object buildLegendGraphic(GetLegendGraphicRequest request);

    /** @param gt2Style */
    protected void checkForRenderingTransformations(Style gt2Style) {
        // Check for rendering transformation

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
                        outputs.values().iterator().next(); // we assume there is only one output
                if (SimpleFeatureCollection.class.isAssignableFrom(output.getType())) {
                    hasVectorTransformation = true;
                    break;
                } else if (GridCoverage2D.class.isAssignableFrom(output.getType())) {
                    hasRasterTransformation = true;
                    break;
                }
            }
        }
    }
}
