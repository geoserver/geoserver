/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org.  All rights reserved.
 * This code is licensed under the GPL 2.0 license, availible at the root
 * application directory.
 */
package org.geoserver.wms.describelayer;

import junit.framework.Test;
import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.wfs.json.JSONType;
import org.geoserver.wms.WMSTestSupport;

/**
 * Unit test suite for {@link GeoJSONDescribeLayerResponse}
 * 
 * @author Carlo Cancellieri
 * @version $Id$
 */
public class DescribeLayerJsonTest extends WMSTestSupport {
	
    /**
     * This is a READ ONLY TEST so we can use one time setup
     */
    public static Test suite() {
        return new OneTimeTestSetup(new DescribeLayerJsonTest());
    }
    
    public void testBuild() throws Exception {
    	try {
	    	new GeoJSONDescribeLayerResponse(getWMS(), "fail");
	    	fail("Should fails");
    	} catch (Exception e) {
    	}
    }

    /**
     * Tests jsonp with custom callback function
     * 
     * @throws Exception
     */
    public void testCustomJSONP() throws Exception {
        String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?version=1.1.1"
                + "&request=DescribeLayer" +
                "&layers=" + layer +
                "&query_layers=" + layer + 
                "&width=20&height=20"+
                "&outputFormat=" +JSONType.jsonp+
                "&format_options="+JSONType.CALLBACK_FUNCTION_KEY+":DescribeLayer";
        
        String result = getAsString(request);

        checkJSONPDescribeLayer(result, layer);
    }
    

    /**
     * Tests JSON
     * 
     * @throws Exception
     */
    public void testSimpleJSON() throws Exception {
    	String layer = MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart();
        String request = "wms?version=1.1.1"
                + "&request=DescribeLayer" +
                "&layers=" + layer +
                "&query_layers=" + layer + 
                "&width=20&height=20"+
                "&outputFormat=" +JSONType.json;
        
        String result = getAsString(request);
        
        checkJSONDescribeLayer(result, layer);
    }
    
    /**
     * @param body Accepts:<br>
     *  	DescribeLayer(...)<br>
     * @param layer
     */
    private void checkJSONPDescribeLayer(String body, String layer){
        assertNotNull(body);

      assertTrue(body.startsWith("DescribeLayer("));
      assertTrue(body.endsWith(")\n"));
      body = body.substring(0, body.length() - 2);
      body = body.substring("DescribeLayer(".length(),
              body.length());
      
      checkJSONDescribeLayer(body, layer);
    }
    
    /**
     * Tests jsonp with custom callback function
     * 
     * @throws Exception
     */
    public void testJSONLayerGroup() throws Exception {
    	
        String layer = NATURE_GROUP;
        String request = "wms?version=1.1.1"
                + "&request=DescribeLayer" +
                "&layers=" + layer +
                "&query_layers=" + layer + 
                "&width=20&height=20"+
                "&outputFormat=" +JSONType.json;
        
        String result = getAsString(request);

        checkJSONDescribeLayerGroup(result, layer);
    }

    
    /**
     * @param body accepts:<br>
      {"WMS_DescribeLayerResponse": {<br>
      	  "version": "1.1.1",<br>
      	  "LayerDescription": {<br>
      	    "name": "cite:Forests",<br>
      	    "owsURL": "",<br>
      	    "owsType": "WFS"<br>
      	  }<br>
      	}}<br>
     */
    private void checkJSONDescribeLayer(String body, String layer){
        assertNotNull(body);

      JSONObject rootObject = JSONObject.fromObject(body);
      JSONObject subObject = rootObject.getJSONObject("WMS_DescribeLayerResponse");
      assertEquals(subObject.get("version"), "1.1.1");
      
      JSONObject layerDesc = subObject.getJSONObject("LayerDescription");
      
      assertEquals(layerDesc.get("name"), layer);
//      assertEquals(layerDesc.get("owsUrl"), "WFS");
      assertEquals(layerDesc.get("owsType"), "WFS");
    }


    /**
     * @param body accepts:<br>
      {"WMS_DescribeLayerResponse": {<br>
		  "version": "1.1.1",<br>
		  "LayerDescription": {<br>
		    "name": "cite:Lakes",<br>
		    "owsURL": "http://localhost:8080/geoserver/wfs/WfsDispatcher?",<br>
		    "owsType": "WFS"<br>
		  },<br>
		  "LayerDescription": {<br>
		    "name": "cite:Forests",<br>
		    "owsURL": "http://localhost:8080/geoserver/wfs/WfsDispatcher?",<br>
		    "owsType": "WFS"<br>
		  }<br>
		}}<br>
     */
	private void checkJSONDescribeLayerGroup(String body, String layer) {
		assertNotNull(body);

		JSONObject rootObject = JSONObject.fromObject(body);
		JSONObject subObject = rootObject
				.getJSONObject("WMS_DescribeLayerResponse");
		assertEquals(subObject.get("version"), "1.1.1");

		JSONArray layerDescs = subObject.getJSONArray("LayerDescription");
		JSONObject layerDesc = layerDescs.getJSONObject(0);
		assertEquals(layerDesc.get("name"), MockData.LAKES.getPrefix() + ":" + MockData.LAKES.getLocalPart());
		assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs/WfsDispatcher?"));
		assertEquals(layerDesc.get("owsType"), "WFS");
		
		layerDesc = layerDescs.getJSONObject(1);
		assertEquals(layerDesc.get("name"),  MockData.FORESTS.getPrefix() + ":" + MockData.FORESTS.getLocalPart());
		assertTrue(layerDesc.get("owsURL").toString().endsWith("geoserver/wfs/WfsDispatcher?"));
		assertEquals(layerDesc.get("owsType"), "WFS");
		
	}
}
