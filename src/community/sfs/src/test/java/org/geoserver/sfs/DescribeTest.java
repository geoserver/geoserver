package org.geoserver.sfs;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.restlet.data.MediaType;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class DescribeTest extends GeoServerTestSupport {
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
    }
    
    protected String root() {
        return "rest/sfs/";
    }
    
    public void testDescribePrimitiveGeoFeature() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "describe/sf:PrimitiveGeoFeature");
        assertEquals(200, response.getErrorCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONArray json = (JSONArray) json(response);
        // print(json);
        
        // check we have just one item in the array
        assertEquals(1, json.size());
        
        // extract the one item containing the description
        JSONObject description = json.getJSONObject(0);
        
        // string
        assertEquals("string", description.get("description"));
        // this one is interesting, as it was an URI
        assertEquals("string", description.get("uriProperty"));
        // geometry
        assertEquals("Polygon", description.get("surfaceProperty"));
        assertEquals("Point", description.get("pointProperty"));
        assertEquals("LineString", description.get("curveProperty"));
        // numbers
        assertEquals("number", description.get("intProperty"));
        assertEquals("number", description.get("decimalProperty"));
        // those two too, they are dates and timestamps
        assertEquals("timestamp", description.get("dateTimeProperty"));
        assertEquals("timestamp", description.get("dateProperty"));
        // boolean
        assertEquals("boolean", description.get("booleanProperty"));
    }
    
    public void testDescribeMissingLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "describe/abc:notThere");
        assertEquals(404, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
        assertEquals("No such layer: abc:notThere", response.getOutputStreamContent());
    }
    
    public void testDescribeCoverage() throws Exception {
        final String tasmania = getLayerId(MockData.TASMANIA_BM);
        MockHttpServletResponse response = getAsServletResponse(root() + "describe/" + tasmania);
        assertEquals(404, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
        assertEquals("No such layer: " + tasmania, response.getOutputStreamContent());
    }
}
