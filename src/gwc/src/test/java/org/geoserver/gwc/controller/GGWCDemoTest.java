package org.geoserver.gwc.controller;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.geoserver.data.test.MockData;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.gwc.GWC;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;
import org.springframework.mock.web.MockHttpServletResponse;

public class GGWCDemoTest extends GeoServerSystemTestSupport {
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        super.onSetUp(testData);

        GWC.get().getConfig().setDirectWMSIntegrationEnabled(false);
    }

    @Test
    public void testGetLayerInWorkspace() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "cite/gwc/demo/" + layerName + "?gridSet=EPSG:4326&format=image/jpeg";
        // final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("text/html"));
    }

    @Test
    public void testGetLayerWithoutWorkspace() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS);
        final String url = "gwc/demo/" + layerName + "?gridSet=EPSG:4326&format=image/jpeg";
        // final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("text/html"));
    }

    @Test
    public void testGetLayerWithBadLayer() throws Exception {
        final String layerName = getLayerId(MockData.BASIC_POLYGONS) + "_NOT";
        final String url = "gwc/demo/" + layerName + "?gridSet=EPSG:4326&format=image/jpeg";
        // final String id = getCatalog().getLayerByName(layerName).getId();

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(400, sr.getStatus());
        assertTrue(sr.getContentType(), sr.getContentType().startsWith("text/html"));
    }
}
