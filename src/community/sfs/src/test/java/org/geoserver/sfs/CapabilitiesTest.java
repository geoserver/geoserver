package org.geoserver.sfs;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.apache.commons.jxpath.JXPathContext;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class CapabilitiesTest extends GeoServerTestSupport {
    
    protected String root() {
        return "rest/sfs/";
    }
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        // just add one as the others will make reprojection trip over assertions during tests
        dataDirectory.addWellKnownType(MockData.PRIMITIVEGEOFEATURE, null);
        // add coverages to make sure they are going to be skipped
        dataDirectory.addWcs11Coverages();
    }
    
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
        assertEquals(-180.0, bbox.getDouble(0));
        assertEquals(-90.0, bbox.getDouble(1));
        assertEquals(180.0, bbox.getDouble(2));
        assertEquals(90.0, bbox.getDouble(3));
    }
}
