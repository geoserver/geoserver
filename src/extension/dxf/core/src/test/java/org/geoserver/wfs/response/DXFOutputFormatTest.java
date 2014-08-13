/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Test the DXFOutputFormat WFS extension.
 * @author Mauro Bartolomeoli, mbarto@infosia.it
 *
 */
public class DXFOutputFormatTest extends WFSTestSupport {


    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        // by default we parse layers format_options as a string
        LayersKvpParser.parseAsList = false;
    }

    /**
     * Checks that dxf contains all the elements of sequence, in order.
     * Used by many tests to verify DXF structure.
     * @param dxf
     * @param sequence
     */
    private void checkSequence(String dxf,String[] sequence,int pos) {        
        for(String item: sequence) {
            pos=dxf.indexOf(item,pos+1);
            assertTrue(pos!=-1);
        }
    }
    
    /**
     * Checks that dxf contains all the elements of sequence, in order.
     * Used by many tests to verify DXF structure.
     * @param dxf
     * @param sequence
     */
    private void checkSequence(String dxf,String[] sequence) {
        checkSequence(dxf,sequence,-1);
    }
    
    /**
     * Test a request with two queries.
     * @throws Exception
     */
    @Test
    public void testMultiLayer() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points,MPoints&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER","POINTS","LAYER","MPOINTS"});        
    }

    /**
     * Test DXF-ZIP format.
     * @throws Exception
     */
    @Test
    public void testZipOutput() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points&outputFormat=dxf-zip");
        // check mime type
        assertEquals("application/zip", resp.getContentType());
    }

    /**
     * Test a Point geometry.
     * @throws Exception
     */
    @Test
    public void testPoints() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "Points");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        checkSequence(sResponse,new String[] {"POINT"},pos);        
        
    }
    /**
     * Test a MultiPoint geometry.
     * @throws Exception
     */
    @Test
    public void testMultiPoints() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=MPoints&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "MPoints");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        // has to insert two points
        checkSequence(sResponse,new String[] {"POINT","POINT"},pos);        
    }

    /**
     * Test a LineString geometry.
     * @throws Exception
     */
    @Test
    public void testLines() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Lines&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "Lines");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        checkSequence(sResponse,new String[] {"LWPOLYLINE"},pos);        
    }

    /**
     * Test a MultiLineString geometry.
     * @throws Exception
     */
    @Test
    public void testMultiLines() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=MLines&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "MLines");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        // has to insert two lwpolyline
        checkSequence(sResponse,new String[] {"LWPOLYLINE","LWPOLYLINE"},pos);        
    }

    /**
     * Test a Polygon geometry.
     * @throws Exception
     */
    @Test
    public void testPolygons() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "Polygons");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        // has to insert an lwpolyline
        checkSequence(sResponse,new String[] {"LWPOLYLINE"},pos);
    }
    
    /**
     * Test writeattributes option.
     * @throws Exception
     */
    @Test
    public void testWriteAttributes() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf&format_options=withattributes:true");
        String sResponse = testBasicResult(resp, "Polygons");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        // has to insert an attribute
        checkSequence(sResponse,new String[] {"ATTRIB", "AcDbAttribute"},pos);
    }

    /**
     * Test a MultiPolygon geometry.
     * @throws Exception
     */
    @Test
    public void testMultiPolygons() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=MPolygons&outputFormat=dxf");
        String sResponse = testBasicResult(resp, "MPolygons");
        int pos = getGeometrySearchStart(sResponse);
        assertTrue(pos != -1);
        // has to insert two lwpolyline
        checkSequence(sResponse,new String[] {"LWPOLYLINE","LWPOLYLINE"},pos);        
    }

    /**
     * Test format option asblocks.
     * 
     * @throws Exception
     */
    @Test
    public void testGeometryAsBlock() {
        try {
            // geometry as blocks false
            MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf");
            String sResponse = resp.getOutputStreamContent();
            assertNotNull(sResponse);
            // no insert block generated
            assertFalse(sResponse.indexOf("INSERT") != -1);
            // geometry as blocks true
            resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf&format_options=asblocks:true");
            sResponse = resp.getOutputStreamContent();
            assertNotNull(sResponse);
            // one insert block generated
            assertTrue(sResponse.indexOf("INSERT") != -1);
        } catch (Throwable t) {
            fail(t.getMessage());
        }
    }

    /**
     * Test format option version support.
     * 
     * @throws Exception
     */
    @Test
    public void testVersion() throws Exception {
        try {
            // good request, version 14
            MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf&format_options=version:14");
            String sResponse = resp.getOutputStreamContent();
            assertNotNull(sResponse);
            assertTrue(sResponse.startsWith("  0"));
            // bad request, version 13: not supported
            resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Polygons&outputFormat=dxf&format_options=version:13");
            sResponse = resp.getOutputStreamContent();
            assertNotNull(sResponse);
            // has to return an exception
            assertTrue(sResponse.indexOf("</ows:ExceptionReport>") != -1);
        } catch (Throwable t) {
            fail(t.getMessage());
        }

    }

    /**
     * Test basic extension functionality: mime/type, headers,
     * not empty output generation. 
     * @param resp
     * @param featureName
     * @return
     * @throws Exception
     */
    public String testBasicResult(MockHttpServletResponse resp, String featureName)
            throws Exception {
        // check mime type
        assertEquals("application/dxf", resp.getContentType());
        // check the content disposition
        assertEquals("attachment; filename=" + featureName + ".dxf", resp
                .getHeader("Content-Disposition"));
        // check for content (without checking in detail)
        String sResponse = resp.getOutputStreamContent();        
        assertNotNull(sResponse);
        return sResponse;

    }

    /**
     * Test the ltypes format option.
     * @throws Exception
     */
    @Test
    public void testCustomLineTypes() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Lines&outputFormat=dxf&format_options=ltypes:DASHED!--_*_!0.5");
        String sResponse = testBasicResult(resp, "Lines");
        checkSequence(sResponse,new String[] {"DASHED"});
    }
    /**
     * Test the colors format option.
     * @throws Exception
     */
    @Test
    public void testCustomColors() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points,MPoints&outputFormat=dxf&format_options=colors:1,2");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER"," 62\n     1","LAYER"," 62\n     2"});        
    }
    
    /**
     * Test custom naming for layers.
     * @throws Exception
     */
    @Test
    public void testLayerNames() throws Exception {
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points,MPoints&outputFormat=dxf&format_options=layers:MyLayer1,MyLayer2");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER","MYLAYER1","LAYER","MYLAYER2"});        
    }
    
    /**
     * Test fix for GEOS-6402.
     * @throws Exception
     */
    @Test
    public void testLayerNamesParsing() throws Exception {
        
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points,MPoints&outputFormat=dxf&format_options=layers:MyLayer1,MyLayer2");
        String sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER","MYLAYER1","LAYER","MYLAYER2"});
        
        // now repeat the test parsing layers format_options as a list, instead of a string
        LayersKvpParser.parseAsList = true;
        resp = getAsServletResponse("wfs?request=GetFeature&version=1.1.0&typeName=Points,MPoints&outputFormat=dxf&format_options=layers:MyLayer1,MyLayer2");
        sResponse = testBasicResult(resp, "Points_MPoints");
        checkSequence(sResponse,new String[] {"LAYER","LAYER","LAYER","MYLAYER1","LAYER","MYLAYER2"});
    }
    
    /**
     * Get a search starting point.
     * @param response
     * @return
     */
    private int getGeometrySearchStart(String response) {
        return response.indexOf("BLOCKS");
    }

}
