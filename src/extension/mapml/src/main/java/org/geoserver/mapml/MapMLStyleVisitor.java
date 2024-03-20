/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.mapml;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.GraphicalSymbol;
import org.geotools.api.style.LineSymbolizer;
import org.geotools.api.style.Mark;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.PolygonSymbolizer;
import org.geotools.api.style.RasterSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Stroke;
import org.geotools.api.style.Symbol;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.filter.visitor.IsStaticExpressionVisitor;
import org.geotools.styling.AbstractStyleVisitor;
import org.geotools.util.logging.Logging;

/**
 * MapML Style Visitor to convert a limited set of SLD elements into CSS style classes for use with
 * MapML XML feature rendering on the client-side. Includes support for basic rendering elements
 * (static only) of PointSymbolizer, LineSymbolizer and PolygonSymbolizer. Skips and logs
 * unsupported elements.
 */
public class MapMLStyleVisitor extends AbstractStyleVisitor {
    static final Logger LOGGER = Logging.getLogger(MapMLStyleVisitor.class);
    public static final String OPACITY = "opacity";

    /** Default radius for point symbolizers, see @link{Graphic#getSize()} */
    private static final Double DEFAULT_RADIUS = 8.0;

    public static final String STROKE_OPACITY = "stroke-opacity";
    public static final String STROKE_DASHARRAY = "stroke-dasharray";
    public static final String STROKE_LINECAP = "stroke-linecap";
    public static final String STROKE_WIDTH = "stroke-width";
    public static final String STROKE = "stroke";
    public static final String FILL = "fill";
    public static final String RADIUS = "r";
    public static final String STROKE_DASHOFFSET = "stroke-dashoffset";
    public static final String FILL_OPACITY = "fill-opacity";

    /** Tolerance used to compare doubles for equality */
    static final double TOLERANCE = 1e-6;

    Double scaleDenominator;

    Map<String, MapMLStyle> styles = new HashMap<>();
    int ruleCounter = 0;
    int symbolizerCounter = 0;
    boolean isElseFilter = false;

    Filter filter;

    private MapMLStyle style;

    @Override
    public void visit(FeatureTypeStyle fts) {
        for (Rule r : fts.rules()) {
            if (scaleDenominator != null && !isWithInScale(r, scaleDenominator)) {
                // skip rules that are not active at this scale
                continue;
            }
            filter = r.getFilter();
            isElseFilter = r.isElseFilter();
            symbolizerCounter = 0;
            ruleCounter++;
            r.accept(this);
        }
    }

    @Override
    public void visit(Fill fill) {
        if (fill.getGraphicFill() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "MapML feature styling does not currently support Graphic Fills");
            }
            return;
        }
        if (isNotNullAndIsStatic(fill.getColor())) {
            String value = fill.getColor().evaluate(null, String.class);
            style.setProperty(FILL, value);
        }
        if (isNotNullAndIsStatic(fill.getOpacity())) {
            Double value = fill.getOpacity().evaluate(null, Double.class);
            style.setProperty(FILL_OPACITY, String.valueOf(value));
        }
    }

    @Override
    public void visit(Stroke stroke) {
        if (stroke.getGraphicStroke() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "MapML feature styling does not currently support Graphic Strokes");
            }
            return;
        }
        if (stroke.getGraphicFill() != null) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "MapML feature styling does not currently support Stroke Graphic Fills");
            }
            return;
        }
        if (isNotNullAndIsStatic(stroke.getColor())) {
            String value = stroke.getColor().evaluate(null, String.class);
            style.setProperty(STROKE, value);
        }
        if (isNotNullAndIsStatic(stroke.getOpacity())) {
            Double value = stroke.getOpacity().evaluate(null, Double.class);
            style.setProperty(STROKE_OPACITY, String.valueOf(value));
        }
        if (isNotNullAndIsStatic(stroke.getWidth())) {
            Double value = stroke.getWidth().evaluate(null, Double.class);
            style.setProperty(STROKE_WIDTH, String.valueOf(value));
        }
        if (isNotNullAndIsStatic(stroke.getLineCap())) {
            String value = stroke.getLineCap().evaluate(null, String.class);
            style.setProperty(STROKE_LINECAP, value);
        }
        if (stroke.getDashArray() != null && stroke.getDashArray().length > 0) {
            String value =
                    IntStream.range(0, stroke.getDashArray().length)
                            .mapToObj(i -> String.valueOf(stroke.getDashArray()[i]))
                            .collect(Collectors.joining(" "));
            style.setProperty(STROKE_DASHARRAY, value);
        }
        if (isNotNullAndIsStatic(stroke.getDashOffset())) {
            Integer value = stroke.getDashOffset().evaluate(null, Integer.class);
            style.setProperty(STROKE_DASHOFFSET, String.valueOf(value));
        }

        if (stroke.getLineJoin() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MapML feature styling does not currently support Line Join");
        }
    }

    @Override
    public void visit(Symbolizer sym) {
        if (sym instanceof RasterSymbolizer) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "MapML feature styling does not currently support Raster Symbolizers");
            }
        } else if (sym instanceof LineSymbolizer) {
            visit((LineSymbolizer) sym);
        } else if (sym instanceof PolygonSymbolizer) {
            visit((PolygonSymbolizer) sym);
        } else if (sym instanceof PointSymbolizer) {
            visit((PointSymbolizer) sym);
        } else if (sym instanceof TextSymbolizer) {
            if (LOGGER.isLoggable(Level.FINE)) {
                LOGGER.log(
                        Level.FINE,
                        "MapML feature styling does not currently support Text Symbolizers");
            }
        } else {
            throw new RuntimeException("visit(Symbolizer) unsupported");
        }
    }

    @Override
    public void visit(PointSymbolizer ps) {
        createStyle(ps);
        if (ps.getGraphic() != null) {
            ps.getGraphic().accept(this);
        }
    }

    @Override
    public void visit(Graphic gr) {
        if (isNotNullAndIsStatic(gr.getOpacity())) {
            double value = gr.getOpacity().evaluate(null, Double.class);
            style.setProperty(OPACITY, String.valueOf(value));
        }
        Double radius = DEFAULT_RADIUS;
        if (isNotNullAndIsStatic(gr.getSize())) {
            double value = gr.getSize().evaluate(null, Double.class);
            radius = value * DEFAULT_RADIUS;
        }
        style.setProperty(RADIUS, String.valueOf(radius));
        if (gr.getRotation() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Graphic Rotation");
        }
        if (gr.getDisplacement() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Graphic Displacement");
        }
        if (gr.getAnchorPoint() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Graphic Anchor Point");
        }
        if (gr.getGap() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(Level.FINE, "MapML feature styling does not currently support Graphic Gap");
        }
        if (gr.getInitialGap() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Graphic Initial Gap");
        }

        for (GraphicalSymbol gs : gr.graphicalSymbols()) {
            if (!(gs instanceof Symbol)) {
                throw new RuntimeException("Don't know how to visit " + gs);
            }

            gs.accept(this);
        }
    }

    @Override
    public void visit(Mark mark) {
        if (isNotNullAndIsStatic(mark.getWellKnownName())) {
            String value = mark.getWellKnownName().evaluate(null, String.class);
            style.setProperty("well-known-name", value);
        }
        if (mark.getExternalMark() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE, "MapML feature styling does not currently support External Marks");
        }
        if (mark.getFill() != null) {
            mark.getFill().accept(this);
        }
        if (mark.getStroke() != null) {
            mark.getStroke().accept(this);
        }
    }

    @Override
    public void visit(LineSymbolizer line) {
        createStyle(line);
        if (line.getPerpendicularOffset() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Line Perpendicular Offset");
        }
        if (line.getStroke() != null) {
            line.getStroke().accept(this);
        }
    }

    @Override
    public void visit(PolygonSymbolizer poly) {
        createStyle(poly);
        if (poly.getDisplacement() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Polygon Displacement");
        }
        if (poly.getPerpendicularOffset() != null && LOGGER.isLoggable(Level.FINE)) {
            LOGGER.log(
                    Level.FINE,
                    "MapML feature styling does not currently support Polygon Perpendicular Offset");
        }

        if (poly.getFill() != null) {
            poly.getFill().accept(this);
        }
        if (poly.getStroke() != null) {
            poly.getStroke().accept(this);
        }
    }

    /**
     * Check if the expression is not null and is static
     *
     * @param ex the expression
     * @return true if the expression is not null and is static
     */
    private boolean isNotNullAndIsStatic(Expression ex) {
        return ex != null && (Boolean) ex.accept(IsStaticExpressionVisitor.VISITOR, null);
    }

    /**
     * Create a style for a symbolizer
     *
     * @param sym the symbolizer
     */
    private void createStyle(Symbolizer sym) {
        style = new MapMLStyle();
        style.setRuleId(ruleCounter);
        style.setSymbolizerId(++symbolizerCounter);
        style.setSymbolizerType(sym.getClass().getSimpleName());
        style.setElseFilter(isElseFilter);
        style.setFilter(filter);
        styles.put(style.getCSSClassName(), style);
    }

    /**
     * Get the styles
     *
     * @return the styles objects
     */
    public Map<String, MapMLStyle> getStyles() {
        return styles;
    }

    /**
     * Set the scale denominator
     *
     * @param scaleDenominator the scale denominator
     */
    public void setScaleDenominator(Double scaleDenominator) {
        this.scaleDenominator = scaleDenominator;
    }

    /**
     * Check if the rule is within the scale
     *
     * @param r the rule
     * @param scaleDenominator the scale denominator
     * @return true if the rule is within the scale
     */
    private boolean isWithInScale(Rule r, double scaleDenominator) {
        return ((r.getMinScaleDenominator() - TOLERANCE) <= scaleDenominator)
                && ((r.getMaxScaleDenominator() + TOLERANCE) > scaleDenominator);
    }
}
