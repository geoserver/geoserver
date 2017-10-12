/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.Color;
import java.util.List;

import org.geoserver.sldservice.utils.classifier.RulesBuilder;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.junit.Test;

public class RulesBuilderTest extends ClassifierTestSupport {
    private RulesBuilder builder;

    @Test
    public void testQuantileClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules = builder.quantileClassification(pointCollection, "foo", Integer.class,
                    4, false, false);
            assertEquals(4, rules.size());
        }
    }

    @Test
    public void testEqualIntervalClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules = builder.equalIntervalClassification(pointCollection, "foo",
                    Integer.class, 4, false, false);
            assertEquals(4, rules.size());
        }
    }

    @Test
    public void testUniqueIntervalClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules = builder.uniqueIntervalClassification(pointCollection, "group",
                    Integer.class, -1, false);
            assertEquals(4, rules.size());

            rules = builder.uniqueIntervalClassification(pointCollection, "id", Integer.class, -1,
                    false);
            assertEquals(8, rules.size());
        }
    }

    @Test
    public void testJenksClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules = builder.jenksClassification(lineCollection, "jenks71", Integer.class,
                    10, false, false);
            assertEquals(10, rules.size());
        }
    }

    @Test
    public void testPolygonStyle() throws Exception {
        if (pointCollection != null) {
            int numClasses = 10;
            List<Rule> rules = builder.equalIntervalClassification(pointCollection, "foo",
                    Integer.class, numClasses, false, false);
            builder.polygonStyle(rules, new BlueColorRamp(), false);
            Rule ruleOne = rules.get(0);
            assertTrue(ruleOne.getSymbolizers()[0] instanceof PolygonSymbolizer);
            PolygonSymbolizer symbolizer = (PolygonSymbolizer) ruleOne.getSymbolizers()[0];
            assertEquals(new Color(0, 0, 49),
                    symbolizer.getFill().getColor().evaluate(null, Color.class));
            assertNotNull(ruleOne.getFilter());
            assertEquals(numClasses, rules.size());
        }
    }

    @Test
    public void testPolygonStyleReverse() throws Exception {
        if (pointCollection != null) {
            int numClasses = 10;
            List<Rule> rules = builder.equalIntervalClassification(pointCollection, "foo",
                    Integer.class, numClasses, false, false);
            builder.polygonStyle(rules, new BlueColorRamp(), true);
            PolygonSymbolizer symbolizer = (PolygonSymbolizer) rules.get(0).getSymbolizers()[0];
            assertEquals(new Color(0, 0, 224),
                    symbolizer.getFill().getColor().evaluate(null, Color.class));
            assertEquals(numClasses, rules.size());
        }
    }

    @Test
    public void testLineStyle() throws Exception {
        if (lineCollection != null) {
            int numClasses = 10;
            List<Rule> rules = builder.jenksClassification(lineCollection, "jenks71", Integer.class,
                    numClasses, false, false);
            builder.lineStyle(rules, new BlueColorRamp(), false);
            Rule ruleOne = rules.get(0);
            assertTrue(ruleOne.getSymbolizers()[0] instanceof LineSymbolizer);
            LineSymbolizer symbolizer = (LineSymbolizer) ruleOne.getSymbolizers()[0];
            assertEquals(new Color(0, 0, 49),
                    symbolizer.getStroke().getColor().evaluate(null, Color.class));
            assertNotNull(ruleOne.getFilter());
            assertEquals(10, rules.size());
        }
    }

    @Test
    public void testLineStyleReverse() throws Exception {
        if (lineCollection != null) {
            int numClasses = 10;
            List<Rule> rules = builder.jenksClassification(lineCollection, "jenks71", Integer.class,
                    numClasses, false, false);
            builder.lineStyle(rules, new BlueColorRamp(), true);
            LineSymbolizer symbolizer = (LineSymbolizer) rules.get(0).getSymbolizers()[0];
            assertEquals(new Color(0, 0, 224),
                    symbolizer.getStroke().getColor().evaluate(null, Color.class));
        }
    }

}
