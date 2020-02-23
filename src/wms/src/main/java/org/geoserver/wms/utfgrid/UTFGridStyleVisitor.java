/* (c) 2015 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.utfgrid;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.geotools.feature.FeatureCollection;
import org.geotools.renderer.composite.BlendComposite.BlendingMode;
import org.geotools.renderer.style.GraphicStyle2D;
import org.geotools.renderer.style.IconStyle2D;
import org.geotools.renderer.style.SLDStyleFactory;
import org.geotools.renderer.style.Style2D;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.opengis.filter.capability.FunctionName;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Function;
import org.opengis.filter.expression.Literal;
import org.opengis.style.GraphicalSymbol;

/**
 * Prepares a style for a UTFGrid generation, in particular:
 *
 * <ul>
 *   <li>Removes all feature type styles with a transform function that is known not to return
 *       vector data
 *   <li>
 *   <li>Replaces all colors with the {@link UTFGridColorFunction}
 *   <li>Replaces all external graphics with an "equivalent" solid color mark (ideally this would be
 *       a black and white version of the external graphic, with the same shape, but it's hard, so
 *       we use a square instead)
 *   <li>Removes all text symbolizers
 * </ul>
 *
 * @author Andrea Aime - GeoSolutions
 */
class UTFGridStyleVisitor extends DuplicatingStyleVisitor {

    private UTFGridColorFunction colorFunction;

    /** Potentially vector transformations (including ones with unknown return type) */
    boolean vectorTransformations = false;

    boolean transformations = false;

    private final Literal LITERAL_ONE = ff.literal(1);

    SLDStyleFactory sldFactory = new SLDStyleFactory();

    public UTFGridStyleVisitor(UTFGridColorFunction colorFunction) {
        this.colorFunction = colorFunction;
    }

    public void visit(Style style) {
        super.visit(style);
        Style copy = (Style) pages.pop();
        List<FeatureTypeStyle> featureTypeStyles = new ArrayList(copy.featureTypeStyles());
        for (Iterator<FeatureTypeStyle> it = featureTypeStyles.iterator(); it.hasNext(); ) {
            FeatureTypeStyle fts = it.next();
            if (fts.rules().isEmpty()) {
                it.remove();
            }
        }
        copy.featureTypeStyles().clear();
        copy.featureTypeStyles().addAll(featureTypeStyles);
        pages.push(copy);
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        super.visit(fts);

        FeatureTypeStyle copy = (FeatureTypeStyle) pages.peek();

        // clean up empty rules
        List<Rule> rules = new ArrayList<>(copy.rules());
        for (Iterator<Rule> it = rules.iterator(); it.hasNext(); ) {
            Rule rule = it.next();
            if (rule.symbolizers().isEmpty()) {
                it.remove();
            }
        }
        copy.rules().clear();
        copy.rules().addAll(rules);

        // try to shave off transformations that won't generate a vector rendering
        if (copy.getTransformation() instanceof Function) {
            transformations = true;
            Function f = (Function) fts.getTransformation();
            Class returnType = getFunctionReturnType(f);
            if (Object.class.equals(returnType)
                    || FeatureCollection.class.isAssignableFrom(returnType)) {
                vectorTransformations = true;
                super.visit(fts);
            } else {
                // shave off
                copy.rules().clear();
            }
        }

        // remove color altering vendor options (colors are keys, we cannot have them be altered)
        Map<String, String> options = copy.getOptions();
        String composite = options.get(FeatureTypeStyle.COMPOSITE);
        if (composite != null && BlendingMode.lookupByName(composite) != null) {
            options.remove(FeatureTypeStyle.COMPOSITE);
            options.remove(FeatureTypeStyle.COMPOSITE_BASE);
        }
    }

    public void visit(Rule rule) {
        super.visit(rule);
        // clean up removed symbolizers
        Rule copy = (Rule) pages.pop();
        List<Symbolizer> symbolizers = new ArrayList(copy.symbolizers());
        for (Iterator<Symbolizer> it = symbolizers.iterator(); it.hasNext(); ) {
            Symbolizer symbolizer = it.next();
            if (symbolizer == null) {
                it.remove();
            }
        }
        copy.symbolizers().clear();
        copy.symbolizers().addAll(symbolizers);
        pages.push(copy);
    };

    /** Returns the function return type, or {@link Object} if it could not be determined */
    Class getFunctionReturnType(Function f) {
        FunctionName name = f.getFunctionName();
        if (name == null || name.getReturn() == null) {
            return Object.class;
        }
        return name.getReturn().getType();
    }

    @Override
    public void visit(Graphic gr) {
        Graphic copy = null;

        Displacement displacementCopy = copy(gr.getDisplacement());
        Expression rotationCopy = copy(gr.getRotation());
        Expression sizeCopy = copy(gr.getSize());
        AnchorPoint anchorCopy = copy(gr.getAnchorPoint());

        // copy the symbols replacing them with a same sized mark filled with the color function
        List<GraphicalSymbol> symbolsCopy = new ArrayList<>();
        for (GraphicalSymbol gs : gr.graphicalSymbols()) {
            if (gs instanceof Mark) {
                Mark markCopy = copy((Mark) gs);
                symbolsCopy.add(markCopy);
            } else if (gs instanceof ExternalGraphic) {
                if (gr.getSize() != null && !Expression.NIL.equals(gr.getSize())) {
                    Mark mark =
                            sf.createMark(
                                    ff.literal("square"),
                                    null,
                                    sf.createFill(colorFunction),
                                    sizeCopy,
                                    Expression.NIL);
                    symbolsCopy.add(mark);
                } else {
                    // it's using the default size, compute it if possible (might be using dynamic
                    // symbolizers...)
                    ExternalGraphic eg = (ExternalGraphic) gs;
                    Literal sizeExpression = estimateGraphicSize(eg);
                    Mark mark =
                            sf.createMark(
                                    ff.literal("square"),
                                    null,
                                    sf.createFill(colorFunction),
                                    sizeExpression,
                                    Expression.NIL);
                    symbolsCopy.add(mark);
                }
            }
        }

        copy = sf.createDefaultGraphic();
        copy.setDisplacement(displacementCopy);
        copy.setAnchorPoint(anchorCopy);
        copy.setOpacity(LITERAL_ONE);
        copy.setRotation(rotationCopy);
        copy.setSize(sizeCopy);
        copy.graphicalSymbols().clear();
        copy.graphicalSymbols().addAll(symbolsCopy);

        if (STRICT) {
            if (!copy.equals(gr)) {
                throw new IllegalStateException("Was unable to duplicate provided Graphic:" + gr);
            }
        }
        pages.push(copy);
    }

    private Literal estimateGraphicSize(ExternalGraphic eg) {
        Graphic testGraphic =
                sf.createGraphic(
                        new ExternalGraphic[] {eg},
                        null,
                        null,
                        LITERAL_ONE,
                        Expression.NIL,
                        ff.literal(0));
        PointSymbolizer testSymbolizer = sf.createPointSymbolizer(testGraphic, null);
        Style2D style = sldFactory.createStyle(null, testSymbolizer);
        int size = SLDStyleFactory.DEFAULT_MARK_SIZE;
        if (style instanceof GraphicStyle2D) {
            GraphicStyle2D gs2d = (GraphicStyle2D) style;
            size = gs2d.getImage().getWidth();
        } else if (style instanceof IconStyle2D) {
            IconStyle2D is2d = (IconStyle2D) style;
            size = is2d.getIcon().getIconWidth();
        }
        Literal sizeExpression = ff.literal(size);
        return sizeExpression;
    }

    @Override
    public void visit(Fill fill) {
        super.visit(fill);
        Fill copy = (Fill) pages.peek();
        if (copy.getGraphicFill() != null) {
            copy.setGraphicFill(null);
        }
        copy.setColor(colorFunction);
        copy.setOpacity(LITERAL_ONE);
    }

    @Override
    public void visit(Stroke stroke) {
        super.visit(stroke);
        Stroke copy = (Stroke) pages.peek();
        if (copy.getGraphicFill() != null) {
            copy.setGraphicFill(null);
        }
        if (copy.getGraphicStroke() != null) {
            copy.setWidth(getSymbolsSize(copy.getGraphicStroke()));
            copy.setGraphicStroke(null);
        }
        copy.setColor(colorFunction);
        copy.setOpacity(LITERAL_ONE);
        if (copy.dashArray() != null) {
            copy.dashArray().clear();
        }
        copy.setDashOffset(null);
    }

    private Expression getSymbolsSize(Graphic graphic) {
        Expression size = graphic.getSize();
        if (size != null && !Expression.NIL.equals(size)) {
            return size;
        } else {
            for (GraphicalSymbol gs : graphic.graphicalSymbols()) {
                if (gs instanceof Mark) {
                    return ff.literal(SLDStyleFactory.DEFAULT_MARK_SIZE);
                } else if (gs instanceof ExternalGraphic) {
                    return estimateGraphicSize((ExternalGraphic) gs);
                }
            }
        }
        return ff.literal(SLDStyleFactory.DEFAULT_MARK_SIZE);
    }

    @Override
    public void visit(TextSymbolizer text) {
        // eliminate text symbolizers
        pages.push(null);
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        pages.push(null);
    }

    public boolean hasVectorTransformations() {
        return vectorTransformations;
    }

    public boolean hasTransformations() {
        return transformations;
    }
}
