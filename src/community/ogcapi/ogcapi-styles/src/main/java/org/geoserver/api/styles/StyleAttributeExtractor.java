/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2002-2015, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geoserver.api.styles;

import java.awt.*;
import java.net.MalformedURLException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import org.geotools.filter.FilterAttributeExtractor;
import org.geotools.renderer.style.ExpressionExtractor;
import org.geotools.styling.AnchorPoint;
import org.geotools.styling.ChannelSelection;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.ContrastEnhancement;
import org.geotools.styling.Displacement;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeConstraint;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Font;
import org.geotools.styling.Graphic;
import org.geotools.styling.Halo;
import org.geotools.styling.ImageOutline;
import org.geotools.styling.LinePlacement;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.OverlapBehavior;
import org.geotools.styling.PointPlacement;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SelectedChannelType;
import org.geotools.styling.ShadedRelief;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleVisitor;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.geotools.styling.Symbol;
import org.geotools.styling.Symbolizer;
import org.geotools.styling.TextSymbolizer;
import org.geotools.styling.TextSymbolizer2;
import org.geotools.styling.UserLayer;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.Literal;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.Geometry;
import org.opengis.style.GraphicalSymbol;

/**
 * A clone of StyleAttributeExtractor that can provide hints about the data type needed for
 * properties. Will be moved back to GeoTools once we have some time to write proper tests for it.
 */
public class StyleAttributeExtractor extends FilterAttributeExtractor implements StyleVisitor {

    /** if the default geometry is used, this will be true. See GEOS-469 */
    boolean defaultGeometryUsed = false;

    /**
     * Symbolizer geometry is enabled by default, but there are relevant cases in which we don't
     * desire that
     */
    boolean symbolizerGeometriesVisitEnabled = true;

    Map<PropertyName, Class> propertyTypes = new LinkedHashMap<>();

    public StyleAttributeExtractor() {
        // simple case
    }

    public StyleAttributeExtractor(SimpleFeatureType featureType) {
        // allows evaluation of attributes against types
        super(featureType);
    }

    /**
     * Returns PropertyNames rather than strings (includes namespace info)
     *
     * @return an array of the attribute found so far during the visit
     */
    public Set<PropertyName> getAttributes() {
        return Collections.unmodifiableSet(propertyNames);
    }

    public boolean isSymbolizerGeometriesVisitEnabled() {
        return symbolizerGeometriesVisitEnabled;
    }

    /** Enables/disables visit of the symbolizer geometry property (on by default) */
    public void setSymbolizerGeometriesVisitEnabled(boolean symbolizerGeometriesVisitEnabled) {
        this.symbolizerGeometriesVisitEnabled = symbolizerGeometriesVisitEnabled;
    }

    /**
     * reads the read-only-property. See GEOS-469
     *
     * @return true if any of the symbolizers visted use the default geometry.
     */
    public boolean getDefaultGeometryUsed() {
        return defaultGeometryUsed;
    }

    /** @see StyleVisitor#visit(Style) */
    public void visit(Style style) {
        style.featureTypeStyles().forEach(ft -> ft.accept(this));
    }

    /** @see StyleVisitor#visit(Rule) */
    public void visit(Rule rule) {
        Filter filter = rule.getFilter();

        if (filter != null) {
            filter.accept(this, null);
        }

        rule.symbolizers().forEach(s -> s.accept(this));
    }

    /** @see StyleVisitor#visit(FeatureTypeStyle) */
    public void visit(FeatureTypeStyle fts) {
        for (Rule rule : fts.rules()) {
            rule.accept(this);
        }
    }

    /** @see StyleVisitor#visit(Fill) */
    public void visit(Fill fill) {
        if (fill.getColor() != null) {
            fill.getColor().accept(this, Color.class);
        }

        if (fill.getGraphicFill() != null) {
            fill.getGraphicFill().accept(this);
        }

        if (fill.getOpacity() != null) {
            fill.getOpacity().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(Stroke) */
    public void visit(Stroke stroke) {
        if (stroke.getColor() != null) {
            stroke.getColor().accept(this, Color.class);
        }

        if (stroke.getDashOffset() != null) {
            stroke.getDashOffset().accept(this, Double.class);
        }

        if (stroke.getGraphicFill() != null) {
            stroke.getGraphicFill().accept(this);
        }

        if (stroke.getGraphicStroke() != null) {
            stroke.getGraphicStroke().accept(this);
        }

        if (stroke.getLineCap() != null) {
            stroke.getLineCap().accept(this, String.class);
        }

        if (stroke.getLineJoin() != null) {
            stroke.getLineJoin().accept(this, String.class);
        }

        if (stroke.getOpacity() != null) {
            stroke.getOpacity().accept(this, Double.class);
        }

        if (stroke.getWidth() != null) {
            stroke.getWidth().accept(this, Double.class);
        }

        if (stroke.dashArray() != null) {
            for (Expression expression : stroke.dashArray()) {
                expression.accept(this, double[].class);
            }
        }
    }

    /** @see StyleVisitor#visit(Symbolizer) */
    public void visit(Symbolizer sym) {
        if (sym instanceof PointSymbolizer) {
            visit((PointSymbolizer) sym);
        }

        if (sym instanceof LineSymbolizer) {
            visit((LineSymbolizer) sym);
        }

        if (sym instanceof PolygonSymbolizer) {
            visit((PolygonSymbolizer) sym);
        }

        if (sym instanceof TextSymbolizer) {
            visit((TextSymbolizer) sym);
        }

        if (sym instanceof RasterSymbolizer) {
            visit((RasterSymbolizer) sym);
        }
    }

    public void visit(RasterSymbolizer rs) {
        if (symbolizerGeometriesVisitEnabled) {
            if (rs.getGeometry() != null) {
                rs.getGeometry().accept(this, Geometry.class);
            }
        }

        if (rs.getImageOutline() != null) {
            rs.getImageOutline().accept(this);
        }

        if (rs.getOpacity() != null) {
            rs.getOpacity().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(PointSymbolizer) */
    public void visit(PointSymbolizer ps) {
        if (symbolizerGeometriesVisitEnabled) {
            if (ps.getGeometry() != null) {
                ps.getGeometry().accept(this, Geometry.class);
            } else {
                this.defaultGeometryUsed = true; // they want the default geometry (see GEOS-469)
            }
        }

        if (ps.getGraphic() != null) {
            ps.getGraphic().accept(this);
        }
    }

    /** @see StyleVisitor#visit(LineSymbolizer) */
    public void visit(LineSymbolizer line) {
        if (symbolizerGeometriesVisitEnabled) {
            if (line.getGeometry() != null) {
                line.getGeometry().accept(this, Geometry.class);
            } else {
                this.defaultGeometryUsed = true; // they want the default geometry (see GEOS-469)
            }
        }

        if (line.getPerpendicularOffset() != null) {
            line.getPerpendicularOffset().accept(this, Double.class);
        }

        if (line.getStroke() != null) {
            line.getStroke().accept(this);
        }
    }

    /** @see StyleVisitor#visit(PolygonSymbolizer) */
    public void visit(PolygonSymbolizer poly) {
        if (symbolizerGeometriesVisitEnabled) {
            if (poly.getGeometry() != null) {
                poly.getGeometry().accept(this, Geometry.class);
            } else {
                this.defaultGeometryUsed = true; // they want the default geometry (see GEOS-469)
            }
        }

        if (poly.getStroke() != null) {
            poly.getStroke().accept(this);
        }

        if (poly.getFill() != null) {
            poly.getFill().accept(this);
        }
    }

    /** @see StyleVisitor#visit(TextSymbolizer) */
    public void visit(TextSymbolizer text) {
        if (symbolizerGeometriesVisitEnabled) {
            if (text.getGeometry() != null) {
                text.getGeometry().accept(this, null);
            } else {
                this.defaultGeometryUsed = true; // they want the default geometry (see GEOS-469)
            }
        }

        if (text instanceof TextSymbolizer2) {
            if (((TextSymbolizer2) text).getGraphic() != null)
                ((TextSymbolizer2) text).getGraphic().accept(this);
        }

        if (text.getFill() != null) {
            text.getFill().accept(this);
        }

        if (text.getHalo() != null) {
            text.getHalo().accept(this);
        }

        if (text.fonts() != null) {
            for (Font font : text.fonts()) {
                if (font.getFamily() != null) {
                    for (Expression list : font.getFamily()) {
                        list.accept(this, null);
                    }
                }
                if (font.getSize() != null) {
                    font.getSize().accept(this, Double.class);
                }

                if (font.getStyle() != null) {
                    font.getStyle().accept(this, String.class);
                }

                if (font.getWeight() != null) {
                    font.getWeight().accept(this, String.class);
                }
            }
        }

        if (text.getHalo() != null) {
            text.getHalo().accept(this);
        }

        if (text.getLabel() != null) {
            text.getLabel().accept(this, null);
        }

        if (text.getLabelPlacement() != null) {
            text.getLabelPlacement().accept(this);
        }

        if (text.getPriority() != null) {
            text.getPriority().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(Graphic) */
    public void visit(Graphic gr) {
        for (GraphicalSymbol symbol : gr.graphicalSymbols()) {
            if (symbol instanceof Symbol) {
                ((Symbol) symbol).accept(this);
            } else {
                throw new RuntimeException("Don't know how to visit " + symbol);
            }
        }

        if (gr.getOpacity() != null) {
            gr.getOpacity().accept(this, Double.class);
        }

        if (gr.getRotation() != null) {
            gr.getRotation().accept(this, Double.class);
        }

        if (gr.getSize() != null) {
            gr.getSize().accept(this, Double.class);
        }

        if (gr.getDisplacement() != null) gr.getDisplacement().accept(this);

        if (gr.getAnchorPoint() != null) gr.getAnchorPoint().accept(this);
    }

    /** @see StyleVisitor#visit(Mark) */
    public void visit(Mark mark) {
        if (mark.getFill() != null) {
            mark.getFill().accept(this);
        }

        if (mark.getStroke() != null) {
            mark.getStroke().accept(this);
        }

        if (mark.getWellKnownName() != null) {
            if (mark.getWellKnownName() instanceof Literal) {
                visitCqlExpression(mark.getWellKnownName().evaluate(null, String.class));
            } else {
                mark.getWellKnownName().accept(this, String.class);
            }
        }
    }

    /** Handles the special CQL expressions embedded in the style markers since the time */
    private void visitCqlExpression(String expression) {
        Expression parsed = ExpressionExtractor.extractCqlExpressions(expression);
        if (parsed != null) parsed.accept(this, null);
    }

    /** @see StyleVisitor#visit(ExternalGraphic) */
    public void visit(ExternalGraphic exgr) {
        // add dynamic support for ExternalGrapic format attribute
        visitCqlExpression(exgr.getFormat());

        try {
            if (exgr.getLocation() != null) visitCqlExpression(exgr.getLocation().toString());
        } catch (MalformedURLException e) {
            throw new RuntimeException(
                    "Errors while inspecting " + "the location of an external graphic", e);
        }
    }

    /** @see StyleVisitor#visit(PointPlacement) */
    public void visit(PointPlacement pp) {
        if (pp.getAnchorPoint() != null) {
            pp.getAnchorPoint().accept(this);
        }

        if (pp.getDisplacement() != null) {
            pp.getDisplacement().accept(this);
        }

        if (pp.getRotation() != null) {
            pp.getRotation().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(AnchorPoint) */
    public void visit(AnchorPoint ap) {
        if (ap.getAnchorPointX() != null) {
            ap.getAnchorPointX().accept(this, Double.class);
        }

        if (ap.getAnchorPointY() != null) {
            ap.getAnchorPointY().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(Displacement) */
    public void visit(Displacement dis) {
        if (dis.getDisplacementX() != null) {
            dis.getDisplacementX().accept(this, Double.class);
        }

        if (dis.getDisplacementY() != null) {
            dis.getDisplacementY().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(LinePlacement) */
    public void visit(LinePlacement lp) {
        if (lp.getPerpendicularOffset() != null) {
            lp.getPerpendicularOffset().accept(this, Double.class);
        }
    }

    /** @see StyleVisitor#visit(Halo) */
    public void visit(Halo halo) {
        if (halo.getFill() != null) {
            halo.getFill().accept(this);
        }

        if (halo.getRadius() != null) {
            halo.getRadius().accept(this, Double.class);
        }
    }

    public void visit(StyledLayerDescriptor sld) {
        StyledLayer[] layers = sld.getStyledLayers();

        for (int i = 0; i < layers.length; i++) {
            if (layers[i] instanceof NamedLayer) {
                ((NamedLayer) layers[i]).accept(this);
            } else if (layers[i] instanceof UserLayer) {
                ((UserLayer) layers[i]).accept(this);
            }
        }
    }

    public void visit(NamedLayer layer) {
        Style[] styles = layer.getStyles();

        for (int i = 0; i < styles.length; i++) {
            styles[i].accept(this);
        }
    }

    public void visit(UserLayer layer) {
        Style[] styles = layer.getUserStyles();

        for (int i = 0; i < styles.length; i++) {
            styles[i].accept(this);
        }
    }

    public void visit(FeatureTypeConstraint ftc) {
        ftc.accept(this);
    }

    public void visit(ColorMap map) {
        ColorMapEntry[] entries = map.getColorMapEntries();

        for (int i = 0; i < entries.length; i++) {
            entries[i].accept(this);
        }
    }

    public void visit(ColorMapEntry entry) {
        entry.accept(this);
    }

    public void visit(ContrastEnhancement contrastEnhancement) {
        contrastEnhancement.accept(this);
    }

    public void visit(ImageOutline outline) {
        outline.getSymbolizer().accept(this);
    }

    public void visit(ChannelSelection cs) {
        cs.accept(this);
    }

    public void visit(OverlapBehavior ob) {
        ob.accept(this);
    }

    public void visit(SelectedChannelType sct) {
        sct.accept(this);
    }

    public void visit(ShadedRelief sr) {
        sr.accept(this);
    }

    @Override
    public Object visit(PropertyName expression, Object data) {
        AttributeDescriptor ad = (AttributeDescriptor) expression.evaluate(featureType);
        if (ad != null) {
            String name = ad.getLocalName();
            propertyTypes.put(expression, ad.getType().getBinding());
        } else if (data instanceof Class) {
            propertyTypes.put(expression, (Class) data);
        } else {
            propertyTypes.put(expression, Object.class);
        }

        return super.visit(expression, data);
    }

    /**
     * Retuns a map from PropertyName to its data type, either retrieved from the feature type if
     * available, or guessed from the style property necessities, otherwise. When multiple types
     * would be allowed thanks to converters, the most specific is used (e.g., Color instead of
     * String)
     */
    public Map<PropertyName, Class> getPropertyTypes() {
        return Collections.unmodifiableMap(propertyTypes);
    }
}
