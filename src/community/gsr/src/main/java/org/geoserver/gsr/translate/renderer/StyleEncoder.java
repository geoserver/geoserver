/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

/* Copyright (c) 2013 - 2017 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gsr.translate.renderer;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;
import net.sf.json.util.JSONBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.gsr.model.geometry.GeometryTypeEnum;
import org.geoserver.gsr.model.label.Label;
import org.geoserver.gsr.model.renderer.ClassBreakInfo;
import org.geoserver.gsr.model.renderer.ClassBreaksRenderer;
import org.geoserver.gsr.model.renderer.Renderer;
import org.geoserver.gsr.model.renderer.SimpleRenderer;
import org.geoserver.gsr.model.renderer.UniqueValueInfo;
import org.geoserver.gsr.model.renderer.UniqueValueRenderer;
import org.geoserver.gsr.model.symbol.MarkerSymbol;
import org.geoserver.gsr.model.symbol.Outline;
import org.geoserver.gsr.model.symbol.PictureMarkerSymbol;
import org.geoserver.gsr.model.symbol.SimpleFillSymbol;
import org.geoserver.gsr.model.symbol.SimpleFillSymbolEnum;
import org.geoserver.gsr.model.symbol.SimpleLineSymbol;
import org.geoserver.gsr.model.symbol.SimpleLineSymbolEnum;
import org.geoserver.gsr.model.symbol.SimpleMarkerSymbol;
import org.geoserver.gsr.model.symbol.SimpleMarkerSymbolEnum;
import org.geoserver.gsr.model.symbol.Symbol;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.util.Converters;
import org.geotools.util.NumberRange;
import org.geotools.util.logging.Logging;
import org.opengis.filter.Filter;
import org.opengis.filter.Or;
import org.opengis.filter.PropertyIsEqualTo;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.style.Description;
import org.opengis.style.Fill;
import org.opengis.style.GraphicalSymbol;

public class StyleEncoder {

    static final Logger LOGGER = Logging.getLogger(StyleEncoder.class);

    /** Max length of a dasharray line to be considered a dot instead of a dash */
    static final int DOT_THRESHOLD = 2;

    private static List<PropertyRangeExtractor> propertyRangeExtractors =
            Arrays.asList(
                    new BetweenExtractor(),
                    new LowerExtractor(),
                    new GreaterExtractor(),
                    new LowerGreaterExtractor());

    //    public static void defaultFillStyle(JSONBuilder json) {
    //        json.object()
    //          .key("type").value("simple")
    //          .key("symbol").object()
    //            .key("type").value("esriSFS")
    //            .key("style").value("esriSFSSolid")
    //            .key("color");
    //            color(json, 255, 0, 0, 255);
    //            json.key("outline").object()
    //              .key("type").value("esriSLS")
    //              .key("style").value("esriSLSSolid")
    //              .key("color");
    //              color(json, 0,  0, 0, 255);
    //              json.key("width").value(1)
    //            .endObject()
    //          .endObject()
    //          .key("label").value("")
    //          .key("description").value("")
    //        .endObject();
    //    }
    //
    //    public static void defaultRasterStyle(JSONBuilder json) {
    //        json.object()
    //          .key("type").value("simple")
    //          .key("symbol").object()
    //            .key("type").value("esriSFS")
    //            .key("style").value("esriSFSSolid")
    //            .key("color").value(null)
    //            .key("outline").object()
    //              .key("type").value("esriSLS")
    //              .key("style").value("esriSLSSolid")
    //              .key("color").value(null)
    //              .key("width").value(1)
    //            .endObject()
    //          .endObject()
    //          .key("label").value("")
    //          .key("description").value("")
    //        .endObject();
    //    }
    //
    //    public static void defaultLineStyle(JSONBuilder json) {
    //        json.object()
    //          .key("type").value("simple")
    //          .key("symbol");
    //          encodeLineStyle(json, new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components
    //          (Color.RED, 1d), 1d));
    //          json.key("label").value("")
    //          .key("description").value("");
    //        json.endObject();
    //    }
    //
    //    public static void defaultMarkStyle(JSONBuilder json) {
    //        json.object()
    //          .key("type").value("simple")
    //          .key("symbol").object()
    //            .key("type").value("esriSMS")
    //            .key("style").value("esriSMSCircle")
    //            .key("color");
    //            color(json, 255, 0, 0, 255);
    //            json.key("outline").object()
    //              .key("type").value("esriSLS")
    //              .key("style").value("esriSLSSolid")
    //              .key("color");
    //              color(json, 0, 0, 0, 255);
    //              json.key("width").value("1");
    //            json.endObject()
    //          .endObject()
    //          .key("label").value("")
    //          .key("description").value("")
    //        .endObject();
    //    }
    //
    //    private static void color(JSONBuilder json, int r, int g, int b, int a) {
    //        json.array().value(r).value(g).value(b).value(a).endArray();
    //    }

    public static List<Label> styleToLabel(Style style) {
        LabelInfoVisitor visitor = new LabelInfoVisitor();
        style.accept(visitor);
        return visitor.getLabelInfo();
    }

    public static Renderer styleToRenderer(Style style) {

        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        if (featureTypeStyles == null || featureTypeStyles.size() != 1) return null;

        FeatureTypeStyle featureTypeStyle = featureTypeStyles.get(0);
        if (featureTypeStyle == null) return null;

        List<Rule> rules = featureTypeStyle.rules();
        if (rules == null || rules.size() == 0) return null;

        // filter out the rules with just text symbolization, they are handled elsewhere
        rules =
                rules.stream()
                        .filter(
                                r ->
                                        r.symbolizers().stream()
                                                .anyMatch(s -> (!(s instanceof TextSymbolizer))))
                        .collect(Collectors.toList());

        Renderer render = rulesToUniqueValueRenderer(rules);
        if (render != null) return render;

        render = rulesToClassBreaksRenderer(rules);
        if (render != null) return render;

        final Symbolizer symbolizer = getSingleSymbolizer(style);
        if (symbolizer != null) {
            final Symbol symbol = symbolizerToSymbol(symbolizer);
            if (symbol != null) {
                return new SimpleRenderer(symbol, "", "");
            } else {
                return null;
            }
        } else {
            return null;
        }
    }

    private static Renderer rulesToClassBreaksRenderer(List<Rule> rules) {
        List<Rule> rulesOther = new LinkedList<>();
        Map<String, ClassBreaksRenderer> map = new LinkedHashMap<>();
        for (Rule rule : rules) {
            ClassBreakInfoMeta meta = ruleToClassBreakInfoMeta(rule);
            if (meta != null) {
                ClassBreaksRenderer renderer = map.get(meta.propertyName);
                if (renderer == null) {
                    double minValue = 0;
                    renderer =
                            new ClassBreaksRenderer(
                                    meta.propertyName, minValue, new LinkedList<>());
                    map.put(meta.propertyName, renderer);
                }
                renderer.getClassBreakInfos().add(meta.classBreakInfo);
            } else {
                rulesOther.add(rule);
            }
        }

        if (map.size() == 1 && rulesOther.size() <= 1) {
            ClassBreaksRenderer classBreaksRenderer = map.values().iterator().next();
            classBreaksRenderer.setMinValue(
                    classBreaksRenderer.getClassBreakInfos().stream()
                            .map(cb -> cb.getClassMinValue())
                            .filter(min -> min != null)
                            .min(Double::compare)
                            .orElse(0d));
            if (rulesOther.size() == 1) {
                // assuming remaining rule is the default.
                Rule rule = rulesOther.get(0);
                String title = null;
                if (rule.getDescription() != null && rule.getDescription().getTitle() != null) {
                    title = rule.getDescription().getTitle().toString();
                }
                if (title == null) title = "";
                // no default label/value in the model yet
                // classBreaksRenderer.setDefaultLabel(title);
                // classBreaksRenderer.setDefaultSymbol(symbolizerToSymbol(rule.symbolizers().get
                // (0)));
            }
            return classBreaksRenderer;
        } else if (map.size() == 0 && rules.size() == 1) {
            try {
                // still a possibility for unique value renderer if there is a single rule that
                // has a categorize function call
                ClassificationFunctionsVisitor visitor = new ClassificationFunctionsVisitor();
                Rule rule = rules.get(0);
                rule.accept(visitor);
                if (visitor.hasCategorize()
                        && !visitor.hasRecode()
                        && !visitor.hasOtherFunctions()) {
                    Set<List<Object>> keySets = visitor.getCategorizeKeys();
                    Set<String> properties = visitor.getClassificationProperty();
                    // only if the same set of keys is applied everywhere, then we can use a unique
                    // renderer
                    if (keySets.size() == 1 && properties.size() == 1) {
                        List<Double> keys =
                                keySets.iterator().next().stream()
                                        .map(
                                                k -> {
                                                    Double v = Converters.convert(k, Double.class);
                                                    if (v == null)
                                                        throw new RuntimeException(
                                                                "Key value was not a number, cannot "
                                                                        + "use class breaks: "
                                                                        + v);
                                                    return v;
                                                })
                                        .collect(Collectors.toList());
                        String property = properties.iterator().next();
                        List<ClassBreakInfo> breaks = new ArrayList<>();
                        Symbolizer symbolizer = rule.getSymbolizers()[0];
                        for (Double key : keys) {
                            Symbolizer erased =
                                    ClassificationFunctionEraser.erase(
                                            symbolizer, property, key - 1);
                            ClassBreakInfo cb =
                                    new ClassBreakInfo(
                                            null, key, "", "", symbolizerToSymbol(erased));
                            breaks.add(cb);
                        }
                        // "above" last value
                        Double lastKey = keys.get(keys.size() - 1);
                        Symbolizer erased =
                                ClassificationFunctionEraser.erase(
                                        symbolizer, property, lastKey + 1);
                        ClassBreakInfo cb =
                                new ClassBreakInfo(
                                        null, Double.MAX_VALUE, "", "", symbolizerToSymbol(erased));
                        breaks.add(cb);
                        return new ClassBreaksRenderer(property, -Double.MAX_VALUE, breaks);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.INFO,
                        "Could not turn style into unique value renderer, "
                                + "exception occured while attempting to discover eventual recode "
                                + "functions",
                        e);
            }
        }

        return null;
    }

    private static class ClassBreakInfoMeta {
        final String propertyName;
        final ClassBreakInfo classBreakInfo;

        public ClassBreakInfoMeta(String propertyName, ClassBreakInfo classBreakInfo) {
            this.propertyName = propertyName;
            this.classBreakInfo = classBreakInfo;
        }
    }

    private static ClassBreakInfoMeta ruleToClassBreakInfoMeta(Rule rule) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        if (symbolizers == null || symbolizers.size() != 1) return null;

        Symbolizer symbolizer = symbolizers.get(0);
        if (symbolizer == null) return null;

        Filter filter = rule.getFilter();
        Optional<PropertyRange> range =
                propertyRangeExtractors.stream()
                        .map(re -> re.getRange(filter))
                        .filter(pr -> pr != null)
                        .findFirst();
        if (!range.isPresent()) {
            return null;
        }

        String title = null, description = null;
        Description desc = rule.getDescription();
        if (desc != null) {
            if (desc.getTitle() != null) {
                title = desc.getTitle().toString();
            }
            if (desc.getAbstract() != null) {
                description = desc.getAbstract().toString();
            }
        }
        if (title == null) title = "";
        if (description == null) description = "";
        PropertyRange propertyRange = range.get();
        NumberRange minMax = propertyRange.getRange();
        return new ClassBreakInfoMeta(
                propertyRange.getPropertyName(),
                new ClassBreakInfo(
                        minMax.getMinimum(),
                        minMax.getMaximum(),
                        title,
                        description,
                        symbolizerToSymbol(symbolizer)));
    }

    private static Renderer rulesToUniqueValueRenderer(List<Rule> rules) {
        List<Rule> rulesOther = new LinkedList<>();
        Map<String, UniqueValueRenderer> map = new LinkedHashMap<>();
        for (Rule rule : rules) {
            UniqueValueInfoMeta meta = ruleToUniqueValueInfoMeta(rule);
            if (meta != null) {
                UniqueValueRenderer renderer =
                        map.computeIfAbsent(
                                meta.propertyName,
                                k ->
                                        new UniqueValueRenderer(
                                                meta.propertyName,
                                                null, // field 2
                                                null, // field 3
                                                ", ", // delimiter, required even with single field
                                                null, // default symbol (set later)
                                                null, // default label (set later)
                                                new LinkedList<>()));
                renderer.getUniqueValueInfos().addAll(meta.uniqueValueInfo);
            } else {
                rulesOther.add(rule);
            }
        }
        if (map.size() == 1 && rulesOther.size() <= 1) {
            UniqueValueRenderer uniqueValueRenderer = map.values().iterator().next();
            if (rulesOther.size() == 1) {
                // an assumption here: if there's one rule left over after
                // a bunch of PropertyEqualTo let's assume it's the default.
                // Robust programatic parsing of typical defaults written in SLD
                // will be a PITA, let's defer.
                Rule rule = rulesOther.get(0);
                String title = null;
                if (rule.getDescription() != null && rule.getDescription().getTitle() != null) {
                    title = rule.getDescription().getTitle().toString();
                }
                if (title == null) title = "";
                uniqueValueRenderer.setDefaultLabel(title);
                uniqueValueRenderer.setDefaultSymbol(symbolizerToSymbol(rule.symbolizers().get(0)));
            }
            return uniqueValueRenderer;
        } else if (map.size() == 0 && rules.size() == 1) {
            try {
                // still a possibility for unique value renderer if there is a single rule that
                // has a recode function call
                ClassificationFunctionsVisitor visitor = new ClassificationFunctionsVisitor();
                Rule rule = rules.get(0);
                rule.accept(visitor);
                if (visitor.hasRecode()
                        && !visitor.hasCategorize()
                        && !visitor.hasOtherFunctions()) {
                    Set<List<Object>> keySets = visitor.getRecodeKeys();
                    Set<String> properties = visitor.getClassificationProperty();
                    // only if the same set of keys is applied everywhere, then we can use a unique
                    // renderer
                    if (keySets.size() == 1 && properties.size() == 1) {
                        List<Object> keys = keySets.iterator().next();
                        String property = properties.iterator().next();
                        List<UniqueValueInfo> uniqueValueInfos = new ArrayList<>();
                        Symbolizer symbolizer = rule.getSymbolizers()[0];
                        for (Object key : keys) {
                            Symbolizer erased =
                                    ClassificationFunctionEraser.erase(symbolizer, property, key);
                            UniqueValueInfo vi =
                                    new UniqueValueInfo(
                                            String.valueOf(key),
                                            String.valueOf(key),
                                            "",
                                            symbolizerToSymbol(erased));
                            uniqueValueInfos.add(vi);
                        }
                        return new UniqueValueRenderer(
                                property, null, null, ", ", null, null, uniqueValueInfos);
                    }
                }
            } catch (Exception e) {
                LOGGER.log(
                        Level.INFO,
                        "Could not turn style into unique value renderer, "
                                + "exception occured while attempting to discover eventual recode "
                                + "functions",
                        e);
            }
        }
        return null;
    }

    private static class UniqueValueInfoMeta {
        final String propertyName;
        final List<UniqueValueInfo> uniqueValueInfo;

        public UniqueValueInfoMeta(String propertyName, List<UniqueValueInfo> uniqueValueInfo) {
            this.propertyName = propertyName;
            this.uniqueValueInfo = uniqueValueInfo;
        }
    }

    private static UniqueValueInfoMeta ruleToUniqueValueInfoMeta(Rule rule) {
        List<Symbolizer> symbolizers = rule.symbolizers();
        if (symbolizers == null || symbolizers.size() != 1) return null;

        Symbolizer symbolizer = symbolizers.get(0);
        if (symbolizer == null) return null;

        Filter filter = rule.getFilter();
        List<String> values = new ArrayList<>();
        String propertyName = null;
        if (filter instanceof PropertyIsEqualTo) {

            PropertyIsEqualTo uniqueValueFilter = (PropertyIsEqualTo) filter;

            Expression expression1 = uniqueValueFilter.getExpression1();
            propertyName =
                    expression1 instanceof PropertyName
                            ? ((PropertyName) expression1).getPropertyName()
                            : null;
            if (propertyName == null) return null;

            Expression expression2 = uniqueValueFilter.getExpression2();
            String valueAsString =
                    expression2 instanceof Literal
                            ? ((Literal) expression2).getValue().toString()
                            : null;
            if (valueAsString == null) return null;
            values.add(valueAsString);
        } else if (filter instanceof Or) {
            Or orFilter = (Or) filter;
            List<Filter> children = flattenOr(orFilter.getChildren());
            if (children == null) return null;
            for (Filter internal : children) {
                if (!(internal instanceof PropertyIsEqualTo)) return null;

                PropertyIsEqualTo uniqueValueFilter = (PropertyIsEqualTo) internal;

                Expression expression1 = uniqueValueFilter.getExpression1();
                String internalPropertyName =
                        expression1 instanceof PropertyName
                                ? ((PropertyName) expression1).getPropertyName()
                                : null;
                if (internalPropertyName == null) return null;

                if (propertyName == null) {
                    propertyName = internalPropertyName;
                } else if (!propertyName.equals(internalPropertyName)) {
                    return null;
                }

                Expression expression2 = uniqueValueFilter.getExpression2();
                String valueAsString =
                        expression2 instanceof Literal
                                ? ((Literal) expression2).getValue().toString()
                                : null;

                values.add(valueAsString);
            }
        }
        if (propertyName == null) return null;
        final String title =
                rule.getDescription() != null && rule.getDescription().getTitle() != null
                        ? rule.getDescription().getTitle().toString()
                        : "";
        final String description =
                rule.getDescription() != null && rule.getDescription().getAbstract() != null
                        ? rule.getDescription().getAbstract().toString()
                        : "";
        List<UniqueValueInfo> uniqueValues = new ArrayList<>();
        values.forEach(
                v ->
                        uniqueValues.add(
                                new UniqueValueInfo(
                                        v, title, description, symbolizerToSymbol(symbolizer))));

        return new UniqueValueInfoMeta(propertyName, uniqueValues);
    }

    private static List<Filter> flattenOr(List<Filter> filters) {
        List<Filter> flat = new ArrayList<>();
        for (Filter filter : filters) {
            if (filter instanceof PropertyIsEqualTo) {
                flat.add(filter);
            } else if (filter instanceof Or) {
                List<Filter> children = flattenOr(((Or) filter).getChildren());
                if (children == null) return null;
                flat.addAll(children);
            } else {
                return null;
            }
        }
        return flat;
    }

    private static Renderer defaultPolyRenderer() {
        SimpleLineSymbol outline =
                new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, new int[] {0, 0, 0, 255}, 1);
        Symbol symbol =
                new SimpleFillSymbol(
                        SimpleFillSymbolEnum.SOLID, new int[] {255, 0, 0, 255}, outline);
        return new SimpleRenderer(symbol, "Polygon", "Default polygon renderer");
    }

    private static Renderer defaultRasterRenderer() {
        SimpleLineSymbol outline = new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, null, 1);
        Symbol symbol = new SimpleFillSymbol(SimpleFillSymbolEnum.SOLID, null, outline);
        return new SimpleRenderer(symbol, "Raster", "Default raster renderer");
    }

    private static Renderer defaultLineRenderer() {
        SimpleLineSymbol outline =
                new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, new int[] {0, 0, 0, 255}, 1);
        return new SimpleRenderer(outline, "Line", "Default line renderer");
    }

    private static Renderer defaultMarkRenderer() {
        Outline outline = new Outline(new int[] {0, 0, 0, 255}, 1);
        SimpleMarkerSymbol marker =
                new SimpleMarkerSymbol(
                        SimpleMarkerSymbolEnum.esriSMSSquare,
                        new int[] {255, 0, 0, 255},
                        24,
                        0,
                        0,
                        0,
                        outline);
        return new SimpleRenderer(marker, "Marker", "Default marker renderer");
    }

    public static Renderer effectiveRenderer(LayerInfo layer) throws IOException {
        Renderer renderer = null;

        StyleInfo styleInfo = layer.getDefaultStyle();
        if (styleInfo != null) {
            Style style = styleInfo.getStyle();
            if (style != null) {
                renderer = styleToRenderer(style);
            }
        }

        if (renderer == null) {
            GeometryTypeEnum gtype =
                    GeometryTypeEnum.forResourceDefaultGeometry(layer.getResource());
            if (gtype != null) {
                switch (gtype) {
                    case ENVELOPE:
                    case POLYGON:
                        if (layer.getResource() instanceof CoverageInfo) {
                            renderer = defaultRasterRenderer();
                        } else {
                            renderer = defaultPolyRenderer(); // TODO: Generate default polygon
                            // style
                        }
                        break;
                    case MULTIPOINT:
                    case POINT:
                        renderer = defaultMarkRenderer(); // TODO: Generate default point style
                        break;
                    case POLYLINE:
                        renderer = defaultLineRenderer(); // TODO: Generate default line style;
                        break;
                    default:
                        renderer = null;
                }
            }
        }

        return renderer;
    }

    public static List<Label> labelingInfo(LayerInfo layer) throws IOException {
        List<Label> labels = null;

        StyleInfo styleInfo = layer.getDefaultStyle();
        if (styleInfo != null) {
            Style style = styleInfo.getStyle();
            if (style != null) {
                labels = styleToLabel(style);
            }
        }

        return labels;
    }

    private static Symbol symbolizerToSymbol(Symbolizer sym) {
        if (sym instanceof PointSymbolizer) {
            return pointSymbolizerToMarkSymbol((PointSymbolizer) sym);
        } else if (sym instanceof LineSymbolizer) {
            return lineSymbolizerToLineSymbol((LineSymbolizer) sym);
        } else if (sym instanceof PolygonSymbolizer) {
            return polygonSymbolizerToFillSymbol((PolygonSymbolizer) sym);
        } else return null; // TODO: Should we throw here?
    }

    private static Symbol polygonSymbolizerToFillSymbol(PolygonSymbolizer sym) {
        final Fill fill = sym.getFill();
        final Stroke stroke = sym.getStroke();
        Color color;
        double opacity;
        final SimpleLineSymbol outline;
        SimpleFillSymbolEnum fillStyle = SimpleFillSymbolEnum.SOLID;
        if (fill != null) {
            color = evaluateWithDefault(fill.getColor(), Color.GRAY);
            opacity = evaluateWithDefault(fill.getOpacity(), 1d);
            Graphic graphicFill = sym.getFill().getGraphicFill();
            if (graphicFill != null && graphicFill.graphicalSymbols().get(0) instanceof Mark) {
                Mark mark = (Mark) graphicFill.graphicalSymbols().get(0);
                if (mark.getWellKnownName() != null) {
                    String markName = mark.getWellKnownName().evaluate(null, String.class);
                    if ("shape://vertline".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.VERTICAL;
                    } else if ("shape://horline".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.HORIZONTAL;
                    } else if ("shape://slash".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.FORWARD_DIAGONAL;
                    } else if ("shape://backslash".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.BACKWARD_DIAGONAL;
                    } else if ("shape://plus".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.CROSS;
                    } else if ("shape://times".equals(markName)) {
                        fillStyle = SimpleFillSymbolEnum.DIAGONAL_CROSS;
                    }

                    Stroke markStroke = mark.getStroke();
                    if (fillStyle != SimpleFillSymbolEnum.SOLID && markStroke != null) {
                        color = evaluateWithDefault(markStroke.getColor(), Color.GRAY);
                        opacity = evaluateWithDefault(markStroke.getOpacity(), 1d);
                    }
                }
            }
        } else {
            color = Color.GRAY;
            opacity = 1d;
        }
        if (stroke != null) {
            Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
            outline =
                    new SimpleLineSymbol(
                            SimpleLineSymbolEnum.SOLID,
                            components(strokeColor, strokeOpacity),
                            strokeWidth);
        } else {
            outline =
                    new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components(Color.BLACK, 1), 1);
        }

        return new SimpleFillSymbol(fillStyle, components(color, opacity), outline);
    }

    private static MarkerSymbol pointSymbolizerToMarkSymbol(PointSymbolizer sym) {
        if (sym.getGraphic() == null) return null;
        if (sym.getGraphic().graphicalSymbols().size() != 1)
            return null; // REVISIT: should we throw instead?
        GraphicalSymbol symbol = sym.getGraphic().graphicalSymbols().get(0);
        if (symbol instanceof Mark) {
            Mark mark = (Mark) symbol;
            String markName = evaluateWithDefault(mark.getWellKnownName(), "circle");
            final Color color;
            final double opacity;
            final double size;
            final double angle = evaluateWithDefault(sym.getGraphic().getRotation(), 0d);
            final double xoffset;
            final double yoffset;
            Fill fill = mark.getFill();
            if (fill != null) {
                color = evaluateWithDefault(fill.getColor(), Color.GRAY);
                opacity = evaluateWithDefault(fill.getOpacity(), 1d);
                size = evaluateWithDefault(sym.getGraphic().getSize(), 16);
            } else {
                color = Color.GRAY;
                opacity = 1d;
                size = 16d;
            }
            Displacement displacement = sym.getGraphic().getDisplacement();
            if (displacement != null) {
                xoffset = evaluateWithDefault(displacement.getDisplacementX(), 0d);
                yoffset = evaluateWithDefault(displacement.getDisplacementY(), 0d);
            } else {
                xoffset = 0d;
                yoffset = 0d;
            }

            final Outline outline;
            final Stroke stroke = mark.getStroke();
            if (stroke != null) {
                Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
                double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
                double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
                outline =
                        new Outline(
                                components(strokeColor, strokeOpacity),
                                (int) Math.round(strokeWidth));
            } else {
                outline = new Outline(components(Color.BLACK, 1d), 1);
            }
            return new SimpleMarkerSymbol(
                    equivalentSMS(markName),
                    components(color, opacity),
                    size,
                    angle,
                    xoffset,
                    yoffset,
                    outline);
        } else if (symbol instanceof ExternalGraphic) {
            ExternalGraphic exGraphic = (ExternalGraphic) symbol;
            URI resourceURI = exGraphic.getOnlineResource().getLinkage();
            byte[] rawData = new byte[4096];
            InputStream stream = null;
            try {
                stream = resourceURI.toURL().openStream();
                int pos = 0;
                int read = 0;
                while ((read = stream.read(rawData, pos, rawData.length - pos)) >= 0) {
                    pos += read;
                    if (pos == rawData.length) {
                        byte[] grown = new byte[rawData.length * 2];
                        System.arraycopy(rawData, 0, grown, 0, pos);
                        rawData = grown;
                    }
                }
                rawData = Arrays.copyOfRange(rawData, 0, pos);
            } catch (FileNotFoundException e) {
                return null;
            } catch (IOException e) {
                throw new RuntimeException("IO Error while loading image icon.", e);
            } finally {
                if (stream != null) {
                    try {
                        stream.close();
                    } catch (IOException e) {
                        // squelch
                    }
                }
            }

            String contentType = exGraphic.getFormat();
            Color color = Color.GRAY;
            Double width = evaluateWithDefault(sym.getGraphic().getSize(), 0d);
            Double height = width;
            if (width == null) {
                try {
                    BufferedImage image = ImageIO.read(new ByteArrayInputStream(rawData));
                    width = (double) image.getWidth();
                    height = (double) image.getHeight();
                } catch (IOException e) {
                    width = 16d;
                    height = width;
                }
            }
            double angle = evaluateWithDefault(sym.getGraphic().getRotation(), 0d);
            Displacement displacement = sym.getGraphic().getDisplacement();
            int xoffset =
                    displacement != null
                            ? evaluateWithDefault(
                                    sym.getGraphic().getDisplacement().getDisplacementX(), 0)
                            : 0;
            int yoffset =
                    displacement != null
                            ? evaluateWithDefault(
                                    sym.getGraphic().getDisplacement().getDisplacementY(), 0)
                            : 0;

            String url = relativizeExternalGraphicImageResourceURI(resourceURI);
            return new PictureMarkerSymbol(
                    rawData,
                    url,
                    contentType,
                    components(color, 1),
                    width,
                    height,
                    angle,
                    xoffset,
                    yoffset);
        }
        return null;
    }

    public static SimpleLineSymbol lineSymbolizerToLineSymbol(LineSymbolizer sym) {
        Stroke stroke = sym.getStroke();
        return strokeToLineSymbol(stroke);
    }

    private static SimpleLineSymbol strokeToLineSymbol(Stroke stroke) {
        final Color color;
        final double opacity;
        final double width;
        List<Float> dashArray;
        final SimpleLineSymbolEnum lineStyle;
        if (stroke != null) {
            opacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            color = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            width = evaluateWithDefault(stroke.getWidth(), 1d);
            dashArray = evaluateWithDefault(stroke.dashArray(), null, Float.class);
        } else {
            color = Color.BLACK;
            opacity = 1d;
            width = 1d;
            dashArray = null;
        }
        if (dashArray == null || dashArray.size() == 0) {
            lineStyle = SimpleLineSymbolEnum.SOLID;
        } else {
            Set<Float> uniqueValues = new java.util.HashSet<>();
            for (float f : dashArray) {
                uniqueValues.add(f);
            }
            if (uniqueValues.size() == 1) {
                if (uniqueValues.iterator().next() <= DOT_THRESHOLD) {
                    lineStyle = SimpleLineSymbolEnum.DOT;
                } else {
                    lineStyle = SimpleLineSymbolEnum.DASH;
                }
            } else if (uniqueValues.size() <= 3) {
                if (dashArray.size() <= 4) {
                    lineStyle = SimpleLineSymbolEnum.DASH_DOT;
                } else {
                    lineStyle = SimpleLineSymbolEnum.DASH_DOT_DOT;
                }
            } else {
                // no direct equivalent
                lineStyle = SimpleLineSymbolEnum.DASH;
            }
        }
        return new SimpleLineSymbol(lineStyle, components(color, opacity), width);
    }

    public static void encodeStyle(JSONBuilder json, Style style) {
        Symbolizer sym = getSingleSymbolizer(style);
        if (sym instanceof PointSymbolizer) {
            encodePointSymbolizer(json, (PointSymbolizer) sym);
        } else if (sym instanceof LineSymbolizer) {
            encodeLineSymbolizer(json, (LineSymbolizer) sym);
        } else if (sym instanceof PolygonSymbolizer) {
            encodePolygonSymbolizer(json, (PolygonSymbolizer) sym);
        }
    }

    public static void encodeSymbol(JSONBuilder json, Symbol symbol) {
        if (symbol instanceof SimpleMarkerSymbol) {
            encodeMarkerSymbol(json, (SimpleMarkerSymbol) symbol);
        } else if (symbol instanceof PictureMarkerSymbol) {
            encodePictureMarkerSymbol(json, (PictureMarkerSymbol) symbol);
        } else if (symbol instanceof SimpleFillSymbol) {
            encodeFillSymbol(json, (SimpleFillSymbol) symbol);
        } else if (symbol instanceof SimpleLineSymbol) {
            encodeLineStyle(json, (SimpleLineSymbol) symbol);
        } else {
            json.value(null);
        }
    }

    private static void encodePolygonSymbolizer(JSONBuilder json, PolygonSymbolizer sym) {
        final Fill fill = sym.getFill();
        final Stroke stroke = sym.getStroke();
        final Color color;
        final double opacity;
        final SimpleLineSymbol outline;
        if (fill != null) {
            color = evaluateWithDefault(fill.getColor(), Color.GRAY);
            opacity = evaluateWithDefault(fill.getOpacity(), 1d);
        } else {
            color = Color.GRAY;
            opacity = 1d;
        }
        if (stroke != null) {
            Color strokeColor = evaluateWithDefault(stroke.getColor(), Color.BLACK);
            double strokeOpacity = evaluateWithDefault(stroke.getOpacity(), 1d);
            double strokeWidth = evaluateWithDefault(stroke.getWidth(), 1d);
            outline =
                    new SimpleLineSymbol(
                            SimpleLineSymbolEnum.SOLID,
                            components(strokeColor, strokeOpacity),
                            strokeWidth);
        } else {
            outline =
                    new SimpleLineSymbol(SimpleLineSymbolEnum.SOLID, components(Color.BLACK, 1), 1);
        }

        encodeFillSymbol(
                json,
                new SimpleFillSymbol(
                        SimpleFillSymbolEnum.SOLID, components(color, opacity), outline));
    }

    private static void encodeFillSymbol(JSONBuilder json, SimpleFillSymbol sym) {
        json.object()
                .key("type")
                .value("esriSFS")
                .key("style")
                .value(sym.getStyle().getStyle())
                .key("color");
        writeInts(json, sym.getColor());
        json.key("outline");
        encodeLineStyle(json, sym.getOutline());
        json.endObject();
    }

    private static void encodeLineSymbolizer(JSONBuilder json, LineSymbolizer sym) {
        SimpleLineSymbol symbol = lineSymbolizerToLineSymbol(sym);
        encodeLineStyle(json, symbol);
    }

    private static int[] components(Color color, double opacity) {
        return new int[] {
            color.getRed(), color.getGreen(), color.getBlue(), (int) Math.round(opacity * 255)
        };
    }

    private static void encodePointSymbolizer(JSONBuilder json, PointSymbolizer sym) {
        MarkerSymbol markSymbol = pointSymbolizerToMarkSymbol(sym);
        if (markSymbol instanceof SimpleMarkerSymbol) {
            encodeMarkerSymbol(json, (SimpleMarkerSymbol) markSymbol);
        }
    }

    private static void encodeMarkerSymbol(JSONBuilder json, SimpleMarkerSymbol sms) {
        json.object()
                .key("type")
                .value("esriSMS")
                .key("style")
                .value(sms.getStyle().getStyle())
                .key("color");
        writeInts(json, sms.getColor());
        json.key("outline").object().key("type").value("SLS").key("style").value("SLSSolid");
        json.key("color");
        writeInts(json, sms.getOutline().getColor());
        json.key("width").value(sms.getOutline().getWidth()).endObject();
        json.key("angle").value(sms.getAngle());
        json.key("size").value(sms.getSize());
        json.key("xoffset").value(sms.getXoffset());
        json.key("yoffset").value(sms.getYoffset());
        json.endObject();
    }

    private static void encodePictureMarkerSymbol(JSONBuilder json, PictureMarkerSymbol symbol) {
        json.object()
                .key("type")
                .value("esriPMS")
                .key("url")
                .value(symbol.getUrl())
                .key("imageData")
                .value(symbol.getImageData())
                .key("contentType")
                .value(symbol.getContentType())
                .key("width")
                .value(symbol.getWidth())
                .key("height")
                .value(symbol.getHeight())
                .key("angle")
                .value(symbol.getAngle())
                .key("xoffset")
                .value(symbol.getXoffset())
                .key("yoffset")
                .value(symbol.getYoffset());
        json.endObject();
    }

    private static void encodeLineStyle(JSONBuilder json, SimpleLineSymbol symbol) {
        json.object()
                .key("type")
                .value("esriSLS")
                .key("style")
                .value(symbol.getStyle().getStyle())
                .key("color");
        writeInts(json, symbol.getColor());
        json.key("width").value(symbol.getWidth()).endObject();
    }

    private static void writeInts(JSONBuilder json, int[] color) {
        json.array();
        for (int c : color) json.value(c);
        json.endArray();
    }

    @SuppressWarnings("unchecked")
    private static <T> T evaluateWithDefault(Expression exp, T def) {
        if (exp == null || def == null) return def;
        try {
            return (T) exp.evaluate(null, def.getClass());
        } catch (IllegalArgumentException | ClassCastException e) {
            return def;
        }
    }

    private static <T> List<T> evaluateWithDefault(
            List<Expression> exps, List<T> def, Class<T> clazz) {
        if (exps == null) return def;
        try {
            List<T> list = new ArrayList<>();
            for (Expression exp : exps) {
                list.add(exp.evaluate(null, clazz));
            }
            return list;
        } catch (IllegalArgumentException | ClassCastException e) {
            return def;
        }
    }

    private static Symbolizer getSingleSymbolizer(Style style) {
        if (style.featureTypeStyles() == null) return null;
        if (style.featureTypeStyles().size() != 1) return null;
        FeatureTypeStyle ftStyle = style.featureTypeStyles().get(0);
        if (ftStyle.rules().size() != 1) return null;
        Rule rule = ftStyle.rules().get(0);
        if (rule.getFilter() != null) return null;
        if (rule.symbolizers().size() != 1) return null;
        return rule.symbolizers().get(0);
    }

    private static SimpleMarkerSymbolEnum equivalentSMS(String markName) {
        if ("circle".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSCircle;
        } else if ("x".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSX;
        } else if ("cross".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSCross;
        } else if ("square".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSSquare;
        } else if ("triangle".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSTriangle;
        } else if ("qgis://diamond".equals(markName)) {
            return SimpleMarkerSymbolEnum.esriSMSDiamond;
        } else {
            return SimpleMarkerSymbolEnum.esriSMSCircle;
        }
    }

    public static void encodeRenderer(JSONBuilder json, Renderer renderer) {
        if (renderer == null) {
            json.value(null);
        } else if (renderer instanceof SimpleRenderer) {
            encodeSimpleRenderer(json, (SimpleRenderer) renderer);
        } else if (renderer instanceof UniqueValueRenderer) {
            encodeUniqueValueRenderer(json, (UniqueValueRenderer) renderer);
        } else if (renderer instanceof ClassBreaksRenderer) {
            encodeClassBreaksRenderer(json, (ClassBreaksRenderer) renderer);
        } else throw new IllegalArgumentException("Unhandled renderer " + renderer);
    }

    private static void encodeClassBreaksRenderer(JSONBuilder json, ClassBreaksRenderer renderer) {
        // TODO Auto-generated method stub
        json.object()
                .key("type")
                .value("classBreaks")
                .key("field")
                .value(renderer.getField())
                .key("minValue")
                .value(renderer.getMinValue());

        json.key("classBreakInfos").array();

        for (ClassBreakInfo info : renderer.getClassBreakInfos()) {
            json.object();
            if (info.getClassMinValue() != null)
                json.key("classMinValue").value(info.getClassMinValue());
            json.key("classMaxValue")
                    .value(info.getClassMaxValue())
                    .key("label")
                    .value(info.getLabel())
                    .key("description")
                    .value(info.getDescription())
                    .key("symbol");
            encodeSymbol(json, info.getSymbol());
            json.endObject();
        }

        json.endArray().endObject();
    }

    private static void encodeSimpleRenderer(JSONBuilder json, SimpleRenderer renderer) {
        json.object().key("type").value("simple").key("symbol");
        encodeSymbol(json, renderer.getSymbol());
        json.key("label")
                .value(renderer.getLabel())
                .key("description")
                .value(renderer.getDescription())
                .endObject();
    }

    private static void encodeUniqueValueRenderer(JSONBuilder json, UniqueValueRenderer renderer) {
        json.object()
                .key("type")
                .value("uniqueValue")
                .key("field1")
                .value(renderer.getField1())
                .key("field2")
                .value(renderer.getField2())
                .key("field3")
                .value(renderer.getField3())
                .key("fieldDelimiter")
                .value(renderer.getFieldDelimiter())
                .key("defaultSymbol");
        encodeSymbol(json, renderer.getDefaultSymbol());
        json.key("defaultLabel").value(renderer.getDefaultLabel()).key("uniqueValueInfos");
        json.array();
        for (UniqueValueInfo info : renderer.getUniqueValueInfos()) {
            json.object()
                    .key("value")
                    .value(info.getValue())
                    .key("label")
                    .value(info.getLabel())
                    .key("description")
                    .value(info.getDescription())
                    .key("symbol");
            encodeSymbol(json, info.getSymbol());
            json.endObject();
        }
        json.endArray().endObject();
    }

    static String relativizeExternalGraphicImageResourceURI(URI resourceURI) {
        String path = resourceURI.getPath();
        int index = path.lastIndexOf('/');
        return "images/" + (index < 0 ? path : path.substring(index + 1));
    }
}
