/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.filter.visitor.IsStaticExpressionVisitor;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleFactory;
import org.geotools.styling.Symbol;
import org.geotools.styling.Symbolizer;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.expression.Expression;
import org.opengis.style.GraphicalSymbol;

/**
 * Utility to inject static property values into a style
 *
 * @author David Winslow, OpenGeo
 */
public final class IconPropertyInjector {
    private final FilterFactory filterFactory;
    private final StyleFactory styleFactory;

    private final Map<String, String> properties;

    private IconPropertyInjector(Map<String, String> properties) {
        this.filterFactory = CommonFactoryFinder.getFilterFactory();
        this.styleFactory = CommonFactoryFinder.getStyleFactory();
        this.properties = properties;
    }

    private List<List<MiniRule>> injectProperties(List<List<MiniRule>> ftStyles) {
        List<List<MiniRule>> result = new ArrayList<List<MiniRule>>();
        for (int ftIdx = 0; ftIdx < ftStyles.size(); ftIdx++) {
            List<MiniRule> origRules = ftStyles.get(ftIdx);
            List<MiniRule> resultRules = new ArrayList<MiniRule>();
            for (int ruleIdx = 0; ruleIdx < origRules.size(); ruleIdx++) {
                MiniRule origRule = origRules.get(ruleIdx);
                List<Symbolizer> resultSymbolizers = new ArrayList<>();
                for (int symbIdx = 0; symbIdx < origRule.symbolizers.size(); symbIdx++) {
                    String key = ftIdx + "." + ruleIdx + "." + symbIdx;
                    if (properties.containsKey(key)) {
                        Symbolizer sym = origRule.symbolizers.get(symbIdx);
                        resultSymbolizers.add(injectPointSymbolizer(key, sym));
                    }
                }
                resultRules.add(new MiniRule(null, false, resultSymbolizers));
            }
            result.add(resultRules);
        }
        return result;
    }

    private boolean isStatic(Expression ex) {
        return (Boolean) ex.accept(IsStaticExpressionVisitor.VISITOR, null);
    }

    private boolean shouldUpdate(String key, Expression exp) {
        return exp != null && properties.containsKey(key) && !isStatic(exp);
    }

    private Expression getLiteral(String key) {
        return filterFactory.literal(properties.get(key));
    }

    private PointSymbolizer injectPointSymbolizer(String key, Symbolizer original) {
        PointSymbolizer copy = styleFactory.createPointSymbolizer();
        Graphic graphic = IconPropertyExtractor.getGraphic(original, true);
        if (graphic != null) {
            copy.setGraphic(injectGraphic(key, graphic));
        }
        return copy;
    }

    private Graphic injectGraphic(String key, Graphic original) {
        final ExternalGraphic[] externalGraphics;
        final Mark[] marks;
        final Symbol[] symbols = new Symbol[0];
        Expression opacity = original.getOpacity();
        Expression size = original.getSize();
        Expression rotation = original.getRotation();

        if (shouldUpdate(key + ".opacity", opacity)) {
            opacity = getLiteral(key + ".opacity");
        }
        if (shouldUpdate(key + ".rotation", rotation)) {
            rotation = getLiteral(key + ".rotation");
        }
        if (shouldUpdate(key + ".size", size)) {
            size = getLiteral(key + ".size");
        }

        if (!original.graphicalSymbols().isEmpty()) {
            List<Mark> markList = new ArrayList<Mark>();
            List<ExternalGraphic> externalGraphicList = new ArrayList<ExternalGraphic>();
            for (GraphicalSymbol symbol : original.graphicalSymbols()) {
                if (symbol instanceof Mark) {
                    markList.add(injectMark(key, (Mark) symbol));
                } else if (symbol instanceof ExternalGraphic) {
                    externalGraphicList.add(injectExternalGraphic(key, (ExternalGraphic) symbol));
                }
            }
            marks = markList.toArray(new Mark[0]);
            externalGraphics = externalGraphicList.toArray(new ExternalGraphic[0]);
        } else {
            marks = new Mark[0];
            externalGraphics = new ExternalGraphic[0];
        }
        return styleFactory.createGraphic(
                externalGraphics, marks, symbols, opacity, size, rotation);
    }

    private ExternalGraphic injectExternalGraphic(String key, ExternalGraphic original) {
        try {
            final String format = original.getFormat();
            final URL location;
            final Expression locationExpression;
            if (original.getLocation() == null) {
                locationExpression = null;
            } else {
                locationExpression =
                        ExpressionExtractor.extractCqlExpressions(
                                original.getLocation().toExternalForm());
            }

            if (locationExpression == null || isStatic(locationExpression)) {
                location = original.getLocation();
            } else {
                location = new URL(properties.get(key + ".url"));
            }
            return styleFactory.createExternalGraphic(location, format);
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }

    private Mark injectMark(String key, Mark mark) {
        final Expression wellKnownName;
        final Stroke stroke;
        final Fill fill;
        final Expression size =
                null; // size and fill are handled only at the PointSymbolizer level - bug?
        final Expression rotation = null;

        if (mark.getWellKnownName() == null || isStatic(mark.getWellKnownName())) {
            wellKnownName = mark.getWellKnownName();
        } else {
            wellKnownName = getLiteral(key + ".name");
        }

        if (mark.getFill() == null) {
            fill = null;
        } else {
            fill = injectFill(key + ".fill", mark.getFill());
        }

        if (mark.getStroke() == null) {
            stroke = null;
        } else {
            stroke = injectStroke(key + ".stroke", mark.getStroke());
        }
        return styleFactory.createMark(wellKnownName, stroke, fill, size, rotation);
    }

    private Stroke injectStroke(String key, Stroke stroke) {
        final Expression color;
        final Expression width;
        final Expression opacity;
        final Expression lineJoin;
        final Expression lineCap;
        final float[] dashArray;
        final Expression dashOffset;
        final Graphic graphicFill;
        final Graphic graphicStroke;

        if (stroke.getColor() == null || isStatic(stroke.getColor())) {
            color = stroke.getColor();
        } else {
            color = getLiteral(key + ".color");
        }

        if (stroke.getDashOffset() == null || isStatic(stroke.getDashOffset())) {
            dashOffset = stroke.getDashOffset();
        } else {
            dashOffset = getLiteral(key + ".linecap");
        }

        if (stroke.getLineCap() == null || isStatic(stroke.getDashOffset())) {
            lineCap = stroke.getLineCap();
        } else {
            lineCap = getLiteral(key + ".linecap");
        }

        if (stroke.getLineJoin() == null || isStatic(stroke.getLineJoin())) {
            lineJoin = stroke.getLineJoin();
        } else {
            lineJoin = getLiteral(key + ".linejoin");
        }

        if (stroke.getOpacity() == null || isStatic(stroke.getOpacity())) {
            opacity = stroke.getOpacity();
        } else {
            opacity = getLiteral(key + ".opacity");
        }

        if (stroke.getWidth() == null || isStatic(stroke.getWidth())) {
            width = stroke.getOpacity();
        } else {
            width = getLiteral(key + ".opacity");
        }

        if (stroke.getGraphicStroke() == null) {
            graphicStroke = null;
        } else {
            graphicStroke = injectGraphic(key + ".graphic", stroke.getGraphicStroke());
        }

        if (stroke.getGraphicFill() == null) {
            graphicFill = null;
        } else {
            graphicFill = injectGraphic(key + ".graphic", stroke.getGraphicFill());
        }

        return styleFactory.createStroke(
                color,
                width,
                opacity,
                lineJoin,
                lineCap,
                stroke.getDashArray(),
                dashOffset,
                graphicFill,
                graphicStroke);
    }

    private Fill injectFill(String key, Fill fill) {
        final Expression color;
        final Expression backgroundColor = null;
        final Expression opacity;
        final Graphic graphicFill;

        if (fill.getColor() == null || isStatic(fill.getColor())) {
            color = fill.getColor();
        } else {
            color = getLiteral(key + ".color");
        }

        if (fill.getOpacity() == null || isStatic(fill.getOpacity())) {
            opacity = fill.getOpacity();
        } else {
            opacity = getLiteral(key + ".opacity");
        }

        if (fill.getGraphicFill() == null) {
            graphicFill = null;
        } else {
            graphicFill = injectGraphic(key + ".graphic", fill.getGraphicFill());
        }

        return styleFactory.createFill(color, backgroundColor, opacity, graphicFill);
    }

    public static Style injectProperties(Style style, Map<String, String> properties) {
        boolean includeNonPointGraphics =
                Boolean.valueOf(
                        properties.getOrDefault(
                                IconPropertyExtractor.NON_POINT_GRAPHIC_KEY, "false"));
        List<List<MiniRule>> ftStyles = MiniRule.minify(style, includeNonPointGraphics);
        StyleFactory factory = CommonFactoryFinder.getStyleFactory();
        return MiniRule.makeStyle(
                factory, new IconPropertyInjector(properties).injectProperties(ftStyles));
    }
}
