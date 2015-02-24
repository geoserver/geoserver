/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.sfs;

import static org.junit.Assert.*;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.jxpath.JXPathContext;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CapabilitiesTest extends GeoServerSystemTestSupport {
    
    protected String root() {
        return "rest/sfs/";
    }

    @Override
    protected void setUpTestData(SystemTestData testData) throws Exception {
        testData.setUpSecurity();
       
        // just add one as the others will make reprojection trip over assertions during tests
        testData.setUpVectorLayer(MockData.PRIMITIVEGEOFEATURE);
        
        // add coverages to make sure they are going to be skipped
        testData.setUpWcs11RasterLayers();
    }

    @Test
    public void testBasicContents() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "capabilities");
        assertEquals(200, response.getErrorCode());
        
        JSONArray json = (JSONArray) json(response);
        // print(json);
        
        // check we have the right number of layers
        assertEquals(getCatalog().getFeatureTypes().size(), json.size());
        
        // extract and check one
        JXPathContext context = JXPathContext.newContext(json);
        JSONObject primitive = (JSONObject) context.getValue(".[name = 'sf:PrimitiveGeoFeature']");
        assertNotNull(primitive);
        assertEquals("urn:ogc:def:crs:EPSG:4326", primitive.get("crs"));
        assertEquals("xy", primitive.get("axisorder"));
        JSONArray bbox = (JSONArray) primitive.get("bbox");
        assertEquals(-180.0, bbox.getDouble(0), 1E-7);
        assertEquals(-90.0, bbox.getDouble(1), 1E-7);
        assertEquals(180.0, bbox.getDouble(2), 1E-7);
        assertEquals(90.0, bbox.getDouble(3), 1E-7);
    }
}
