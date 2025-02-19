/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.geoserver.data.test.MockData;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.wfs.kvp.BBoxKvpParser;
import org.geoserver.wms.GetMapRequest;
import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.factory.Hints;
import org.junit.BeforeClass;
import org.junit.Test;

public class MapMLFormatLinkTest extends GeoServerWicketTestSupport {

    @BeforeClass
    public static void setup() {
        CRS.reset("all");
        System.setProperty("org.geotools.referencing.forceXY", "true");
        Hints.putSystemDefault(Hints.FORCE_AXIS_ORDER_HONORING, "http");
        Hints.putSystemDefault(Hints.LENIENT_DATUM_SHIFT, true);
    }

    @Test
    public void testSupportedCodes() throws Exception {
        Set<String> codes = CRS.getSupportedCodes("MapML");
        for (String code : codes) {
            if ("WGS84(DD)".equals(code)) continue;
            testSupportedCode("MapML:" + code);
        }
    }

    private void testSupportedCode(String code) throws FactoryException {
        transformLink(code, code);
    }

    @Test
    public void testCompatibleCodes() throws Exception {
        transformLink("CRS:84", "MapML:WGS84");
        transformLink("EPSG:3857", "MapML:OSMTILE");
        transformLink("EPSG:5936", "MapML:APSTILE");
        transformLink("EPSG:3978", "MapML:CBMTILE");
    }

    @Test
    public void testIncompatibleCode() throws Exception {
        MapMLFormatLink link = new MapMLFormatLink();
        String epsgCode = "EPSG:32632";
        Map<String, String> parameters = new HashMap<>();
        parameters.put("srs", epsgCode);
        parameters.put("layers", MockData.LINES.getLocalPart());
        GetMapRequest request = new GetMapRequest();
        CoordinateReferenceSystem crs = CRS.decode(epsgCode, true);
        request.setCrs(crs);
        request.setSRS(epsgCode);
        request.setBbox(new ReferencedEnvelope(1000000, 1100000, 4000000, 4100000, crs));
        link.customizeRequest(request, parameters);
        assertEquals("MapML:WGS84", parameters.get("srs"));
        // bbox has been transfomed to WGS84
        String bbox = parameters.get("bbox");
        assertNotNull(bbox);
        ReferencedEnvelope envelope = (ReferencedEnvelope) new BBoxKvpParser().parse(bbox);
        assertEquals(14.5, envelope.getMinX(), 0.1);
        assertEquals(36, envelope.getMinY(), 0.1);
        assertEquals(15.7, envelope.getMaxX(), 0.1);
        assertEquals(37, envelope.getMaxY(), 0.1);
    }

    private void transformLink(String epsgCode, String mapMLCode) throws FactoryException {
        MapMLFormatLink link = new MapMLFormatLink();
        Map<String, String> parameters = new HashMap<>();
        parameters.put("srs", epsgCode);
        parameters.put("layers", MockData.LINES.getLocalPart());
        GetMapRequest request = new GetMapRequest();
        request.setCrs(CRS.decode(epsgCode, true));
        link.customizeRequest(request, parameters);
        assertEquals(mapMLCode, parameters.get("srs"));
    }
}
