/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.resource;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import javax.xml.namespace.QName;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.catalog.DimensionInfo;
import org.geoserver.catalog.DimensionPresentation;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.DimensionInfoImpl;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.data.test.SystemTestData.LayerProperty;
import org.opengeo.gsr.JsonSchemaTest;
import org.junit.Test;
import org.junit.Before;

import static org.junit.Assert.*;

public class QueryResourceTimeTest extends ResourceTest {
    public static final QName TIME_ELEVATION = 
            new QName(MockData.CITE_URI, "TimeElevation", MockData.CITE_PREFIX);


    @Override
    @SuppressWarnings("rawtypes")
    public void onSetUp(SystemTestData testData) {
        try {
            Map<LayerProperty, Object> props = Collections.emptyMap();
            testData.addVectorLayer(TIME_ELEVATION, props, "TimeElevation.properties", QueryResourceTimeTest.class, getCatalog());

            FeatureTypeInfo info = getCatalog().getFeatureTypeByName(
                    TIME_ELEVATION.getNamespaceURI(),
                    TIME_ELEVATION.getLocalPart());
            DimensionInfo di = new DimensionInfoImpl();
            
            di.setEnabled(true);
            di.setAttribute("time");
            di.setPresentation(DimensionPresentation.CONTINUOUS_INTERVAL);
            info.getMetadata().put(ResourceInfo.TIME, di);
            getCatalog().save(info);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    
    @Test
    public void testRootResource() throws Exception {
        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(TIME_ELEVATION.getNamespaceURI(), TIME_ELEVATION.getLocalPart());
        assertNotNull(typeInfo);
        DimensionInfo dimensionInfo = typeInfo.getMetadata().get(ResourceInfo.TIME, DimensionInfo.class);
        assertNotNull(dimensionInfo);
        assertTrue(dimensionInfo.isEnabled());
        assertEquals("time", dimensionInfo.getAttribute());
        assertNotNull(getCatalog().getLayerByName(TIME_ELEVATION.getLocalPart()));
        String rootResource = getAsString(baseURL + "cite/MapServer?f=json");
        assertTrue(JsonSchemaTest.validateJSON(rootResource, "gsr-ms/1.0/root.json"));
        JSONObject json = JSONObject.fromObject(rootResource);
        assertTrue(json.containsKey("timeInfo"));
    }
    
    @Test
    public void testTimeQuery() throws Exception {
        String query = 
                baseURL +
                "cite" +
                "/MapServer/" +
                "12" +
                "/query" + 
                "?f=json&geometryType=esriGeometryEnvelope&geometry=-180,-90,180,90";
        String result;
        result = getAsString(query + "&time=1304294400000"); // 2011-05-02Z in milliseconds since UNIX epoch
        assertNFeatures(result, 1);
        
        result = getAsString(query + "&time=NULL,1304294400000"); // 2011-05-02Z in milliseconds since UNIX epoch
        assertNFeatures(result, 1);
        
        result = getAsString(query + "&time=1304294400000,NULL"); // 2011-05-02Z in milliseconds since UNIX epoch
        assertNFeatures(result, 2);
    }
    
    private static void assertNFeatures(String jsonFeatureCollection, int n) {
        JSONObject json = JSONObject.fromObject(jsonFeatureCollection);
        assertTrue(jsonFeatureCollection + " should contain features", json.containsKey("features"));
        assertTrue(jsonFeatureCollection + " should have an array in the features field", json.get("features") instanceof JSONArray);
        assertTrue(jsonFeatureCollection + " should have " + n + " features", json.getJSONArray("features").size() == n);
    }
}
