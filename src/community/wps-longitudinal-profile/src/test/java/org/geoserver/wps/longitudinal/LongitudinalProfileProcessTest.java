/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import javax.xml.namespace.QName;
import net.sf.json.JSONArray;
import net.sf.json.JSONNull;
import net.sf.json.JSONObject;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.data.util.DefaultProgressListener;
import org.junit.Test;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.w3c.dom.Document;

public class LongitudinalProfileProcessTest extends WPSTestSupport {
    // layers
    public static final String COVERAGE_LAYER_NAME = "dataProfile";
    public static final String COVERAGE_LAYER_NAME_4326 = "dataProfile4326";
    private static final QName PROFILE = new QName(MockData.DEFAULT_URI, COVERAGE_LAYER_NAME, MockData.DEFAULT_PREFIX);
    private static final QName PROFILE_4326 =
            new QName(MockData.DEFAULT_URI, COVERAGE_LAYER_NAME_4326, MockData.DEFAULT_PREFIX);
    private static final QName ADJ_LAYER = new QName(MockData.DEFAULT_URI, "AdjustmentLayer", MockData.DEFAULT_PREFIX);

    // test constants
    public static final String PROCESS_FAILED_PATH = "/wps:ExecuteResponse/wps:Status/wps:ProcessFailed";
    public static final String EXCEPTION_MESSAGE_PATH =
            PROCESS_FAILED_PATH + "/ows:ExceptionReport/ows:Exception/ows:ExceptionText";
    public static final String TEMPLATE_BASIC = "templateBasic.xml";
    public static final String TEMPLATE_CHAINING = "templateChaining.xml";
    public static final String TEMPLATE_TARGET_PROJECTION = "templateTargetProjection.xml";
    public static final String TEMPLATE_ALL_PARAMETERS = "templateAllParameters.xml";

    // test inputs
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
        testData.addRasterLayer(PROFILE_4326, "dem.zip", null, Collections.emptyMap(), getCatalog());
    }

    private String loadTemplate(String templateName, Map<String, String> values) throws IOException {
        String template = new String(Files.readAllBytes(Path.of("src/test/resources/" + templateName)));
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

        assertXpathExists(PROCESS_FAILED_PATH, d);
        String msg = xp.evaluate(EXCEPTION_MESSAGE_PATH, d);
        assertThat(msg, containsString("Either layerName or coverage must be provided"));
    }

    @Test
    public void testTooManyPoints() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME,
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "0.01"));

        Document d = postAsDOM(root(), requestXml);
        assertEquals("wps:ExecuteResponse", d.getDocumentElement().getNodeName());

        assertXpathExists(PROCESS_FAILED_PATH, d);
        String msg = xp.evaluate(EXCEPTION_MESSAGE_PATH, d);
        assertThat(
                msg,
                containsString("Too many points in the line, please increase the distance parameter "
                        + "or reduce the line length. Would extract 174603 points, but maximum is 50000"));
    }

    @Test
    public void testBasicProfileLayer() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME,
                        "GEOMETRY", LINESTRING_2154_WKT,
                        "DISTANCE", "300"));

        checkBasicProfile(requestXml, COVERAGE_LAYER_NAME);
    }

    @Test
    public void testBasicProfileCoverage() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_CHAINING,
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
        assertEquals(-5.666957, profile3.get("slope"));
        assertEquals(844028.75, profile3.get("x"));
        assertEquals(6420077.0, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1169.2932, profile5.get("totalDistanceToThisPoint"));
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(14.603475, profile5.get("slope"));
        assertEquals(844490.5, profile5.get("x"));
        assertEquals(6420187.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(1746.0248, profile7.get("totalDistanceToThisPoint"));
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(-0.26008636, profile7.get("slope"));
        assertEquals(844102.7, profile7.get("x"));
        assertEquals(6420614.0, profile7.get("y"));
    }

    @Test
    public void testReprojectCRS() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_TARGET_PROJECTION,
                Map.of(
                        "LAYER_NAME",
                        COVERAGE_LAYER_NAME,
                        "GEOMETRY",
                        LINESTRING_2154_WKT,
                        "DISTANCE",
                        "300",
                        "TARGET_PROJECTION",
                        "EPSG:3857"));

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(214.94, infos.get("altitudePositive"));
        assertEquals(-64.28, infos.get("altitudeNegative"));
        assertEquals(2463.6123, infos.get("totalDistance"));
        assertEquals(536188.94, infos.get("firstPointX"));
        assertEquals(5600680.0, infos.get("firstPointY"));
        assertEquals(537077.4, infos.get("lastPointX"));
        assertEquals(5601034.5, infos.get("lastPointY"));
        assertEquals(COVERAGE_LAYER_NAME, infos.get("layer"));
        assertEquals(8, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(8, profile.size());
        // Since checking all profiles will be excessive we will check only some in the middle
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(980.1413, profile3.get("totalDistanceToThisPoint"));
        assertEquals(164.11, profile3.get("altitude"));
        assertEquals(-4.0205417, profile3.get("slope"));
        assertEquals(536955.75, profile3.get("x"));
        assertEquals(5600277.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1649.2133, profile5.get("totalDistanceToThisPoint"));
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(10.360578, profile5.get("slope"));
        assertEquals(537609.9, profile5.get("x"));
        assertEquals(5600418.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(2463.6123, profile7.get("totalDistanceToThisPoint"));
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(-0.18418169, profile7.get("slope"));
        assertEquals(537077.4, profile7.get("x"));
        assertEquals(5601034.5, profile7.get("y"));
    }

    @Test
    public void testCorrectReprojection() throws Exception {
        String request2154 = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME,
                        "GEOMETRY", LINESTRING_2154_EWKT,
                        "DISTANCE", "300"));

        String request4326 = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME,
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
                TEMPLATE_ALL_PARAMETERS,
                Map.of(
                        "LAYER_NAME",
                        COVERAGE_LAYER_NAME,
                        "GEOMETRY",
                        LINESTRING_2154_WKT,
                        "DISTANCE",
                        "200",
                        "TARGET_PROJECTION",
                        "EPSG:4326"));

        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(195.69, infos.get("altitudePositive"));
        assertEquals(-67.03, infos.get("altitudeNegative"));
        // check it's a meaningful distance in meters, not some random number in degrees
        assertEquals(1746.9653, infos.get("totalDistance"));
        assertEquals(4.8166676, infos.get("firstPointX"));
        assertEquals(44.867462, infos.get("firstPointY"));
        assertEquals(4.8246484, infos.get("lastPointX"));
        assertEquals(44.869717, infos.get("lastPointY"));
        assertEquals(COVERAGE_LAYER_NAME, infos.get("layer"));
        assertEquals(11, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(11, profile.size());

        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(457.51718, profile3.get("totalDistanceToThisPoint"));
        assertEquals(155.56, profile3.get("altitude"));
        assertEquals(-0.07868561, profile3.get("slope"));
        assertEquals(4.8206177, profile3.get("x"));
        assertEquals(44.864452, profile3.get("y"));

        JSONObject profile6 = (JSONObject) profile.get(6);
        assertEquals(991.8213, profile6.get("totalDistanceToThisPoint"));
        assertEquals(144.7, profile6.get("altitude"));
        assertEquals(11.830339, profile6.get("slope"));
        assertEquals(4.827228, profile6.get("x"));
        assertEquals(44.86546, profile6.get("y"));

        JSONObject profile9 = (JSONObject) profile.get(9);
        assertEquals(1554.6177, profile9.get("totalDistanceToThisPoint"));
        assertEquals(127.35, profile9.get("altitude"));
        assertEquals(-12.243462, profile9.get("slope"));
        assertEquals(4.826243, profile9.get("x"));
        assertEquals(44.86841, profile9.get("y"));
    }

    @Test
    public void testProfileLayerNoDistance() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME,
                        "GEOMETRY", LINESTRING_2154_EWKT));
        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");

        // Dataset is ~ 4m in resolution. Diagonal resolution is ~5.65
        assertEquals(258.0, infos.get("altitudePositive"));
        assertEquals(-107.34, infos.get("altitudeNegative"));
        assertEquals(1746.0248, infos.get("totalDistance"));
        assertEquals(843478.25, infos.get("firstPointX"));
        assertEquals(6420349.0, infos.get("firstPointY"));
        assertEquals(844102.7, infos.get("lastPointX"));
        assertEquals(6420614.0, infos.get("lastPointY"));

        assertEquals(310, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(310, profile.size());

        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(16.935959, profile3.get("totalDistanceToThisPoint"));
        assertEquals(175.16, profile3.get("altitude"));
        assertEquals(-1.2399652, profile3.get("slope"));
        assertEquals(843490.1, profile3.get("x"));
        assertEquals(6420336.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(28.226597, profile5.get("totalDistanceToThisPoint"));
        assertEquals(176.8, profile5.get("altitude"));
        assertEquals(36.667545, profile5.get("slope"));
        assertEquals(843498.0, profile5.get("x"));
        assertEquals(6420328.5, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(39.51724, profile7.get("totalDistanceToThisPoint"));
        assertEquals(175.67, profile7.get("altitude"));
        assertEquals(-25.684994, profile7.get("slope"));
        assertEquals(843505.9, profile7.get("x"));
        assertEquals(6420320.5, profile7.get("y"));
    }

    @Test
    public void testProfileLayer4326NoDistance() throws Exception {
        String requestXml = loadTemplate(
                TEMPLATE_BASIC,
                Map.of(
                        "LAYER_NAME", COVERAGE_LAYER_NAME_4326,
                        "GEOMETRY", LINESTRING_4326_EWKT));
        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");

        assertEquals(256.9, infos.get("altitudePositive"));
        assertEquals(-106.03, infos.get("altitudeNegative"));
        assertEquals(1746.9653, infos.get("totalDistance"));
        assertEquals(4.8166676, infos.get("firstPointX"));
        assertEquals(44.867462, infos.get("firstPointY"));
        assertEquals(4.8246484, infos.get("lastPointX"));
        assertEquals(44.869717, infos.get("lastPointY"));

        assertEquals(305, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(305, profile.size());
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(18.300476, profile3.get("totalDistanceToThisPoint"));
        assertEquals(174.79, profile3.get("altitude"));
        assertEquals(-8.032576, profile3.get("slope"));
        assertEquals(4.8168254, profile3.get("x"));
        assertEquals(44.867340, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(30.500803, profile5.get("totalDistanceToThisPoint"));
        assertEquals(176.42, profile5.get("altitude"));
        assertEquals(29.015612, profile5.get("slope"));
        assertEquals(4.816931, profile5.get("x"));
        assertEquals(44.86726, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(42.701138, profile7.get("totalDistanceToThisPoint"));
        assertEquals(177.02, profile7.get("altitude"));
        assertEquals(22.130537, profile7.get("slope"));
        assertEquals(4.8170360, profile7.get("x"));
        assertEquals(44.86718, profile7.get("y"));
    }

    @Test
    public void processCancellationTest() throws Exception {
        GeoServer geoServer = getGeoServer();
        CountDownLatch latch = new CountDownLatch(1);
        LongitudinalProfileProcess process = new LongitudinalProfileProcess(geoServer) {

            @Override
            protected DistanceSlopeCalculator getDistanceSlopeCalculator(CoordinateReferenceSystem projection) {
                return new DistanceSlopeCalculator(projection) {

                    @Override
                    public void next(Point next, double altitude) throws TransformException {
                        // wait for the latch to be released, to ensure the process cannot finish before
                        // the cancellation gets issued
                        try {
                            latch.await();
                        } catch (InterruptedException e) {
                            throw new RuntimeException(e);
                        }
                        super.next(next, altitude);
                    }
                };
            }
        };

        Geometry geometry = new WKTReader().read(LINESTRING_2154_WKT);
        DefaultProgressListener monitor = new DefaultProgressListener();

        // start in background thread
        Future<LongitudinalProfileProcess.LongitudinalProfileProcessResult> future =
                CompletableFuture.supplyAsync(() -> {
                    try {
                        return process.execute(COVERAGE_LAYER_NAME, null, null, geometry, 300d, null, 0, null, monitor);
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        // perform cancellation
        monitor.setCanceled(true);

        // release the latch to allow the process to finish
        latch.countDown();

        // check the result is null (cancelled)
        assertNull(future.get());
    }
}
