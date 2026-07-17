/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.kvp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.custommonkey.xmlunit.XMLAssert;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;
import org.w3c.dom.Document;

/** Integration Test for WPS KVP with BoundBox */
public class ExecuteKVPIntegrationTest extends WPSTestSupport {

    @Test
    public void testExecuteKvpWithBoundingBoxInput() throws Exception {
        String dataInputs = "bounds=-105.36,39.82,-105.16,39.96,EPSG:4326@datatype=wps:BoundingBoxData";

        String path = "wps?service=WPS"
                + "&version=1.0.0"
                + "&request=Execute"
                + "&identifier=gs:Bbox"
                + "&datainputs=" + dataInputs;

        MockHttpServletResponse response = getAsServletResponse(path);
        assertEquals(200, response.getStatus());

        Document dom = getAsDOM(path);
        assertNotNull(dom);

        // Verify no exceptions when parsing bounds
        XMLAssert.assertXpathNotExists("//ows:ExceptionReport", dom);
        XMLAssert.assertXpathExists("//wps:ExecuteResponse", dom);
    }
}
