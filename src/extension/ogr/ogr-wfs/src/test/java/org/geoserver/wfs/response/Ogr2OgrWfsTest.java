/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs.response;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;
import static org.junit.Assert.*;

import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Document;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class Ogr2OgrWfsTest extends GeoServerSystemTestSupport {
    
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);
        
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("", "http://www.opengis.net/wfs");
        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
    }

    @Before
    public void setup() {
        Assume.assumeTrue(Ogr2OgrTestUtil.isOgrAvailable());
        OgrConfiguration.DEFAULT.ogr2ogrLocation = Ogr2OgrTestUtil.getOgr2Ogr();
        OgrConfiguration.DEFAULT.gdalData = Ogr2OgrTestUtil.getGdalData();
        
        // force reload of the config, some tests alter it
        Ogr2OgrConfigurator configurator = applicationContext.getBean(Ogr2OgrConfigurator.class);
        configurator.loadConfiguration();
    }
    
    @Test
    public void testCapabilities() throws Exception {
        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);
        
        // while we cannot know what formats are available, the other tests won't pass if KML is not there 
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
    }
    
    @Test
    public void testEmptyCapabilities() throws Exception {
        Ogr2OgrOutputFormat of = applicationContext.getBean(Ogr2OgrOutputFormat.class);
        of.clearFormats();
        
        String request = "wfs?request=GetCapabilities&version=1.0.0";
        Document dom = getAsDOM(request);
        // print(dom);
        
        // this used to NPE 
        assertXpathEvaluatesTo("0", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:OGR-KML)", dom);
        assertXpathEvaluatesTo("1", "count(//wfs:GetFeature/wfs:ResultFormat/wfs:SHAPE-ZIP)", dom);
    }

    @Test
    public void testSimpleRequest() throws Exception {
        String request = "wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);
        
        // check content type
        assertEquals("application/vnd.google-earth.kml", resp.getContentType());
        
        // read back
        Document dom = dom(getBinaryInputStream(resp));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }
    
    @Test
    public void testSimpleRequest20() throws Exception {
        String request = "wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) + "&version=2.0.0&service=wfs&outputFormat=OGR-KML&srsName=EPSG:4326";
        MockHttpServletResponse resp = getAsServletResponse(request);
        
        // check content type
        assertEquals("application/vnd.google-earth.kml", resp.getContentType());
        
        // read back
        Document dom = dom(getBinaryInputStream(resp));
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }
    
    @Test
    public void testDoubleRequest() throws Exception {
        String request = "wfs?request=GetFeature&typename=" + getLayerId(MockData.BUILDINGS) 
            + "," + getLayerId(MockData.BRIDGES) + "&version=1.0.0&service=wfs&outputFormat=OGR-KML";
        MockHttpServletResponse resp = getAsServletResponse(request);
        
        // check content type
        assertEquals("application/zip", resp.getContentType());
        
        // check content disposition
        assertEquals("attachment; filename=Buildings.zip", resp.getHeader("Content-Disposition"));
        
        // read back
        ZipInputStream zis = new ZipInputStream(getBinaryInputStream(resp));

        // get buildings entry
        ZipEntry entry = null;
        entry = zis.getNextEntry();
        while (entry != null) {
            if (entry.getName().equals("Buildings.kml")) {
                break;
            }
            entry = zis.getNextEntry();
        }
        
        assertNotNull(entry);
        assertEquals("Buildings.kml", entry.getName());

        // parse the kml to check it's really xml... 
        Document dom = dom(zis);
        // print(dom);

        // some very light assumptions on the contents, since we
        // cannot control how ogr encodes the kml... let's just assess
        // it's kml with the proper number of features
        assertEquals("kml", dom.getDocumentElement().getTagName());
        assertEquals(2, dom.getElementsByTagName("Placemark").getLength());
    }
}
