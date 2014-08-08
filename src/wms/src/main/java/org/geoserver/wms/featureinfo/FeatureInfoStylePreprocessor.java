/* Copyright (c) 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.geoserver.wms.SymbolizerFilteringVisitor;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.RuleImpl;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.GeometryDescriptor;
import org.opengis.filter.Filter;
import org.opengis.filter.expression.Expression;
import org.opengis.filter.expression.PropertyName;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.MultiPolygon;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Removes text symbolizers, makes sure lines and polygons are painted at least with a solid color
 * to ensure we match even when hitting in spaces between dashes or spaced fills
 * 
 * @author Andrea Aime - GeoSolutions
 */
class FeatureInfoStylePreprocessor extends SymbolizerFilteringVisitor {

    StyleBuilder sb = new StyleBuilder();

    FeatureType schema;

    Set<Expression> geometriesOnPolygonSymbolizer = new HashSet<Expression>();

    Set<Expression> geometriesOnLineSymbolizer = new HashSet<Expression>();

    Set<Rule> extraRules = new HashSet<Rule>();

    private PropertyName defaultGeometryExpression;

    private boolean addSolidLineSymbolier;

    public FeatureInfoStylePreprocessor(FeatureType schema) {
        this.schema = schema;
        this.defaultGeometryExpression = ff.property("");
    }

    public void visit(org.geotools.styling.TextSymbolizer ts) {
        pages.push(null);
    }

    @Override
    public void visit(Fill fill) {
        super.visit(fill);
        Fill copy = (Fill) pages.peek();
        if (copy.getGraphicFill() != null) {
            copy.setGraphicFill(null);
            copy.setColor(sb.colorExpression(Color.BLACK));
            copy.setOpacity(ff.literal(1));
        }
    }

    /**
     * Force a solid color, otherwise we might decide the user did not click on the polygon because
     * the area in which he clicked is fully transparent
     */
    @Override
    public void visit(PolygonSymbolizer poly) {
        super.visit(poly);
        PolygonSymbolizer copy = (PolygonSymbolizer) pages.peek();
        Fill fill = copy.getFill();
        if (fill == null) {
            copy.setFill(sb.createFill());
        }
        Stroke stroke = copy.getStroke();
        addStrokeSymbolizerIfNecessary(stroke);
        addGeometryExpression(poly.getGeometry(), geometriesOnPolygonSymbolizer);
    }

    @Override
    public void visit(LineSymbolizer line) {
        super.visit(line);
        LineSymbolizer copy = (LineSymbolizer) pages.peek();
        Stroke stroke = copy.getStroke();
        addStrokeSymbolizerIfNecessary(stroke);
        addGeometryExpression(line.getGeometry(), geometriesOnLineSymbolizer);
    }
    
    private void addGeometryExpression(Expression geometry,
            Set<Expression> expressions) {
        if(isDefaultGeometry(geometry)) {
            expressions.add(defaultGeometryExpression);
        } else {
            expressions.add(geometry);
        }
        
    }

    private boolean isDefaultGeometry(Expression geometry) {
        if(geometry == null) {
            return true;
        } 
        
        if(!(geometry instanceof PropertyName)) {
            return false;
        }
        
        PropertyName pn = (PropertyName) geometry;
        if("".equals(pn.getPropertyName())) {
            return true;
        }
        
        GeometryDescriptor gd = schema.getGeometryDescriptor();
        if(gd == null) {
            return false;
        }
        return gd.getLocalName().equals(pn.getPropertyName());
    }
    
    @Override
    public void visit(Style style) {
        super.visit(style);
        Style copy = (Style) pages.peek();
        // merge the feature type styles sharing the same transformation
        List<FeatureTypeStyle> featureTypeStyles = copy.featureTypeStyles();
        List<FeatureTypeStyle> reduced = new ArrayList<FeatureTypeStyle>();
        FeatureTypeStyle current = null;
        for (FeatureTypeStyle fts : featureTypeStyles) {
            if(current == null || !sameTranformation(current.getTransformation(), fts.getTransformation())) {
                current = fts;
                reduced.add(current);
            } else {
                // flatten, we don't need to draw a pretty picture and having multiple FTS
                // would result in the feature being returned twice, since we cannot
                // assume feature ids to be stable either
                current.rules().addAll(fts.rules());
            } 
        }
        
        // replace
        copy.featureTypeStyles().clear();
        copy.featureTypeStyles().addAll(reduced);
    }

    private boolean sameTranformation(Expression t1, Expression t2) {
        return (t1 == null && t2 == null) || t1.equals(t2);
    }

    @Override
    public void visit(FeatureTypeStyle fts) {
        extraRules.clear();
        super.visit(fts);
        if(extraRules.size() > 0) {
            FeatureTypeStyle copy = (FeatureTypeStyle) pages.peek();
            copy.rules().addAll(extraRules);
        }
    }

    @Override
    public void visit(Rule rule) {
        geometriesOnLineSymbolizer.clear();
        geometriesOnPolygonSymbolizer.clear();
        addSolidLineSymbolier = false;
        super.visit(rule);
        Rule copy = (Rule) pages.peek();
        if (addSolidLineSymbolier) {
            // add also a black line to make sure we get something in output even
            // if the user clicks in between symbols or dashes
            LineSymbolizer ls = sb.createLineSymbolizer(Color.BLACK);
            copy.symbolizers().add(ls);
        }
        // check all the geometries that are on line, but not on polygon
        geometriesOnLineSymbolizer.removeAll(geometriesOnPolygonSymbolizer);
        for (Expression geom : geometriesOnLineSymbolizer) {
            Object result = geom.evaluate(schema);
            Class geometryType = getTargetGeometryType(result);
             if(Polygon.class.isAssignableFrom(geometryType) ||
                     MultiPolygon.class.isAssignableFrom(geometryType)) {
                 // we know it's a polygon type, but there is no polygon symbolizer, add one
                 // in the current rule
                 copy.symbolizers().add(sb.createPolygonSymbolizer());
             } else if(geometryType.equals(Geometry.class)) {
                 // dynamic, we need to add an extra rule then to paint as polygon
                 // only if the actual geometry is a polygon type
                 Filter polygon = ff.equal(ff.function("geometryType", geom), ff.literal("Polygon"), false);
                 Filter multiPolygon = ff.equal(ff.function("geometryType", geom), ff.literal("MultiPolygon"), false);
                 Filter geomCheck = ff.or(Arrays.asList(polygon, multiPolygon));
                 Filter ruleFilter = copy.getFilter();
                 Filter filter = ruleFilter == null || ruleFilter == Filter.INCLUDE ? geomCheck : ff.and(geomCheck, ruleFilter);
                 RuleImpl extra = new RuleImpl(copy);
                 extra.setFilter(filter);
                 extra.symbolizers().clear();
                 extra.symbolizers().add(sb.createPolygonSymbolizer());
                 extraRules.add(extra);
             }
            
        }
    }

    private Class getTargetGeometryType(Object descriptor) {
        if (!(descriptor instanceof GeometryDescriptor)) {
            // we don't know what this will be, we probably evaluated a filter function
            return Geometry.class;
        } else {
            // see if we are dealing with a polygon
            return ((GeometryDescriptor) descriptor).getType().getBinding();
        }
    }

    private void addStrokeSymbolizerIfNecessary(Stroke stroke) {
        if (stroke != null) {
            float[] dashArray = stroke.getDashArray();
            Graphic graphicStroke = stroke.getGraphicStroke();
            if (graphicStroke != null || dashArray != null && dashArray.length > 0) {
                addSolidLineSymbolier = true;
            }
        }
    }
}
