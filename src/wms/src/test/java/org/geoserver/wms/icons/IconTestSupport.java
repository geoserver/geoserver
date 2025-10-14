/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import java.awt.Color;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.api.filter.Filter;
import org.geotools.api.filter.FilterFactory;
import org.geotools.api.filter.expression.Expression;
import org.geotools.api.style.ExternalGraphic;
import org.geotools.api.style.FeatureTypeStyle;
import org.geotools.api.style.Fill;
import org.geotools.api.style.Font;
import org.geotools.api.style.Graphic;
import org.geotools.api.style.PointSymbolizer;
import org.geotools.api.style.Rule;
import org.geotools.api.style.Style;
import org.geotools.api.style.StyleFactory;
import org.geotools.api.style.Symbolizer;
import org.geotools.api.style.TextSymbolizer;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.JTSFactoryFinder;
import org.geotools.styling.SLD;
import org.junit.BeforeClass;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;

public class IconTestSupport {

    protected static SimpleFeature fieldIs1;
    protected static SimpleFeature fieldIs2;
    protected static StyleFactory styleFactory;
    protected static FilterFactory filterFactory;
    protected static SimpleFeatureType featureType;

    @BeforeClass
    public static void classSetup() {
        styleFactory = CommonFactoryFinder.getStyleFactory();
        filterFactory = CommonFactoryFinder.getFilterFactory();
        GeometryFactory geometryFactory = JTSFactoryFinder.getGeometryFactory();
        SimpleFeatureTypeBuilder typeBuilder = new SimpleFeatureTypeBuilder();
        typeBuilder.setName("example");
        typeBuilder.setNamespaceURI("http://example.com/");
        typeBuilder.setSRS("EPSG:4326");
        typeBuilder.add("field", String.class);
        typeBuilder.add("geom", Point.class, "EPSG:4326");
        featureType = typeBuilder.buildFeatureType();
        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("field", "1");
        featureBuilder.set("geom", geometryFactory.createPoint(new Coordinate(0, 0)));
        fieldIs1 = featureBuilder.buildFeature(null);
        featureBuilder.set("field", "2");
        featureBuilder.set("geom", geometryFactory.createPoint(new Coordinate(0, 0)));
        fieldIs2 = featureBuilder.buildFeature(null);
    }

    protected String queryString(Map<String, String> params) {
        try {
            StringBuilder buff = new StringBuilder();
            boolean first = true;
            for (Map.Entry<String, String> entry : params.entrySet()) {
                if (first) {
                    first = false;
                } else {
                    buff.append("&");
                }
                buff.append(entry.getKey()).append("=").append(URLEncoder.encode(entry.getValue(), "UTF-8"));
            }
            return buff.toString();
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException(e);
        }
    }

    protected final Fill fill(Color color, Double opacity) {
        Expression colorExpr = color == null ? null : filterFactory.literal(color);
        Expression opacityExpr = opacity == null ? null : filterFactory.literal(opacity);
        return styleFactory.fill(null, colorExpr, opacityExpr);
    }

    protected final Font font(String fontFace, String style, String weight, Integer size) {
        List<Expression> fontFaceList =
                fontFace == null ? null : Collections.singletonList(filterFactory.literal(fontFace));
        Expression styleExpr = style == null ? null : filterFactory.literal(style);
        Expression weightExpr = weight == null ? null : filterFactory.literal(weight);
        Expression sizeExpr = size == null ? null : filterFactory.literal(size);
        return styleFactory.font(fontFaceList, styleExpr, weightExpr, sizeExpr);
    }

    protected final TextSymbolizer text(String name, String label, Font font, Fill fill) {
        Expression geometry = filterFactory.property("");
        return styleFactory.textSymbolizer(
                name, geometry, null, null, filterFactory.property(label), font, null, null, fill);
    }

    protected final PointSymbolizer mark(String name, Color stroke, Color fill, float opacity, int size) {
        return SLD.pointSymbolizer(SLD.createPointStyle(name, stroke, fill, opacity, size));
    }

    protected final PointSymbolizer externalGraphic(String url, String format) {
        ExternalGraphic exGraphic = styleFactory.createExternalGraphic(url, format);
        Graphic graphic = styleFactory.createGraphic(new ExternalGraphic[] {exGraphic}, null, null, null, null, null);
        return styleFactory.createPointSymbolizer(graphic, null);
    }

    protected final PointSymbolizer grayCircle() {
        return mark("circle", Color.BLACK, Color.GRAY, 1f, 16);
    }

    protected final PointSymbolizer redStar() {
        return mark("star", Color.BLACK, Color.RED, 1f, 16);
    }

    protected final Rule rule(Filter filter, Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        rule.setFilter(filter);
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final Rule catchAllRule(Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final Rule elseRule(Symbolizer... symbolizer) {
        Rule rule = styleFactory.createRule();
        rule.setElseFilter(true);
        for (Symbolizer s : symbolizer) rule.symbolizers().add(s);
        return rule;
    }

    protected final FeatureTypeStyle featureTypeStyle(Rule... rules) {
        FeatureTypeStyle ftStyle = styleFactory.createFeatureTypeStyle();
        for (Rule r : rules) ftStyle.rules().add(r);
        return ftStyle;
    }

    protected final Style styleFromRules(Rule... rules) {
        return style(featureTypeStyle(rules));
    }

    protected final Style style(FeatureTypeStyle... ftStyles) {
        Style style = styleFactory.createStyle();
        for (FeatureTypeStyle f : ftStyles) style.featureTypeStyles().add(f);
        return style;
    }
}
