/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.junit.Assert.*;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class DimensionsRasterCapabilitiesTest extends WMSDimensionsTestSupport {
    
    @Test
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        Element e = dom.getDocumentElement();
        assertEquals("WMT_MS_Capabilities", e.getLocalName());
        assertXpathEvaluatesTo("1", "count(//Layer[Name='sf:watertemp'])", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("0", "count(//Layer/Extent)", dom);
    }
    
    @Test
    public void testDefaultElevationUnits() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, null, null);
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        
        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(DimensionInfo.ELEVATION_UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
    }

    @Test
    public void testElevationList() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.LIST, null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
//        print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0,100.0", "//Layer/Extent", dom);
    }
    
    @Test
    public void testElevationContinuous() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.CONTINUOUS_INTERVAL, 
                null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/100.0", "//Layer/Extent", dom);
    }
    
    @Test
    public void testElevationDiscreteNoResolution() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.DISCRETE_INTERVAL, 
                null, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/100.0", "//Layer/Extent", dom);
    }
    
    @Test
    public void testElevationDiscreteManualResolution() throws Exception {
        setupRasterDimension(ResourceInfo.ELEVATION, DimensionPresentation.DISCRETE_INTERVAL, 
                10.0, UNITS, UNIT_SYMBOL);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo(UNITS, "//Layer/Dimension/@units", dom);
        assertXpathEvaluatesTo(UNIT_SYMBOL, "//Layer/Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("elevation", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("0.0", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("0.0/100.0/10.0", "//Layer/Extent", dom);
    }
    
    @Test
    public void testTimeList() throws Exception {
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.LIST, null, null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
//        print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("current", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z,2008-11-01T00:00:00.000Z", "//Layer/Extent", dom);
    }
    
    @Test
    public void testTimeContinuous() throws Exception {
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.CONTINUOUS_INTERVAL, null, null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        //print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("current", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/P1D", "//Layer/Extent", dom);
    }
    
    @Test
    public void testTimeResolution() throws Exception {
        setupRasterDimension(ResourceInfo.TIME, DimensionPresentation.DISCRETE_INTERVAL, 
                new Double(1000 * 60 * 60 * 12), null, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.1.1"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//Layer/Dimension)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//Layer/Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//Layer/Extent)", dom);
        assertXpathEvaluatesTo("time", "//Layer/Extent/@name", dom);
        assertXpathEvaluatesTo("current", "//Layer/Extent/@default", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00.000Z/2008-11-01T00:00:00.000Z/PT12H", "//Layer/Extent", dom);
    }
    
    
}
