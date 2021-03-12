/* (c) 2020 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import org.junit.Test;

public class GeoJSONGetComplexFeaturesResponseWFSTest
        extends GeoJSONGetComplexFeaturesResponseTest {

    @Test
    public void testGeoJSONResponse() throws Exception {
        setUpMappedFeature("MappedFeatureGeoJSON.json");
        StringBuffer sb = new StringBuffer("wfs?request=GetFeature&version=2.0");
        sb.append("&TYPENAME=gsml:MappedFeature&outputFormat=");
        sb.append("application/json");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertEquals(5, features.size());
        for (int i = 0; i < features.size(); i++) {
            JSONObject feature = (JSONObject) features.get(i);
            checkMappedFeature(feature);
        }
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryWithGET() throws Exception {
        setUpMappedFeature("MappedFeatureGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application/json")
                        .append(
                                "&cql_filter=features.gsml:GeologicUnit.description = 'Olivine basalt'");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryPointingToExpr() throws Exception {
        setUpMappedFeature("MappedFeatureGeoJSON.json");
        StringBuilder sb =
                new StringBuilder("wfs?request=GetFeature&version=2.0")
                        .append("&TYPENAME=gsml:MappedFeature&outputFormat=")
                        .append("application/json")
                        .append("&cql_filter= features.name = 'FeatureName: MURRADUC BASALT'");
        JSONObject result = (JSONObject) getJson(sb.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        JSONObject feature = features.getJSONObject(0);
        assertEquals("FeatureName: MURRADUC BASALT", feature.getString("name"));
        checkMappedFeature(feature);
        checkAdditionalInfo(result);
    }

    @Test
    public void testGeoJSONQueryWithPOST() throws Exception {
        setUpMappedFeature("MappedFeatureGeoJSON.json");
        StringBuilder xml =
                new StringBuilder("<wfs:GetFeature ")
                        .append(" service=\"WFS\" ")
                        .append(" outputFormat=\"application/json\" ")
                        .append(" version=\"1.0.0\" ")
                        .append(" xmlns:gsml=\"urn:cgi:xmlns:CGI:GeoSciML:2.0\" ")
                        .append(" xmlns:wfs=\"http://www.opengis.net/wfs\" ")
                        .append(" xmlns:ogc=\"http://www.opengis.net/ogc\" ")
                        .append(">")
                        .append(" <wfs:Query typeName=\"gsml:MappedFeature\">")
                        .append(" <ogc:Filter><ogc:PropertyIsEqualTo> ")
                        .append(
                                "<ogc:PropertyName>features.gsml:GeologicUnit.description</ogc:PropertyName>")
                        .append("<ogc:Literal>Olivine basalt</ogc:Literal>")
                        .append("</ogc:PropertyIsEqualTo></ogc:Filter></wfs:Query>")
                        .append("</wfs:GetFeature>");
        JSONObject result = (JSONObject) postJson(xml.toString());
        JSONArray features = (JSONArray) result.get("features");
        assertTrue(features.size() == 1);
        assertEquals(((JSONObject) features.get(0)).get("@id").toString(), "mf4");
        checkMappedFeature(features.getJSONObject(0));
        checkAdditionalInfo(result);
    }
}
