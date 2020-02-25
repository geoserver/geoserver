/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.wfs.json;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wfs.WFSTestSupport;
import org.junit.Test;

/** @author carlo cancellieri - GeoSolutions */
public class GeoJsonDescribeTest extends WFSTestSupport {

    @Test
    public void testDescribePrimitiveGeoFeatureJSON() throws Exception {
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.json
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        testOutput(output);
    }

    private void testOutput(String output) {
        JSONObject description = JSONObject.fromObject(output);
        assertEquals(description.get("elementFormDefault"), "qualified");
        assertEquals(description.get("targetNamespace"), "http://cite.opengeospatial.org/gmlsf");
        assertEquals(description.get("targetPrefix"), "sf");
        JSONArray array = description.getJSONArray("featureTypes");
        // print(array);

        assertEquals(1, array.size());
        JSONObject feature = array.getJSONObject(0);

        assertEquals(feature.get("typeName"), "PrimitiveGeoFeature");

        JSONArray props = feature.getJSONArray("properties");
        assertNotNull(props);

        // description
        int i = 0;
        assertEquals("description", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // point property (second geometry)
        assertEquals("name", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // surfaceProperty property
        assertEquals("surfaceProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("gml:Polygon", props.getJSONObject(i).get("type"));
        assertEquals("Polygon", props.getJSONObject(i).get("localType"));

        ++i;
        // point property (second geometry)
        assertEquals("pointProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:Point", props.getJSONObject(i).get("type"));
        assertEquals("Point", props.getJSONObject(i).get("localType"));

        ++i;
        // curve property (second geometry)
        assertEquals("curveProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:LineString", props.getJSONObject(i).get("type"));
        assertEquals("LineString", props.getJSONObject(i).get("localType"));

        ++i;
        // int property
        assertEquals("intProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:int", props.getJSONObject(i).get("type"));
        assertEquals("int", props.getJSONObject(i).get("localType"));

        ++i;
        // Uri property
        assertEquals("uriProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // measurand property
        assertEquals("measurand", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:string", props.getJSONObject(i).get("type"));
        assertEquals("string", props.getJSONObject(i).get("localType"));

        ++i;
        // dateProperty time
        assertEquals("dateTimeProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:date-time", props.getJSONObject(i).get("type"));
        assertEquals("date-time", props.getJSONObject(i).get("localType"));

        ++i;
        // dateProperty time
        assertEquals("dateProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:date", props.getJSONObject(i).get("type"));
        assertEquals("date", props.getJSONObject(i).get("localType"));

        ++i;
        // boolean
        assertEquals("decimalProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:number", props.getJSONObject(i).get("type"));
        assertEquals("number", props.getJSONObject(i).get("localType"));

        ++i;
        // boolean
        assertEquals("booleanProperty", props.getJSONObject(i).get("name"));
        assertEquals(Integer.valueOf(0), props.getJSONObject(i).get("minOccurs"));
        assertEquals(Integer.valueOf(1), props.getJSONObject(i).get("maxOccurs"));
        assertEquals(true, props.getJSONObject(i).get("nillable"));
        assertEquals("xsd:boolean", props.getJSONObject(i).get("type"));
        assertEquals("boolean", props.getJSONObject(i).get("localType"));
    }

    @Test
    public void testDescribePrimitiveGeoFeatureJSONP() throws Exception {
        JSONType.setJsonpEnabled(true);
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.jsonp
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE));
        JSONType.setJsonpEnabled(false);
        // removing specific parts
        output = output.substring(0, output.length() - 2);
        output = output.substring(JSONType.CALLBACK_FUNCTION.length() + 1, output.length());
        testOutput(output);
    }

    @Test
    public void testDescribePrimitiveGeoFeatureJSONPCustom() throws Exception {
        JSONType.setJsonpEnabled(true);
        String output =
                getAsString(
                        "wfs?service=WFS&request=DescribeFeatureType&version=1.0.0&outputFormat="
                                + JSONType.jsonp
                                + "&typeName="
                                + getLayerId(SystemTestData.PRIMITIVEGEOFEATURE)
                                + "&format_options="
                                + JSONType.CALLBACK_FUNCTION_KEY
                                + ":custom");
        JSONType.setJsonpEnabled(false);
        // removing specific parts
        assertTrue(output.startsWith("custom("));
        output = output.substring(0, output.length() - 2);
        output = output.substring("custom".length() + 1, output.length());
        testOutput(output);
    }
}
