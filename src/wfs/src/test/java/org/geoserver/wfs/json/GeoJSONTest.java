/*
 * Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.Collections;

import javax.xml.namespace.QName;

import net.sf.json.JSON;
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
       
    public static QName LINE3D = new QName(SystemTestData.CITE_URI, "Line3D", SystemTestData.CITE_PREFIX);
    
    @Override
    protected void setUpInternal(SystemTestData data) throws Exception {
        File security = new File(getTestData().getDataDirectoryRoot(), "security");
        security.mkdir();
        File layers = new File(security, "layers.properties");
        IOUtils.copy(GeoJSONTest.class.getResourceAsStream("layers_ro.properties"), layers);
        data.addVectorLayer (LINE3D, Collections.EMPTY_MAP, getClass(), getCatalog());
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
        MockHttpServletResponse response = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="+JSONType.json);
        assertEquals("application/json", response.getContentType());
        String out = response.getOutputStreamContent();

    	
    	JSONObject rootObject = JSONObject.fromObject( out );
    	assertEquals(rootObject.get("type"),"FeatureCollection");
    	JSONArray featureCol = rootObject.getJSONArray("features");
    	JSONObject aFeature = featureCol.getJSONObject(0);
    	assertEquals(aFeature.getString("geometry_name"),"surfaceProperty");
    }
    
    @Test
    public void testGetSimpleJson() throws Exception {    
        MockHttpServletResponse response = getAsServletResponse("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="+JSONType.simple_json);
        assertEquals("application/json", response.getContentType());
        String out = response.getOutputStreamContent();
        
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
    
    @Test
    public void testGetFeatureCount() throws Exception {        
        //request without filter
        String out = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=10&outputformat="+JSONType.json);
        JSONObject rootObject = JSONObject.fromObject( out );
        assertEquals(rootObject.get("totalFeatures"),5);

        //request with filter (featureid=PrimitiveGeoFeature.f001)
        String out2 = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=10&outputformat="+JSONType.json+"&featureid=PrimitiveGeoFeature.f001");
        JSONObject rootObject2 = JSONObject.fromObject( out2 );
        assertEquals(rootObject2.get("totalFeatures"),1);
        
        //check if maxFeatures doesn't affect totalFeatureCount; set Filter and maxFeatures
        String out3 = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="+JSONType.json+"&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002");
        JSONObject rootObject3 = JSONObject.fromObject( out3 );
        assertEquals(rootObject3.get("totalFeatures"),2);
        
        //request with multiple featureTypes and Filter
        String out4 = getAsString("wfs?request=GetFeature&version=1.0.0&typename=sf:PrimitiveGeoFeature,sf:AggregateGeoFeature&outputformat="+JSONType.json + "&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002,AggregateGeoFeature.f009");
        JSONObject rootObject4 = JSONObject.fromObject( out4 );
        assertEquals(rootObject4.get("totalFeatures"),3);
        
    }

    @Test
    public void testGetFeatureCountWfs20() throws Exception {        
        //request without filter
        String out = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=10&outputformat="+JSONType.json);
        JSONObject rootObject = JSONObject.fromObject( out );
        assertEquals(rootObject.get("totalFeatures"),5);

        //request with filter (featureid=PrimitiveGeoFeature.f001)
        String out2 = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=10&outputformat="+JSONType.json+"&featureid=PrimitiveGeoFeature.f001");
        JSONObject rootObject2 = JSONObject.fromObject( out2 );
        assertEquals(rootObject2.get("totalFeatures"),1);
        
        //check if maxFeatures doesn't affect totalFeatureCount; set Filter and maxFeatures
        String out3 = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature&maxfeatures=1&outputformat="+JSONType.json+"&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002");
        JSONObject rootObject3 = JSONObject.fromObject( out3 );
        assertEquals(rootObject3.get("totalFeatures"),2);
        
        //request with multiple featureTypes and Filter
        String out4 = getAsString("wfs?request=GetFeature&version=2.0.0&typename=sf:PrimitiveGeoFeature,sf:AggregateGeoFeature&outputformat="+JSONType.json + "&featureid=PrimitiveGeoFeature.f001,PrimitiveGeoFeature.f002,AggregateGeoFeature.f009");
        JSONObject rootObject4 = JSONObject.fromObject( out4 );
        assertEquals(rootObject4.get("totalFeatures"),3);
        
    }
 
    @Test
    public void testGetFeatureLine3D() throws Exception {
        JSONObject collection = (JSONObject) getAsJSON("wfs?request=GetFeature&version=1.0.0&typename=" + getLayerId(LINE3D)
                + "&outputformat=" + JSONType.json);
        // print(collection);
        assertEquals(1, collection.getInt("totalFeatures"));
        assertEquals("4327", collection.getJSONObject("crs").getJSONObject("properties").getString("code"));
        JSONArray features = collection.getJSONArray("features");
        assertEquals(1, features.size());
        JSONObject feature = features.getJSONObject(0);
        JSONObject geometry = feature.getJSONObject("geometry");
        assertEquals("LineString", geometry.getString("type"));
        JSONArray coords = geometry.getJSONArray("coordinates");
        JSONArray c1 = coords.getJSONArray(0);
        assertEquals(0, c1.getInt(0));
        assertEquals(0, c1.getInt(1));
        assertEquals(50, c1.getInt(2));
        JSONArray c2 = coords.getJSONArray(1);
        assertEquals(120, c2.getInt(0));
        assertEquals(0, c2.getInt(1));
        assertEquals(100, c2.getInt(2));
    }
}
