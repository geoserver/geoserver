/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs3;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.jayway.jsonpath.DocumentContext;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Locale;
import org.geoserver.data.test.MockData;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TilingSchemesTest extends WFS3TestSupport {

    private Locale originalLocale;

    @Before
    public void setup() {
        originalLocale = Locale.getDefault();
        // locale setting to test coordinate encode on US locale
        Locale.setDefault(Locale.ITALY);
    }

    @After
    public void onFinish() {
        Locale.setDefault(originalLocale);
    }

    /** Tests the "wfs3/tilingScheme" json response */
    @Test
    public void testTilingSchemesResponse() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes", 200);
        String scheme1 = jsonDoc.read("$.tilingSchemes[0]", String.class);
        String scheme2 = jsonDoc.read("$.tilingSchemes[1]", String.class);
        HashSet<String> schemesSet = new HashSet<>(Arrays.asList(scheme1, scheme2));
        assertTrue(schemesSet.contains("GlobalCRS84Geometric"));
        assertTrue(schemesSet.contains("GoogleMapsCompatible"));
        assertTrue(schemesSet.size() == 2);
    }

    @Test
    public void testTilingSchemeDescriptionGoogleMapsCompatible() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/GoogleMapsCompatible", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                new Double[] {-20037508.34, -20037508.34},
                "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
                559082263.9508929d,
                0.001d,
                1073741824,
                "tileMatrix[30].matrixWidth");
    }

    @Test
    public void testTilingSchemeDescriptionGoogleMapsCompatibleOnCollections() throws Exception {
        String roadSegments = getEncodedName(MockData.ROAD_SEGMENTS);
        DocumentContext jsonDoc =
                getAsJSONPath(
                        "wfs3/collections/" + roadSegments + "/tiles/GoogleMapsCompatible", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                new Double[] {-20037508.34d, -20037508.34d},
                "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
                559082263.9508929d,
                0.001d,
                1073741824,
                "tileMatrix[30].matrixWidth");
    }

    public void checkTilingSchemeData(
            DocumentContext jsonDoc,
            String s,
            Double[] bboxLowerCorner,
            String s3,
            double v,
            double v2,
            int i,
            String s4) {
        assertEquals(s, jsonDoc.read("boundingBox.crs", String.class));
        assertEquals(
                bboxLowerCorner[0],
                jsonDoc.read("boundingBox.lowerCorner[0]", Double.class),
                0.001d);
        assertEquals(
                bboxLowerCorner[1],
                jsonDoc.read("boundingBox.lowerCorner[1]", Double.class),
                0.001d);
        assertEquals(s3, jsonDoc.read("wellKnownScaleSet", String.class));
        assertEquals(v, jsonDoc.read("tileMatrix[0].scaleDenominator", Double.class), v2);
        assertEquals(Integer.valueOf(i), jsonDoc.read(s4, Integer.class));
    }

    @Test
    public void testTilingSchemeDescriptionGlobalCRS84Geometric() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/GlobalCRS84Geometric", 200);
        checkTilingSchemeData(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/4326",
                new Double[] {-180d, -90d},
                "http://www.opengis.net/def/wkss/OGC/1.0/GlobalCRS84Geometric",
                2.795411320143589E8d,
                0.000000000000001E8d,
                4194304,
                "tileMatrix[21].matrixWidth");
        assertEquals(90d, jsonDoc.read("tileMatrix[21].topLeftCorner[0]", Double.class), 0.001d);
        assertEquals(-180d, jsonDoc.read("tileMatrix[21].topLeftCorner[1]", Double.class), 0.001d);
    }

    @Test
    public void testTilingSchemeDescriptionError() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/errorNameX", 500);
        assertEquals("Invalid gridset name errorNameX", jsonDoc.read("description", String.class));
    }
}
