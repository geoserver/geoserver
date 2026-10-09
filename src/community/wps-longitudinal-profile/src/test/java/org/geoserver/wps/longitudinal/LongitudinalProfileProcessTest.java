/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.longitudinal;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathExists;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import java.awt.image.BufferedImage;
import java.awt.image.WritableRaster;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Future;
import javax.xml.namespace.QName;
import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.wps.WPSException;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.cs.AxisDirection;
import org.geotools.api.referencing.cs.CoordinateSystemAxis;
import org.geotools.api.referencing.operation.TransformException;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.grid.GridCoverageFactory;
import org.geotools.data.util.DefaultProgressListener;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.measure.Units;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultEngineeringCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultCoordinateSystemAxis;
import org.geotools.referencing.datum.DefaultEngineeringDatum;
import org.junit.Test;
import org.kordamp.json.JSONArray;
import org.kordamp.json.JSONNull;
import org.kordamp.json.JSONObject;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.io.WKTReader;
import org.w3c.dom.Document;

public class LongitudinalProfileProcessTest extends WPSTestSupport {
    // distances and slopes come from geodesic computations, allow for last digit drifts across GeoTools versions
    private static final double DELTA = 1e-3;
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
                        + "or reduce the line length. Would extract at least 116994 points, but maximum is 50000"));
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

    private void checkBasicProfile(String requestXml, String expectedLayer) throws Exception {
        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        JSONObject infos = response.getJSONObject("infos");
        assertEquals(37.03, infos.get("altitudePositive"));
        assertEquals(-64.28, infos.get("altitudeNegative"));
        assertEquals(1746.9653, infos.getDouble("totalDistance"), DELTA);
        assertEquals(843478.25, infos.get("firstPointX"));
        assertEquals(6420349.0, infos.get("firstPointY"));
        assertEquals(844102.7, infos.get("lastPointX"));
        assertEquals(6420614.0, infos.get("lastPointY"));
        if (expectedLayer != null) {
            assertEquals(expectedLayer, infos.get("layer"));
        } else {
            assertEquals(JSONNull.getInstance(), infos.get("layer"));
        }
        assertEquals(8, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(8, profile.size());
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(694.9856, profile3.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(164.11, profile3.get("altitude"));
        assertEquals(-5.6639104, profile3.getDouble("slope"), DELTA);
        assertEquals(844028.75, profile3.get("x"));
        assertEquals(6420077.0, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1169.9227, profile5.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(14.595616, profile5.getDouble("slope"), DELTA);
        assertEquals(844490.5, profile5.get("x"));
        assertEquals(6420187.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(1746.9653, profile7.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(-0.259946, profile7.getDouble("slope"), DELTA);
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
        assertEquals(37.03, infos.get("altitudePositive"));
        assertEquals(-64.28, infos.get("altitudeNegative"));
        assertEquals(1746.9653, infos.getDouble("totalDistance"), DELTA);
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
        assertEquals(694.9856, profile3.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(164.11, profile3.get("altitude"));
        assertEquals(-5.6639104, profile3.getDouble("slope"), DELTA);
        assertEquals(536955.75, profile3.get("x"));
        assertEquals(5600277.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(1169.9227, profile5.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(178.82, profile5.get("altitude"));
        assertEquals(14.595616, profile5.getDouble("slope"), DELTA);
        assertEquals(537609.9, profile5.get("x"));
        assertEquals(5600418.0, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(1746.9653, profile7.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(150.66, profile7.get("altitude"));
        assertEquals(-0.259946, profile7.getDouble("slope"), DELTA);
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
    public void testDistanceIndependentOfTargetProjection() throws Exception {
        assertSameDistance(COVERAGE_LAYER_NAME, LINESTRING_2154_EWKT, "EPSG:2154", "300");
        // no distance, the automatic sampling step must not depend on the target projection either
        assertSameDistance(COVERAGE_LAYER_NAME, LINESTRING_2154_EWKT, "EPSG:2154", null);
        assertSameDistance(COVERAGE_LAYER_NAME_4326, LINESTRING_4326_EWKT, "EPSG:4326", null);
    }

    @Test
    public void testDistanceInTargetProjectionUnit() throws Exception {
        // a coverage in San Francisco, inside the EPSG:2227 area of use
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", true);
        ReferencedEnvelope envelope = new ReferencedEnvelope(-13582000, -13580000, 4550000, 4552000, crs);
        GridCoverage2D coverage = createTwoByTwoCoverage(envelope);
        Geometry geometry = new WKTReader().read("LINESTRING(-13581900 4551000, -13580100 4551000)");
        geometry.setUserData(crs);

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        LongitudinalProfileProcess.LongitudinalProfileProcessResult meters =
                process.execute(null, coverage, null, geometry, 100d, crs, 0, null, false, null);
        CoordinateReferenceSystem feetCRS = CRS.decode("EPSG:2227", true);
        LongitudinalProfileProcess.LongitudinalProfileProcessResult feet =
                process.execute(null, coverage, null, geometry, 100d, feetCRS, 0, null, false, null);

        // EPSG:2227 uses US survey feet, 1200/3937 meters each
        double metersDistance = meters.getOperationInfo().getTotalDistance();
        assertEquals(metersDistance * 3937 / 1200, feet.getOperationInfo().getTotalDistance(), 1e-6);
        // slope is a ratio of altitude over ground run, it must not depend on the reporting unit
        List<ProfileInfo> metersProfile = meters.getProfileInfoList();
        List<ProfileInfo> feetProfile = feet.getProfileInfoList();
        double maxSlope = metersProfile.stream()
                .mapToDouble(p -> Math.abs(p.getSlope()))
                .max()
                .orElse(0);
        assertThat(maxSlope, greaterThan(0d));
        for (int i = 0; i < metersProfile.size(); i++)
            assertEquals(metersProfile.get(i).getSlope(), feetProfile.get(i).getSlope(), 1e-6);
    }

    @Test
    public void testProjectedDistance() throws Exception {
        Geometry geometry = new WKTReader().read(LINESTRING_2154_WKT);
        CoordinateReferenceSystem mercator = CRS.decode("EPSG:3857", true);
        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        double ground = process.execute(COVERAGE_LAYER_NAME, null, null, geometry, 300d, mercator, 0, null, false, null)
                .getOperationInfo()
                .getTotalDistance();
        LongitudinalProfileProcess.LongitudinalProfileProcessResult projected =
                process.execute(COVERAGE_LAYER_NAME, null, null, geometry, 300d, mercator, 0, null, true, null);

        // distances and slopes follow the returned coordinates on the projection plane
        List<ProfileInfo> profile = projected.getProfileInfoList();
        double total = 0;
        for (int i = 1; i < profile.size(); i++) {
            ProfileInfo previous = profile.get(i - 1);
            ProfileInfo current = profile.get(i);
            double run = Math.hypot(current.getX() - previous.getX(), current.getY() - previous.getY());
            total += run;
            assertEquals(total, current.getTotalDistanceToThisPoint(), 1e-6);
            assertEquals((current.getAltitude() - previous.getAltitude()) * 100 / run, current.getSlope(), 1e-6);
        }
        assertEquals(total, projected.getOperationInfo().getTotalDistance(), 1e-6);
        // Web Mercator stretches distances by about 1/cos(45 degrees) in France
        assertEquals(Math.sqrt(2), total / ground, 0.02);
    }

    @Test
    public void testProjectedDistanceGeographicTarget() throws Exception {
        Geometry geometry = new WKTReader().read(LINESTRING_2154_WKT);
        CoordinateReferenceSystem wgs84 = CRS.decode("EPSG:4326", true);
        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        WPSException e = assertThrows(
                WPSException.class,
                () -> process.execute(COVERAGE_LAYER_NAME, null, null, geometry, 300d, wgs84, 0, null, true, null));
        assertThat(e.getMessage(), containsString("projected targetProjection"));
    }

    private void assertSameDistance(String layer, String geometry, String nativeSrs, String distance) throws Exception {
        JSONObject nativeInfos = getTargetProjectionInfos(layer, geometry, nativeSrs, distance);
        JSONObject mercatorInfos = getTargetProjectionInfos(layer, geometry, "EPSG:3857", distance);
        assertEquals(nativeInfos.get("processedPoints"), mercatorInfos.get("processedPoints"));
        assertEquals(nativeInfos.getDouble("totalDistance"), mercatorInfos.getDouble("totalDistance"), 0.01);
    }

    private JSONObject getTargetProjectionInfos(String layer, String geometry, String srs, String distance)
            throws Exception {
        Map<String, String> values = new HashMap<>(Map.of("LAYER_NAME", layer, "GEOMETRY", geometry));
        values.put("TARGET_PROJECTION", srs);
        if (distance != null) values.put("DISTANCE", distance);
        String requestXml = loadTemplate(TEMPLATE_TARGET_PROJECTION, values);
        JSONObject response = (JSONObject) postAsJSON(root(), requestXml, "application/xml");
        return response.getJSONObject("infos");
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
        assertEquals(39.78, infos.get("altitudePositive"));
        assertEquals(-67.03, infos.get("altitudeNegative"));
        // check it's a meaningful distance in meters, not some random number in degrees
        assertEquals(1746.9653, infos.getDouble("totalDistance"), DELTA);
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
        assertEquals(457.51718, profile3.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(155.56, profile3.get("altitude"));
        assertEquals(-0.07868561, profile3.getDouble("slope"), DELTA);
        assertEquals(4.8206177, profile3.get("x"));
        assertEquals(44.864452, profile3.get("y"));

        JSONObject profile6 = (JSONObject) profile.get(6);
        assertEquals(991.8213, profile6.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(144.7, profile6.get("altitude"));
        assertEquals(11.830339, profile6.getDouble("slope"), DELTA);
        assertEquals(4.827228, profile6.get("x"));
        assertEquals(44.86546, profile6.get("y"));

        JSONObject profile9 = (JSONObject) profile.get(9);
        assertEquals(1554.6177, profile9.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(127.35, profile9.get("altitude"));
        assertEquals(-12.243462, profile9.getDouble("slope"), DELTA);
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
        assertEquals(80.09, infos.get("altitudePositive"));
        assertEquals(-107.34, infos.get("altitudeNegative"));
        assertEquals(1746.9653, infos.getDouble("totalDistance"), DELTA);
        assertEquals(843478.25, infos.get("firstPointX"));
        assertEquals(6420349.0, infos.get("firstPointY"));
        assertEquals(844102.7, infos.get("lastPointX"));
        assertEquals(6420614.0, infos.get("lastPointY"));

        assertEquals(310, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(310, profile.size());

        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(16.945093, profile3.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(175.16, profile3.get("altitude"));
        assertEquals(-1.2392969, profile3.getDouble("slope"), DELTA);
        assertEquals(843490.1, profile3.get("x"));
        assertEquals(6420336.5, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(28.24182, profile5.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(176.8, profile5.get("altitude"));
        assertEquals(36.64778, profile5.getDouble("slope"), DELTA);
        assertEquals(843498.0, profile5.get("x"));
        assertEquals(6420328.5, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(39.538548, profile7.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(175.67, profile7.get("altitude"));
        assertEquals(-25.671152, profile7.getDouble("slope"), DELTA);
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

        assertEquals(78.99, infos.get("altitudePositive"));
        assertEquals(-106.03, infos.get("altitudeNegative"));
        assertEquals(1746.9653, infos.getDouble("totalDistance"), DELTA);
        assertEquals(4.8166676, infos.get("firstPointX"));
        assertEquals(44.867462, infos.get("firstPointY"));
        assertEquals(4.8246484, infos.get("lastPointX"));
        assertEquals(44.869717, infos.get("lastPointY"));

        assertEquals(305, infos.get("processedPoints"));
        assertNotNull(infos.get("executedTime"));
        JSONArray profile = response.getJSONArray("profile");
        assertEquals(305, profile.size());
        JSONObject profile3 = (JSONObject) profile.get(3);
        assertEquals(18.300476, profile3.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(174.79, profile3.get("altitude"));
        assertEquals(-8.032576, profile3.getDouble("slope"), DELTA);
        assertEquals(4.8168254, profile3.get("x"));
        assertEquals(44.867340, profile3.get("y"));

        JSONObject profile5 = (JSONObject) profile.get(5);
        assertEquals(30.500803, profile5.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(176.42, profile5.get("altitude"));
        assertEquals(29.015612, profile5.getDouble("slope"), DELTA);
        assertEquals(4.816931, profile5.get("x"));
        assertEquals(44.86726, profile5.get("y"));

        JSONObject profile7 = (JSONObject) profile.get(7);
        assertEquals(42.701138, profile7.getDouble("totalDistanceToThisPoint"), DELTA);
        assertEquals(177.02, profile7.get("altitude"));
        assertEquals(22.130537, profile7.getDouble("slope"), DELTA);
        assertEquals(4.8170360, profile7.get("x"));
        assertEquals(44.86718, profile7.get("y"));
    }

    @Test
    public void testTwoPointProfileAltitudeIncrement() throws Exception {
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", true);
        GridCoverage2D coverage = createTwoByTwoCoverage(new ReferencedEnvelope(0, 2, 0, 2, crs));
        Geometry geometry = new WKTReader().read("LINESTRING(0.5 1.5, 1.5 1.5)");
        geometry.setUserData(crs);

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        LongitudinalProfileProcess.LongitudinalProfileProcessResult result =
                process.execute(null, coverage, null, geometry, 10d, null, 0, null, false, null);

        OperationInfo info = result.getOperationInfo();
        assertEquals(25.0, info.getAltitudePositive(), 0);
        assertEquals(0.0, info.getAltitudeNegative(), 0);

        List<ProfileInfo> profile = result.getProfileInfoList();
        assertEquals(2, profile.size());
        assertEquals(200.0, profile.get(0).getAltitude(), 0);
        assertEquals(225.0, profile.get(1).getAltitude(), 0);
    }

    @Test
    public void testDistanceStepOnGround() throws Exception {
        // around 60 degrees north Mercator doubles the ground distances
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", true);
        GridCoverage2D coverage = createTwoByTwoCoverage(new ReferencedEnvelope(0, 2000, 8400000, 8402000, crs));
        Geometry geometry = new WKTReader().read("LINESTRING(100 8401000, 1900 8401000)");
        geometry.setUserData(crs);

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        List<ProfileInfo> profile = process.execute(null, coverage, null, geometry, 100d, null, 0, null, false, null)
                .getProfileInfoList();

        // 1800 projected meters are about 902 ground meters, 10 steps of at most 100 meters
        assertEquals(11, profile.size());
        assertMaxStep(profile, 100d);
    }

    @Test
    public void testDistanceStepOnGroundVaryingScale() throws Exception {
        // from the equator to 60 degrees north the Mercator scale goes from 1 to 2, averaging about 1.26
        CoordinateReferenceSystem crs = CRS.decode("EPSG:3857", true);
        GridCoverage2D coverage = createTwoByTwoCoverage(new ReferencedEnvelope(-1000, 1000, 0, 8400000, crs));
        Geometry geometry = new WKTReader().read("LINESTRING(500 1000, 500 8399000)");
        geometry.setUserData(crs);

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        List<ProfileInfo> profile = process.execute(null, coverage, null, geometry, 100000d, null, 0, null, false, null)
                .getProfileInfoList();

        // the average scale would give 126 km steps near the equator
        assertMaxStep(profile, 100000d);
    }

    @Test
    public void testDistanceNonLengthUnit() throws Exception {
        // a local CRS with dimensionless axes, there is no way to turn meters into its units
        CoordinateSystemAxis x = new DefaultCoordinateSystemAxis("x", AxisDirection.EAST, Units.ONE);
        CoordinateSystemAxis y = new DefaultCoordinateSystemAxis("y", AxisDirection.NORTH, Units.ONE);
        CoordinateReferenceSystem crs = new DefaultEngineeringCRS(
                "grid", DefaultEngineeringDatum.UNKNOWN, new DefaultCartesianCS("grid", x, y));
        GridCoverage2D coverage = createTwoByTwoCoverage(new ReferencedEnvelope(0, 2, 0, 2, crs));
        Geometry geometry = new WKTReader().read("LINESTRING(0.5 1, 1.5 1)");

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        WPSException e = assertThrows(
                WPSException.class,
                () -> process.execute(null, coverage, null, geometry, 1d, null, 0, null, false, null));
        assertThat(e.getMessage(), containsString("axis unit is not a length in grid"));
    }

    private static void assertMaxStep(List<ProfileInfo> profile, double maxStep) {
        for (int i = 1; i < profile.size(); i++) {
            double step = profile.get(i).getTotalDistanceToThisPoint()
                    - profile.get(i - 1).getTotalDistanceToThisPoint();
            assertThat(step, lessThanOrEqualTo(maxStep));
        }
    }

    @Test
    public void testSlopeWithFeetAltitudes() throws Exception {
        // a DEM in a US survey feet CRS, without band unit the altitudes are assumed in feet too
        CoordinateReferenceSystem crs = CRS.decode("EPSG:2227", true);
        ReferencedEnvelope envelope = new ReferencedEnvelope(6000000, 6000200, 2100000, 2100200, crs);
        GridCoverage2D coverage = createTwoByTwoCoverage(envelope);
        Geometry geometry = new WKTReader().read("LINESTRING(6000050 2100150, 6000150 2100150)");
        geometry.setUserData(crs);

        LongitudinalProfileProcess process = new LongitudinalProfileProcess(getGeoServer());
        List<ProfileInfo> profile = process.execute(null, coverage, null, geometry, 1000d, null, 0, null, false, null)
                .getProfileInfoList();

        // 25 feet up over about 100 feet on the ground, not over 100 meters
        assertEquals(2, profile.size());
        assertEquals(25, profile.get(1).getSlope(), 0.01);
    }

    private GridCoverage2D createTwoByTwoCoverage(ReferencedEnvelope envelope) {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);
        WritableRaster raster = image.getRaster();
        raster.setSample(0, 0, 0, 200);
        raster.setSample(1, 0, 0, 225);
        raster.setSample(0, 1, 0, 230);
        raster.setSample(1, 1, 0, 240);

        return new GridCoverageFactory().create("two-by-two", image, envelope);
    }

    @Test
    public void processCancellationTest() throws Exception {
        GeoServer geoServer = getGeoServer();
        CountDownLatch latch = new CountDownLatch(1);
        LongitudinalProfileProcess process = new LongitudinalProfileProcess(geoServer) {

            @Override
            protected DistanceSlopeCalculator getDistanceSlopeCalculator(
                    GridCoverage2D coverage,
                    int altitudeIndex,
                    CoordinateReferenceSystem projection,
                    boolean projectedDistance) {
                return new DistanceSlopeCalculator(coverage, altitudeIndex, projection, projectedDistance) {

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
                        return process.execute(
                                COVERAGE_LAYER_NAME, null, null, geometry, 300d, null, 0, null, false, monitor);
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
