/* Copyright (c) 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package com.boundlessgeo.gsr.model.feature.resource;

import com.boundlessgeo.gsr.api.feature.LayerResource;
import com.boundlessgeo.gsr.resource.ResourceTest;
import org.junit.Test;

import net.sf.json.JSON;
import net.sf.json.JSONObject;
import static org.junit.Assert.*;

/**
 * Test FeatureService @link {@link LayerResource} access.
 * @author Jody Garnett (Boundless)
 */
public class LayerResourceTest extends ResourceTest {
	
    private final String query(String service, String layer, String params) {
        return baseURL + service + "/FeatureServer/" + layer + params;
    }
    
    @Test
    public void testBasicQuery() throws Exception {
        String q = query("cite", "1", "?f=json");
        JSON result = getAsJSON(q);
        assertTrue(String.valueOf(result) + " is a JSON object", result instanceof JSONObject);
        
        JSONObject json = (JSONObject) result;
        
        Object type = json.get("type");
        
        assertEquals( "Feature Layer", type );
    }
}
