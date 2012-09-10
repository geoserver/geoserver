package org.geoserver.wfs.json;

import javax.xml.namespace.QName;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.wfs.WFSTestSupport;

public class GeoJsonDescribeTest extends WFSTestSupport {

    public void testDescribePrimitiveGeoFeatureJSON() throws Exception {
        String output = getAsString("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="+JSONType.json+"&typeName="
                + getLayerId(MockData.PRIMITIVEGEOFEATURE));
        testOutput(output);
    }

    private void testOutput(String output) {
        JSONArray array = JSONArray.fromObject(output);
        // print(array);

        assertEquals(1, array.size());
        JSONObject description = array.getJSONObject(0);
        basicSchemaChecks(description, MockData.PRIMITIVEGEOFEATURE);

        // it's a Polygon
        JSONObject topProperties = description.getJSONObject("properties");
        assertEquals("Polygon", topProperties.getJSONObject("geometry").get("type"));

        // check some of the non geometric properties
        JSONObject props = topProperties.getJSONObject("properties").getJSONObject("properties");

        // description
        assertEquals("string", props.getJSONObject("description").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("description").get("minimum"));

        // point property (second geometry)
        assertEquals("Point", props.getJSONObject("pointProperty").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("pointProperty").get("minimum"));

        // int property
        assertEquals("integer", props.getJSONObject("intProperty").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("intProperty").get("minimum"));

        // date time
        assertEquals("string", props.getJSONObject("dateTimeProperty").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("dateTimeProperty").get("minimum"));
        assertEquals("date-time", props.getJSONObject("dateTimeProperty").get("format"));

        // date
        assertEquals("string", props.getJSONObject("dateProperty").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("dateProperty").get("minimum"));
        assertEquals("date", props.getJSONObject("dateProperty").get("format"));

        // boolean
        assertEquals("boolean", props.getJSONObject("booleanProperty").get("type"));
        assertEquals(new Integer(0), props.getJSONObject("dateProperty").get("minimum"));
    }

    public void testDescribePrimitiveGeoFeatureJSONP() throws Exception {
        String output = getAsString("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="+JSONType.jsonp+"&typeName="
                + getLayerId(MockData.PRIMITIVEGEOFEATURE));

        // removing specific parts
        output = output.substring(0, output.length() - 2);
        output = output.substring(JSONType.CALLBACK_FUNCTION.length() + 1,
                output.length());
        testOutput(output);
    }
    
    public void testDescribePrimitiveGeoFeatureJSONPCustom() throws Exception {
        String output = getAsString("wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="+JSONType.jsonp+"&typeName="
                + getLayerId(MockData.PRIMITIVEGEOFEATURE) + "&format_options=callback:custom");

        // removing specific parts
        assertTrue(output.startsWith("custom("));
        output = output.substring(0, output.length() - 2);
        output = output.substring("custom".length() + 1,
                output.length());
        testOutput(output);
    }

    private void basicSchemaChecks(JSONObject description, QName name) {
        assertEquals(getLayerId(name), description.get("name"));
        assertEquals("object", description.get("type"));
        assertEquals("Feature", description.get("extends"));
        assertNotNull(description.getJSONObject("properties").get("geometry"));
        assertNotNull(description.get("properties"));
    }
}
