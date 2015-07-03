/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.rest;

import static junit.framework.Assert.assertEquals;
import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.RegexFileFilter;
import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geotools.data.DataUtilities;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class StructuredCoverageStoresTest extends CatalogRESTTestSupport {
    
    protected static QName WATTEMP = new QName(MockData.WCS_PREFIX, "watertemp", MockData.WCS_PREFIX);
    
    List<File> movedFiles = new ArrayList<File>();

    private XpathEngine xpath;

    private File mosaic;

    @BeforeClass
    public static void setupTimeZone() {
        System.setProperty("user.timezone", "GMT");
    }
    
    @Before
    public void prepare() {
        xpath = XMLUnit.newXpathEngine();
    }
    
    @AfterClass
    public static void cleanupTimeZone() {
        System.clearProperty("user.timezone");
    }
    
    @Before
    public void setupWaterTemp() throws IOException {
        getTestData().addRasterLayer(WATTEMP, "watertemp.zip", null, null, SystemTestData.class, getCatalog());
        
        // drop the stores
        getGeoServer().reset();
        
        mosaic = new File(testData.getDataDirectoryRoot(), WATTEMP.getLocalPart());
        for (File file : FileUtils.listFiles(mosaic, new RegexFileFilter("NCOM_.*100_.*tiff"), null)) {
            File target = new File(file.getParentFile().getParentFile(), file.getName());
            movedFiles.add(target);
            if(target.exists()) {
                assertTrue(target.delete());
            }
            assertTrue(file.renameTo(target));
        };
        for (File file : FileUtils.listFiles(mosaic, new RegexFileFilter("watertemp.*"), null)) {
            assertTrue(file.delete());
        };
    }
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        testData.addWorkspace(SystemTestData.WCS_PREFIX, SystemTestData.WCS_URI, getCatalog());

        // setup the namespace context for this test
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("gf", "http://www.geoserver.org/rest/granules");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }
    
    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        // nothing to do
        testData.setUpSecurity();
    }


    @Test 
    public void testIndexResources() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp.xml");
        // print(dom);
        assertXpathEvaluatesTo("watertemp", "/coverageStore/name", dom);
        
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp.xml");
        // print(dom);
        assertXpathEvaluatesTo("watertemp", "/coverage/name", dom);
        assertXpathEvaluatesTo("watertemp", "/coverage/nativeName", dom);
        // todo: check there is a link to the index
        
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index.xml");
        // print(dom);
        assertXpathEvaluatesTo("4", "count(//Schema/attributes/Attribute)", dom);
        assertXpathEvaluatesTo("com.vividsolutions.jts.geom.MultiPolygon", "/Schema/attributes/Attribute[name='the_geom']/binding", dom);
        assertXpathEvaluatesTo("java.lang.String", "/Schema/attributes/Attribute[name='location']/binding", dom);
        assertXpathEvaluatesTo("java.util.Date", "/Schema/attributes/Attribute[name='ingestion']/binding", dom);
        assertXpathEvaluatesTo("java.lang.Integer", "/Schema/attributes/Attribute[name='elevation']/binding", dom);
        
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation", dom);
        
        // get the granules ids
        String octoberId = xpath.evaluate("//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/@fid", dom);
        String novemberId = xpath.evaluate("//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/@fid", dom);
        
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/" + octoberId + ".xml");   
        // print(dom);
        assertXpathEvaluatesTo(octoberId, "//gf:watertemp/@fid", dom);
        assertXpathEvaluatesTo("NCOM_wattemp_000_20081031T0000000_12.tiff", "//gf:watertemp/gf:location", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp/gf:elevation", dom);
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/" + novemberId + ".xml");
        // print(dom);
        assertXpathEvaluatesTo(novemberId, "//gf:watertemp/@fid", dom);
        assertXpathEvaluatesTo("NCOM_wattemp_000_20081101T0000000_12.tiff", "//gf:watertemp/gf:location", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp/gf:elevation", dom);
    }
    
    @Test
    public void testMissingGrandule() throws Exception {
        MockHttpServletResponse response = getAsServletResponse( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/notThere.xml");
        assertEquals(404, response.getStatusCode());
    }
    
    @Test
    public void testGetWrongGranule() throws Exception {
        // Parameters for the request
        String ws = "wcs";
        String cs = "watertemp";
        String g = "notThere.html";
        // Request path
        String requestPath = "/rest/workspaces/" + ws + "/coveragestores/" + cs + "/coverages/" + cs + "/index/granules/" + g;
        // Exception path
        String exception = "Could not find a granule with id " + g + " in coveage " + ws + ":" + cs;
        // First request should thrown an exception
        MockHttpServletResponse response = getAsServletResponse(requestPath);
        assertEquals(404, response.getStatusCode());
        assertTrue(response.getOutputStreamContent().contains(
                exception));
        // Same request with ?quietOnNotFound should not throw an exception
        response = getAsServletResponse(requestPath + "?quietOnNotFound=true");
        assertEquals(404, response.getStatusCode());
        assertFalse(response.getOutputStreamContent().contains(
                exception));
        // No exception thrown
        assertTrue(response.getOutputStreamContent().isEmpty());
    }
    
    @Test
    public void testDeleteSingleGranule() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        
        // get the granule ids
        String octoberId = xpath.evaluate("//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/@fid", dom);
        assertNotNull(octoberId);
        
        // delete it
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules/" + octoberId);
        assertEquals(200, response.getStatusCode());

        // check it's gone from the index
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("1", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo("0", "count(//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff'])", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation", dom);
    }
    
    @Test
    public void testDeleteAllGranules() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        // print(dom);
        
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules");
        assertEquals(200, response.getStatusCode());

        // check it's gone from the index
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("0", "count(//gf:watertemp)", dom);
    }
    
    @Test
    public void testDeleteByFilter() throws Exception {
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        assertXpathEvaluatesTo("2", "count(//gf:watertemp)", dom);
        // print(dom);
        
        MockHttpServletResponse response = deleteAsServletResponse("/rest/workspaces/wcs/coveragestores/watertemp/coverages" +
        		"/watertemp/index/granules?filter=ingestion=2008-11-01T00:00:00Z");
        assertEquals(200, response.getStatusCode());

        // check it's gone from the index
        dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("1", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation", dom);
    }
    
    @Test
    public void testHarvestSingle() throws Exception {
        File file = movedFiles.get(0);
        File target = new File(mosaic, file.getName());
        assertTrue(file.renameTo(target));
        
        URL url = DataUtilities.fileToURL(target.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response = postAsServletResponse("/rest/workspaces/wcs/coveragestores/watertemp/external.imagemosaic", 
                body, "text/plain");
        assertEquals(202, response.getStatusCode());
        
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("3", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo("1", "count(//gf:watertemp[gf:location = '" + file.getName() + "'])", dom);
    }
    
    @Test
    public void testHarvestMulti() throws Exception {
        for (File file : movedFiles) {
            File target = new File(mosaic, file.getName());
            assertTrue(file.renameTo(target));
        }
        
        // re-harvest the entire mosaic (two files refreshed, two files added)
        URL url = DataUtilities.fileToURL(mosaic.getCanonicalFile());
        String body = url.toExternalForm();
        MockHttpServletResponse response = postAsServletResponse("/rest/workspaces/wcs/coveragestores/watertemp/external.imagemosaic", 
                body, "text/plain");
        assertEquals(202, response.getStatusCode());
        
        Document dom = getAsDOM( "/rest/workspaces/wcs/coveragestores/watertemp/coverages/watertemp/index/granules.xml");
        // print(dom);
        assertXpathEvaluatesTo("4", "count(//gf:watertemp)", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081031T0000000_12.tiff']/gf:elevation", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("0", "//gf:watertemp[gf:location = 'NCOM_wattemp_000_20081101T0000000_12.tiff']/gf:elevation", dom);
        assertXpathEvaluatesTo("2008-10-31T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081031T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("100", "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081031T0000000_12.tiff']/gf:elevation", dom);
        assertXpathEvaluatesTo("2008-11-01T00:00:00Z", "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081101T0000000_12.tiff']/gf:ingestion", dom);
        assertXpathEvaluatesTo("100", "//gf:watertemp[gf:location = 'NCOM_wattemp_100_20081101T0000000_12.tiff']/gf:elevation", dom);
    }
    
}
