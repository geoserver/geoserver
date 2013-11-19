package org.geoserver.sldservice.utils.classifier;

import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.junit.Test;
import java.awt.Color;
import java.util.List;


import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class RulesBuilderTest extends ClassifierTestSupport {
    private RulesBuilder builder;

    public RulesBuilderTest(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        builder = new RulesBuilder();
    }

    @Test
    public void testQuantileClassification() throws Exception {
        List <Rule> rules =  builder.quantileClassification(pointCollection, "foo", 4, false);
        assertEquals(4, rules.size());
    }

    @Test
    public void testEqualIntervalClassification() throws Exception {
        List <Rule> rules =  builder.equalIntervalClassification(pointCollection, "foo", 4, false);
        assertEquals(4, rules.size());
    }

    @Test
    public void testUniqueIntervalClassification() throws Exception {
        List <Rule> rules =  builder.uniqueIntervalClassification(pointCollection, "group");
        assertEquals(4, rules.size());

        rules =  builder.uniqueIntervalClassification(pointCollection, "id");
        assertEquals(8, rules.size());
    }

    @Test
    public void testJenksClassification() throws Exception {
        List <Rule> rules =  builder.jenksClassification(lineCollection, "jenks71", 10, false);
        assertEquals(10, rules.size());
    }

    @Test
    public void testPolygonStyle() throws Exception {
        int numClasses = 10;
        List <Rule> rules =  builder.equalIntervalClassification(pointCollection, "foo", numClasses, false);
        builder.polygonStyle(rules, new BlueColorRamp(), false);
        Rule ruleOne = rules.get(0);
        assertTrue(ruleOne.getSymbolizers()[0] instanceof PolygonSymbolizer);
        PolygonSymbolizer symbolizer = (PolygonSymbolizer)ruleOne.getSymbolizers()[0];
        assertEquals(new Color(0, 0, 49), symbolizer.getFill().getColor().evaluate(null, Color.class));
        assertNotNull(ruleOne.getFilter());
        assertEquals(numClasses, rules.size());
    }

    @Test
    public void testPolygonStyleReverse() throws Exception {
        int numClasses = 10;
        List <Rule> rules =  builder.equalIntervalClassification(pointCollection, "foo", numClasses, false);
        builder.polygonStyle(rules, new BlueColorRamp(), true);
        PolygonSymbolizer symbolizer = (PolygonSymbolizer)rules.get(0).getSymbolizers()[0];
        assertEquals(new Color(0, 0, 224), symbolizer.getFill().getColor().evaluate(null, Color.class));
        assertEquals(numClasses, rules.size());
    }

    @Test
    public void testLineStyle() throws Exception {
        int numClasses = 10;
        List <Rule> rules =  builder.jenksClassification(lineCollection, "jenks71", numClasses, false);
        builder.lineStyle(rules, new BlueColorRamp(), false);
        Rule ruleOne = rules.get(0);
        assertTrue(ruleOne.getSymbolizers()[0] instanceof LineSymbolizer);
        LineSymbolizer symbolizer = (LineSymbolizer)ruleOne.getSymbolizers()[0];
        assertEquals(new Color(0, 0, 49), symbolizer.getStroke().getColor().evaluate(null, Color.class));
        assertNotNull(ruleOne.getFilter());
        assertEquals(10, rules.size());
    }

    @Test
    public void testLineStyleReverse() throws Exception {
        int numClasses = 10;
        List <Rule> rules =  builder.jenksClassification(lineCollection, "jenks71", numClasses, false);
        builder.lineStyle(rules, new BlueColorRamp(), true);
        LineSymbolizer symbolizer = (LineSymbolizer)rules.get(0).getSymbolizers()[0];
        assertEquals(new Color(0, 0, 224), symbolizer.getStroke().getColor().evaluate(null, Color.class));
    }

}
