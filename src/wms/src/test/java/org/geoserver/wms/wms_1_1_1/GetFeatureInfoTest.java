/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.wms_1_1_1;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.logging.Level;

import javax.xml.namespace.QName;

import junit.framework.Test;

import org.geoserver.data.test.MockData;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.geotools.util.logging.Logging;
import org.w3c.dom.Document;

public class GetFeatureInfoTest extends WMSTestSupport {
    
    public static String WCS_PREFIX = "wcs";
    public static String WCS_URI = "http://www.opengis.net/wcs/1.1.1";
    public static QName TASMANIA_BM = new QName(WCS_URI, "BlueMarble", WCS_PREFIX);
    public static QName SQUARES = new QName(MockData.CITE_URI, "squares", MockData.CITE_PREFIX);

    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GetFeatureInfoTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        Logging.getLogger("org.geoserver.ows").setLevel(Level.OFF);
        WMSInfo wmsInfo = getGeoServer().getService(WMSInfo.class);
        wmsInfo.setMaxBuffer(50);
        getGeoServer().save(wmsInfo);
    }

    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addStyle("thickStroke", GetFeatureInfoTest.class.getResource("thickStroke.sld"));
        dataDirectory.addStyle("raster", GetFeatureInfoTest.class.getResource("raster.sld"));
        dataDirectory.addStyle("rasterScales", GetFeatureInfoTest.class.getResource("rasterScales.sld"));
        dataDirectory.addCoverage(TASMANIA_BM, GetFeatureInfoTest.class.getResource("tazbm.tiff"),
                "tiff", "raster");
        dataDirectory.addStyle("squares", GetFeatureInfoTest.class.getResource("squares.sld"));
        dataDirectory.addPropertiesType(SQUARES, GetFeatureInfoTest.class.getResource("squares.properties"),
                null);
        
        // this also adds the raster style
        dataDirectory.addCoverage(new QName(MockData.SF_URI, "mosaic", MockData.SF_PREFIX), 
               MockData.class.getResource("raster-filter-test.zip"), null, "raster");
    }
    
    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimple() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        //System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    /**
     * Tests a simple GetFeatureInfo works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testSimpleHtml() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document dom = getAsDOM(request);
        
        // count lines that do contain a forest reference
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'Forests.')])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBuffer() throws Exception {
        // to setup the request and the buffer I rendered BASIC_POLYGONS using GeoServer, then played
        // against the image coordinates
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.1.1&bbox=-4.5,-2.,4.5,7&styles=&format=jpeg&info_format=text/html" +
                "&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300";
        Document dom = getAsDOM(base + "&x=85&y=230");
        // make sure the document is empty, as we chose an area with no features inside
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the extended buffer, make sure it's in
        dom = getAsDOM(base + "&x=85&y=230&buffer=40");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
        
        // this one would end up catching everything (3 features) if it wasn't that we say the max buffer at 50
        // in the WMS configuration
        dom = getAsDOM(base + "&x=85&y=230&buffer=300");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testAutoBuffer() throws Exception {
        String layer = getLayerId(MockData.BASIC_POLYGONS);
        String base = "wms?version=1.1.1&bbox=-4.5,-2.,4.5,7&format=jpeg&info_format=text/html" +
                "&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=300&height=300&x=114&y=229";
        Document dom = getAsDOM(base + "&styles=");
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);

        // another request that will catch one feature due to the style with a thick stroke, make sure it's in
        dom = getAsDOM(base + "&styles=thickStroke");
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'BasicPolygons.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'BasicPolygons.1107531493630'])", dom);
    }
    
    /**
     * Tests GetFeatureInfo with a buffer specified works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testBufferScales() throws Exception {
        String layer = getLayerId(SQUARES);
        String base = "wms?version=1.1.1&format=png&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&styles=squares&bbox=0,0,10000,10000&feature_count=10";
        
        // first request, should provide no result, scale is 1:100
        int w = (int) (100.0 / 0.28 * 1000); // dpi compensation
        Document dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        // make sure the document is empty, the style we chose has thin lines
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr)", dom);
        
        // second request, should provide oe result, scale is 1:50
        w = (int) (200.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);

        // third request, should provide two result, scale is 1:10
        w = (int) (1000.0 / 0.28 * 1000); // dpi compensation
        dom = getAsDOM(base + "&width=" + w + "&height=" + w + "&x=20&y=" + (w - 20));
        // print(dom);
        assertXpathEvaluatesTo("2", "count(/html/body/table/tr/td[starts-with(.,'squares.')])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.1'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/td[. = 'squares.2'])", dom);
        
    }
    
    /**
     * Tests a GetFeatureInfo again works, and that the result contains the
     * expected polygon
     * 
     * @throws Exception
     */
    public void testTwoLayers() throws Exception {
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/html&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10&info";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        // GEOS-2603 GetFeatureInfo returns html tables without css style if more than one layer is selected
        assertTrue(result.indexOf("<style type=\"text/css\">") > 0);
    }
    
    /**
     * Tests that FEATURE_COUNT is respected globally, not just per layer
     * 
     * @throws Exception
     */
    public void testTwoLayersFeatureCount() throws Exception {
        // this request hits on two overlapping features, a lake and a forest
        String layer = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String request = "wms?REQUEST=GetFeatureInfo&EXCEPTIONS=application%2Fvnd.ogc.se_xml&" +
        		"BBOX=-0.002356%2C-0.004819%2C0.005631%2C0.004781&SERVICE=WMS&VERSION=1.1.0&X=267&Y=325" +
        		"&INFO_FORMAT=application/vnd.ogc.gml" +
        		"&QUERY_LAYERS=" + layer + "&Layers=" + layer + " &Styles=&WIDTH=426&HEIGHT=512" +
        	    "&format=image%2Fpng&srs=EPSG%3A4326";
        // no feature count, just one should be returned
        Document dom = getAsDOM(request);
        assertXpathEvaluatesTo("1", "count(//gml:featureMember)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);
        
        // feature count set to 2, both features should be there
        dom = getAsDOM(request + "&FEATURE_COUNT=2");
        // print(dom);
        assertXpathEvaluatesTo("2", "count(//gml:featureMember)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Forests)", dom);
        assertXpathEvaluatesTo("1", "count(//cite:Lakes)", dom);
    }


    /**
     * Check GetFeatureInfo returns an error if the format is not known, instead
     * of returning the text format as in
     * http://jira.codehaus.org/browse/GEOS-1924
     * 
     * @throws Exception
     */
    public void testUknownFormat() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=unknown/format&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        Document doc = dom(get(request), true);
        // print(doc);
        assertXpathEvaluatesTo("1", "count(//ServiceExceptionReport/ServiceException)", doc);
        assertXpathEvaluatesTo("InvalidFormat", "/ServiceExceptionReport/ServiceException/@code", doc);
        assertXpathEvaluatesTo("info_format", "/ServiceExceptionReport/ServiceException/@locator", doc);
    }
    
    public void testCoverage() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-2574
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
        		"&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
        		"&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testCoverageGML() throws Exception {
        // http://jira.codehaus.org/browse/GEOS-3996
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                        "&layers=" + layer + "&styles=&bbox=146.5,-44.5,148,-43&width=600&height=600" + 
                        "&info_format=application/vnd.ogc.gml&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        Document dom = getAsDOM(request);
        //print(dom);
        
        assertXpathEvaluatesTo("26.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:RED_BAND", dom);
        assertXpathEvaluatesTo("70.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:GREEN_BAND", dom);
        assertXpathEvaluatesTo("126.0", "//wfs:FeatureCollection/gml:featureMember/wcs:BlueMarble/wcs:BLUE_BAND", dom);
    }
    
    public void testCoverageScales() throws Exception {
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=rasterScales&bbox=146.5,-44.5,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&x=300&y=300&srs=EPSG:4326";
        
        // this one should be blank
        Document dom = getAsDOM(request + "&width=300&height=300");
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
        
        // this one should draw the coverage
        dom = getAsDOM(request + "&width=600&height=600");
        // we also have the charset which may be platf. dep.
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'RED_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'GREEN_BAND'])", dom);
        assertXpathEvaluatesTo("1", "count(/html/body/table/tr/th[. = 'BLUE_BAND'])", dom);
    }
    
    public void testOutsideCoverage() throws Exception {
        // a request which is way large on the west side, lots of blank space
        String layer = getLayerId(TASMANIA_BM);
        String request = "wms?service=wms&request=GetFeatureInfo&version=1.1.1" +
                "&layers=" + layer + "&styles=raster&bbox=0,-90,148,-43" + 
                "&info_format=text/html&query_layers=" + layer + "&width=300&height=300&x=10&y=150&srs=EPSG:4326";
        
        // this one should be blank, but not be a service exception
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/html)", dom);
        assertXpathEvaluatesTo("0", "count(/html/body/table/tr/th)", dom);
    }
    
    /**
     * Check we report back an exception when query_layer contains layers not part of LAYERS
     * @throws Exception
     */
    public void testUnkonwnQueryLayer() throws Exception {
        String layers1 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.LAKES);
        String layers2 = getLayerId(MockData.FORESTS) + "," + getLayerId(MockData.BRIDGES);
        String request = "wms?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg&info_format=text/html&request=GetFeatureInfo&layers="
                + layers1 + "&query_layers=" + layers2 + "&width=20&height=20&x=10&y=10&info";
        
        Document dom = getAsDOM(request + "");
        assertXpathEvaluatesTo("1", "count(/ServiceExceptionReport)", dom);
    }
    
    public void testLayerQualified() throws Exception {
        String layer = "Forests";
        String q = "?version=1.1.1&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String request = "cite/Ponds/wms" + q;
        Document dom = getAsDOM(request);
        assertEquals("ServiceExceptionReport", dom.getDocumentElement().getNodeName());
        
        request = "cite/Forests/wms" + q;
        String result = getAsString(request);
        //System.out.println(result);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testNonExactVersion() throws Exception {
        String layer = getLayerId(MockData.FORESTS);
        String request = "wms?version=1.0.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
                "&info_format=text/plain&request=GetFeatureInfo&layers="
                + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        String result = getAsString(request);
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
        
        request = "wms?version=1.1.0&bbox=-0.002,-0.002,0.002,0.002&styles=&format=jpeg" +
        "&info_format=text/plain&request=GetFeatureInfo&layers="
        + layer + "&query_layers=" + layer + "&width=20&height=20&x=10&y=10";
        result = getAsString(request);
        
        assertNotNull(result);
        assertTrue(result.indexOf("Green Forest") > 0);
    }
    
    public void testRasterFilterRed() throws Exception {
        String response = getAsString("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=" +
        		"&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetFeatureInfo&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150" +
                "&transparent=false&CQL_FILTER=location like 'red%25' + " +
                "&query_layers=sf:mosaic&x=10&y=10");
        
        assertTrue(response.indexOf("RED_BAND = 255.0") > 0);
        assertTrue(response.indexOf("GREEN_BAND = 0.0") > 0);
        assertTrue(response.indexOf("BLUE_BAND = 0.0") > 0);
    }
    
    public void testRasterFilterGreen() throws Exception {
        String response = getAsString("wms?bgcolor=0x000000&LAYERS=sf:mosaic&STYLES=" +
                "&FORMAT=image/png&SERVICE=WMS&VERSION=1.1.1" +
                "&REQUEST=GetFeatureInfo&SRS=EPSG:4326&BBOX=0,0,1,1&WIDTH=150&HEIGHT=150" +
                "&transparent=false&CQL_FILTER=location like 'green%25' + " +
                "&query_layers=sf:mosaic&x=10&y=10");
        
        assertTrue(response.indexOf("RED_BAND = 0.0") > 0);
        assertTrue(response.indexOf("GREEN_BAND = 255.0") > 0);
        assertTrue(response.indexOf("BLUE_BAND = 0.0") > 0);
    }
    
   
    
    
}
