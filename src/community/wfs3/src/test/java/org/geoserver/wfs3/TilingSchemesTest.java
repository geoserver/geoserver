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
        checkGoogleMapsCompatible(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                "-20037508.340000 -20037508.340000",
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
        checkGoogleMapsCompatible(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/3857",
                "-20037508.340000 -20037508.340000",
                "http://www.opengis.net/def/wkss/OGC/1.0/GoogleMapsCompatible",
                559082263.9508929d,
                0.001d,
                1073741824,
                "tileMatrix[30].matrixWidth");
    }

    public void checkGoogleMapsCompatible(
            DocumentContext jsonDoc,
            String s,
            String s2,
            String s3,
            double v,
            double v2,
            int i,
            String s4) {
        assertEquals(s, jsonDoc.read("boundingBox.crs", String.class));
        assertEquals(s2, jsonDoc.read("boundingBox.lowerCorner", String.class));
        assertEquals(s3, jsonDoc.read("wellKnownScaleSet", String.class));
        assertEquals(v, jsonDoc.read("tileMatrix[0].scaleDenominator", Double.class), v2);
        assertEquals(new Integer(i), jsonDoc.read(s4, Integer.class));
    }

    @Test
    public void testTilingSchemeDescriptionGlobalCRS84Geometric() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/GlobalCRS84Geometric", 200);
        checkGoogleMapsCompatible(
                jsonDoc,
                "http://www.opengis.net/def/crs/EPSG/0/4326",
                "-180.000000 -90.000000",
                "http://www.opengis.net/def/wkss/OGC/1.0/GlobalCRS84Geometric",
                2.795411320143589E8d,
                0.000000000000001E8d,
                4194304,
                "tileMatrix[21].matrixWidth");
        assertEquals(
                "90.000000 -180.000000",
                jsonDoc.read("tileMatrix[21].topLeftCorner", String.class));
    }

    @Test
    public void testTilingSchemeDescriptionError() throws Exception {
        DocumentContext jsonDoc = getAsJSONPath("wfs3/tilingSchemes/errorNameX", 500);
        assertEquals("Invalid gridset name errorNameX", jsonDoc.read("description", String.class));
    }
}
