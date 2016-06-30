/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2007 OpenPlans
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.capabilities;

import static junit.framework.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geoserver.platform.GeoServerEnvironment;
import org.geoserver.wms.WMSInfo;
import org.geoserver.wms.WMSTestSupport;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Unit test suite for {@link GetCapabilitiesResponse}
 * 
 * @author Simone Giannecchini - GeoSolutions
 * @version $Id$
 */
public class GetCapabilitiesReponseTest extends WMSTestSupport {
	    
    /**
     * Tests ContentDisposition
     * 
     */
    @Test
    public void testSimple() throws Exception {
        String request = "wms?version=1.1.1&request=GetCapabilities&service=WMS";
        MockHttpServletResponse result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals("inline; filename=getcapabilities_1.1.1.xml", result.getHeader("content-disposition"));
        
        request = "wms?version=1.3.0&request=GetCapabilities&service=WMS";
        result = getAsServletResponse(request);
        Assert.assertTrue(result.containsHeader("content-disposition"));
        Assert.assertEquals("inline; filename=getcapabilities_1.3.0.xml", result.getHeader("content-disposition"));
    }
    
    @Test public void testGeoServerEnvParametrization() throws Exception {
        WMSInfo wms = getGeoServer().getService(WMSInfo.class);
        try {
            if (GeoServerEnvironment.ALLOW_ENV_PARAMETRIZATION) {
                System.setProperty("TEST_SYS_PROPERTY", "Test Property Set");
                wms.setAbstract("${TEST_SYS_PROPERTY}");
                getGeoServer().save(wms);
                
                Document dom = getAsDOM("wms?request=GetCapabilities");
                //print(dom);
                
                // basic check on xpath node
                Element e = dom.getDocumentElement();
                assertEquals("WMS_Capabilities", e.getLocalName());

                // init xmlunit
                Map<String, String> namespaces = new HashMap<String, String>();
                namespaces.put("wms", "http://www.opengis.net/wms");
                namespaces.put("ows", "http://www.opengis.net/ows/1.1");
                namespaces.put("gml", "http://www.opengis.net/gml");
                namespaces.put("xlink", "http://www.w3.org/1999/xlink");
                XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));
                XpathEngine xpath =  XMLUnit.newXpathEngine();
                assertEquals("1", xpath.evaluate("count(//wms:Service/wms:Abstract)", dom));
                assertEquals("Test Property Set", xpath.evaluate("//wms:Service/wms:Abstract", dom));
            }
        } finally {
            wms.setCiteCompliant(false);
            wms.setAbstract(null);
            getGeoServer().save(wms);
        }        
    }
}
