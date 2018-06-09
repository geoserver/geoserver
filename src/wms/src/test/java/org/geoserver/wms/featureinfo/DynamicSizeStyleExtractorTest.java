/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.featureinfo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.awt.Color;
import java.util.List;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.Mark;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.StyleBuilder;
import org.geotools.styling.Symbolizer;
import org.junit.Before;
import org.junit.Test;

public class DynamicSizeStyleExtractorTest {

    StyleBuilder sb = new StyleBuilder();
    private Rule staticPolygonRule;
    private Rule staticLineRule;
    private DynamicSizeStyleExtractor visitor;

    @Before
    public void setup() {
        staticPolygonRule = sb.createRule(sb.createPolygonSymbolizer(Color.RED));
        staticLineRule = sb.createRule(sb.createLineSymbolizer(Color.BLUE, 1d));

        visitor = new DynamicSizeStyleExtractor();
    }

    @Test
    public void testOneFtsFullyStatic() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        fts.rules().add(staticLineRule);
        style.featureTypeStyles().add(fts);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testTwoFtsFullyStatic() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle("Feature", staticLineRule);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testMixDynamicStroke() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setWidth(sb.getFilterFactory().property("myAttribute"));
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle(ls);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testMultipleSymbolizers() {
        Style style = sb.createStyle();
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setWidth(sb.getFilterFactory().property("myAttribute"));
        FeatureTypeStyle fts = sb.createFeatureTypeStyle(sb.createPolygonSymbolizer());
        style.featureTypeStyles().add(fts);
        fts.rules().get(0).symbolizers().add(ls);
        fts.rules().get(0).symbolizers().add(sb.createLineSymbolizer());

        checkSingleSymbolizer(style, ls);
    }

    private void checkSingleSymbolizer(Style style, LineSymbolizer ls) {
        // we should get back only the dynamic one
        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNotNull(copy);
        List<FeatureTypeStyle> featureTypeStyles = copy.featureTypeStyles();
        assertEquals(1, featureTypeStyles.size());
        List<Rule> rules = featureTypeStyles.get(0).rules();
        assertEquals(1, rules.size());
        List<Symbolizer> symbolizers = rules.get(0).symbolizers();
        assertEquals(1, symbolizers.size());
        assertEquals(ls, symbolizers.get(0));
    }

    @Test
    public void testMixDynamicGraphicStroke() {
        Style style = sb.createStyle();
        FeatureTypeStyle fts1 = sb.createFeatureTypeStyle("Feature", staticPolygonRule);
        Graphic graphic = sb.createGraphic(null, sb.createMark("square"), null);
        graphic.setSize(sb.getFilterFactory().property("myAttribute"));
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);
        FeatureTypeStyle fts2 = sb.createFeatureTypeStyle(ls);
        style.featureTypeStyles().add(fts1);
        style.featureTypeStyles().add(fts2);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testDynamicSymbolizerStrokeLineSymbolizer() {
        ExternalGraphic dynamicSymbolizer =
                sb.createExternalGraphic("file://./${myAttribute}.jpeg", "image/jpeg");
        Graphic graphic = sb.createGraphic(dynamicSymbolizer, null, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        checkSingleSymbolizer(style, ls);
    }

    @Test
    public void testStaticGraphicLineSymbolizer() {
        ExternalGraphic dynamicSymbolizer =
                sb.createExternalGraphic("file://./hello.jpeg", "image/jpeg");
        Graphic graphic = sb.createGraphic(dynamicSymbolizer, null, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }

    @Test
    public void testDynamicStrokeInGraphicMark() {
        Stroke markStroke = sb.createStroke();
        markStroke.setWidth(sb.getFilterFactory().property("myAttribute"));
        Mark mark = sb.createMark("square");
        mark.setStroke(markStroke);
        Graphic graphic = sb.createGraphic(null, mark, null);
        LineSymbolizer ls = sb.createLineSymbolizer();
        ls.getStroke().setGraphicStroke(graphic);

        Style style = sb.createStyle(ls);

        checkSingleSymbolizer(style, ls);
    }

    @Test // this one should fail now??
    public void testDynamicStrokeInGraphicFill() {
        Stroke markStroke = sb.createStroke();
        markStroke.setWidth(sb.getFilterFactory().property("myAttribute"));
        Mark mark = sb.createMark("square");
        mark.setStroke(markStroke);
        Graphic graphic = sb.createGraphic(null, mark, null);
        PolygonSymbolizer ps = sb.createPolygonSymbolizer();
        ps.getFill().setGraphicFill(graphic);

        Style style = sb.createStyle(ps);
        style.accept(visitor);
        Style copy = (Style) visitor.getCopy();
        assertNull(copy);
    }
}
