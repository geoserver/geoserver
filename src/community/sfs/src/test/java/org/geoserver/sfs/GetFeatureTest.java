package org.geoserver.sfs;

import java.io.IOException;
import java.io.StringWriter;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.geojson.GeoJSON;
import org.restlet.data.MediaType;

import com.mockrunner.mock.web.MockHttpServletResponse;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;

public class GetFeatureTest extends GeoServerTestSupport {
    
    @Override
    protected void populateDataDirectory(MockData dataDirectory) throws Exception {
        super.populateDataDirectory(dataDirectory);
        dataDirectory.addWcs11Coverages();
    }
    
    protected String root() {
        return "rest/sfs/";
    }
    
    public void testGetMissingLayer() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/abc:notThere");
        assertEquals(404, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
        assertEquals("No such layer: abc:notThere", response.getOutputStreamContent());
    }
    
    public void testGetCoverage() throws Exception {
        final String tasmania = getLayerId(MockData.TASMANIA_BM);
        MockHttpServletResponse response = getAsServletResponse(root() + "data/" + tasmania);
        assertEquals(404, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
        assertEquals("No such layer: " + tasmania, response.getOutputStreamContent());
    }
    
    public void testGetAll() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/sf:PrimitiveGeoFeature");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject features = (JSONObject) json(response);
        // print(features);
        assertEquals("FeatureCollection", features.getString("type"));
        assertEquals(5 , features.getJSONArray("features").size());
    }
    
    public void testGetAllCount() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/sf:PrimitiveGeoFeature?mode=count");
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getContentType().startsWith(MediaType.TEXT_PLAIN.getName()));
        
        assertEquals("5", response.getOutputStreamContent());
    }
    
    public void testGetAllBounds() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/sf:PrimitiveGeoFeature?mode=bounds");
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getContentType().startsWith(MediaType.APPLICATION_JSON.getName()));
        
        JSONArray bbox = (JSONArray) json(response);
        // print(bbox);
        final double EPS = 0.0001;
        assertEquals(34.94, bbox.getDouble(0), EPS);
        assertEquals(-10.52, bbox.getDouble(1), EPS);
        assertEquals(59.41276, bbox.getDouble(2), EPS);
        assertEquals(30.899, bbox.getDouble(3), EPS);
    }
    
    public void testEqualityFilter() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?queryable=FID&FID__eq=113");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        testFirstBuilding(collection);
    }
    
    public void testRestrictAttributes() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?queryable=FID&FID__eq=113&attrs=FID");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        // print(collection);
        assertEquals("FeatureCollection", collection.getString("type"));
        final JSONArray features = collection.getJSONArray("features");
        assertEquals(1 , features.size());
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals("Feature", feature.getString("type"));
        assertTrue(feature.has("geometry"));
        final JSONObject properties = feature.getJSONObject("properties");
        assertEquals("113", properties.getString("FID"));
        assertFalse(properties.has("ADDRESS"));
    }
    
    public void testNoGeometry() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?queryable=FID&FID__eq=113&no_geom=true");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        // print(collection);
        assertEquals("FeatureCollection", collection.getString("type"));
        final JSONArray features = collection.getJSONArray("features");
        assertEquals(1 , features.size());
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals("Feature", feature.getString("type"));
        assertFalse(feature.has("geometry"));
        final JSONObject properties = feature.getJSONObject("properties");
        assertEquals("113", properties.getString("FID"));
        assertEquals("123 Main Street", properties.getString("ADDRESS"));
    }
    
    public void testILikeFilter() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?queryable=ADDRESS&ADDRESS__ilike=123%20m%25");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        // print(collection);
        testFirstBuilding(collection);
    }

    private void testFirstBuilding(JSONObject collection) {
        assertEquals("FeatureCollection", collection.getString("type"));
        final JSONArray features = collection.getJSONArray("features");
        assertEquals(1 , features.size());
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals("Feature", feature.getString("type"));
        assertEquals("113", feature.getJSONObject("properties").getString("FID"));
    }
    
    public void testLimit() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?limit=1");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        testFirstBuilding(collection);
    }
    
    public void testLimitOffset() throws Exception {
        // property data store does not support offset        
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?limit=1&offset=2");
        assertEquals(500, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
    }
    
    public void testSort() throws Exception {
        // property data store does not support sorting        
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?order_by=ADDRESS");
        assertEquals(500, response.getStatusCode());
        assertEquals(MediaType.TEXT_PLAIN.getName(), response.getContentType());
    }
    
    public void testPointFilter() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?lon=0.0008&lat=0.0005&tolerance=0.0001");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        testFirstBuilding(collection);
    }
    
    public void testPointFilterLarge() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?lon=0.0008&lat=0.0005&tolerance=0.01");
        assertEquals(200, response.getStatusCode());
        assertEquals(MediaType.APPLICATION_JSON.getName(), response.getContentType());
        
        JSONObject collection = (JSONObject) json(response);
        // print(collection);
        testAllBuildings(collection);
    }

    private void testAllBuildings(JSONObject collection) {
        assertEquals("FeatureCollection", collection.getString("type"));
        final JSONArray features = collection.getJSONArray("features");
        assertEquals(2 , features.size());
        JSONObject feature = (JSONObject) features.get(0);
        assertEquals("Feature", feature.getString("type"));
        assertEquals("113", feature.getJSONObject("properties").getString("FID"));
        feature = (JSONObject) features.get(1);
        assertEquals("Feature", feature.getString("type"));
        assertEquals("114", feature.getJSONObject("properties").getString("FID"));
    }
    
    public void testBBoxFilter() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?bbox=0.0008,0.0005,0.00012,0.0007");
        JSONObject collection = (JSONObject) json(response);
        testFirstBuilding(collection);
    }
    
    public void testBBoxFilterTolerance() throws Exception {
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?bbox=0.0008,0.0005,0.00012,0.0007&tolerance=0.01");
        JSONObject collection = (JSONObject) json(response);
        testAllBuildings(collection);
    }
    
    public void testGeometryFilter() throws Exception {
        String jsonGeometry = buildJSONLineString();
        
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?geometry=" + jsonGeometry);
        JSONObject collection = (JSONObject) json(response);
        testFirstBuilding(collection);
    }
    
    public void testGeometryFilterTolerance() throws Exception {
        String jsonGeometry = buildJSONLineString();
        
        MockHttpServletResponse response = getAsServletResponse(root() + "data/cite:Buildings?geometry=" + jsonGeometry + "&tolerance=0.01");
        JSONObject collection = (JSONObject) json(response);
        testAllBuildings(collection);
    }

    private String buildJSONLineString() throws ParseException, IOException {
        WKTReader reader = new WKTReader();
        Geometry g = reader.read("LINESTRING(0.0008 0.0005,0.00012 0.0007)");
        final StringWriter sw = new StringWriter();
        GeoJSON.write(g, sw);
        String jsonGeometry = sw.getBuffer().toString();
        return jsonGeometry;
    }
}
