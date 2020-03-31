package com.boundlessgeo.gsr;

import com.boundlessgeo.gsr.controller.ControllerTest;
import net.sf.json.JSON;
import net.sf.json.JSONObject;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertFalse;

public class QGISIntegrationTest extends ControllerTest {

    @Test
    public void testListFeatureLayers() throws Exception {
        //Get root
        JSONObject result = (JSONObject) getAsJSON(getBaseURL() + "cite/FeatureServer?f=json");
        assertFalse(result.has("error"));
        List<String> ids = new ArrayList<>();

        for (Object obj : result.getJSONArray("layers")) {
            JSONObject layer = (JSONObject) obj;
            ids.add(layer.getString("id"));
        }
        //Get each layer by id
        for (String id : ids) {
            result =  (JSONObject) getAsJSON(getBaseURL() + "cite/FeatureServer/"+id+"?f=json");
            assertFalse(result.has("error"));
            assertFalse(result.toString().isEmpty());
        }
    }
    @Test
    public void testGetFeatureLayer() throws Exception {
        //get layer by id (cite:Buildings)
        JSONObject result = (JSONObject) getAsJSON(getBaseURL() + "cite/FeatureServer/2?f=json");
        assertFalse(result.has("error"));
        assertFalse(result.toString().isEmpty());
        //get ids
        String idField = null;
        List<String> outFields = new ArrayList<>();
        String outFieldString = "";
        for (Object obj : result.getJSONArray("fields")) {
            JSONObject field = (JSONObject) obj;

            outFields.add(field.getString("name"));
            outFieldString = outFieldString+","+field.getString("name");
            if ("esriFieldTypeOID".equals(field.getString("type")) && idField == null) {
                idField = field.getString("name");
            }
        }
        outFieldString = outFieldString.substring(1);
        if (idField == null) {
            idField = result.getString("objectIdField");
        }
        result = (JSONObject) getAsJSON(getBaseURL() + "cite/FeatureServer/2/query?f=json&where="+idField+"%3D"+idField+"&returnIdsOnly=true");

        //Every 100 ids, get all features
        List<Long> ids = new ArrayList<>();
        String idString = "";
        for (Object obj : result.getJSONArray("objectIds")) {
            ids.add((Long) obj);
            idString = idString+","+obj.toString();
        }
        idString = idString.substring(1);

        result = (JSONObject) getAsJSON(getBaseURL() + "cite/FeatureServer/2/query?f=json&objectIds=" + idString
                + "&outFields=" + outFieldString + "&inSR=4326&outSR=4326&returnGeometry=true"
                + "&returnM=false&returnZ=false&geometry=-85.944465,37.997286,-85.442908,38.378011"
                + "&geometryType=esriGeometryEnvelope&spatialRel=esriSpatialRelEnvelopeIntersects");

        assertFalse(result.has("error"));
        assertFalse(result.toString().isEmpty());
    }

}
