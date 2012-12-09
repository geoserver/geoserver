/*
 * Copyright (c) 2001 - 2010 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.util.IOUtils;
import org.geoserver.wfs.WFSInfo;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * 
 * @author carlo cancellieri - GeoSolutions
 *
 */
public class GeoJSONTest extends WFSTestSupport {
       
    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        File security = new File(getTestData().getDataDirectoryRoot(), "security");
        security.mkdir();
        File layers = new File(security, "layers.properties");
        IOUtils.copy(GeoJSONTest.class.getResourceAsStream("layers_ro.properties"), layers);
    }
	
    @Test
    public void testFeatureBoundingDisabledCollection() throws Exception {
    	/* In GML we have the option not to compute the bounds in the response, 
    	 * and by default we don't, but GeoServer can be configured to return 
    	 * the bounds, in that case it will issue a bounds query against the store, 
    	 * which might take a long time (that depends a lot on what the store can do, 
    	 * some can compute it quickly, no idea what SDE).
    	 * For GeoJSON it seems that the "feature bounding" flag is respected 
    	 * for the single feature bounds, but not for the collection.
    	 * Looking at the spec ( http://geojson.org/geojson-spec.html ) it seems to 
    	 * me the collection bbox is not required:
    	 * "To include information on the coordinate range for geometries, features, 
    	 * or feature collections, a GeoJSON object may have a member named "bbox""
    	 * disable Feature bounding */
        
    	GeoServer gs = getGeoServer();
        
        WFSInfo wfs = getWFS();
        boolean before = wfs.isFeatureBounding();
        wfs.setFeatureBounding(false);
        try {
            gs.save( wfs );
             
        	String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:AggregateGeoFeature&maxfeatures=3&outputformat="+JSONType.json);
        	JSONObject rootObject = JSONObject.fromObject( out );
         	
        	JSONObject bbox = rootObject.getJSONObject("bbox");
        	assertEquals(JSONNull.getInstance(), bbox);
        } finally {
        	wfs.setFeatureBounding(before);
            gs.save( wfs );
        }
    	
    }
    
    @Test
    public void testGet() throws Exception {	
    	String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="+JSONType.json);
    	
    	JSONObject rootObject = JSONObject.fromObject( out );
    	assertEquals(rootObject.get("type"),"FeatureCollection");
    	JSONArray featureCol = rootObject.getJSONArray("features");
    	JSONObject aFeature = featureCol.getJSONObject(0);
    	assertEquals(aFeature.getString("geometry_name"),"surfaceProperty");
    }

    @Test
    public void testPost() throws Exception {
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "outputFormat=\""+JSONType.json+"\" "
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

    @Test
    public void testGeometryCollection() throws Exception {
    	String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:AggregateGeoFeature&maxfeatures=3&outputformat="+JSONType.json);
    	
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
    
    @Test
    public void testMixedCollection() throws Exception {
        String xml = "<wfs:GetFeature " + "service=\"WFS\" " + "outputFormat=\""+JSONType.json+"\" "
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

    @Test
    public void testCallbackFunction() throws Exception {
        JSONType.setJsonpEnabled(true);
        MockHttpServletResponse resp = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="
                + JSONType.jsonp + "&format_options=" + JSONType.CALLBACK_FUNCTION_KEY + ":myFunc");
        JSONType.setJsonpEnabled(false);
        String out = resp.getOutputStreamContent();

        assertEquals(JSONType.jsonp, resp.getContentType());
        assertTrue(out.startsWith("myFunc("));
        assertTrue(out.endsWith(")"));

        // extract the json and check it
        out = out.substring(7, out.length() - 1);
        JSONObject rootObject = JSONObject.fromObject(out);
        assertEquals(rootObject.get("type"), "FeatureCollection");
        JSONArray featureCol = rootObject.getJSONArray("features");
        JSONObject aFeature = featureCol.getJSONObject(0);
        assertEquals(aFeature.getString("geometry_name"), "surfaceProperty");
    }
}
