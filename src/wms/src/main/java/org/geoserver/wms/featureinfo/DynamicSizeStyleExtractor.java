/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import org.geotools.renderer.style.DynamicSymbolFactoryFinder;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.renderer.style.ExternalGraphicFactory;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.visitor.DuplicatingStyleVisitor;
import org.geotools.util.logging.Logging;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.NilExpression;
import org.opengis.style.GraphicalSymbol;

/**
 * Extract the portion of the style whose sizes depend on attribute values
 *
 * @author Andrea Aime - GeoSolutions
 */
class DynamicSizeStyleExtractor extends DuplicatingStyleVisitor {

    static final Logger LOGGER = Logging.getLogger(DynamicSizeStyleExtractor.class);

    boolean dynamic = false;

    public void visit(Rule rule) {
        super.visit(rule);
        Rule copy = (Rule) pages.peek();
        List<Symbolizer> nonNullCopies = new ArrayList<Symbolizer>();
        for (Symbolizer s : copy.symbolizers()) {
            if (s != null) {
                nonNullCopies.add(s);
            }
        }

        if (nonNullCopies.size() == 0) {
            pages.pop();
            pages.push(null);
        } else {
            copy.symbolizers().clear();
            copy.symbolizers().addAll(nonNullCopies);
        }
    }

    @Override
    public void visit(Fill fill) {
        // whatever goes on in a Fill does not affect the search area of fills
        boolean dynamicPrevious = false;
        try {
            super.visit(fill);
        } finally {
            dynamic = dynamicPrevious;
        }
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        super.visit(fts);
        FeatureTypeStyle copy = (FeatureTypeStyle) pages.peek();
        List<Rule> nonNullCopies = new ArrayList<Rule>();
        for (Rule r : copy.rules()) {
            if (r != null) {
                nonNullCopies.add(r);
            }
        }

        if (nonNullCopies.size() == 0) {
            pages.pop();
            pages.push(null);
        } else {
            copy.rules().clear();
            copy.rules().addAll(nonNullCopies);
        }
    }

    @Override
    public void visit(Style style) {
        super.visit(style);
        Style copy = (Style) pages.peek();
        List<FeatureTypeStyle> nonNullCopies = new ArrayList<FeatureTypeStyle>();
        for (FeatureTypeStyle ft : copy.featureTypeStyles()) {
            if (ft != null) {
                nonNullCopies.add(ft);
            }
        }

        if (nonNullCopies.size() == 0) {
            pages.pop();
            pages.push(null);
        } else {
            copy.featureTypeStyles().clear();
            copy.featureTypeStyles().addAll(nonNullCopies);
        }
    }

    @Override
    public void visit(LineSymbolizer line) {
        dynamic = false;
        super.visit(line);
        if (!dynamic) {
            pages.pop();
            pages.push(null);
        }
    }

    @Override
    public void visit(PolygonSymbolizer poly) {
        dynamic = false;
        super.visit(poly);
        if (!dynamic) {
            pages.pop();
            pages.push(null);
        }
    }

    @Override
    public void visit(PointSymbolizer ps) {
        dynamic = false;
        super.visit(ps);
        if (!dynamic) {
            pages.pop();
            pages.push(null);
        }
    }

    @Override
    public void visit(RasterSymbolizer raster) {
        // nothing to do, this style cannot make the buffer grow
        pages.push(null);
    }

    @Override
    public void visit(TextSymbolizer text) {
        // nothing to do, this style cannot make the buffer grow
        pages.push(null);
    }

    @Override
    public void visit(Graphic gr) {
        super.visit(gr);
        Expression sizeExpression = gr.getSize();
        if (!dynamic) {
            dynamic =
                    !(sizeExpression != null
                                    && (sizeExpression instanceof Literal
                                            || sizeExpression instanceof NilExpression))
                            || hasDynamicGraphic(gr);
        }
    }

    private boolean hasDynamicGraphic(Graphic gr) {
        // not a fixed size, let's see if it has dynamic graphics inside
        for (GraphicalSymbol gs : gr.graphicalSymbols()) {
            if (gs instanceof ExternalGraphic) {
                ExternalGraphic eg = (ExternalGraphic) gs;
                try {
                    Icon icon = null;
                    if (eg.getInlineContent() != null) {
                        icon = eg.getInlineContent();
                    } else {
                        String location = eg.getLocation().toExternalForm();
                        // expand embedded cql expression
                        Expression expanded = ExpressionExtractor.extractCqlExpressions(location);
                        // if not a literal there is an attribute dependency
                        if (!(expanded instanceof Literal)) {
                            return true;
                        }

                        Iterator<ExternalGraphicFactory> it =
                                DynamicSymbolFactoryFinder.getExternalGraphicFactories();
                        while (it.hasNext()) {
                            try {
                                icon = it.next().getIcon(null, expanded, eg.getFormat(), 16);
                            } catch (Exception e) {
                                LOGGER.log(
                                        Level.FINE,
                                        "Error occurred evaluating external graphic",
                                        e);
                            }
                        }

                        // evaluate the icon if found, if not SLD asks us to go to the next one
                        // and thus we should ignore this one
                        if (icon != null) {
                            break;
                        }
                    }
                } catch (MalformedURLException e) {
                    LOGGER.log(
                            Level.FINE,
                            "Failed to check graphics for attribute embedded in the path " + eg,
                            e);
                }
            }
        }

        return false;
    }

    @Override
    public void visit(Stroke stroke) {
        super.visit(stroke);
        if (!(stroke.getWidth() instanceof Literal)) {
            dynamic = true;
        }
    }
}
