/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.io.IOException;
import java.util.Iterator;
import java.util.TreeSet;

import org.geoserver.sldservice.rest.resource.ClassifierResource;
import org.geotools.data.DataUtilities;
import org.geotools.data.memory.MemoryDataStore;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.factory.Hints;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.filter.function.FilterFunction_parseDouble;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.mockito.Mockito;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.restlet.resource.Representation;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;


public class ClassifierTestSupport extends SLDServiceBaseTest {

    private static final int DEFAULT_INTERVALS = 2;

    protected SimpleFeatureCollection pointCollection, lineCollection;

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());

    protected SimpleFeatureType dataType;

    protected SimpleFeature[] testFeatures;
    
    ClassifierResource resource;
    
    private static final String sldPrefix = "<StyledLayerDescriptor><NamedLayer><Name>feature</Name><UserStyle><FeatureTypeStyle>";
    private static final String sldPostfix = "</FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";
    

    @Override
    public void setUp() throws Exception  {
        super.setUp();
        
        
        
        resource = new ClassifierResource(context, request, response, catalog);
        
        dataType = DataUtilities.createType("classification.test1",
                "id:0,name:string,foo:int,bar:double,geom:Point,group:String");

        int iVal[] = new int[] { 4, 90, 20, 43, 29, 61, 8, 12 };
        double dVal[] = new double[] { 2.5, 80.433, 24.5, 9.75, 18, 53, 43.2, 16 };
        String[] names = new String[] {"foo", "bar", "bar", "foo", "foobar", "bar", "foo", "foo"};

        testFeatures = new SimpleFeature[iVal.length];
        GeometryFactory fac = new GeometryFactory();

        for (int i = 0; i < iVal.length; i++) {
            testFeatures[i] = SimpleFeatureBuilder.build(dataType,
                    new Object[] { new Integer(i + 1), names[i], new Integer(iVal[i]), new Double(dVal[i]),
                            fac.createPoint(new Coordinate(iVal[i], iVal[i])), "Group" + (i % 4) },
                    "classification.t" + (i + 1));
        }

        MemoryDataStore store = new MemoryDataStore();
        store.createSchema(dataType);
        store.addFeatures(testFeatures);
        SimpleFeatureSource featureSource = store.getFeatureSource("test1");
        pointCollection = featureSource.getFeatures();



        double[] jenks71 = { 50.12, 83.9, 76.43, 71.61, 79.66, 84.84, 87.87, 92.45, 119.9, 155.3,
                131.5, 111.8, 96.78, 86.75, 62.41, 96.37, 75.51, 77.29, 85.41, 116.4, 58.5, 75.29,
                66.32, 62.65, 80.45, 72.76, 63.67, 60.27, 68.45, 100.1, 55.3, 54.07, 57.49, 73.52,
                68.25, 64.28, 50.64, 52.47, 68.19, 57.4, 39.72, 60.66, 57.59, 38.22, 57.22, 67.04,
                47.29, 71.05, 50.53, 34.63, 59.65, 62.06, 52.89, 56.35, 57.26, 53.77, 59.89, 55.44,
                45.4, 52.21, 49.38, 51.15, 54.27, 54.32, 41.2, 34.58, 50.11, 52.05, 33.82, 39.88,
                36.24, 41.02, 46.13, 51.15, 32.28, 33.26, 31.78, 31.28, 50.52, 47.21, 32.69, 38.3,
                33.83, 40.3, 40.62, 32.14, 31.66, 26.09, 39.84, 24.83, 28.2, 31.19, 37.57, 27.16,
                23.42, 18.57, 30.97, 17.82, 15.57, 15.93, 28.71, 32.22 };
        SimpleFeature[] features = new SimpleFeature[jenks71.length];
        SimpleFeatureType jenksType = DataUtilities.createType("jenks71", "id:0,jenks71:double,geom:LineString");
        for(int i=0;i<jenks71.length;i++) {

            features[i] = SimpleFeatureBuilder.build(jenksType, new Object[] { new Integer(i + 1), new Double(jenks71[i]),
                    fac.createLineString(new Coordinate[]{new Coordinate(jenks71[i], jenks71[i]),
                    new Coordinate(jenks71[i]-1.0, jenks71[i]+1.0)})},"jenks"+i);
        }
        MemoryDataStore jenks = new MemoryDataStore();
        jenks.createSchema(jenksType);
        jenks.addFeatures(features);
        lineCollection = jenks.getFeatureSource("jenks71").getFeatures();
        
        Mockito.doReturn(featureSource).when(resourcePool)
                .getFeatureSource(Mockito.eq(testFeatureTypeInfo), Mockito.any(Hints.class));
    }

    public void testClassifyForFeatureDefault() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "foo");
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                DEFAULT_INTERVALS);
        checkRule(rules[0], "#680000", org.opengis.filter.And.class);
        checkRule(rules[1], "#B20000", org.opengis.filter.And.class);
    }
    
    public void testClassifyOpenRange() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "id");
        attributes.put("intervals", 3);
        attributes.put("open", true);
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
        checkRule(rules[1], "#8C0000", org.opengis.filter.And.class);
        checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsGreaterThan.class);
    }
    
    public void testQuantile() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "foo");
        attributes.put("intervals", 3);
        attributes.put("open", true);
        attributes.put("method", "quantile");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        assertTrue(rules[0].getTitle().contains("20.0"));
        assertTrue(rules[1].getTitle().contains("20.0"));
        assertTrue(rules[1].getTitle().contains("61.0"));
        assertTrue(rules[2].getTitle().contains("61.0"));
    }
    
    public void testJenks() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "foo");
        attributes.put("intervals", 3);
        attributes.put("open", true);
        attributes.put("method", "jenks");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        assertTrue(rules[0].getTitle().contains("12.0"));
        assertTrue(rules[1].getTitle().contains("12.0"));
        assertTrue(rules[1].getTitle().contains("29.0"));
        assertTrue(rules[2].getTitle().contains("29.0"));
    }
    
    public void testEqualInterval() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "foo");
        attributes.put("intervals", 3);
        attributes.put("open", true);
        attributes.put("method", "equalInterval");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        assertTrue(rules[0].getTitle().contains("32.6"));
        assertTrue(rules[1].getTitle().contains("32.6"));
        assertTrue(rules[1].getTitle().contains("61.3"));
        assertTrue(rules[2].getTitle().contains("61.3"));
    }
    
    public void testUnique() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "name");
        attributes.put("intervals", 3);
        attributes.put("method", "uniqueInterval");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#8C0000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#C30000", org.opengis.filter.PropertyIsEqualTo.class);
        TreeSet<String> orderedRules = new TreeSet<String>();
        orderedRules.add(rules[0].getTitle());
        orderedRules.add(rules[1].getTitle());
        orderedRules.add(rules[2].getTitle());
        Iterator iter = orderedRules.iterator();
        assertEquals("bar", iter.next());
        assertEquals("foo", iter.next());
        assertEquals("foobar", iter.next());
    }
    
    public void testBlueRamp() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "name");
        attributes.put("intervals", 3);
        attributes.put("method", "uniqueInterval");
        attributes.put("ramp", "blue");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        checkRule(rules[0], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
        
    }
    
    public void testReverse() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "name");
        attributes.put("intervals", 3);
        attributes.put("method", "uniqueInterval");
        attributes.put("ramp", "blue");
        attributes.put("reverse", true);
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        checkRule(rules[0], "#0000C3", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#00008C", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#000055", org.opengis.filter.PropertyIsEqualTo.class);
        
    }
    
    public void testNormalize() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "id");
        attributes.put("intervals", 3);
        attributes.put("open", true);
        attributes.put("normalize", true);
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        checkRule(rules[0], "#550000", org.opengis.filter.PropertyIsLessThanOrEqualTo.class);
        org.opengis.filter.PropertyIsLessThanOrEqualTo filter = (org.opengis.filter.PropertyIsLessThanOrEqualTo) rules[0].getFilter();
        assertTrue(filter.getExpression1() instanceof FilterFunction_parseDouble);
    }
    
    public void testCustomRamp() throws IOException {
        attributes.put("layer", FEATURETYPE_LAYER);
        attributes.put("attribute", "name");
        attributes.put("intervals", 3);
        attributes.put("method", "uniqueInterval");
        attributes.put("ramp", "custom");
        attributes.put("startColor", "0xFF0000");
        attributes.put("endColor", "0x0000FF");
        
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        Rule[] rules = checkRules(resultXml.replace("<Rules>", sldPrefix).replace("</Rules>", sldPostfix),
                3);
        
        checkRule(rules[0], "#FF0000", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[1], "#AA0055", org.opengis.filter.PropertyIsEqualTo.class);
        checkRule(rules[2], "#5500AA", org.opengis.filter.PropertyIsEqualTo.class);
        
    }
    
    private void checkRule(Rule rule, String color, Class<?> filterType) {
        assertNotNull(rule.getFilter());
        assertTrue(filterType.isAssignableFrom(rule.getFilter().getClass()));
        assertNotNull(rule.getSymbolizers());
        assertEquals(1, rule.getSymbolizers().length);
        assertTrue(rule.getSymbolizers()[0] instanceof PolygonSymbolizer);
        PolygonSymbolizer symbolizer = (PolygonSymbolizer) rule.getSymbolizers()[0];
        assertNotNull(symbolizer.getFill());
        assertEquals(color, symbolizer.getFill().getColor().toString());
    }

    public void testClassifyForCoverageIsEmpty() throws IOException {
        
        attributes.put("layer", COVERAGE_LAYER);
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        assertEquals("<list/>", resultXml);
    }
    
    private Rule[] checkRules(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(classes, rules.length);
        return rules;
    }

    @Override
    protected String getServiceUrl() {
        return "classify";
    }
}