/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_3;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.wms.WMSDimensionsTestSupport;
import org.w3c.dom.Document;

public class DimensionsVectorCapabilitiesTest extends WMSDimensionsTestSupport {
    
    public void testNoDimension() throws Exception {
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);

        assertXpathEvaluatesTo("1", "count(//wms:Layer[wms:Name='sf:TimeElevation'])", dom);
        assertXpathEvaluatesTo("0", "count(//wms:Layer/wms:Dimension)", dom);
    }

    public void testElevationList() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("EPSG:5030", "//wms:Layer/wms:Dimension/@units", dom);
        assertXpathEvaluatesTo("m", "//wms:Layer/wms:Dimension/@unitSymbol", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0,1.0,2.0,3.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationContinuous() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.CONTINUOUS_INTERVAL, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("EPSG:5030", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/3.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationDiscrerteNoResolution() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.DISCRETE_INTERVAL, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("EPSG:5030", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/1.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testElevationDiscrerteManualResolution() throws Exception {
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.DISCRETE_INTERVAL, 2.0);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("EPSG:5030", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("elevation", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("0.0/3.0/2.0", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeList() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z,2011-05-02T00:00:00.000Z,2011-05-03T00:00:00.000Z,2011-05-04T00:00:00.000Z", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeContinuous() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.CONTINUOUS_INTERVAL, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/P3D", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeResolution() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.DISCRETE_INTERVAL, new Double(1000 * 60 * 60 * 24));
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check dimension has been declared
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension/@units", dom);
        // check we have the extent        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("time", "//wms:Layer/wms:Dimension/@name", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z/2011-05-04T00:00:00.000Z/P1D", "//wms:Layer/wms:Dimension", dom);
    }
    
    public void testTimeElevation() throws Exception {
        setupVectorDimension(ResourceInfo.TIME, "time", DimensionPresentation.LIST, null);
        setupVectorDimension(ResourceInfo.ELEVATION, "elevation", DimensionPresentation.LIST, null);
        
        Document dom = dom(get("wms?request=getCapabilities&version=1.3.0"), false);
        // print(dom);
        
        // check both dimension has been declared
        assertXpathEvaluatesTo("2", "count(//wms:Layer/wms:Dimension)", dom);
        assertXpathEvaluatesTo("EPSG:5030", "//wms:Layer/wms:Dimension[@name='elevation']/@units", dom);
        assertXpathEvaluatesTo("ISO8601", "//wms:Layer/wms:Dimension[@name='time']/@units", dom);
        // check we have the extent for elevation        
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension[@name='elevation'])", dom);
        assertXpathEvaluatesTo("0.0", "//wms:Layer/wms:Dimension[@name='elevation']/@default", dom);
        assertXpathEvaluatesTo("0.0,1.0,2.0,3.0", "//wms:Layer/wms:Dimension[@name='elevation']", dom);
        // check we have the extent for time
        assertXpathEvaluatesTo("1", "count(//wms:Layer/wms:Dimension[@name='time'])", dom);
        assertXpathEvaluatesTo("current", "//wms:Layer/wms:Dimension[@name='time']/@default", dom);
        assertXpathEvaluatesTo("2011-05-01T00:00:00.000Z,2011-05-02T00:00:00.000Z,2011-05-03T00:00:00.000Z,2011-05-04T00:00:00.000Z", 
                "//wms:Layer/wms:Dimension[@name='time']", dom);
    }

}
