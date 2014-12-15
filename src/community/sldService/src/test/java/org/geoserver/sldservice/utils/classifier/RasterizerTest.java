/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * Copyright (C) 2007-2008-2009 GeoSolutions S.A.S.
 *  http://www.geo-solutions.it
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sldservice.utils.classifier;

import java.io.IOException;
import java.io.StringReader;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.sldservice.rest.resource.RasterizerResource;
import org.geotools.styling.ColorMap;
import org.geotools.styling.ColorMapEntry;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.NamedLayer;
import org.geotools.styling.RasterSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.Style;
import org.geotools.styling.StyledLayer;
import org.geotools.styling.StyledLayerDescriptor;
import org.restlet.resource.Representation;
import org.springframework.context.support.ClassPathXmlApplicationContext;

public class RasterizerTest extends SLDServiceBaseTest {
    
    RasterizerResource resource;
    
    @Override
    public void setUp() throws Exception {
        super.setUp();
        
        ClassPathXmlApplicationContext appContext = new ClassPathXmlApplicationContext("sldservice-applicationContext.xml");
        appContext.refresh();
        new GeoServerExtensions().setApplicationContext(appContext);
        
        resource = new RasterizerResource(context, request, response, catalog);
    }
    
    public void testRasterizeWithNoParams() throws IOException {
        initRequestUrl(request, "xml");
        
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        assertEquals("<list/>", resultXml);
    }
    
    public void testRasterizeWithFeatureType() throws IOException {
        
        attributes.put("layer", FEATURETYPE_LAYER);
        initRequestUrl(request, "xml");
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        assertEquals("<list/>", resultXml);
    }
    
    public void testRasterizeWithCoverage() throws IOException {
        attributes.put("layer", COVERAGE_LAYER);
        attributes.put("classes", 3);
        initRequestUrl(request, "sld");
        
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        checkColorMap(resultXml, 3);
    }
    
    public void testRasterizeOptions() throws IOException {
        attributes.put("layer", COVERAGE_LAYER);
        attributes.put("classes", 5);
        attributes.put("min", 10.0);
        attributes.put("max", 50.0);
        attributes.put("digits", 1);
        attributes.put("ramp", "custom");
        attributes.put("startColor", "0xFF0000");
        attributes.put("endColor", "0x0000FF");
        initRequestUrl(request, "sld");
        
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        ColorMap map = checkColorMap(resultXml, 5);
        
        
        checkColorEntry(map.getColorMapEntries()[1], "#FF0000", "10.0", "1.0");
        checkColorEntry(map.getColorMapEntries()[5], "#3300CC", "50.0", "1.0");
    }
    
    public void testRasterizeOptions2() throws IOException {
        attributes.put("layer", COVERAGE_LAYER);
        attributes.put("classes", 5);
        attributes.put("min", 10.0);
        attributes.put("max", 50.0);
        attributes.put("digits", 2);
        
        initRequestUrl(request, "sld");
        
        resource.handleGet();
        assertNotNull(responseEntity);
        assertTrue(responseEntity instanceof Representation);
        Representation representation = (Representation)responseEntity;
        String resultXml = representation.getText().replace("\r", "").replace("\n", "");
        ColorMap map = checkColorMap(resultXml, 5);
        
        
        checkColorEntry(map.getColorMapEntries()[1], "#420000", "10.00", "1.0");
        checkColorEntry(map.getColorMapEntries()[5], "#D40000", "50.00", "1.0");
    }

    private ColorMap checkColorMap(String resultXml, int classes) {
        Rule[] rules = checkSLD(resultXml);
        assertEquals(1, rules.length);
        Rule rule = rules[0];
        assertNotNull(rule.getSymbolizers());
        assertEquals(1, rule.getSymbolizers().length);
        assertTrue(rule.getSymbolizers()[0] instanceof RasterSymbolizer);
        RasterSymbolizer symbolizer = (RasterSymbolizer)rule.getSymbolizers()[0];
        assertNotNull(symbolizer.getColorMap());
        assertEquals(classes + 1, symbolizer.getColorMap().getColorMapEntries().length);
        ColorMapEntry firstEntry = symbolizer.getColorMap().getColorMapEntries()[0];
        checkColorEntry(firstEntry, "#000000", "transparent", "0");
        return symbolizer.getColorMap();
    }

    private void checkColorEntry(ColorMapEntry firstEntry, String color, String label, String opacity) {
        assertEquals(color, firstEntry.getColor().toString());
        assertEquals(label, firstEntry.getLabel());
        assertEquals(opacity, firstEntry.getOpacity().toString());
    }
    
    @Override
    protected String getServiceUrl() {
        return "rasterize";
    }
}
