/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.icons;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import static org.junit.Assume.*;

import java.awt.Color;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.geotools.styling.PointSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;

public class IconPropertyInjectorTest extends IconTestSupport {

    static <T> T assertSingleElement(Iterable<T> elements) {
        Iterator<T> i = elements.iterator();

        assertTrue("Expected one element but got none", i.hasNext());
        T result = i.next();
        assertFalse("Expected one element but got more", i.hasNext());

        return result;
    }

    @SuppressWarnings("unchecked")
    static <T, U extends T> U assertSingleElement(Iterable<T> elements, Class<U> clazz) {
        T result = assertSingleElement(elements);
        assertThat(result, instanceOf(clazz));
        return (U) result;
    }

    static <T> T assumeSingleElement(Iterable<T> elements) {
        Iterator<T> i = elements.iterator();

        assumeTrue("Expected one element but got none", i.hasNext());
        T result = i.next();
        assumeFalse("Expected one element but got more", i.hasNext());

        return result;
    }

    @SuppressWarnings("unchecked")
    static <T, U extends T> U assumeSingleElement(Iterable<T> elements, Class<U> clazz) {
        T result = assertSingleElement(elements);
        assumeThat(result, instanceOf(clazz));
        return (U) result;
    }

    @Test
    public void testSimplePointStyle() throws Exception {
        Style result;
        {
            Symbolizer symb = grayCircle();
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assertSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
        }
    }

    @Test
    public void testSimplePointStyleOff() throws Exception {
        Style result;
        {
            Symbolizer symb = grayCircle();
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            // properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            assertThat(rule.symbolizers().size(), is(0));
        }
    }

    @Test
    public void testSimpleGraphicStyle() throws Exception {
        Style result;
        {
            Symbolizer symb = this.externalGraphic("http://example.com/foo.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/foo.png"));
        }
    }

    @Test
    public void testSubstitutedGraphicStyle() throws Exception {
        Style result;
        {
            Symbolizer symb =
                    this.externalGraphic("http://example.com/${PROV_ABBR}.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.url", "http://example.com/BC.png");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/BC.png"));
        }
    }

    @Test
    public void testUnneccessaryURLInjection() throws Exception {
        Style result;
        {
            Symbolizer symb = this.externalGraphic("http://example.com/NF.png", "image/png");
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.url", "http://example.com/BC.png");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/NF.png"));
        }
    }

    @Test
    public void testRotation() throws Exception {
        Style result;
        {
            PointSymbolizer symb = this.externalGraphic("http://example.com/foo.png", "image/png");
            symb.getGraphic().setRotation(filterFactory.property("heading"));
            Style input = styleFromRules(catchAllRule(symb));
            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");
            properties.put("0.0.0.rotation", "45.0");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assumeSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Graphic eg = symb.getGraphic();
            assertThat(eg.getRotation().evaluate(null).toString(), is("45.0"));
        }
    }

    @Test
    public void testFilteredRulesPickFirstExternal() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = externalGraphic("http://example.com/foo.png", "image/png");
            PointSymbolizer symb2 = externalGraphic("http://example.com/bar.png", "image/png");
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/foo.png"));
        }
    }

    @Test
    public void testFilteredRulesPickSecondExternal() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = externalGraphic("http://example.com/foo.png", "image/png");
            PointSymbolizer symb2 = externalGraphic("http://example.com/bar.png", "image/png");
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.1.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            ExternalGraphic eg =
                    assertSingleElement(
                            symb.getGraphic().graphicalSymbols(), ExternalGraphic.class);
            assertThat(
                    eg.getOnlineResource().getLinkage().toString(),
                    is("http://example.com/bar.png"));
        }
    }

    @Test
    public void testFilteredRulesPickFirstMark() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = mark("arrow", Color.BLACK, Color.RED, 1f, 16);
            PointSymbolizer symb2 = mark("arrow", Color.BLACK, Color.BLUE, 1f, 16);
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.0.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Mark mark = assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
            assertThat(mark.getFill().getColor().evaluate(null, Color.class), is(Color.RED));
        }
    }

    @Test
    public void testFilteredRulesPickSecondMark() throws Exception {
        Style result;
        {
            Filter f1 = filterFactory.less(filterFactory.property("foo"), filterFactory.literal(4));
            Filter f2 =
                    filterFactory.greaterOrEqual(
                            filterFactory.property("foo"), filterFactory.literal(4));
            PointSymbolizer symb1 = mark("arrow", Color.BLACK, Color.RED, 1f, 16);
            PointSymbolizer symb2 = mark("arrow", Color.BLACK, Color.BLUE, 1f, 16);
            Style input = styleFromRules(rule(f1, symb1), rule(f2, symb2));

            Map<String, String> properties = new HashMap<String, String>();
            properties.put("0.1.0", "");

            result = IconPropertyInjector.injectProperties(input, properties);
        }
        {
            FeatureTypeStyle fts = assumeSingleElement(result.featureTypeStyles());
            Rule rule = assertSingleElement(fts.rules());
            PointSymbolizer symb = assertSingleElement(rule.symbolizers(), PointSymbolizer.class);
            Mark mark = assertSingleElement(symb.getGraphic().graphicalSymbols(), Mark.class);
            assertThat(mark.getFill().getColor().evaluate(null, Color.class), is(Color.BLUE));
        }
    }

    @Test
    public void testGraphicFallbacks() {
        FilterFactory ff = CommonFactoryFinder.getFilterFactory();
        Style style = SLD.createPointStyle("circle", Color.RED, Color.yellow, 0.5f, 10f);
        Graphic g = SLD.graphic(SLD.pointSymbolizer(style));
        g.setRotation(ff.literal(45));
        g.setOpacity(ff.literal(0.5));

        Map<String, String> props = new HashMap<String, String>();
        props.put("0.0.0", "");

        style = IconPropertyInjector.injectProperties(style, props);
        g = SLD.graphic(SLD.pointSymbolizer(style));

        assertEquals(10.0, g.getSize().evaluate(null, Double.class), 0.1);
        assertEquals(45.0, g.getRotation().evaluate(null, Double.class), 0.1);
        assertEquals(0.5, g.getOpacity().evaluate(null, Double.class), 0.1);
    }
}
