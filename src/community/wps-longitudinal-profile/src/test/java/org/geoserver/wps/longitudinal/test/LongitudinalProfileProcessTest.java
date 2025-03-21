/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal.test;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.junit.Test;
import org.w3c.dom.Document;

public class LongitudinalProfileProcessTest extends WPSTestSupport {
    private static final QName PROFILE = new QName(MockData.DEFAULT_URI, "dataProfile", MockData.DEFAULT_PREFIX);
    private static final QName ADJ_LAYER = new QName(MockData.DEFAULT_URI, "AdjustmentLayer", MockData.DEFAULT_PREFIX);

    private static final String LINESTRING_2154_WKT =
            "LINESTRING(843478.269971218 6420348.7621933, 843797.900998497 6420021.75658605, 844490.474212848 6420187.03857354, 844102.691178047 6420613.93854596)";
    private static final String LINESTRING_2154_EWKT = "SRID=2154;" + LINESTRING_2154_WKT;
    private static final String LINESTRING_4326_EWKT =
            "SRID=4326;LINESTRING(4.816667349546753 44.86746046117114, 4.820617515841021 44.86445081066109, 4.829431492334357 44.86579440463876, 4.82464829777395 44.869717699053616)";

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        String styleName = "raster";
        testData.addStyle(styleName, "raster.sld", MockData.class, getCatalog());

        Map<SystemTestData.LayerProperty, Object> props = new HashMap<>();
        props.put(SystemTestData.LayerProperty.STYLE, styleName);

        testData.addRasterLayer(PROFILE, "coverage.zip", null, Collections.emptyMap(), getCatalog());
        testData.addVectorLayer(
                ADJ_LAYER,
                Map.of(SystemTestData.LayerProperty.SRS, 2154),
                "AdjustmentLayer.properties",
                MockData.class,
                getCatalog());
    }

    private String loadTemplate(String templateName, Map<String, String> values) throws IOException {
        String template = new String(Files.readAllBytes(Paths.get("src/test/resources/" + templateName)));
        for (Map.Entry<String, String> entry : values.entrySet()) {
            template = template.replace("${" + entry.getKey() + "}", entry.getValue());
        }
        return template;
    }

    @Test
    public void testNoInputLayer() throws Exception {
        String requestXml =
                loadTemplate("templateNoInputLayer.xml", Map.of("GEOMETRY", LINESTRING_2154_WKT, "DISTANCE", "300"));

        Document d = postAsDOM(root(), requestXml);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());

        assertXpathExists("/wps:ExecuteResponse/wps:Status/wps:ProcessFailed", d);
        String msg = xp.evaluate(
                "/wps:ExecuteResponse/wps:Status/wps:ProcessFailed/ows:ExceptionReport/ows:Exception/ows:ExceptionText",
                d);
        assertThat(msg, containsString("Either layerName or coverage must be provided"));
    }

    @Test
    public void testBasicProfileLayer() throws Exception {
        String requestXml = loadTemplate(
                "templateBasic.xml",
                Map.of(
                        "LAYER_NAME", "dataProfile",
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "300"));

        checkBasicProfile(requestXml, "dataProfile");
    }

    @Test
    public void testBasicProfileCoverage() throws Exception {
        String requestXml = loadTemplate(
                "templateChaining.xml",
                Map.of(
                        "COVERAGE_ID", "gs__dataProfile",
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "300"));

        checkBasicProfile(requestXml, null);
    }

    private void checkBasicProfile(String requestXml, String expctedLayer) throws Exception {
        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(214.94, infos.get("altitudePositive"));
        assertEquals(-64.28, infos.get("altitudeNegative"));
        assertEquals(1746.0248, infos.get("totalDistance"));
        assertEquals(843478.25, infos.get("firstPointX"));
        assertEquals(6420349.0, infos.get("firstPointY"));
        assertEquals(844102.7, infos.get("lastPointX"));
        assertEquals(6420614.0, infos.get("lastPointY"));
        if (expctedLayer != null) {
            assertEquals(expctedLayer, infos.get("layer"));
        } else {
            assertEquals(JSONNull.getInstance(), infos.get("layer"));
        }
        assertEquals(8, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(8, profile.size());
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(694.61163, profile3.get("totalDistanceToThisPoint"));
        assertEquals(164.11, profile3.get("altitude"));
        assertEquals(69.1453, profile3.get("slope"));
        assertEquals(844028.75, profile3.get("x"));
        assertEquals(6420077.0, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1169.2932, profile5.get("totalDistanceToThisPoint"));
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(75.34314, profile5.get("slope"));
        assertEquals(844490.5, profile5.get("x"));
        assertEquals(6420187.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(1746.0248, profile7.get("totalDistanceToThisPoint"));
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(52.246147, profile7.get("slope"));
        assertEquals(844102.7, profile7.get("x"));
        assertEquals(6420614.0, profile7.get("y"));
    }

    @Test
    public void testReprojectCRS() throws Exception {
        String requestXml = loadTemplate(
                "templateTargetProjection.xml",
                Map.of(
                        "LAYER_NAME", "dataProfile",
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "300",
                        "TARGET_PROJECTION", "EPSG:3857"));

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(214.94, infos.get("altitudePositive"));
        assertEquals(-64.28, infos.get("altitudeNegative"));
        assertEquals(2463.6123, infos.get("totalDistance"));
        assertEquals(536188.94, infos.get("firstPointX"));
        assertEquals(5600680.0, infos.get("firstPointY"));
        assertEquals(537077.4, infos.get("lastPointX"));
        assertEquals(5601034.5, infos.get("lastPointY"));
        assertEquals("dataProfile", infos.get("layer"));
        assertEquals(8, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(8, profile.size());
        // Since checking all profiles will be excessive we will check only some in the middle
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(980.1413, profile3.get("totalDistanceToThisPoint"));
        assertEquals(164.11, profile3.get("altitude"));
        assertEquals(49.056587, profile3.get("slope"));
        assertEquals(536955.75, profile3.get("x"));
        assertEquals(5600277.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1649.2133, profile5.get("totalDistanceToThisPoint"));
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(53.45293, profile5.get("slope"));
        assertEquals(537609.9, profile5.get("x"));
        assertEquals(5600418.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(2463.6123, profile7.get("totalDistanceToThisPoint"));
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(36.998417, profile7.get("slope"));
        assertEquals(537077.4, profile7.get("x"));
        assertEquals(5601034.5, profile7.get("y"));
    }

    @Test
    public void testCorrectReprojection() throws Exception {
        String request2154 = loadTemplate(
                "templateBasic.xml",
                Map.of(
                        "LAYER_NAME", "dataProfile",
                        "GEOMETRY", LINESTRING_2154_EWKT,
                        "DISTANCE", "300"));

        String request4326 = loadTemplate(
                "templateBasic.xml",
                Map.of(
                        "LAYER_NAME", "dataProfile",
                        "GEOMETRY", LINESTRING_4326_EWKT,
                        "DISTANCE", "300"));

        JSONObject response2154 = (JSONObject) postAsJSON(root(), request2154, "application/xml");
        JSONObject response4326 = (JSONObject) postAsJSON(root(), request4326, "application/xml");
        JSONObject infos2154 = response2154.getJSONObject("infos");
        JSONObject infos4326 = response4326.getJSONObject("infos");

        assertEquals(infos2154.get("altitudePositive"), infos4326.get("altitudePositive"));
        assertEquals(infos2154.get("altitudeNegative"), infos4326.get("altitudeNegative"));
        assertEquals(infos2154.get("processedPoints"), infos4326.get("processedPoints"));

        JSONArray profiles2154 = (JSONArray) response2154.get("profile");
        JSONArray profiles4326 = (JSONArray) response4326.get("profile");

        for (int i = 0; i < profiles2154.size(); i++) {
            JSONObject p1 = (JSONObject) profiles2154.get(i);
            JSONObject p2 = (JSONObject) profiles4326.get(i);
            assertEquals(p1.get("altitude"), p2.get("altitude"));
        }
    }

    @Test
    public void testAllParams() throws Exception {
        String requestXml = loadTemplate(
                "templateAllParameters.xml",
                Map.of(
                        "LAYER_NAME", "dataProfile",
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "200",
                        "TARGET_PROJECTION", "EPSG:4326"));

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(195.69, infos.get("altitudePositive"));
        assertEquals(-67.03, infos.get("altitudeNegative"));
        assertEquals(0.020068234, infos.get("totalDistance"));
        assertEquals(4.8166676, infos.get("firstPointX"));
        assertEquals(44.867462, infos.get("firstPointY"));
        assertEquals(4.8246484, infos.get("lastPointX"));
        assertEquals(44.869717, infos.get("lastPointY"));
        assertEquals("dataProfile", infos.get("layer"));
        assertEquals(11, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(11, profile.size());

        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(0.0049660658, profile3.get("totalDistanceToThisPoint"));
        assertEquals(155.56, profile3.get("altitude"));
        assertEquals(102.00278, profile3.get("slope"));
        assertEquals(4.8206177, profile3.get("x"));
        assertEquals(44.864452, profile3.get("y"));

        JSONObject profile6 = (JSONObject) profile.get(6);
        assertEquals(0.011652884, profile6.get("totalDistanceToThisPoint"));
        assertEquals(144.7, profile6.get("altitude"));
        assertEquals(81.24586, profile6.get("slope"));
        assertEquals(4.827228, profile6.get("x"));
        assertEquals(44.86546, profile6.get("y"));

        JSONObject profile9 = (JSONObject) profile.get(9);
        assertEquals(0.018006066, profile9.get("totalDistanceToThisPoint"));
        assertEquals(127.35, profile9.get("altitude"));
        assertEquals(66.208275, profile9.get("slope"));
        assertEquals(4.826243, profile9.get("x"));
        assertEquals(44.86841, profile9.get("y"));
    }
}
