/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2007-2008-2009 GeoSolutions S.A.S., http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.Converter;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.awt.*;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.*;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.media.jai.PlanarImage;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.ows.kvp.FormatOptionsKvpParser;
import org.geoserver.rest.ResourceNotFoundException;
import org.geoserver.rest.RestBaseController;
import org.geoserver.rest.RestException;
import org.geoserver.rest.converters.XStreamMessageConverter;
import org.geoserver.sldservice.utils.classifier.ColorRamp;
import org.geoserver.sldservice.utils.classifier.RasterSymbolizerBuilder;
import org.geoserver.sldservice.utils.classifier.RulesBuilder;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.CustomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RandomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.geotools.data.Query;
import org.geotools.data.util.NullProgressListener;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.visitor.*;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.image.util.ImageUtilities;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ColorMapEntryImpl;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.factory.Hints;
import org.geotools.util.logging.Logging;
import org.geotools.xml.styling.SLDTransformer;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.PropertyDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.PropertyIsBetween;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.spatial.BBOX;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** ClassifierController. */
@RestController
@ControllerAdvice
@RequestMapping(path = RestBaseController.ROOT_PATH + "/sldservice")
public class ClassifierController extends BaseSLDServiceController {
    private static final FilterFactory2 FF = CommonFactoryFinder.getFilterFactory2();
    private static final Logger LOGGER = Logging.getLogger(ClassifierController.class);
    private static final int FIRST_BAND = 1;

    @Autowired
    public ClassifierController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("Rules", RulesList.class);
        xstream.registerConverter(new StyleConverter());
        xstream.allowTypes(new Class[] {RulesList.class, JSONObject.class});
    }

    @GetMapping(
        path = "/{layerName}/classify",
        produces = {
            MediaType.APPLICATION_JSON_VALUE,
            MediaType.APPLICATION_XML_VALUE,
            MediaType.TEXT_HTML_VALUE
        }
    )
    public Object classify(
            @PathVariable String layerName,
            @RequestParam(value = "attribute", required = false) String property,
            @RequestParam(value = "method", required = false, defaultValue = "equalInterval")
                    String method,
            @RequestParam(value = "intervals", required = false, defaultValue = "2")
                    Integer intervals,
            @RequestParam(value = "intervalsForUnique", required = false, defaultValue = "-1")
                    Integer intervalsForUnique,
            @RequestParam(value = "open", required = false, defaultValue = "false") boolean open,
            @RequestParam(value = "ramp", required = false, defaultValue = "red")
                    String customColors,
            @RequestParam(value = "startColor", required = false) String startColor,
            @RequestParam(value = "endColor", required = false) String endColor,
            @RequestParam(value = "midColor", required = false) String midColor,
            @RequestParam(value = "colors", required = false) String colors,
            @RequestParam(value = "reverse", required = false, defaultValue = "false")
                    Boolean reverse,
            @RequestParam(value = "strokeColor", required = false, defaultValue = "")
                    String strokeColor,
            @RequestParam(value = "strokeWeight", required = false, defaultValue = "1")
                    Double strokeWeight,
            @RequestParam(value = "pointSize", required = false, defaultValue = "15")
                    Integer pointSize,
            @RequestParam(value = "normalize", required = false, defaultValue = "false")
                    Boolean normalize,
            @RequestParam(value = "viewparams", required = false, defaultValue = "")
                    String viewParams,
            @RequestParam(value = "customClasses", required = false, defaultValue = "")
                    String customClasses,
            @RequestParam(value = "fullSLD", required = false, defaultValue = "false")
                    Boolean fullSLD,
            @RequestParam(value = "cache", required = false, defaultValue = "600") long cachingTime,
            @RequestParam(value = "continuous", required = false, defaultValue = "false")
                    boolean continuous,
            @RequestParam(value = "bbox", required = false) ReferencedEnvelope bbox,
            @RequestParam(value = "stddevs", required = false) Double stddevs,
            @RequestParam(value = "env", required = false) String env,
            @RequestParam(value = "percentages", required = false) boolean percentages,
            @RequestParam(value = "percentagesScale", required = false) Integer percentagesScale,
            final HttpServletResponse response)
            throws Exception {
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            throw new ResourceNotFoundException("No such layer: " + layerName);
        }
        // default to the layer own CRS if not provided as 5th parameter
        if (bbox != null && bbox.getCoordinateReferenceSystem() == null) {
            bbox = new ReferencedEnvelope(bbox, layerInfo.getResource().getCRS());
        }
        if (stddevs != null && stddevs <= 0) {
            throw new RestException(
                    "stddevs must be a positive floating point number", HttpStatus.BAD_REQUEST);
        }
        if (cachingTime > 0) {
            response.setHeader(
                    "cache-control",
                    CacheControl.maxAge(cachingTime, TimeUnit.SECONDS)
                            .cachePublic()
                            .getHeaderValue());
        }
        ColorRamp ramp =
                this.getColorRamp(
                        customClasses, customColors, startColor, endColor, midColor, colors);
        final List<Rule> rules;
        if (env != null) {
            RestEnvVariableCallback.setOptions(env);
        }
        try {

            ResourceInfo obj = layerInfo.getResource();
            /* Check if it's feature type or coverage */
            if (obj instanceof FeatureTypeInfo) {
                Color stroke =
                        (strokeColor != null && !strokeColor.isEmpty())
                                ? Color.decode(strokeColor)
                                : null;
                rules =
                        getVectorRules(
                                property,
                                method,
                                intervals,
                                intervalsForUnique,
                                open,
                                customClasses,
                                reverse,
                                normalize,
                                viewParams,
                                strokeWeight,
                                stroke,
                                pointSize,
                                (FeatureTypeInfo) obj,
                                ramp,
                                bbox,
                                stddevs,
                                percentages,
                                percentagesScale);
            } else if (obj instanceof CoverageInfo) {
                rules =
                        getRasterRules(
                                property,
                                method,
                                intervals,
                                intervalsForUnique,
                                open,
                                customClasses,
                                reverse,
                                normalize,
                                (CoverageInfo) obj,
                                ramp,
                                continuous,
                                bbox,
                                stddevs,
                                percentages,
                                percentagesScale);
            } else {
                throw new RestException(
                        "The classifier can only work against vector or raster data, "
                                + layerInfo.prefixedName()
                                + " is neither",
                        HttpStatus.BAD_REQUEST);
            }
        } catch (IllegalArgumentException e) {
            throw new RestException(e.getMessage(), HttpStatus.BAD_REQUEST, e);
        }

        if (rules == null || rules.isEmpty()) {
            throw new RestException(
                    "Could not generate any rule, there is likely no data matching the request (layer is empty, of filtered down to no matching features/pixels)",
                    HttpStatus.NOT_FOUND);
        }

        if (fullSLD) {
            StyledLayerDescriptor sld = SF.createStyledLayerDescriptor();
            NamedLayer namedLayer = SF.createNamedLayer();
            namedLayer.setName(layerName);
            Style userStyle = SF.createStyle();
            FeatureTypeStyle fts = SF.createFeatureTypeStyle();
            fts.rules().addAll(rules);
            userStyle.featureTypeStyles().add(fts);
            namedLayer.addStyle(userStyle);
            sld.addStyledLayer(namedLayer);

            try {
                return sldAsString(sld);
            } catch (TransformerException e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(
                            Level.FINE,
                            "Exception occurred while transforming the style "
                                    + e.getLocalizedMessage(),
                            e);
            }

        } else {
            RulesList jsonRules = null;
            if (rules != null) jsonRules = generateRulesList(layerName, rules);

            if (jsonRules != null) {
                return wrapObject(jsonRules, RulesList.class);
            } else {
                throw new InvalidRules();
            }
        }
        return wrapObject(new RulesList(layerName), RulesList.class);
    }

    private List<Color> getCustomColors(String customClasses) {
        List<Color> colors = new ArrayList<Color>();
        for (String value : customClasses.split(";")) {
            String[] parts = value.split(",");
            colors.add(Color.decode(parts[2]));
        }
        return colors;
    }

    private RangedClassifier getCustomClassifier(
            String customClasses, Class<?> propertyType, boolean normalize) {
        List<Comparable> min = new ArrayList<Comparable>();
        List<Comparable> max = new ArrayList<Comparable>();
        for (String value : customClasses.split(";")) {
            String[] parts = value.split(",");
            if (parts.length != 3) {
                throw new RuntimeException("wrong custom class: " + value);
            }
            min.add(
                    (Comparable)
                            Converters.convert(
                                    parts[0], normalizePropertyType(propertyType, normalize)));
            max.add(
                    (Comparable)
                            Converters.convert(
                                    parts[1], normalizePropertyType(propertyType, normalize)));
        }

        return new RangedClassifier(
                min.toArray(new Comparable[] {}), max.toArray(new Comparable[] {}));
    }

    private Class normalizePropertyType(Class<?> propertyType, boolean normalize) {
        if (normalize
                && (Integer.class.isAssignableFrom(propertyType)
                        || Long.class.isAssignableFrom(propertyType))) {
            return Double.class;
        }
        return propertyType;
    }

    @ResponseStatus(value = HttpStatus.BAD_REQUEST, reason = "Error generating Classification!")
    private class InvalidRules extends RuntimeException {
        private static final long serialVersionUID = -5538194136398411147L;
    }

    /** */
    private RulesList generateRulesList(String layer, List<Rule> rules) {
        final RulesList ruleList = new RulesList(layer);
        for (Rule rule : rules) {
            ruleList.addRule(jsonRule(rule));
        }

        return ruleList;
    }

    /**
     * @param obj Rule object
     * @return a string with json rule representation
     */
    private JSONObject jsonRule(Object obj) {
        JSONObject jsonObj = null;
        String xmlRule;
        XMLSerializer xmlS = new XMLSerializer();

        SLDTransformer transform = new SLDTransformer();
        transform.setIndentation(2);
        try {
            xmlRule = transform.transform(obj);
            xmlS.setRemoveNamespacePrefixFromElements(true);
            xmlS.setSkipNamespaces(true);
            jsonObj = (JSONObject) xmlS.read(xmlRule);
        } catch (TransformerException e) {
            if (LOGGER.isLoggable(Level.FINE))
                LOGGER.log(
                        Level.FINE,
                        "Exception occurred while transforming the rule " + e.getLocalizedMessage(),
                        e);
        }

        return jsonObj;
    }

    private List<Rule> getRasterRules(
            String property,
            String method,
            Integer intervals,
            Integer intervalsForUnique,
            boolean open,
            String customClasses,
            Boolean reverse,
            Boolean normalize,
            CoverageInfo coverageInfo,
            ColorRamp ramp,
            boolean continuous,
            ReferencedEnvelope bbox,
            Double stddevs,
            boolean percentages,
            Integer percentagesScale)
            throws Exception {
        int selectedBand = getRequestedBand(property); // one based band name
        // read the image to be classified
        ImageReader imageReader =
                new ImageReader(
                                coverageInfo,
                                selectedBand,
                                RasterSymbolizerBuilder.DEFAULT_MAX_PIXELS,
                                bbox)
                        .invoke();
        boolean bandSelected = imageReader.isBandSelected();
        RenderedImage image = imageReader.getImage();
        RasterSymbolizerBuilder builder =
                new RasterSymbolizerBuilder(percentages, percentagesScale);
        builder.setStandardDeviations(stddevs);
        ColorMap colorMap;
        try {
            if (customClasses.isEmpty()) {
                if ("equalInterval".equals(method)) {
                    colorMap =
                            builder.equalIntervalClassification(image, intervals, open, continuous);
                } else if ("uniqueInterval".equals(method)) {
                    colorMap = builder.uniqueIntervalClassification(image, intervalsForUnique);
                } else if ("quantile".equals(method) || "equalArea".equals(method)) {
                    colorMap = builder.quantileClassification(image, intervals, open, continuous);
                } else if ("jenks".equals(method)) {
                    colorMap = builder.jenksClassification(image, intervals, open, continuous);
                } else {
                    throw new RestException(
                            "Unknown classification method " + method, HttpStatus.BAD_REQUEST);
                }
            } else {
                RangedClassifier classifier =
                        getCustomClassifier(customClasses, Double.class, normalize);
                colorMap = builder.createCustomColorMap(image, classifier, open, continuous);
            }
        } finally {
            cleanImage(image);
        }

        // apply the color ramp
        boolean skipFirstEntry =
                !"uniqueInterval".equals(method)
                        && !open
                        && !continuous
                        && colorMap.getColorMapEntries().length > 1;
        builder.applyColorRamp(colorMap, ramp, skipFirstEntry, reverse);

        // check for single valued colormaps
        if (colorMap.getColorMapEntries().length == 1) {
            adaptSingleValueColorMap(image, colorMap);
        }

        // wrap the colormap into a raster symbolizer and rule
        RasterSymbolizer rasterSymbolizer = SF.createRasterSymbolizer();
        rasterSymbolizer.setColorMap(colorMap);
        if (bandSelected) {
            SelectedChannelType grayChannel =
                    SF.createSelectedChannelType(
                            String.valueOf(selectedBand), (ContrastEnhancement) null);
            ChannelSelection channelSelection =
                    SF.createChannelSelection(new SelectedChannelType[] {grayChannel});
            rasterSymbolizer.setChannelSelection(channelSelection);
        }

        Rule rule = SF.createRule();
        rule.symbolizers().add(rasterSymbolizer);
        return Collections.singletonList(rule);
    }

    private void adaptSingleValueColorMap(RenderedImage image, ColorMap colorMap) {
        // force it to be visible, depending on the method the first entry might be
        // transparent
        ColorMapEntry cm0 = colorMap.getColorMapEntry(0);
        cm0.setOpacity(FF.literal(1));

        // should always be a literal, just covering for possible future changes
        if (cm0.getQuantity() instanceof Literal) {
            // wrap it between two values that are slightly below and above
            Float value = cm0.getQuantity().evaluate(null, Float.class);
            int intBits = Float.floatToIntBits(value);
            ColorMapEntry cm1 = new ColorMapEntryImpl();
            cm1.setQuantity(FF.literal(Float.intBitsToFloat(intBits + 1)));
            cm0.setQuantity(FF.literal(Float.intBitsToFloat(intBits - 1)));

            cm1.setColor(cm0.getColor());
            cm1.setLabel(cm0.getLabel());
            cm1.setOpacity(cm0.getOpacity());

            colorMap.addColorMapEntry(cm1);
            colorMap.setType(ColorMap.TYPE_INTERVALS);
        } else {
            // make it formally valid, but the value match will not really not work
            colorMap.setType(ColorMap.TYPE_VALUES);
        }
    }

    /** Returns the selected band */
    private int getRequestedBand(String property) {
        // if no selection is provided, the code picks the first band
        if (property == null) {
            return FIRST_BAND;
        }
        Integer selectedBand = Converters.convert(property, Integer.class);
        if (selectedBand == null) {
            throw new RestException(
                    "Invalid property value for raster layer, it should be a band number, but was "
                            + property,
                    HttpStatus.BAD_REQUEST);
        }
        return selectedBand;
    }

    /** Performs a full disposal of the coverage in question */
    private void cleanImage(RenderedImage image) {
        if (image instanceof PlanarImage) {
            ImageUtilities.disposePlanarImageChain((PlanarImage) image);
        }
    }

    private List<Rule> getVectorRules(
            String property,
            String method,
            Integer intervals,
            Integer intervalsForUnique,
            boolean open,
            String customClasses,
            Boolean reverse,
            Boolean normalize,
            String viewParams,
            double strokeWeight,
            Color strokeColor,
            int pointSize,
            FeatureTypeInfo obj,
            ColorRamp ramp,
            ReferencedEnvelope bbox,
            Double stddevs,
            Boolean percentages,
            Integer percentagesScale)
            throws IOException, TransformException, FactoryException {
        if (property == null || property.isEmpty()) {
            throw new IllegalArgumentException(
                    "Vector classification requires a classification property to be specified");
        }

        RulesBuilder builder = new RulesBuilder(percentages, percentagesScale);
        builder.setStrokeColor(strokeColor);
        builder.setStrokeWeight(strokeWeight);
        builder.setPointSize(pointSize);

        final FeatureType ftType = obj.getFeatureType();
        FeatureCollection ftCollection = null;
        if (customClasses.isEmpty() || percentages) {
            Query query = new Query(ftType.getName().getLocalPart(), Filter.INCLUDE);
            if (bbox != null) {
                ReferencedEnvelope nativeBBOX =
                        bbox.transform(ftType.getCoordinateReferenceSystem(), true);
                BBOX filter = FF.bbox(FF.property(""), nativeBBOX);
                query.setFilter(filter);
            }
            query.setHints(getQueryHints(viewParams));
            ftCollection =
                    obj.getFeatureSource(new NullProgressListener(), null).getFeatures(query);

            if (stddevs != null) {
                NumberRange stdDevRange =
                        getStandardDeviationsRange(property, ftCollection, stddevs);
                PropertyIsBetween between =
                        FF.between(
                                FF.property(property),
                                FF.literal(stdDevRange.getMinimum()),
                                FF.literal(stdDevRange.getMaximum()));
                if (query.getFilter() == Filter.INCLUDE) {
                    query.setFilter(between);
                } else {
                    query.setFilter(FF.and(query.getFilter(), between));
                }

                // re-query
                ftCollection =
                        obj.getFeatureSource(new NullProgressListener(), null).getFeatures(query);
            }
        }

        List<Rule> rules = null;
        final PropertyDescriptor pd = ftType.getDescriptor(property);
        if (pd == null) {
            throw new RestException(
                    "Could not find property "
                            + property
                            + ", available attributes are: "
                            + ftType.getDescriptors()
                                    .stream()
                                    .map(p -> p.getName().getLocalPart())
                                    .collect(Collectors.joining(", ")),
                    HttpStatus.BAD_REQUEST);
        }
        Class<?> propertyType = pd.getType().getBinding();

        if (customClasses.isEmpty()) {
            if ("equalInterval".equals(method)) {
                rules =
                        builder.equalIntervalClassification(
                                ftCollection, property, propertyType, intervals, open, normalize);
            } else if ("uniqueInterval".equals(method)) {
                rules =
                        builder.uniqueIntervalClassification(
                                ftCollection,
                                property,
                                propertyType,
                                intervalsForUnique,
                                normalize);
            } else if ("quantile".equals(method)) {
                rules =
                        builder.quantileClassification(
                                ftCollection, property, propertyType, intervals, open, normalize);
            } else if ("jenks".equals(method)) {
                rules =
                        builder.jenksClassification(
                                ftCollection, property, propertyType, intervals, open, normalize);
            } else if ("equalArea".equals(method)) {
                rules =
                        builder.equalAreaClassification(
                                ftCollection, property, propertyType, intervals, open, normalize);
            } else {
                throw new RestException(
                        "Unknown classification method " + method, HttpStatus.BAD_REQUEST);
            }
        } else {
            RangedClassifier groups = getCustomClassifier(customClasses, propertyType, normalize);
            if (percentages) {
                double[] percentagesAr =
                        builder.getCustomPercentages(
                                ftCollection, groups, property, propertyType, normalize);
                groups.setPercentages(percentagesAr);
            }
            rules =
                    open
                            ? builder.openRangedRules(groups, property, propertyType, normalize)
                            : builder.closedRangedRules(groups, property, propertyType, normalize);
        }

        final Class geomT = ftType.getGeometryDescriptor().getType().getBinding();
        if (geomT.isAssignableFrom(Point.class) && strokeColor != null) {
            builder.setIncludeStrokeForPoints(true);
        }
        if (ramp != null) {
            /*
             * Line Symbolizer
             */
            if (geomT == LineString.class || geomT == MultiLineString.class) {
                builder.lineStyle(rules, ramp, reverse);
            }

            /*
             * Point Symbolizer
             */
            else if (geomT == Point.class || geomT == MultiPoint.class) {
                builder.pointStyle(rules, ramp, reverse);
            }

            /*
             * Polygon Symbolyzer
             */
            else if (geomT == MultiPolygon.class || geomT == Polygon.class) {
                builder.polygonStyle(rules, ramp, reverse);
            }
        }

        return rules;
    }

    /**
     * Returns a range of N standard deviations around the mean for the given attribute and
     * collection
     */
    private NumberRange getStandardDeviationsRange(
            String property, FeatureCollection features, double numStandardDeviations)
            throws IOException {
        final StandardDeviationVisitor standardDeviationVisitor =
                new StandardDeviationVisitor(FF.property(property));
        features.accepts(standardDeviationVisitor, null);
        final double mean = standardDeviationVisitor.getMean();
        final CalcResult result = standardDeviationVisitor.getResult();
        if (result.getValue() == null) {
            throw new RestException(
                    "The standard deviation visit did not find any value, the dataset is empty or previous filters removed all values",
                    HttpStatus.BAD_REQUEST);
        }
        final double standardDeviation = standardDeviationVisitor.getResult().toDouble();

        return new NumberRange(
                Double.class,
                mean - standardDeviation * numStandardDeviations,
                mean + standardDeviation * numStandardDeviations);
    }

    private ColorRamp getColorRamp(
            String customClasses,
            String colorRamp,
            String startColor,
            String endColor,
            String midColor,
            String colors) {
        ColorRamp ramp = null;
        if (customClasses.isEmpty() && colorRamp != null && colorRamp.length() > 0) {
            if (colorRamp.equalsIgnoreCase("random")) ramp = new RandomColorRamp();
            else if (colorRamp.equalsIgnoreCase("red")) ramp = new RedColorRamp();
            else if (colorRamp.equalsIgnoreCase("blue")) ramp = new BlueColorRamp();
            else if (colorRamp.equalsIgnoreCase("jet")) ramp = new JetColorRamp();
            else if (colorRamp.equalsIgnoreCase("gray")) ramp = new GrayColorRamp();
            else if (colorRamp.equalsIgnoreCase("custom")) {
                Color startColorDecoded = (startColor != null ? Color.decode(startColor) : null);
                Color endColorDecoded = (endColor != null ? Color.decode(endColor) : null);
                Color midColorDecoded = (midColor != null ? Color.decode(midColor) : null);
                List<Color> colorsDecoded = null;
                if (colors != null) {
                    Stream<String> colorsStream = Stream.of(colors.split(","));
                    colorsDecoded =
                            colorsStream.map(c -> Color.decode(c)).collect(Collectors.toList());
                }
                if (colorsDecoded != null) {
                    CustomColorRamp tramp = new CustomColorRamp();
                    tramp.setInputColors(colorsDecoded);
                    ramp = tramp;
                } else if (startColorDecoded != null && endColorDecoded != null) {
                    CustomColorRamp tramp = new CustomColorRamp();
                    tramp.setStartColor(startColorDecoded);
                    tramp.setEndColor(endColorDecoded);
                    if (midColorDecoded != null) tramp.setMid(midColorDecoded);
                    ramp = tramp;
                }
            }
        } else {
            final List<Color> customColors = getCustomColors(customClasses);
            ramp =
                    new ColorRamp() {

                        @Override
                        public void setNumClasses(int numClass) {}

                        @Override
                        public int getNumClasses() {
                            return customColors.size();
                        }

                        @Override
                        public List<Color> getRamp() throws Exception {
                            return customColors;
                        }

                        @Override
                        public void revert() {}
                    };
        }
        return ramp;
    }

    private Hints getQueryHints(String viewParams) {
        if (viewParams != null && !viewParams.isEmpty()) {
            FormatOptionsKvpParser parser = new FormatOptionsKvpParser();
            Map<String, String> params;
            try {
                params = (Map<String, String>) parser.parse(viewParams);
                return new Hints(Hints.VIRTUAL_TABLE_PARAMETERS, params);
            } catch (Exception e) {
                throw new RestException("Invalid viewparams", HttpStatus.INTERNAL_SERVER_ERROR);
            }
        }
        return null;
    }

    /** @author Fabiani */
    public class RulesList {
        private String layerName;

        private List<JSONObject> rules = new ArrayList<JSONObject>();

        public RulesList(final String layer) {
            setLayerName(layer);
        }

        public void addRule(JSONObject object) {
            rules.add(object);
        }

        public List<JSONObject> getRules() {
            return rules;
        }

        /** @param layerName the layerName to set */
        public void setLayerName(String layerName) {
            this.layerName = layerName;
        }

        /** @return the layerName */
        public String getLayerName() {
            return layerName;
        }
    }

    /** @author Fabiani */
    public class StyleConverter implements Converter {

        /**
         * @see com.thoughtworks.xstream.converters.ConverterMatcher#canConvert(java .lang.Class)
         */
        public boolean canConvert(Class clazz) {
            return RulesList.class.isAssignableFrom(clazz)
                    || JSONObject.class.isAssignableFrom(clazz);
        }

        /**
         * @see com.thoughtworks.xstream.converters.Converter#marshal(java.lang.Object ,
         *     com.thoughtworks.xstream.io.HierarchicalStreamWriter,
         *     com.thoughtworks.xstream.converters.MarshallingContext)
         */
        public void marshal(
                Object value, HierarchicalStreamWriter writer, MarshallingContext context) {

            if (value instanceof RulesList) {
                RulesList obj = (RulesList) value;

                for (JSONObject rule : obj.getRules()) {
                    if (!rule.isEmpty() && !rule.isNullObject() && !rule.isArray()) {
                        writer.startNode("Rule");
                        for (Object key : rule.keySet()) {
                            writer.startNode((String) key);
                            writeChild(writer, rule.get(key));
                            writer.endNode();
                        }
                        writer.endNode();
                    }
                }
            } else if (value instanceof JSONObject) {
                JSONObject obj = (JSONObject) value;
                writeChild(writer, obj);
            }
        }

        private void writeChild(HierarchicalStreamWriter writer, Object object) {
            if (object instanceof JSONObject && !((JSONObject) object).isArray()) {
                for (Object key : ((JSONObject) object).keySet()) {
                    final Object obj = ((JSONObject) object).get(key);
                    if (obj instanceof JSONArray) {
                        for (int i = 0; i < ((JSONArray) obj).size(); i++) {
                            final JSONObject child = (JSONObject) ((JSONArray) obj).get(i);
                            writer.startNode((String) key);
                            for (Object cKey : child.keySet()) {
                                writeKey(writer, child, (String) cKey);
                            }
                            writer.endNode();
                        }
                    } else {
                        writeKey(writer, (JSONObject) object, (String) key);
                    }
                }
            } else if (object instanceof JSONArray) {
                for (int i = 0; i < ((JSONArray) object).size(); i++) {
                    final Object child = ((JSONArray) object).get(i);
                    if (child instanceof JSONObject) {
                        for (Object key : ((JSONObject) child).keySet()) {
                            if (((JSONObject) child).get(key) instanceof String)
                                writer.addAttribute(
                                        (String) key, (String) ((JSONObject) child).get(key));
                            else writeChild(writer, ((JSONObject) child).get(key));
                        }

                    } else {
                        writeChild(writer, child);
                    }
                }
            } else {
                writer.setValue(object.toString());
            }
        }

        private void writeKey(HierarchicalStreamWriter writer, final JSONObject child, String key) {
            if (key.startsWith("@")) {
                writer.addAttribute(key.substring(1), (String) child.get(key));
            } else if (key.startsWith("#")) {
                writer.setValue((String) child.get(key));
            } else {
                writer.startNode(key);
                writeChild(writer, child.get(key));
                writer.endNode();
            }
        }

        /**
         * @see
         *     com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.HierarchicalStreamReader,com.thoughtworks.xstream.converters.UnmarshallingContext)
         */
        public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
