package org.geoserver.wfs.response;

import java.io.File;

import junit.framework.Test;
import junit.textui.TestRunner;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.util.IOUtils;
import org.geoserver.wfs.WFSTestSupport;

import com.mockrunner.mock.web.MockHttpServletResponse;

public class GeoJSONTest extends WFSTestSupport {
    
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new GeoJSONTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        File security = new File(getTestData().getDataDirectoryRoot(), "security");
        security.mkdir();
        File layers = new File(security, "layers.properties");
        IOUtils.copy(GeoJSONTest.class.getResourceAsStream("layers_ro.properties"), layers);
    }
	
    public void testGet() throws Exception {	
    	String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat=json");
    	
    	JSONObject rootObject = JSONObject.fromObject( out );
    	assertEquals(rootObject.get("type"),"FeatureCollection");
    	JSONArray featureCol = rootObject.getJSONArray("features");
    	JSONObject aFeature = featureCol.getJSONObject(0);
    	assertEquals(aFeature.getString("geometry_name"),"surfaceProperty");
    }

    public void testPost() throws Exception {
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "outputFormat=\"json\" "
                + "version=\"1.0.0\" "
                + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
                + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
                + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
                + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\"> "
                + "</wfs:Query> " + "</wfs:GetFeature>";

        String out = postAsServletResponse( "wfs", xml ).getOutputStreamContent();
    	
    	JSONObject rootObject = JSONObject.fromObject( out );
    	assertEquals(rootObject.get("type"),"FeatureCollection");
    	JSONArray featureCol = rootObject.getJSONArray("features");
    	JSONObject aFeature = featureCol.getJSONObject(0);
    	assertEquals(aFeature.getString("geometry_name"),"surfaceProperty");
    }

    public void testGeometryCollection() throws Exception {
    	String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:AggregateGeoFeature&maxfeatures=3&outputformat=json");
    	
    	JSONObject rootObject = JSONObject.fromObject( out );
    	assertEquals(rootObject.get("type"),"FeatureCollection");
    	JSONArray featureCol = rootObject.getJSONArray("features");
    	JSONObject aFeature = featureCol.getJSONObject(1);
    	JSONObject aGeometry = aFeature.getJSONObject("geometry");
    	assertEquals(aGeometry.getString("type"),"MultiLineString");
    	JSONArray geomArray = aGeometry.getJSONArray("coordinates");
    	geomArray = geomArray.getJSONArray(0);
    	geomArray = geomArray.getJSONArray(0);
    	assertEquals(geomArray.getString(0), "55.174");
    }
    
    public void testMixedCollection() throws Exception {
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "outputFormat=\"json\" "
        + "version=\"1.0.0\" "
        + "xmlns:cdf=\"http://www.opengis.net/cite/data\" "
        + "xmlns:ogc=\"http://www.opengis.net/ogc\" "
        + "xmlns:wfs=\"http://www.opengis.net/wfs\" " + "> "
        + "<wfs:Query typeName=\"sf:PrimitiveGeoFeature\" /> "
        + "<wfs:Query typeName=\"sf:AggregateGeoFeature\" /> "
        + "</wfs:GetFeature>";
        //System.out.println("\n" + xml + "\n");
        
        String out  = postAsServletResponse( "wfs", xml).getOutputStreamContent();

        JSONObject rootObject = JSONObject.fromObject( out );
        //System.out.println(rootObject.get("type"));
        assertEquals(rootObject.get("type"),"FeatureCollection");
        
        JSONArray featureCol = rootObject.getJSONArray("features");
        
        // Check that there are at least two different types of features in here
        JSONObject aFeature = featureCol.getJSONObject(1);
        //System.out.println(aFeature.getString("id").substring(0,19));
        assertTrue(aFeature.getString("id").substring(0,19).equalsIgnoreCase("PrimitiveGeoFeature"));          
        aFeature = featureCol.getJSONObject(6);
        //System.out.println(aFeature.getString("id").substring(0,19));
        assertTrue(aFeature.getString("id").substring(0,19).equalsIgnoreCase("AggregateGeoFeature"));
               
        // Check that a feature has the expected attributes
        JSONObject aGeometry = aFeature.getJSONObject("geometry");
        //System.out.println(aGeometry.getString("type"));
        assertEquals(aGeometry.getString("type"),"MultiLineString");
    }
    
    public void testCallbackFunction() throws Exception {    
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat=json&format_options=callback:myFunc");
        String out = resp.getOutputStreamContent();

        assertEquals("text/javascript", resp.getContentType());
        assertTrue(out.startsWith("myFunc("));
        assertTrue(out.endsWith(")"));

        // extract the json and check it
        out = out.substring(7, out.length() - 1);
        JSONObject rootObject = JSONObject.fromObject( out );
        assertEquals(rootObject.get("type"),"FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"),"surfaceProperty");
    }
    
    public static void main(String[] args) {
        TestRunner runner = new TestRunner();
        runner.run(GeoJSONTest.class);
    }
}
