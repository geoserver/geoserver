/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.awt.*;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.geoserver.sldservice.utils.classifier.impl.BlueColorRamp;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.text.cql2.CQL;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.util.factory.GeoTools;
import org.junit.Before;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;

public class RulesBuilderTest {
    private static final int MAX_COLOR_INT = 255;
    private static final int MIN_COLOR_INT = 52;
    private RulesBuilder builder;

    protected SimpleFeatureCollection pointCollection, lineCollection, polygonCollection;

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    protected SimpleFeatureType dataType;

    protected SimpleFeature[] testFeatures;

    @Before
    public void setUp() throws Exception {
        builder = new RulesBuilder();

        dataType =
                DataUtilities.createType(
                        "classification.test1",
                        "id:0,name:string,foo:int,bar:double,geom:Point,group:String");

        int iVal[] = new int[] {4, 90, 20, 43, 29, 61, 8, 12};
        double dVal[] = new double[] {2.5, 80.433, 24.5, 9.75, 18, 53, 43.2, 16};
        String[] names = new String[] {"foo", "bar", "bar", "foo", "foobar", "bar", "foo", "foo"};

        testFeatures = new SimpleFeature[iVal.length];
        GeometryFactory fac = new GeometryFactory();

        for (int i = 0; i < iVal.length; i++) {
            testFeatures[i] =
                    SimpleFeatureBuilder.build(
                            dataType,
                            new Object[] {
                                Integer.valueOf(i + 1),
                                names[i],
                                Integer.valueOf(iVal[i]),
                                Double.valueOf(dVal[i]),
                                fac.createPoint(new Coordinate(iVal[i], iVal[i])),
                                "Group" + (i % 4)
                            },
                            "classification.t" + (i + 1));
        }

        MemoryDataStore store = new MemoryDataStore();
        store.createSchema(dataType);
        store.addFeatures(testFeatures);
        SimpleFeatureSource featureSource = store.getFeatureSource("test1");
        pointCollection = featureSource.getFeatures();

        double[] jenks71 = {
            50.12, 83.9, 76.43, 71.61, 79.66, 84.84, 87.87, 92.45, 119.9, 155.3, 131.5, 111.8,
            96.78, 86.75, 62.41, 96.37, 75.51, 77.29, 85.41, 116.4, 58.5, 75.29, 66.32, 62.65,
            80.45, 72.76, 63.67, 60.27, 68.45, 100.1, 55.3, 54.07, 57.49, 73.52, 68.25, 64.28,
            50.64, 52.47, 68.19, 57.4, 39.72, 60.66, 57.59, 38.22, 57.22, 67.04, 47.29, 71.05,
            50.53, 34.63, 59.65, 62.06, 52.89, 56.35, 57.26, 53.77, 59.89, 55.44, 45.4, 52.21,
            49.38, 51.15, 54.27, 54.32, 41.2, 34.58, 50.11, 52.05, 33.82, 39.88, 36.24, 41.02,
            46.13, 51.15, 32.28, 33.26, 31.78, 31.28, 50.52, 47.21, 32.69, 38.3, 33.83, 40.3, 40.62,
            32.14, 31.66, 26.09, 39.84, 24.83, 28.2, 31.19, 37.57, 27.16, 23.42, 18.57, 30.97,
            17.82, 15.57, 15.93, 28.71, 32.22
        };
        SimpleFeature[] features = new SimpleFeature[jenks71.length];
        SimpleFeatureType jenksType =
                DataUtilities.createType("jenks71", "id:0,jenks71:double,geom:LineString");
        for (int i = 0; i < jenks71.length; i++) {

            features[i] =
                    SimpleFeatureBuilder.build(
                            jenksType,
                            new Object[] {
                                Integer.valueOf(i + 1),
                                Double.valueOf(jenks71[i]),
                                fac.createLineString(
                                        new Coordinate[] {
                                            new Coordinate(jenks71[i], jenks71[i]),
                                            new Coordinate(jenks71[i] - 1.0, jenks71[i] + 1.0)
                                        })
                            },
                            "jenks" + i);
        }
        MemoryDataStore jenks = new MemoryDataStore();
        jenks.createSchema(jenksType);
        jenks.addFeatures(features);
        lineCollection = jenks.getFeatureSource("jenks71").getFeatures();

        SimpleFeatureType polygonType =
                DataUtilities.createType(
                        "polygons",
                        "id:0,name:string,foo:int,bar:double,geom:Polygon,group:String");

        List<SimpleFeature> polygonFeatures =
                Arrays.stream(testFeatures)
                        .map(
                                sf -> {
                                    SimpleFeatureBuilder fb = new SimpleFeatureBuilder(polygonType);
                                    fb.init(sf);
                                    Geometry geom = (Geometry) sf.getAttribute("geom");
                                    double radius =
                                            Math.sqrt((Integer) sf.getAttribute("foo") / Math.PI);
                                    fb.set("geom", geom.buffer(radius));
                                    return fb.buildFeature(sf.getID());
                                })
                        .collect(Collectors.toList());
        store.createSchema(polygonType);
        store.addFeatures(polygonFeatures);
        SimpleFeatureSource polygonSource = store.getFeatureSource("polygons");
        polygonCollection = polygonSource.getFeatures();
    }

    @Test
    public void testQuantileClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules =
                    builder.quantileClassification(
                            pointCollection, "foo", Integer.class, 4, false, false);
            assertEquals(4, rules.size());
        }
    }

    @Test
    public void testEqualIntervalClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules =
                    builder.equalIntervalClassification(
                            pointCollection, "foo", Integer.class, 4, false, false);
            assertEquals(4, rules.size());
        }
    }

    @Test
    public void testUniqueIntervalClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules =
                    builder.uniqueIntervalClassification(
                            pointCollection, "group", Integer.class, -1, false);
            assertEquals(4, rules.size());

            rules =
                    builder.uniqueIntervalClassification(
                            pointCollection, "id", Integer.class, -1, false);
            assertEquals(8, rules.size());
        }
    }

    @Test
    public void testJenksClassification() throws Exception {
        if (pointCollection != null) {
            List<Rule> rules =
                    builder.jenksClassification(
                            lineCollection, "jenks71", Integer.class, 10, false, false);
            assertEquals(10, rules.size());
        }
    }

    @Test
    public void testPolygonStyle() throws Exception {
        if (pointCollection != null) {
            int numClasses = 10;
            List<Rule> rules =
                    builder.equalIntervalClassification(
                            pointCollection, "foo", Integer.class, numClasses, false, false);
            builder.polygonStyle(rules, new BlueColorRamp(), false);
            Rule ruleOne = rules.get(0);
            assertTrue(ruleOne.symbolizers().get(0) instanceof PolygonSymbolizer);
            PolygonSymbolizer symbolizer = (PolygonSymbolizer) ruleOne.symbolizers().get(0);
            assertEquals(
                    new Color(0, 0, MIN_COLOR_INT),
                    symbolizer.getFill().getColor().evaluate(null, Color.class));
            assertNotNull(ruleOne.getFilter());
            assertEquals(numClasses, rules.size());
        }
    }

    @Test
    public void testPolygonStyleReverse() throws Exception {
        if (pointCollection != null) {
            int numClasses = 10;
            List<Rule> rules =
                    builder.equalIntervalClassification(
                            pointCollection, "foo", Integer.class, numClasses, false, false);
            builder.polygonStyle(rules, new BlueColorRamp(), true);
            PolygonSymbolizer symbolizer = (PolygonSymbolizer) rules.get(0).symbolizers().get(0);
            assertEquals(
                    new Color(0, 0, MAX_COLOR_INT),
                    symbolizer.getFill().getColor().evaluate(null, Color.class));
            assertEquals(numClasses, rules.size());
        }
    }

    @Test
    public void testLineStyle() throws Exception {
        if (lineCollection != null) {
            int numClasses = 10;
            List<Rule> rules =
                    builder.jenksClassification(
                            lineCollection, "jenks71", Integer.class, numClasses, false, false);
            builder.lineStyle(rules, new BlueColorRamp(), false);
            Rule ruleOne = rules.get(0);
            assertTrue(ruleOne.symbolizers().get(0) instanceof LineSymbolizer);
            LineSymbolizer symbolizer = (LineSymbolizer) ruleOne.symbolizers().get(0);
            assertEquals(
                    new Color(0, 0, MIN_COLOR_INT),
                    symbolizer.getStroke().getColor().evaluate(null, Color.class));
            assertNotNull(ruleOne.getFilter());
            assertEquals(10, rules.size());
        }
    }

    @Test
    public void testLineStyleReverse() throws Exception {
        if (lineCollection != null) {
            int numClasses = 10;
            List<Rule> rules =
                    builder.jenksClassification(
                            lineCollection, "jenks71", Integer.class, numClasses, false, false);
            builder.lineStyle(rules, new BlueColorRamp(), true);
            LineSymbolizer symbolizer = (LineSymbolizer) rules.get(0).symbolizers().get(0);
            assertEquals(
                    new Color(0, 0, MAX_COLOR_INT),
                    symbolizer.getStroke().getColor().evaluate(null, Color.class));
        }
    }

    @Test
    public void testEqualAreaClassification() throws Exception {
        List<Rule> rules =
                builder.equalAreaClassification(
                        polygonCollection, "foo", Integer.class, 5, false, false);
        assertEquals(4, rules.size());
        assertEquals(CQL.toFilter("foo >= 4.0 AND foo < 43.0"), rules.get(0).getFilter());
        assertEquals(CQL.toFilter("foo >= 43.0 AND foo < 61.0"), rules.get(1).getFilter());
        assertEquals(CQL.toFilter("foo >= 61.0 AND foo < 90.0"), rules.get(2).getFilter());
        assertEquals(CQL.toFilter("foo = 90.0"), rules.get(3).getFilter());
    }
}
