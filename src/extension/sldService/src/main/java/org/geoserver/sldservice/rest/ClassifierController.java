/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
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
import java.awt.Color;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import javax.servlet.http.HttpServletResponse;
import javax.xml.transform.TransformerException;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.xml.XMLSerializer;
import org.geoserver.catalog.Catalog;
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
import org.geoserver.sldservice.utils.classifier.RulesBuilder;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.CustomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.GrayColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.JetColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RandomColorRamp;
import org.geoserver.sldservice.utils.classifier.impl.RedColorRamp;
import org.geotools.data.Query;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.filter.function.RangedClassifier;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLDTransformer;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.util.Converters;
import org.geotools.util.NullProgressListener;
import org.geotools.util.logging.Logging;
import org.locationtech.jts.geom.LineString;
import org.locationtech.jts.geom.MultiLineString;
import org.locationtech.jts.geom.MultiPoint;
import org.locationtech.jts.geom.MultiPolygon;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.Polygon;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.Filter;
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
    private static final Logger LOGGER = Logging.getLogger(ClassifierController.class);

    @Autowired
    public ClassifierController(@Qualifier("catalog") Catalog catalog) {
        super(catalog);
    }

    /*
     * (non-Javadoc)
     *
     * @see org.geoserver.rest.RestBaseController#configurePersister(org.geoserver.config.util.XStreamPersister,
     * org.geoserver.rest.converters.XStreamMessageConverter)
     */
    @Override
    public void configurePersister(XStreamPersister persister, XStreamMessageConverter converter) {
        XStream xstream = persister.getXStream();
        xstream.alias("Rules", RulesList.class);
        xstream.registerConverter(new StyleConverter());
        xstream.allowTypes(new Class[] {RulesList.class, JSONObject.class});
    }

    /**
     * final String layerName = (String) attributes.get("layer"); final String property =
     * form.getFirstValue("attribute"); final String method = form.getFirstValue("method",
     * "equalInterval"); final String intervals = form.getFirstValue("intervals", "2"); final String
     * intervalsForUnique = form.getFirstValue("intervals", "-1"); final String open =
     * form.getFirstValue("open", "false"); final String colorRamp = form.getFirstValue("ramp",
     * "red"); final boolean reverse = Boolean.parseBoolean(form.getFirstValue("reverse")); final
     * boolean normalize = Boolean.parseBoolean(form.getFirstValue("normalize"));
     *
     * @param layerName
     * @param property
     * @param method
     * @param intervals
     * @param intervalsForUnique
     * @param open
     * @param colorRamp
     * @param reverse
     * @param normalize
     * @return
     */
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
                    String intervals,
            @RequestParam(value = "intervalsForUnique", required = false, defaultValue = "-1")
                    String intervalsForUnique,
            @RequestParam(value = "open", required = false, defaultValue = "false") String open,
            @RequestParam(value = "ramp", required = false, defaultValue = "red") String colorRamp,
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
            final HttpServletResponse response) {
        LayerInfo layerInfo = catalog.getLayerByName(layerName);
        if (layerInfo == null) {
            throw new ResourceNotFoundException("No such layer: " + layerName);
        }
        if (layerInfo != null && layerInfo.getResource() instanceof FeatureTypeInfo) {
            String color = null;
            if (!strokeColor.equals("")) {
                try {
                    color = URLDecoder.decode(strokeColor, "UTF-8");
                } catch (UnsupportedEncodingException e1) {
                    throw new RuntimeException("strokeColor is not correct: " + strokeColor, e1);
                }
            }
            try {
                viewParams = URLDecoder.decode(viewParams, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("viewParams are not correct: " + viewParams, e1);
            }
            try {
                customClasses = URLDecoder.decode(customClasses, "UTF-8");
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("customClasses are not correct: " + customClasses, e1);
            }
            try {
                if (startColor != null) {
                    startColor = URLDecoder.decode(startColor, "UTF-8");
                }
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("startColor is not correct: " + startColor, e1);
            }
            try {
                if (endColor != null) {
                    endColor = URLDecoder.decode(endColor, "UTF-8");
                }
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("endColor is not correct: " + endColor, e1);
            }
            try {
                if (midColor != null) {
                    midColor = URLDecoder.decode(midColor, "UTF-8");
                }
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("midColor is not correct: " + midColor, e1);
            }
            try {
                if (colors != null) {
                    colors = URLDecoder.decode(colors, "UTF-8");
                }
            } catch (UnsupportedEncodingException e1) {
                throw new RuntimeException("colors are not correct: " + colors, e1);
            }
            if (cachingTime > 0) {
                response.setHeader(
                        "cache-control",
                        CacheControl.maxAge(cachingTime, TimeUnit.SECONDS)
                                .cachePublic()
                                .getHeaderValue());
            }
            final List<Rule> rules =
                    this.generateClassifiedSLD(
                            layerName,
                            property,
                            method,
                            intervals,
                            intervalsForUnique,
                            open,
                            customClasses,
                            colorRamp,
                            startColor,
                            endColor,
                            midColor,
                            colors,
                            reverse,
                            normalize,
                            viewParams,
                            strokeWeight,
                            color != null ? Color.decode(color) : null,
                            pointSize);
            if (fullSLD) {

                StyleBuilder sb = new StyleBuilder();
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

    /**
     * @param layer
     * @param rules
     * @return
     */
    private RulesList generateRulesList(String layer, List<Rule> rules) {
        final RulesList ruleList = new RulesList(layer);
        for (Rule rule : rules) {
            ruleList.addRule(jsonRule(rule));
        }

        return ruleList;
    }

    /**
     * @param Rule object
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

    /**
     * @param layerName
     * @param property
     * @param method
     * @param intervals
     * @param intervalsForUnique
     * @param open
     * @param colorRamp
     * @param reverse
     * @param normalize
     * @return
     */
    private List<Rule> generateClassifiedSLD(
            String layerName,
            String property,
            String method,
            String intervals,
            String intervalsForUnique,
            String open,
            String customClasses,
            String colorRamp,
            String startColor,
            String endColor,
            String midColor,
            String colors,
            Boolean reverse,
            Boolean normalize,
            String viewParams,
            double strokeWeight,
            Color strokeColor,
            int pointSize) {
        RulesBuilder builder = new RulesBuilder();
        builder.setStrokeColor(strokeColor);
        builder.setStrokeWeight(strokeWeight);
        builder.setPointSize(pointSize);
        /* Looks in attribute map if there is the featureType param */
        if (property != null && property.length() > 0) {
            /* First try to find as a FeatureType */
            try {
                LayerInfo layerInfo = catalog.getLayerByName(layerName);
                if (layerInfo != null) {
                    ResourceInfo obj = layerInfo.getResource();
                    /* Check if it's feature type or coverage */
                    if (obj instanceof FeatureTypeInfo) {
                        final FeatureType ftType = ((FeatureTypeInfo) obj).getFeatureType();
                        FeatureCollection ftCollection = null;
                        if (customClasses.isEmpty()) {
                            Query query =
                                    new Query(ftType.getName().getLocalPart(), Filter.INCLUDE);
                            query.setHints(getQueryHints(viewParams));
                            ftCollection =
                                    ((FeatureTypeInfo) obj)
                                            .getFeatureSource(new NullProgressListener(), null)
                                            .getFeatures(query);
                        }

                        List<Rule> rules = null;
                        Class<?> propertyType =
                                ftType.getDescriptor(property).getType().getBinding();

                        if (customClasses.isEmpty()) {
                            if ("equalInterval".equals(method)) {
                                rules =
                                        builder.equalIntervalClassification(
                                                ftCollection,
                                                property,
                                                propertyType,
                                                Integer.parseInt(intervals),
                                                Boolean.parseBoolean(open),
                                                normalize);
                            } else if ("uniqueInterval".equals(method)) {
                                rules =
                                        builder.uniqueIntervalClassification(
                                                ftCollection,
                                                property,
                                                propertyType,
                                                Integer.parseInt(intervalsForUnique),
                                                normalize);
                            } else if ("quantile".equals(method)) {
                                rules =
                                        builder.quantileClassification(
                                                ftCollection,
                                                property,
                                                propertyType,
                                                Integer.parseInt(intervals),
                                                Boolean.parseBoolean(open),
                                                normalize);
                            } else if ("jenks".equals(method)) {
                                rules =
                                        builder.jenksClassification(
                                                ftCollection,
                                                property,
                                                propertyType,
                                                Integer.parseInt(intervals),
                                                Boolean.parseBoolean(open),
                                                normalize);
                            }
                        } else {
                            RangedClassifier groups =
                                    getCustomClassifier(customClasses, propertyType, normalize);
                            rules =
                                    Boolean.parseBoolean(open)
                                            ? builder.openRangedRules(
                                                    groups, property, propertyType, normalize)
                                            : builder.closedRangedRules(
                                                    groups, property, propertyType, normalize);
                        }

                        final Class geomT = ftType.getGeometryDescriptor().getType().getBinding();
                        if (geomT.isAssignableFrom(Point.class) && strokeColor != null) {
                            builder.setIncludeStrokeForPoints(true);
                        }
                        ColorRamp ramp = null;
                        if (customClasses.isEmpty()
                                && colorRamp != null
                                && colorRamp.length() > 0) {
                            if (colorRamp.equalsIgnoreCase("random")) ramp = new RandomColorRamp();
                            else if (colorRamp.equalsIgnoreCase("red")) ramp = new RedColorRamp();
                            else if (colorRamp.equalsIgnoreCase("blue")) ramp = new BlueColorRamp();
                            else if (colorRamp.equalsIgnoreCase("jet")) ramp = new JetColorRamp();
                            else if (colorRamp.equalsIgnoreCase("gray")) ramp = new GrayColorRamp();
                            else if (colorRamp.equalsIgnoreCase("custom")) {
                                Color startColorDecoded =
                                        (startColor != null ? Color.decode(startColor) : null);
                                Color endColorDecoded =
                                        (endColor != null ? Color.decode(endColor) : null);
                                Color midColorDecoded =
                                        (midColor != null ? Color.decode(midColor) : null);
                                List<Color> colorsDecoded = null;
                                if (colors != null) {
                                    Stream<String> colorsStream = Stream.of(colors.split(","));
                                    colorsDecoded =
                                            colorsStream
                                                    .map(c -> Color.decode(c))
                                                    .collect(Collectors.toList());
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
                }
            } catch (NoSuchElementException e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(
                            Level.FINE,
                            "The following exception has occurred " + e.getLocalizedMessage(),
                            e);
            } catch (IOException e) {
                if (LOGGER.isLoggable(Level.FINE))
                    LOGGER.log(
                            Level.FINE,
                            "The following exception has occurred " + e.getLocalizedMessage(),
                            e);
            }
        }

        return null;
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
         * @see com.thoughtworks.xstream.converters.Converter#unmarshal(com.thoughtworks.xstream.io.
         *     HierarchicalStreamReader,com.thoughtworks.xstream.converters.UnmarshallingContext)
         */
        public Object unmarshal(HierarchicalStreamReader arg0, UnmarshallingContext arg1) {
            // TODO Auto-generated method stub
            return null;
        }
    }
}
