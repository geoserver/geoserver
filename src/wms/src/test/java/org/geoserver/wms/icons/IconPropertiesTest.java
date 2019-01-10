/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import static org.geotools.filter.text.ecql.ECQL.toExpression;
import static org.geotools.filter.text.ecql.ECQL.toFilter;
import static org.junit.Assert.assertEquals;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collections;
import org.geotools.filter.text.cql2.CQLException;
import org.geotools.styling.Fill;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.expression.Expression;
import org.opengis.style.Stroke;

public class IconPropertiesTest extends IconTestSupport {
    @Test
    public void testSimpleStyleEncodesNoProperties() {
        final Style simple = styleFromRules(catchAllRule(grayCircle()));
        assertEquals("0.0.0=", encode(simple, fieldIs1));
    }

    @Test
    public void testWorkspacedStyleEncodesNoProperties() {
        final Style simple = styleFromRules(catchAllRule(grayCircle()));
        assertEquals("0.0.0=", encode("workspace", simple, fieldIs1));
    }

    @Test
    public void testFilters() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Rule a = rule(toFilter("field = 1"), symbolizer);
        final Rule b = rule(toFilter("field = 2"), symbolizer);
        Style style = styleFromRules(a, b);

        assertEquals("0.0.0=", encode(style, fieldIs1));
        assertEquals("0.1.0=", encode(style, fieldIs2));
    }

    @Test
    public void testMultipleSymbolizers() {
        final PointSymbolizer symbolizer = grayCircle();
        final Rule a = catchAllRule(symbolizer, symbolizer);
        final Style style = styleFromRules(a);

        assertEquals("0.0.0=&0.0.1=", encode(style, fieldIs1));
    }

    @Test
    public void testMultipleFeatureTypeStyle() {
        final PointSymbolizer symbolizer = grayCircle();
        final Style s =
                style(
                        featureTypeStyle(catchAllRule(symbolizer)),
                        featureTypeStyle(catchAllRule(symbolizer)));
        assertEquals("0.0.0=&1.0.0=", encode(s, fieldIs1));
    }

    @Test
    public void testElseFilter() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Style style =
                styleFromRules(rule(toFilter("field = 1"), symbolizer), elseRule(symbolizer));
        assertEquals("0.0.0=", encode(style, fieldIs1));
        assertEquals("0.1.0=", encode(style, fieldIs2));
    }

    @Test
    public void testDynamicMark() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Mark mark = (Mark) symbolizer.getGraphic().graphicalSymbols().get(0);
        mark.setWellKnownName(toExpression("if_then_else(equalTo(field, 1), 'circle', 'square')"));
        final Style s = styleFromRules(catchAllRule(symbolizer));
        assertEquals("0.0.0=&0.0.0.name=circle", encode(s, fieldIs1));
        assertEquals("0.0.0=&0.0.0.name=square", encode(s, fieldIs2));
    }

    @Test
    public void testDynamicColor() throws CQLException {
        Expression color = toExpression("if_then_else(equalTo(field, 1), '#8080C0', '#CC8030')");
        Stroke stroke = styleFactory.stroke(color, null, null, null, null, null, null);
        Fill fill = styleFactory.fill(null, color, null);
        Mark mark = styleFactory.mark(toExpression("circle"), fill, stroke);
        Graphic graphic =
                styleFactory.graphic(Collections.singletonList(mark), null, null, null, null, null);
        PointSymbolizer symbolizer =
                styleFactory.pointSymbolizer(
                        "symbolizer", toExpression("geom"), null, null, graphic);

        final Style s = styleFromRules(catchAllRule(symbolizer));
        assertEquals(
                "0.0.0=&0.0.0.fill.color=%238080C0&0.0.0.name=square&0.0.0.stroke.color=%238080C0",
                encode(s, fieldIs1));
        assertEquals(
                "0.0.0=&0.0.0.fill.color=%23CC8030&0.0.0.name=square&0.0.0.stroke.color=%23CC8030",
                encode(s, fieldIs2));
    }

    @Test
    public void testDynamicOpacity() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Graphic graphic = symbolizer.getGraphic();
        graphic.setOpacity(toExpression("1 / field"));
        final Style s = styleFromRules(catchAllRule(symbolizer));
        assertEquals("0.0.0=&0.0.0.opacity=1.0", encode(s, fieldIs1));
        assertEquals("0.0.0=&0.0.0.opacity=0.5", encode(s, fieldIs2));
    }

    @Test
    public void testDynamicRotation() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Graphic graphic = symbolizer.getGraphic();
        graphic.setRotation(toExpression("45 * field"));
        final Style s = styleFromRules(catchAllRule(symbolizer));
        assertEquals("0.0.0=&0.0.0.rotation=45.0", encode(s, fieldIs1));
        assertEquals("0.0.0=&0.0.0.rotation=90.0", encode(s, fieldIs2));
    }

    @Test
    public void testDynamicSize() throws CQLException {
        final PointSymbolizer symbolizer = grayCircle();
        final Graphic graphic = symbolizer.getGraphic();
        graphic.setSize(toExpression("field * 16"));
        final Style s = styleFromRules(catchAllRule(symbolizer));
        assertEquals("0.0.0=&0.0.0.size=16.0", encode(s, fieldIs1));
        assertEquals("0.0.0=&0.0.0.size=32.0", encode(s, fieldIs2));
    }

    @Test
    public void testDynamicURL() throws CQLException, UnsupportedEncodingException {
        // TODO: This test should overlay two different images
        final PointSymbolizer symbolizer =
                externalGraphic("http://127.0.0.1/foo${field}.png", "image/png");
        final Style style = styleFromRules(catchAllRule(symbolizer, symbolizer));
        final String url = URLEncoder.encode("http://127.0.0.1/", "UTF-8");
        assertEquals(
                "0.0.0=&0.0.0.url=" + url + "foo1.png&0.0.1=&0.0.1.url=" + url + "foo1.png",
                encode(style, fieldIs1));
        assertEquals(
                "0.0.0=&0.0.0.url=" + url + "foo2.png&0.0.1=&0.0.1.url=" + url + "foo2.png",
                encode(style, fieldIs2));
    }

    @Test
    public void testPublicURL() throws CQLException {
        final PointSymbolizer symbolizer = externalGraphic("http://127.0.0.1/foo.png", "image/png");
        final Style style = styleFromRules(catchAllRule(symbolizer));
        assertEquals("http://127.0.0.1/foo.png", encode(style, fieldIs1));
    }

    @Test
    public void testLocalFile() throws Exception {
        final PointSymbolizer symbolizer = externalGraphic("file:foo.png", "image/png");
        final Style style = styleFromRules(catchAllRule(symbolizer));
        assertEquals("http://127.0.0.1/styles/foo.png", encode(style, fieldIs1));
    }

    @Test
    public void testLocalFileRotate() throws Exception {
        final PointSymbolizer symbolizer = externalGraphic("file:foo.png", "image/png");
        final Graphic graphic = symbolizer.getGraphic();
        graphic.setRotation(toExpression("45 * field"));
        final Style style = styleFromRules(catchAllRule(symbolizer));
        IconProperties prop1 = IconPropertyExtractor.extractProperties(style, fieldIs1);
        assertEquals(
                "http://127.0.0.1/styles/foo.png", prop1.href("http://127.0.0.1/", null, "test"));
        assertEquals(45.0d, prop1.getHeading(), 0.0001);
        IconProperties prop2 = IconPropertyExtractor.extractProperties(style, fieldIs2);
        assertEquals(
                "http://127.0.0.1/styles/foo.png", prop2.href("http://127.0.0.1/", null, "test"));
        assertEquals(90.0d, prop2.getHeading(), 0.0001);
    }

    @Test
    public void testTwoLocalFilesRotate() throws Exception {
        final PointSymbolizer symbolizer1 = externalGraphic("file:foo.png", "image/png");
        final PointSymbolizer symbolizer2 = externalGraphic("file:bar.png", "image/png");
        final Graphic graphic1 = symbolizer1.getGraphic();
        graphic1.setRotation(toExpression("45 * field"));
        final Graphic graphic2 = symbolizer2.getGraphic();
        graphic2.setRotation(toExpression("22.5 * field"));
        final Style style = styleFromRules(catchAllRule(symbolizer1, symbolizer2));
        IconProperties prop = IconPropertyExtractor.extractProperties(style, fieldIs1);
        assertEquals(
                "http://127.0.0.1/kml/icon/test?0.0.0=&0.0.0.rotation=45.0&0.0.1=&0.0.1.rotation=22.5",
                prop.href("http://127.0.0.1/", null, "test"));
        assertEquals(0.0d, prop.getHeading(), 0.0001);
    }

    @Test
    public void testTwoLocalFilesOneRotate() throws Exception {
        final PointSymbolizer symbolizer1 = externalGraphic("file:foo.png", "image/png");
        final PointSymbolizer symbolizer2 = externalGraphic("file:bar.png", "image/png");
        final Graphic graphic1 = symbolizer1.getGraphic();
        graphic1.setRotation(toExpression("45 * field"));
        final Graphic graphic2 = symbolizer2.getGraphic();
        graphic2.setRotation(Expression.NIL);
        final Style style = styleFromRules(catchAllRule(symbolizer1, symbolizer2));
        IconProperties prop = IconPropertyExtractor.extractProperties(style, fieldIs1);
        assertEquals(
                "http://127.0.0.1/kml/icon/test?0.0.0=&0.0.0.rotation=45.0&0.0.1=",
                prop.href("http://127.0.0.1/", null, "test"));
        assertEquals(0, prop.getHeading(), 0d);
    }

    @Test
    public void testMarkRotate() throws Exception {
        final PointSymbolizer symbolizer = grayCircle();
        final Graphic graphic = symbolizer.getGraphic();
        graphic.setRotation(toExpression("45 * field"));
        final Style s = styleFromRules(catchAllRule(symbolizer));
        IconProperties prop = IconPropertyExtractor.extractProperties(s, fieldIs1);
        assertEquals(
                "http://127.0.0.1/kml/icon/test?0.0.0=&0.0.0.rotation=45.0",
                prop.href("http://127.0.0.1/", null, "test"));
        assertEquals(0.0d, prop.getHeading(), 0.0001);
    }

    protected String encode(Style style, SimpleFeature feature) {
        return IconPropertyExtractor.extractProperties(style, feature)
                .href("http://127.0.0.1/", null, "test")
                .replace("http://127.0.0.1/kml/icon/test?", "");
    }

    protected String encode(String workspace, Style style, SimpleFeature feature) {
        return IconPropertyExtractor.extractProperties(style, feature)
                .href("http://127.0.0.1/", workspace, "test")
                .replace("http://127.0.0.1/kml/icon/" + workspace + "/test?", "");
    }
}
